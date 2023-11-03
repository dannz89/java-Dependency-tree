package com.ddt.dependencyutils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DependencySerializer
        extends JsonSerializer<Dependency> {
    public final static Logger logger = LoggerFactory.getLogger(DependencySerializer.class);

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
            case DEPENDENCIES -> {
                gen.writeArrayFieldStart("dependencies");
                if (!dependency.hasDependencies()) {
                    gen.writeObject(null);
                    gen.writeEndArray();
                    break;
                }
                dependency.getDependencies().values().forEach(val -> {
                    try {
                        gen.writeObject(val);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                });
                gen.writeEndArray();
            }
            case DEPENDANTS -> {
                gen.writeArrayFieldStart("dependants");
                if (!dependency.hasDependants()) {
                    gen.writeObject(null);
                    gen.writeEndArray();
                    break;
                }

                dependency.getDependants().values().forEach(val -> {
                    try {
                        gen.writeObject(val);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                });

                gen.writeEndArray();
            }
        }

        gen.writeEndObject();
    }
}
