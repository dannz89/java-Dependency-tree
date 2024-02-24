package com.ddt.dependencyutils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.ddt.dependencyutils.exception.CircularDependencyException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;

/**
 * TODO: Re-think equals method such that a more detailed comparison can be made than only the data key.
 * At the moment, It's possible for two instances that share the same key to report equality. So if an object
 * with the same key value, even if it's not an identical object ref, will report equal and fail the circular
 * dependency check.
 *
 * @param <K>
 * @param <V>
 */

@JsonSerialize(using = DependencySerializer.class)
public class Dependency<K, V> {
    private final static Logger logger = LoggerFactory.getLogger(Dependency.class);
    @JsonProperty("dataKey")
    private K dataKey;
    @JsonProperty("data")
    private V data;
    @JsonProperty("dependencies")
    private Map<K, Dependency<K, V>> dependencies = null;
    @JsonProperty("dependants")
    private Map<K, Dependency<K, V>> dependants = null;
    @JsonProperty("finished")
    private boolean finished = false;
    @JsonIgnore
    private boolean isADependency = false;

    public DependencyForest getDependencyForest() {
        return dependencyForest;
    }

    @JsonIgnore
    private DependencyForest dependencyForest;

    @JsonIgnore
    private DependencyForest.SerializingScheme serializingScheme = DependencyForest.SerializingScheme.DEPENDANTS;
    private static boolean serializeDependencies = false;

    private static List<Dependency<?,?>> outermostLeafDependencies;
    private static List<Dependency<?,?>> dependenciesWithNoDependencies;

    public Dependency() {
    }
    @JsonCreator
    public Dependency(@JsonProperty("dataKey") K dataKey, @JsonProperty("data") V data) {
        this();
        this.dataKey = dataKey;
        this.data = data;
    }

    private void addDependant(Dependency<K,V> dependant){
        if (this.hasForest()) {
            dependant.setDependencyForest(getDependencyForest());
        }
        if(this.dependants==null){
            this.dependants = new ConcurrentHashMap<>();
        }
        this.dependants.put(dependant.getDataKey(),dependant);
    }


    public void addDependency(Dependency<K, V> dependency)
            throws CircularDependencyException, NullPointerException {
        if (dependency == null) return;

        /**
         * Order is crucial. Even if we have no dependencies, the new dependency may still have this Dependency
         * as an ancestor dependency, so we have to check first.
         */

        // Check first.
        validateNewDependency(this,dependency);

        // If we get here, we didn't throw a CircularReferenceException so the new dependency is valid.
        if (dependencies == null) {
            dependencies = new ConcurrentHashMap<K, Dependency<K, V>>();
        }

        // Save time if it's already been added.
        if (dependencies.containsKey(dependency.getDataKey())
                && dependencies.get(dependency.getDataKey()).equals(dependency)) return;

        dependencies.put(dependency.getDataKey(), dependency);

        dependency.setIsADependency(true);
        dependency.addDependant(this);

        // We now have dependencies.
        if (hasForest()) {
            dependency.setDependencyForest(dependencyForest);
            dependency.setSerializingScheme(dependencyForest.getSerializingScheme());
            dependencyForest.updateAllDependencies();
        }
    }

    private void setIsADependency(boolean isADependency){
        this.isADependency=isADependency;
    }

    public boolean isADependency() {
        return isADependency;
    }

    public void setSerializingScheme(DependencyForest.SerializingScheme serializingScheme) {
        this.serializingScheme = serializingScheme;
        if (hasDependencies()) {
            getDependencies().values().forEach(dep -> dep.setSerializingScheme(getSerializingScheme()));
        }
    }

    public void setFinished(boolean finished){
        this.finished = finished;
    }
    public boolean isFinished(){
        return this.finished;
    }

    /**
     * Returns true if dependency is an instance of Dependency and the contents of its dataKey and
     *
     * @param dependency
     * @return
     */
    @Override
    public boolean equals(Object dependency) {
        if(this == dependency) return true;
        if(dependency == null || getClass() != dependency.getClass()) return false;
        Dependency<?,?> that = (Dependency<?,?>)dependency;

        if (!Objects.equals(dataKey, that.dataKey)) return false;

        if ((finished != that.finished)) return false;

        return Objects.equals(data, that.data);
    }

