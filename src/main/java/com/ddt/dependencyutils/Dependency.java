package com.ddt.dependencyutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ddt.dependencyutils.exception.CircularDependencyException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

@JsonSerialize(using = DependencySerializer.class)
public class Dependency<K, V> {
    private Dependency listHead = null;

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

    private static boolean serializeDependencies = false;
    private static List<Dependency<?,?>> outermostLeafDependencies;
    private static List<Dependency<?,?>> dependenciesWithNoDependencies;

    public Dependency(){
        if(Dependency.outermostLeafDependencies ==null) {
            Dependency.outermostLeafDependencies = new ArrayList<>();
        }
        if(Dependency.dependenciesWithNoDependencies ==null) {
            Dependency.dependenciesWithNoDependencies = new ArrayList<>();
        }
        Dependency.outermostLeafDependencies.add(this);
        Dependency.dependenciesWithNoDependencies.add(this);
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
        if(dependency == null) {
            throw new NullPointerException("Dependency cannot be null");
        }

        validateNewDependency(this,dependency);

        // If we get here, we didn't throw a CircularReferenceException so the new dependency is valid.
        if(this.dependencies ==null){
            this.dependencies = new HashMap<K, Dependency<K, V>>();
        }

        // Save time validating.
        if(this.dependencies.containsKey(dependency.getDataKey())
                && this.dependencies.get(dependency.getDataKey()).equals(dependency)) return;

        this.dependencies.put(dependency.getDataKey(),dependency);
        dependency.setIsADependency(true);
        // We now have dependencies.
        if(Dependency.dependenciesWithNoDependencies.contains(this)){
            Dependency.dependenciesWithNoDependencies.remove((this));
        }
        dependency.addDependant(this);
    }

    private void setIsADependency(boolean isADependency){
        this.isADependency=isADependency;

        if(isADependency && Dependency.outermostLeafDependencies.contains(this)){
            Dependency.outermostLeafDependencies.remove(this);
        }
    }

    public static boolean getSerializeDependencies(){return Dependency.serializeDependencies;}

    public static void setSerializeDependencies(boolean serializeDependencies) {
        Dependency.serializeDependencies = serializeDependencies;}

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

    public boolean hasDependencies() {
        return this.getDependencies()!=null && this.getDependencies().size()>0;
    }
    public boolean hasDependants() { return this.getDependants()!=null && this.getDependants().size()>0;}

    @Override
    public String toString() {
        return this.dataKey.toString();
    }

    /**
     * Generates a string represendint the tree.
     * @return
     */
    public static ArrayList<String>allTreesToStrings(){
        if(Dependency.outermostLeafDependencies ==null || Dependency.outermostLeafDependencies.size()==0) return null;

        ArrayList<String> trees = new ArrayList();
        for(Dependency<?,?> dependency : Dependency.outermostLeafDependencies) {
            trees.add(dependency.treeToString());
        }
        return trees;
    }

    public static ArrayList<String>allDependantTreesToStrings(){
        if(Dependency.dependenciesWithNoDependencies ==null|| Dependency.dependenciesWithNoDependencies.size()==0) return null;
        ArrayList<String> trees = new ArrayList();
        for(Dependency<?,?> dependency : Dependency.dependenciesWithNoDependencies) {
            trees.add(dependency.dependantTreeToString());
        }
        return trees;
    }

    public static void clearDependencyTrees(){
        if(dependenciesWithNoDependencies !=null ) dependenciesWithNoDependencies.clear();
        if(outermostLeafDependencies !=null ) outermostLeafDependencies.clear();
    }

    public static List<Dependency<?,?>> getOutermostLeafDependencies() {
        return Dependency.outermostLeafDependencies;
    }
    public static List<Dependency<?,?>> getDependenciesWithNoDependencies() { return Dependency.dependenciesWithNoDependencies; }

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

    public static Dependency fromJson(String Json)
            throws JsonProcessingException, JsonMappingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Dependency.class, new DependencyDeserializer());
        objectMapper.registerModule(module);
        return objectMapper.readValue(Json, Dependency.class);
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


