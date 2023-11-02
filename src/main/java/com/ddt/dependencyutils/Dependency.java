package com.ddt.dependencyutils;

import java.util.*;

import com.ddt.dependencyutils.exception.CircularDependencyException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.core.annotation.Order;

@JsonSerialize(using = DependencySerializer.class)
public class Dependency<K, V> {
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
        if(dataKey == null) {
            throw new NullPointerException("Dependency key cannot be null");
        }
        this.dataKey = dataKey;
        this.data = data;
    }

    private void addDependant(Dependency<K,V> dependant){
        if(this.dependants==null){
            this.dependants = new HashMap<>();
        }
        this.dependants.put(dependant.getDataKey(),dependant);
    }


    public void addDependency(Dependency<K, V> dependency)
            throws CircularDependencyException, NullPointerException {
        if (dependency == null) return;

        /**
         * Order is crucial. Even if we have no dependencies, the new dependency may still have this Dependency
         * as an ancestor dependency so we have to check first.
         */

        // Check first.
        validateNewDependency(this,dependency);

        // If we get here, we didn't throw a CircularReferenceException so the new dependency is valid.
        if (dependencies == null) {
            dependencies = new HashMap<K, Dependency<K, V>>();
        }

        // Save time if it's already been added.
        if (dependencies.containsKey(dependency.getDataKey())
                && dependencies.get(dependency.getDataKey()).equals(dependency)) return;

        dependencies.put(dependency.getDataKey(), dependency);

        dependency.setIsADependency(true);
        dependency.addDependant(this);
        // We now have dependencies.
        if (hasForest()) {
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
    }

    public void setFinished(boolean finished){
        this.finished = finished;
    }
    public boolean isFinished(){
        return this.finished;
    }

    @Override
    public boolean equals(Object dependency) {
        if(this == dependency) return true;
        if(dependency == null || getClass() != dependency.getClass()) return false;
        Dependency<?,?> that = (Dependency<?,?>)dependency;
        return dataKey != null ? dataKey.equals(that.dataKey) : that.dataKey == null;
    }

    public DependencyForest.SerializingScheme getSerializingScheme() {
        return serializingScheme;
    }

    /**
     * Tests whether this Dependency has any parent or ancestor Dependency with dataKey.equals(key)
     *
     * @param key
     * @return
     */
    public boolean hasDependant(K key) {
        if (!hasDependants()) return false;

        if (getDependants().containsKey(key)) return true;

        for (Dependency dependant : getDependants().values()) {
            if (dependant.hasDependant(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if this Dependency has any parent or ancestor dependency with dataKey.equals(key).
     *
     * @param key
     * @return
     */
    public boolean hasDependency(K key) {
        if (!hasDependencies()) return false;

        if (getDependencies().containsKey(key)) return true;

        for (Dependency dependency : getDependencies().values()) {
            if (dependency.hasDependency(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses the dependency tree and returns the node with getDataKey().equals(key) else null if not found.
     *
     * @param key
     * @return
     */
    public Dependency<K, V> getDependency(K key) {
        if (!hasDependency(key)) return null;

        if (getDependencies().containsKey(key)) {
            return getDependencies().get(key);
        }

        for (Dependency dependency : getDependencies().values()) {
            return dependency.getDependency(key);
        }

        //Should never get here.
        return null;
    }

    /**
     * Traverses the dependants tree of this Dependency and returns the node with getDataKey().equals(key) else
     * null if not found.
     *
     * @param key
     * @return
     */
    public Dependency<K, V> getDependant(K key) {
        if (!hasDependant(key)) return null;

        if (getDependants().containsKey(key)) {
            return getDependants().get(key);
        }

        for (Dependency dependant : getDependants().values()) {
            return dependant.getDependant(key);
        }

        //Should never get here.
        return null;
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
        return this.dataKey.toString();
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

    private String treeToString(int recursionLevel, Dependency<K, V> dependency){
        StringBuffer sb = null;

        if(recursionLevel==0){
            String title = "===> ["
                    + dependency.getDataKey()
                    + " dependencies=("
                    + (dependency.getDependencies()==null? 0 : dependency.getDependencies().size())
                    + ")] <===";
            sb = new StringBuffer(title);
        } else {
            sb = new StringBuffer("-".repeat(recursionLevel)+ dependency.toString()
                    + "("
                    + (dependency.getDependencies() == null ? 0 : dependency.getDependencies().size())
                    + ")");
        }

        if(!dependency.hasDependencies()) return sb.toString() + "\n"+"-".repeat(recursionLevel)+"<< NO DEPENDENCIES >>\n";

        sb.append("\n");

        for(K dependencyKey : dependency.getDependencies().keySet()){
            sb.append(treeToString(recursionLevel+1, dependency.getDependencies().get(dependencyKey)));
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
     * This checks for circular dependencies from the node being added downwards. But it serves as an overall
     * check for the entire tree from head to outermost leaf because it gets called every time a child is
     * added.
     * @param newDependency
     * @throws CircularDependencyException
     */
    private void validateNewDependency(
            Dependency<K, V> dependantDependency,
            Dependency<K, V> newDependency)
            throws CircularDependencyException {
        // Trying to add itself as a dependency. Circular reference.
        if (newDependency == dependantDependency) throw new CircularDependencyException(newDependency);

        // No further dependency hierarchy to check.
        if(!newDependency.hasDependencies()) return;

        for(K _dependencyKey : newDependency.getDependencies().keySet()) {
            validateNewDependency(dependantDependency,newDependency.getDependencies().get(_dependencyKey));
        }
    }
}


