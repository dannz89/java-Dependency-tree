package com.ddt.dependencyutils;

import com.ddt.dependencyutils.exception.CircularDependencyException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class DependencyDeserializer extends StdDeserializer<Dependency> {
    public DependencyDeserializer() {
        super(Dependency.class);
    }

    @Override
    public Dependency deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = jp.getCodec().readTree(jp);
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
                Dependency _dependency = objectMapper.treeToValue(dependencyNode,Dependency.class);
                try {
                    dependency.addDependency(_dependency);
                } catch (CircularDependencyException e) {
                    // Handle circular dependency exception
                    // You can decide how to handle validation errors
                    ctxt.reportInputMismatch(Dependency.class,"Circular reference exception adding ["+dependency.getDataKey()+"]");
                    e.printStackTrace();
                }
            }
        }

        return dependency;
    }
}
