package com.abstratt.kirra.mdd.rest;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.mdd.rest.TupleParser;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class InstanceResource extends AbstractKirraRepositoryResource {

	@Get("json")
	public Representation getInstance() {
		String objectId = getObjectId();
		Entity targetEntity = getTargetEntity();
		
		Instance instance = "_template".equals(objectId) ? getRepository().newInstance(targetEntity.getEntityNamespace(), targetEntity.getName()) : lookupInstance(objectId);
		ResourceUtils.ensure(instance != null, "Instance not found: " + objectId, Status.CLIENT_ERROR_NOT_FOUND);
		return jsonToStringRepresentation(getInstanceJSONRepresentation(instance, targetEntity));
	}

	protected String getObjectId() {
		return (String) getRequestAttributes().get("objectId");
	}

	@Put("json")
	public Representation update(Representation requestRepresentation) throws IOException {
		String objectId = getObjectId();
		JsonNode toUpdate = JsonHelper.parse(requestRepresentation.getReader());
		Instance existingInstance = lookupInstance(objectId);
		if (existingInstance == null) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new EmptyRepresentation();
		}
		Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
		TupleParser.updateInstanceFromJsonRepresentation(toUpdate, entity, existingInstance);
		setStatus(Status.SUCCESS_OK);
	    return jsonToStringRepresentation(getInstanceJSONRepresentation(getRepository().updateInstance(existingInstance), entity));
	}

	protected Instance lookupInstance(String objectId) {
		Entity targetEntity = getTargetEntity();
		return getRepository().getInstance(targetEntity.getNamespace(), targetEntity.getName(), objectId, true);
	}
	
	@Delete
	public Representation delete() {
		String objectId = getObjectId();
		String entityName = (String) getRequestAttributes().get("entityName");
		String entityNamespace = (String) getRequestAttributes().get("entityNamespace");

	    getRepository().deleteInstance(entityNamespace, entityName, objectId);
		return new EmptyRepresentation();
	}
}
