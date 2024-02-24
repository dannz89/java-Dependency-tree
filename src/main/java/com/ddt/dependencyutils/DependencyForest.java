package com.ddt.dependencyutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dependency-aware container class for root nodes of multiple dependency trees.
 *
 * @param <K>
 * @param <V>
 */
public class DependencyForest<K, V> {
    private final static Logger logger = LoggerFactory.getLogger(DependencyForest.class);
    private List<Dependency<K, V>> outermostLeafDependencies;
    private List<Dependency<K, V>> dependenciesWithNoDependencies;

    private Map<K, Dependency<K, V>> allNodes;

    private String name;

    public enum SerializingScheme {DEPENDENCIES, DEPENDANTS}

    ;
    private SerializingScheme serializingScheme = SerializingScheme.DEPENDANTS;

    /**
     *
     */
    public DependencyForest() {
        outermostLeafDependencies = new CopyOnWriteArrayList<>();
        dependenciesWithNoDependencies = new CopyOnWriteArrayList<>();
        allNodes = new ConcurrentHashMap<>();
    }

    /**
     * @param serializingScheme
     */
    public DependencyForest(SerializingScheme serializingScheme) {
        this();
        setSerializingScheme(serializingScheme);
    }

    /**
     * @param serializingScheme
     */
    public void setSerializingScheme(SerializingScheme serializingScheme) {
        this.serializingScheme = serializingScheme;
        allNodes.values().forEach(v -> {
            v.setSerializingScheme(getSerializingScheme());
        });
    }

    /**
     * @return
     */
    public SerializingScheme getSerializingScheme() {
        return this.serializingScheme;
    }

    /**
     * Returns the number of Dependency objects stored in this DependencyForest.
     * @return
     */
    public int size() {
        return allNodes.size();
    }

    /**
     * Setes a user-friendly label for this DependencyForest.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the user-friendly name of this DependencyForest
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * If this DependencyForest contains a dependency with dataKey.equals(key). Not this is not the same as
     * the get method which returns the reference to the Dependency Object exactly matching the one supplied
     * in the method parameter.
     * @param key
     * @return
     */
    public boolean containsKey(K key) {
        return (allNodes != null && allNodes.containsKey(key));
    }

    /**
     * Retrieves the Dependency with the key matching the value of the key param.
     * @param key
     * @return Dependency or null
     */
    public Dependency<K, V> get(K key) {
        if (allNodes != null && containsKey(key)) {
            return allNodes.get(key);
        }
        return null;
    }

    /**
     * Returns a List<Dependency<K,V>> of the roots or leaf Dependencies according to getSerializingScheme().
     *
     * @return
     */
    public List<Dependency<K, V>> getAllTrees() {
        return (getSerializingScheme() == SerializingScheme.DEPENDANTS) ? getDependenciesWithNoDependencies() : this.getOutermostLeafDependencies();
    }

    /**
     * Convenience friendly-named method to get all Dependencies with no Dependencies.
     *
     * @return
     */
    public List<Dependency<K, V>> getRootNodes() {
        return getDependenciesWithNoDependencies();
    }

    /**
     * Adds a new dependency to the forest.
     * @param dependency
     */
    public void addDependency(Dependency<K, V> dependency) {
        if (allNodes.values().stream().anyMatch(val -> val.equals(dependency))) return;

        dependency.setDependencyForest(this);
        dependency.setSerializingScheme(getSerializingScheme());
        allNodes.put(dependency.getDataKey(), dependency);
        updateAllDependencies();


        //Add all nodes found in both the dependencies and dependants directions. This may mean surplus calls
        //to add are made if nodes being added share common dependencies / dependants but see above.
        if (dependency.hasDependants()) dependency.getDependants().values().forEach(this::addDependency);
        if (dependency.hasDependencies()) dependency.getDependencies().values().forEach(this::addDependency);
    }

