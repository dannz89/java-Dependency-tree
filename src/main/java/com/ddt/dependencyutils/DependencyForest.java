package com.ddt.dependencyutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-aware container class for root nodes of multiple dependency trees.
 *
 * @param <K>
 * @param <V>
 */
public class DependencyForest<K, V> {
    private final static Logger logger = LoggerFactory.getLogger(DependencyForest.class);
    private boolean serializeDependencies = false;
    private List<Dependency<K, V>> outermostLeafDependencies;
    private List<Dependency<K, V>> dependenciesWithNoDependencies;

    private Map<K, Dependency<K, V>> allNodes;

    public enum SerializingScheme {DEPENDENCIES, DEPENDANTS}

    ;
    private SerializingScheme serializingScheme = SerializingScheme.DEPENDANTS;

    /**
     *
     */
    public DependencyForest() {
        outermostLeafDependencies = new ArrayList<>();
        dependenciesWithNoDependencies = new ArrayList<>();
        allNodes = new HashMap<>();
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
        allNodes.forEach((k, v) -> v.setSerializingScheme(this.serializingScheme));
    }

    /**
     * @return
     */
    public SerializingScheme getSerializingScheme() {
        return this.serializingScheme;
    }

    /**
     * @return
     */
    public int size() {
        return allNodes.size();
    }


    public boolean containsKey(K key) {
        return (allNodes != null && allNodes.containsKey(key));
    }

    public Dependency<K, V> get(K key) {
        if (allNodes != null && containsKey(key)) {
            return allNodes.get(key);
        }
        return null;
    }

    public Map<K, Dependency<K, V>> getAllTrees() {
        return allNodes;
    }

    public void addDependency(Dependency<K, V> dependency) {
        /**
         * This is likely to happen a lot so try to make it quicker.
         */
        if (allNodes != null
                && allNodes.containsValue(dependency)) {
            logger.info("Dependency [" + dependency + "] already added.");
            return;
        }

        logger.info("Adding dependency: [" + dependency + "]");

        dependency.setDependencyForest(this);
        dependency.setSerializingScheme(getSerializingScheme());
        allNodes.put(dependency.getDataKey(), dependency);
        updateAllDependencies();

        /**
         *  Add all nodes found in both the dependencies and dependants directions. This may mean surplus calls
         *  to add are made if nodes being added share common dependencies / dependants but see above.
         */
        if (dependency.hasDependants()) dependency.getDependants().values().forEach(dep -> addDependency(dep));
        if (dependency.hasDependencies()) dependency.getDependencies().values().forEach(dep -> addDependency(dep));
    }

    public void updateDependency(Dependency<K, V> dependency) {
        if (!containsKey(dependency.getDataKey())) return;

        // There is probably a much nicer way of doing all this too rather than just forcing it.
        if (dependency.hasDependencies() && dependenciesWithNoDependencies.contains(dependency))
            dependenciesWithNoDependencies.remove(dependency);
        if (!dependency.hasDependencies() && !dependenciesWithNoDependencies.contains(dependency))
            dependenciesWithNoDependencies.add(dependency);

        if (dependency.hasDependants() && outermostLeafDependencies.contains(dependency))
            outermostLeafDependencies.remove(dependency);
        if (!dependency.hasDependants() && !outermostLeafDependencies.contains(dependency))
            outermostLeafDependencies.add(dependency);
    }

    public void updateAllDependencies() {
        allNodes.values().forEach(dep -> updateDependency(dep));
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

    public String toJson() {
        StringBuffer sb = new StringBuffer();
        if (getSerializingScheme() == SerializingScheme.DEPENDANTS) {
            dependenciesWithNoDependencies.forEach(dep -> sb.append(dep.toJson()));
        }
        if (getSerializingScheme() == SerializingScheme.DEPENDENCIES) {
            outermostLeafDependencies.forEach(dep -> sb.append(dep.toJson()));
        }
        return sb.toString();
    }

    /**
     * Generates a string represendint the tree.
     *
     * @return
     */
    public ArrayList<String> allTreesToStrings() {
        ArrayList<String> trees = new ArrayList();

        switch (getSerializingScheme()) {
            case DEPENDANTS -> {
                if (dependenciesWithNoDependencies == null || dependenciesWithNoDependencies.size() == 0) break;
                for (Dependency<K, V> dependency : dependenciesWithNoDependencies) {
                    trees.add(dependency.dependantTreeToString());
                }
            }
            case DEPENDENCIES -> {
                if (outermostLeafDependencies == null || outermostLeafDependencies.size() == 0) break;
                for (Dependency<K, V> dependency : outermostLeafDependencies) {
                    trees.add(dependency.treeToString());
                }
            }
        }

        return trees;
    }
}
