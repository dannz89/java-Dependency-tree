package com.ddt.dependencyutils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DependencySerializer extends JsonSerializer<Dependency> {
    public DependencySerializer() {
    }

    @Override
    public void serialize(Dependency dependency, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("dataKey", dependency.getDataKey().toString());
        gen.writeStringField("data", dependency.getData().toString());
        gen.writeBooleanField("finished",dependency.isFinished());
        // Add other fields you want to include in the JSON output

        switch (dependency.getSerializingScheme()) {
            case DEPENDENCIES -> gen.writeObjectField("dependencies", dependency.getDependencies());
            case DEPENDANTS -> gen.writeObjectField("dependants", dependency.getDependants());
        }

        gen.writeEndObject();
    }
}