    public DependencyForest.SerializingScheme getSerializingScheme() {
        return serializingScheme;
    }

    /**
     * Tests whether this Dependency has any parent or ancestor Dependency with dataKey.equals(key)
     *
     * @param dependant
     * @return
     */
    public boolean hasDependant(Dependency<K, V> dependant) {
        if (!hasDependants()) return false;

        if (getDependants().values().stream().anyMatch(val -> val.equals(dependant))) return true;

        for (Dependency _dependant : getDependants().values()) {
            if (dependant.hasDependant(_dependant)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if this Dependency has any parent or ancestor dependency with dataKey.equals(key).
     *
     * @param dependency
     * @return
     */
    public boolean hasDependency(Dependency<K, V> dependency) {
        if (!hasDependencies()) return false;

        if (getDependencies().values().stream().anyMatch(value -> value.equals(dependency))) return true;

        for (Dependency<K, V> _dependency : getDependencies().values()) {
            if (_dependency.hasDependency(dependency)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Traverses the dependency tree and returns the node with getDataKey().equals(key) else null if not found.
     *
     * @param dependency
     * @return
     */
    public Dependency<K, V> getDependency(Dependency<K, V> dependency) {
        if (!hasDependency(dependency)) return null;

        if (getDependencies().containsKey(dependency.getDataKey())) {
            return getDependencies().get(dependency.getDataKey());
        }

        for (Dependency _dependency : getDependencies().values()) {
            return dependency.getDependency(_dependency);
        }

        //Should never get here.
        return null;
    }

    /**
     * Traverses the dependants tree of this Dependency and returns the node with getDataKey().equals(key) else
     * null if not found.
     *
     * @param dependant
     * @return
     */
    public Dependency<K, V> getDependant(Dependency<K, V> dependant) {
        if (!hasDependant(dependant)) return null;

        if (getDependants().containsKey(dependant.getDataKey())) {
            return getDependants().get(getDataKey());
        }

        for (Dependency _dependant : getDependants().values()) {
            return dependant.getDependant(_dependant);
        }

        //Should never get here.
        return null;
    }

    /**
     * Removes a dependant from the tree, setting all its dependencies
     *
     * @param dependency The Dependency to remove.
     */
    public void removeDependency(Dependency<K, V> dependency) {
        // If the Dependency doesn't exist in this tree, do nothing (nothing to remove).
        if (!hasDependency(dependency)) return;

        // This is not nonsensical. If K,V of this Dependency were String,String and the caller did this:
        // Dependency<String,String> dep = new Dependency("KeyValue","ValueValue"); Their Dependency object value
        // may exactly match one of ours but have the same Object ref. getDependency() returns the correct
        // Object ref for us to remove.
        Dependency<K, V> dependencyToRemove = getDependency(dependency);

        // Get the victim's parents and children and set the parents of all the children to all the parents.
        // Note that sometimes, this will result in a CircularDependencyException because the Dependency
        // being removed is not the only parent of its child Dependencies. These in turn may share a common
        // ancestor with the Dependency being removed in which case, they cannot be re-added as a Dependency.
        // But it's also ok NOT to add these Dependencies as parents of the children because they will not
        // be orphaned from the tree if this is the case. This will result in only orphaned children being
        // grafted back on to the tree with the removed Dependency's parents.
        // If we are going to remove dependency 3 (dataKey.equals("3"), this looks like this:
        //
        // Before:
        //
        // Z-
        // C-
        // A+
        // B+
        // |-1+
        // |  |-2+
        // |     |---e+
        // |          ||
        // |          ||
        // |          ||
        // |---------3+ (dependencies A,B)
        //            ||-f+ (Dependencies e,3)
        //            ||-g+ (Dependencies e,3)
        //                ||-h (Dependencies f,g)
        //            |-qq (Dependencies 3)
        //
        // Delete 3
        //
        // Z-
        // C-
        // A+
        // B+
        //  |-1+
        //  |  |-2+
        //  |     |---e+
        //  |          |
        //  |          |
        //  |          |
        //  |          |-f+ (Dependencies: e)
        //  |          |-g+ (Dependencies: e)
        //  |             |-h (Dependencies: f,g)
        //  |-qq (Dependencies: A,B)


        // Children / dependants - objects that are dependANT on 'me', that I'm a dependency OF.
        Map<K, Dependency<K, V>> children = dependencyToRemove.getDependants();
        if (children != null && !children.isEmpty()) {
            children.values().forEach(child -> logger.info("child: {}", child.getDataKey()));
        }

        // Parents / dependencies - objects <b>I</b> depend on.
        Map<K, Dependency<K, V>> parents = dependencyToRemove.getDependencies();

        // Now we have the parents and children stored, we can rip the node from the tree.
        removeDependency(getDependencies(), dependencyToRemove);

        // Now the node has been removed set its parents to be the new parents of all its former children.
        if (children != null && !children.isEmpty()) {
            // Always remove the dependency-to-remove from its children's lists of dependencies (parents).
            children.values().forEach(child -> child.getDependencies().remove(dependencyToRemove.getDataKey()));

            if (parents != null && !parents.isEmpty()) {
                // Remove the dependency-to-remove from its parent dependencies dependants arrays.
                parents.values().forEach(parent -> parent.getDependants().remove(dependencyToRemove.getDataKey()));

                // For each child, add all the parents that were previously the parents of the Dependency
                // that was removed.
                children.values().forEach(child -> parents.values().forEach(parent -> {
                    try {
                        logger.debug("Adding dependency {} to child {}", parent.getDataKey(), child.getDataKey());
                        Map<K, Dependency<K, V>> parentRootNodes = parent.getRootNodes();
                        Map<K, Dependency<K, V>> childRootNodes = child.getRootNodes();

                        // addDependency() will also take care of setting the parent as a dependant of the given child.
                        child.addDependency(parent);
                    } catch (CircularDependencyException ce) {
                        logger.debug("child dependency key {} and parent dependency key {} share common ancestor dependency. Not adding to avoid circular reference.", child.getDataKey(), parent.getDataKey());
                    }
                }));
            }
        }
    }

    /**
     * Recursively searches the Dependency tree, finds the dependency to remove and removes it from its array.
     *
     * @param dependencies       List of Dependency objects to search for removal of dependency.
     * @param dependencyToRemove The Dependency object to remove.
     */
    private void removeDependency(@NonNull Map<K, Dependency<K, V>> dependencies, @NonNull Dependency<K, V> dependencyToRemove) {
        // If this list contains the one we want to remove then remove it and return.
        if (dependencies.containsValue(dependencyToRemove)) {
            dependencies.remove(dependencyToRemove.getDataKey());
            return;
        }

        // Recurse further to keep looking for the object.
        dependencies.values().forEach(dep -> {
            if (dep.hasDependencies()) {
                removeDependency(dep.getDependencies(), dependencyToRemove);
            }
        });
    }

    /**
     * Convenience method to determine whether this Dependency has no dependencies and is therefore a root node.
     *
     * @return true if there are no dependencies else false.
     */
    public boolean isRootNode() {
        return !hasDependencies();
    }

    /**
     * Convenience method to determine whether this Dependency has no dependants and is therefore a leaf node.
     *
     * @return
     */
    public boolean isLeafNode() {
        return !hasDependants();
    }

    /**
     * Builds a ConcurrentHashMap<K,Dependency<K,V>> of all ancestor nodes of this Dependency which themselves
     * have no dependencies or in other words, are root nodes. Remember that one Dependency leaf node or branch
     * node can have multiple root nodes. This method acts as an initializer for the private method:
     * private Map<K,Dependency<K,V>> getRootNode(Map<K,Dependency<K,V>> rootNodes) by passing a null value
     * to that method so that it knows it is the first call.
     *
     * @return a Map containing all root Dependency nodes.
     */
    public Map<K, Dependency<K, V>> getRootNodes() {
        return getRootNodes(null);
    }

    /**
     * Builds a ConcurrentHashMap<K,Dependency<K,V>> of all ancestor nodes of this Dependency which themselves
     * have no dependencies or in other words, are root nodes. Remember that one Dependency leaf node or branch
     * node can have multiple root nodes. If param is null, a new ConcurrentHashMap is instantiated.
     *
     * @param rootNodes Map in which to store root nodes.
     * @return a Map containing all root nodes of this Dependency.
     */
    private Map<K, Dependency<K, V>> getRootNodes(Map<K, Dependency<K, V>> rootNodes) {
        Map<K, Dependency<K, V>> roots = (rootNodes != null) ? rootNodes : new ConcurrentHashMap<>();

        // If we are the root node then we must be the only root node. Really this is just a way if calling
        // hasDependencies().
        if (isRootNode()) {
            roots.put(getDataKey(), this);
            return roots;
        }

        // If we are here, hasDependencies() is true and we aren't a root node. So we recurse UP the
        // dependency tree (from leaves to roots), until we find all the root nodes.
        for (Dependency<K, V> dependency : getDependencies().values()) {
            dependency.getRootNodes(roots);
        }

        return roots;
    }

    public boolean hasForest() {
        return dependencyForest != null;
    }

    public void setDependencyForest(DependencyForest<K, V> dependencyForest) {
        if (this.dependencyForest == dependencyForest || dependencyForest == null) return;

        this.dependencyForest = dependencyForest;
        if (this.hasDependencies()) {
            this.getDependencies().values().forEach(dep -> dep.setDependencyForest(dependencyForest));
        }
        if (this.hasDependants()) {
            this.getDependants().values().forEach(dep -> dep.setDependencyForest(dependencyForest));
        }
    }

    public boolean hasDependencies() {
        return this.getDependencies()!=null && this.getDependencies().size()>0;
    }
    public boolean hasDependants() { return this.getDependants()!=null && this.getDependants().size() > 0;
    }

    public int size() {
        int[] size = {1};

        if (serializingScheme == DependencyForest.SerializingScheme.DEPENDANTS && getDependants() != null) {
            getDependants().forEach((dependantKey, dependantValue) -> size[0] += dependantValue.size());
        }

        if (serializingScheme == DependencyForest.SerializingScheme.DEPENDENCIES && getDependencies() != null) {
            getDependencies().forEach((dependencyKey, dependencyValue) -> size[0] += dependencyValue.size());
        }

        return size[0];
    }

    @Override
    public String toString() {
        return this.toJson();
    }

    public String dependantTreeToString(){
        return dependantTreeToString(0,this);
    }

    private String dependantTreeToString(int recursionLevel, Dependency<K,V> dependency){
        StringBuffer sb = null;
        if(recursionLevel==0){
            String title = "===> ["
                    + dependency.getDataKey()
                    + " dependants=("
                    + (dependency.getDependants()==null? 0 : dependency.getDependants().size())
                    + ")] <===";
            sb = new StringBuffer(title);
        } else {
            sb = new StringBuffer("-".repeat(recursionLevel)+ dependency.toString()
                    + "("
                    + (dependency.getDependants() == null ? 0 : dependency.getDependants().size())
                    + ")");
        }
        if(!dependency.hasDependants()) return sb.toString() + "\n"+"-".repeat(recursionLevel)+"<< NO DEPENDANTS >>\n";

        sb.append("\n");

        for(K dependantKey : dependency.getDependants().keySet()){
            sb.append(dependantTreeToString(recursionLevel+1, dependency.getDependants().get(dependantKey)));
        }

        return sb.toString();
    }

    /**
     * Converts this object to JSON.
     *
     * @return
     */
    public String toJson(){
        try {
            DependencySerializer serializer = new DependencySerializer();
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Dependency.class,serializer);
            objectMapper.registerModule(module);
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     *
     * @param Json
     * @return
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public static Collection<Dependency> fromJson(String Json)
            throws JsonProcessingException, JsonMappingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Collection.class, new DependencyDeserializer());
        objectMapper.registerModule(module);
        return objectMapper.readValue(Json, Collection.class);
    }


    public String treeToString(){
        return treeToString(0,this);
    }

    private String treeToString(int recursionLevel, Dependency<K, V> dependency) {
        StringBuilder sb = null;

        switch (getSerializingScheme()) {
            case DEPENDENCIES -> {
                if (recursionLevel == 0) {
                    String title = "===> ["
                            + dependency.getDataKey()
                            + " dependencies=("
                            + (dependency.getDependencies() == null ? 0 : dependency.getDependencies().size())
                            + ")] <===";
                    sb = new StringBuilder(title);
                } else {
                    sb = new StringBuilder("-".repeat(recursionLevel) + dependency.toString()
                            + "("
                            + (dependency.getDependencies() == null ? 0 : dependency.getDependencies().size())
                            + ")");
                }

                if (!dependency.hasDependencies())
                    return sb.toString() + "\n" + "-".repeat(recursionLevel) + "<< NO DEPENDENCIES >>\n";

                sb.append("\n");

                for (K dependencyKey : dependency.getDependencies().keySet()) {
                    sb.append(treeToString(recursionLevel + 1, dependency.getDependencies().get(dependencyKey)));
                }

            }
            case DEPENDANTS -> {
                if (recursionLevel == 0) {
                    String title = "===> ["
                            + dependency.getDataKey()
                            + " dependencies=("
                            + (dependency.getDependants() == null ? 0 : dependency.getDependants().size())
                            + ")] <===";
                    sb = new StringBuilder(title);
                } else {
                    sb = new StringBuilder("-".repeat(recursionLevel) + dependency.toString()
                            + "("
                            + (dependency.getDependants() == null ? 0 : dependency.getDependants().size())
                            + ")");
                }

                if (!dependency.hasDependants())
                    return sb.toString() + "\n" + "-".repeat(recursionLevel) + "<< NO DEPENDANTS >>\n";

                sb.append("\n");

                for (K dependencyKey : dependency.getDependants().keySet()) {
                    sb.append(treeToString(recursionLevel + 1, dependency.getDependants().get(dependencyKey)));
                }
            }
        }

        return sb.toString();
    }

    public K getDataKey(){
        return this.dataKey;
    }
    public V getData() { return this.data; }

    public Map<K, Dependency<K, V>> getDependencies() {
        return this.dependencies;
    }
    public Map<K, Dependency<K,V>> getDependants(){ return this.dependants; }

    /**
     * @param dependant
     * @param newDependency
     * @throws CircularDependencyException
     */
    private void validateNewDependency(Dependency<K, V> dependant, Dependency<K, V> newDependency)
            throws CircularDependencyException {
        validateNewDependency(dependant, newDependency, true);
    }


    /**
     * This checks for circular dependencies from the node being added downwards. But it serves as an overall
     * check for the entire tree from head to outermost leaf because it gets called every time a child is
     * added.
     * @param newDependency
     * @throws CircularDependencyException
     */
    private void validateNewDependency(
            Dependency<K, V> dependantDependency,
            Dependency<K, V> newDependency,
            boolean first)
    throws CircularDependencyException {
        // Trying to add itself as a dependency. Circular reference.
        if (newDependency == dependantDependency) throw new CircularDependencyException(newDependency);

        // Stop right there. We are trying to add a Dependency to ourselves which already has ourselves as a dependency.
        if (first
                && newDependency.hasDependencies()
                && newDependency.hasDependency(dependantDependency))
            throw new CircularDependencyException(newDependency);

        // It's ok to re-add a dependency, although it will overwrite the original.
        if (first && dependantDependency.hasDependencies()
                && dependantDependency.getDependencies().values().stream().anyMatch(dep -> dep.equals(newDependency)))
            return;

        // If we get here then it's definitely a circular dependency because the new dependency is an ancestor dependency.
        if (dependantDependency.hasDependency(newDependency)) throw new CircularDependencyException(newDependency);
    }
}


