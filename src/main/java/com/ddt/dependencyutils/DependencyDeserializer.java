package com.ddt.dependencyutils;

import com.ddt.dependencyutils.exception.CircularDependencyException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DependencyDeserializer extends StdDeserializer<Collection<Dependency>> {
    private DependencyForest.SerializingScheme serializingScheme = DependencyForest.SerializingScheme.DEPENDANTS;
    public DependencyDeserializer() {
        super(Collection.class);
    }

    public void setSerializingScheme(DependencyForest.SerializingScheme serializingScheme) {
        this.serializingScheme = serializingScheme;
    }

    public DependencyForest.SerializingScheme getSerializingScheme() {
        return serializingScheme;
    }

    @Override
    public Collection<Dependency> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Collection<Dependency> dependencies = new ArrayList<>();

        try {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            if (rootNode.isArray()) {
                for (JsonNode dependencyTree : rootNode) {
                    dependencies.add(parseSingleTree(jp, ctxt, dependencyTree));
                }
            } else {
                dependencies.add(parseSingleTree(jp, ctxt, rootNode));
            }

        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
        }
        return dependencies;
    }

    private Dependency parseSingleTree(JsonParser jp, DeserializationContext ctxt, JsonNode node)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String dataKey = node.get("dataKey").asText();
        String data = node.get("data").asText();
        boolean finished = node.get("finished").asBoolean();

        // Create a new Dependency object using the constructor
        Dependency dependency = new Dependency(dataKey, data);
        dependency.setFinished(finished);

        JsonNode dependenciesNode = node.get("dependencies");

        if (dependenciesNode != null) {
            // Deserialize and add dependencies
            for (JsonNode dependencyNode : dependenciesNode) {
                Dependency _dependency = objectMapper.treeToValue(dependencyNode, Dependency.class);
                try {
                    dependency.addDependency(_dependency);
                } catch (CircularDependencyException e) {
                    // Handle circular dependency exception
                    // You can decide how to handle validation errors
                    ctxt.reportInputMismatch(Dependency.class, "Circular reference exception adding [" + dependency.getDataKey() + "]");
                    e.printStackTrace();
                }
            }
        }
        return dependency;
    }
}
