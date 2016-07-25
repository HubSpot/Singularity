package com.hubspot.singularity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;

public class SingularityMesosTaskLabelsDeserializer extends JsonDeserializer<List<SingularityMesosTaskLabel>> {
    @Override
    public List<SingularityMesosTaskLabel> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final ObjectCodec oc = jp.getCodec();
        final JsonNode node = oc.readTree(jp);

        if (node.isArray()) {
            final List<SingularityMesosTaskLabel> labels = new ArrayList<>();

            for (JsonNode item : node) {
                labels.add(oc.treeToValue(item, SingularityMesosTaskLabel.class));
            }

            return labels;
        } else if (node.isObject()) {
            final List<SingularityMesosTaskLabel> labels = new ArrayList<>();

            final Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                final Map.Entry<String, JsonNode> entry = iterator.next();
                labels.add(new SingularityMesosTaskLabel(entry.getKey(), Optional.of(entry.getValue().textValue())));
            }

            return labels;
        } else {
            throw new JsonParseException("Don't know how to deserialize a List<SingularityMesosTaskLabel> object from node type " + node.getNodeType(), jp.getCurrentLocation());
        }
    }
}