    /**
     * This method maintains the lists of root and outermost leaf dependencies, checking all dependencies
     * to see whether they belong in either list, removing them if they no longer do and adding them if they
     * do.
     * @param dependency
     */
    public void updateDependency(Dependency<K, V> dependency) {
        if (!containsKey(dependency.getDataKey())) return;

        // There is probably a much nicer way of doing all this too rather than just forcing it.
        if (dependency.hasDependencies()) dependenciesWithNoDependencies.remove(dependency);

        if (!dependency.hasDependencies() && !dependenciesWithNoDependencies.contains(dependency))
            dependenciesWithNoDependencies.add(dependency);

        if (dependency.hasDependants()) outermostLeafDependencies.remove(dependency);

        if (!dependency.hasDependants() && !outermostLeafDependencies.contains(dependency))
            outermostLeafDependencies.add(dependency);

        dependency.setSerializingScheme(getSerializingScheme());
    }

    public void updateAllDependencies() {
        allNodes.values().forEach(this::updateDependency);
    }

    public List<Dependency<K, V>> getOutermostLeafDependencies() {
        return outermostLeafDependencies;
    }

    public List<Dependency<K, V>> getDependenciesWithNoDependencies() {
        return dependenciesWithNoDependencies;
    }

    /**
     * Deletes all Dependency trees, effectively emptying the forest.
     */
    public void clear() {
        allNodes.clear();
        dependenciesWithNoDependencies.clear();
        outermostLeafDependencies.clear();
    }

    /**
     * Converts this DependencyForest to a JSON array of JSON Dependency trees for all nodes currently held.
     * @return
     */
    public String toJson() {
        StringBuffer sb = new StringBuffer();
        if (getSerializingScheme() == SerializingScheme.DEPENDANTS) {
            sb.append("[");
            boolean[] first = {true};
            dependenciesWithNoDependencies.forEach(dep -> {
                sb.append((!first[0]) ? "," : "").append(dep.toJson());
                if (first[0]) first[0] = false;
            });

            sb.append("]");
        }

        if (getSerializingScheme() == SerializingScheme.DEPENDENCIES) {
            sb.append("[");
            boolean[] first = {true};
            outermostLeafDependencies.forEach(dep -> {
                sb.append((!first[0]) ? "," : "").append(dep.toJson());
                if (first[0]) first[0] = false;
            });

            sb.append("]");
        }
        return sb.toString();
    }

    public Map<K, Dependency<K, V>> getAllNodes() {
        return allNodes;
    }

    public Dependency<K, V> get(Dependency<K, V> _candiate) {
        return hasDependency(_candiate) ? getDependency(_candiate) : null;
    }

    /**
     * Retrieves the reference to the Dependency object with identical values (per Dependency.equals) to
     * the dependency param.
     *
     * @param dependency
     * @return
     */
    public Dependency<K, V> getDependency(Dependency<K,V> dependency) {
        if (!hasDependency(dependency)) return null;
        return allNodes.get(dependency.getDataKey());
    }

    /**
     * Checks to see if a dependency with identical values exists in this DependencyForest.
     *
     * @param dependency
     * @return true if dependency exists else false
     */
    public boolean hasDependency(Dependency<K,V> dependency) {
        return allNodes.values().stream().anyMatch(val -> val.equals(dependency));
    }

    /**
     * Generates a string represendint the tree. More used for debugging purposes than anything else.
     *
     * @return a list of all root or leaf nodes according to getSerializingScheme().
     */
    public ArrayList<String> allTreesToStrings() {
        ArrayList<String> trees = new ArrayList<>();

        switch (getSerializingScheme()) {
            case DEPENDANTS -> {
                if (dependenciesWithNoDependencies == null || dependenciesWithNoDependencies.isEmpty()) break;
                for (Dependency<K, V> dependency : dependenciesWithNoDependencies) {
                    trees.add(dependency.dependantTreeToString());
                }
            }
            case DEPENDENCIES -> {
                if (outermostLeafDependencies == null || outermostLeafDependencies.isEmpty()) break;
                for (Dependency<K, V> dependency : outermostLeafDependencies) {
                    trees.add(dependency.treeToString());
                }
            }
        }

        return trees;
    }
}
