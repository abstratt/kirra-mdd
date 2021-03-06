package com.abstratt.kirra.mdd.rest;

import org.restlet.data.Reference;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.rest.common.Paths;

public class KirraReferenceBuilder {
    private Reference baseReference;

    public KirraReferenceBuilder(Reference baseReference) {
        this.baseReference = baseReference;
    }

    public Reference buildEntityReference(String namespace, String entityName) {
        return getBaseReference().addSegment(Paths.ENTITIES).addSegment(namespace + '.' + entityName);
    }

    public Reference buildInstanceListReference(String namespace, String entityName) {
        return getBaseReference().addSegment(Paths.INSTANCES).addSegment(namespace + '.' + entityName);
    }

    public Reference buildInstanceReference(Instance instance) {
        return buildInstanceListReference(instance.getEntityNamespace(), instance.getEntityName()).addSegment(instance.getObjectId());
    }

    public Reference getEntitiesReference() {
        return baseReference.clone().addSegment(Paths.ENTITIES).addSegment("");
    }

    public Reference getServicesReference() {
        return baseReference.clone().addSegment(Paths.SERVICES).addSegment("");
    }

    private Reference getBaseReference() {
        return baseReference.clone();
    }
}
