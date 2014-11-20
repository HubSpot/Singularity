package com.hubspot.mesos;

import static com.hubspot.mesos.SingularityResourceRequest.CPU_RESOURCE_NAME;
import static com.hubspot.mesos.SingularityResourceRequest.MEMORY_RESOURCE_NAME;
import static com.hubspot.mesos.SingularityResourceRequest.PORT_COUNT_RESOURCE_NAME;
import static com.hubspot.mesos.SingularityResourceRequest.findNumberResourceRequest;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TestSingularityResourceRequest
{
    @Test
    public void testFromResources()
    {
        Resources r = new Resources(1.2, 1024, 5);

        List<SingularityResourceRequest> resources = r.getAsResourceRequestList();

        assertEquals(r.getCpus(), findNumberResourceRequest(resources, CPU_RESOURCE_NAME, Double.MAX_VALUE).doubleValue(), 0.001);
        assertEquals(r.getMemoryMb(), findNumberResourceRequest(resources, MEMORY_RESOURCE_NAME, Double.MAX_VALUE).doubleValue(), 0.001);
        assertEquals(r.getNumPorts(), findNumberResourceRequest(resources, PORT_COUNT_RESOURCE_NAME, Integer.MAX_VALUE).intValue());
    }
}
