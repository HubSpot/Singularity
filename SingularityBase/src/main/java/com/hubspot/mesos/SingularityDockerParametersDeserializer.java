package com.hubspot.mesos;

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

public class SingularityDockerParametersDeserializer extends JsonDeserializer<SingularityDockerParameters> {
    @Override
    public SingularityDockerParameters deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final ObjectCodec oc = jp.getCodec();
        final JsonNode node = oc.readTree(jp);

        if (node.isArray()) {
            final List<SingularityDockerParameter> parameters = new ArrayList<>();

            for (JsonNode item : node) {
                parameters.add(oc.treeToValue(item, SingularityDockerParameter.class));
            }

            return new SingularityDockerParameters(parameters);
        } else if (node.isObject()) {
            final List<SingularityDockerParameter> parameters = new ArrayList<>();

            final Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                final Map.Entry<String, JsonNode> entry = iterator.next();
                parameters.add(new SingularityDockerParameter(entry.getKey(), entry.getValue().textValue()));
            }

            return new SingularityDockerParameters(parameters);
        } else {
            throw new JsonParseException("Don't know how to deserialize a List<SingularityDockerParameter> object from node type " + node.getNodeType(), jp.getCurrentLocation());
        }
    }
}
