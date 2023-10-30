package com.ddt.dependencyutils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.scheduling.config.Task;


import java.io.IOException;

public class DependencySerializer extends JsonSerializer<Dependency> {
    private boolean serializeDependencies;

    public DependencySerializer(){
        this.serializeDependencies = Dependency.getSerializeDependencies();
    }

    @Override
    public void serialize(Dependency dependency, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("dataKey", dependency.getDataKey().toString());
        gen.writeStringField("data", dependency.getData().toString());
        gen.writeBooleanField("finished",dependency.isFinished());
        // Add other fields you want to include in the JSON output

        if (serializeDependencies) {
            // Serialize dependencies
            gen.writeObjectField("dependencies", dependency.getDependencies());
        } else {
            // Serialize dependants
            gen.writeObjectField("dependants", dependency.getDependants());
        }

        gen.writeEndObject();
    }
}
