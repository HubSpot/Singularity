package com.hubspot.singularity.metrics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

import javax.net.SocketFactory;

import com.codahale.metrics.graphite.Graphite;
import com.hubspot.mesos.JavaUtils;

public class GraphiteWithTags extends Graphite {
    private final Map<String, String> tags;

    public GraphiteWithTags(InetSocketAddress address, SocketFactory socketFactory, Charset charset, Map<String, String> tags) {
        super(address, socketFactory, charset);
        this.tags = tags;
    }

    @Override
    public void send(String name, String value, long timestamp) throws IOException {
        if (!tags.isEmpty()) {
            name += '[' + JavaUtils.COMMA_EQUALS_MAP_JOINER.join(tags) + ']';
        }
        super.send(name, value, timestamp);
    }
}
