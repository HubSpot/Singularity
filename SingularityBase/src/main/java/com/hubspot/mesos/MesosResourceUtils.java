package com.hubspot.mesos;

import static com.google.common.base.Preconditions.checkState;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Value.Ranges;
import org.apache.mesos.Protos.Value.Set;

public final class MesosResourceUtils
{
    private MesosResourceUtils() {
        throw new AssertionError("Do not instantiate");
    }

    public static double getScalar(Resource resource) {
        checkNotNull(resource, "resource is null");
        checkState(resource.hasScalar(), "resource %s is not a scalar!", resource);
        checkState(resource.getScalar().hasValue(), "resource %s is a scalar but has no value!", resource);

        return resource.getScalar().getValue();
    }

    public static Ranges getRanges(Resource resource) {
        checkNotNull(resource, "resource is null");
        checkState(resource.hasRanges(), "resource %s is not a range!", resource);

        return resource.getRanges();
    }


    public static Set getSet(Resource resource) {
        checkNotNull(resource, "resource is null");
        checkState(resource.hasSet(), "resource %s is not a set!", resource);

        return resource.getSet();
    }


}

