package com.hubspot.singularity.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.dropwizard.configuration.ConfigurationSourceProvider;

public class MergingSourceProvider implements ConfigurationSourceProvider {
    private final ConfigurationSourceProvider delegate;
    private final String defaultConfigurationPath;
    private final ObjectMapper objectMapper;
    private final YAMLFactory yamlFactory;

    public MergingSourceProvider(ConfigurationSourceProvider delegate, String defaultConfigurationPath, ObjectMapper objectMapper, YAMLFactory yamlFactory) {
        this.delegate = delegate;
        this.defaultConfigurationPath = defaultConfigurationPath;
        this.objectMapper = objectMapper;
        this.yamlFactory = yamlFactory;
    }

    @Override
    public InputStream open(String path) throws IOException {
        final JsonNode originalNode = objectMapper.readTree(yamlFactory.createParser(delegate.open(defaultConfigurationPath)));
        final JsonNode overrideNode = objectMapper.readTree(yamlFactory.createParser(delegate.open(path)));

        if (!(originalNode instanceof ObjectNode && overrideNode instanceof ObjectNode)) {
            throw new SingularityConfigurationMergeException(String.format("Both %s and %s need to be YAML objects", defaultConfigurationPath, path));
        }

        merge((ObjectNode)originalNode, (ObjectNode)overrideNode);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        objectMapper.writeTree(yamlFactory.createGenerator(baos), originalNode);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static void merge(ObjectNode to, ObjectNode from) {
        Iterator<String> newFieldNames = from.fieldNames();

        while (newFieldNames.hasNext()) {
            String newFieldName = newFieldNames.next();
            JsonNode oldVal = to.get(newFieldName);
            JsonNode newVal = from.get(newFieldName);

            if (oldVal == null || oldVal.isNull()) {
                to.set(newFieldName, newVal);
            } else if (oldVal.isArray() && newVal.isArray()) {
                ((ArrayNode) oldVal).removeAll();
                ((ArrayNode) oldVal).addAll((ArrayNode) newVal);
            } else if (oldVal.isObject() && newVal.isObject()) {
                merge((ObjectNode) oldVal, (ObjectNode) newVal);
            } else if (!(newVal == null || newVal.isNull())) {
                to.set(newFieldName, newVal);
            }
        }
    }
}
