package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.mdd.rest.InstanceJSONRepresentation;
import com.abstratt.kirra.mdd.rest.RelatedInstanceJSONRepresentation;
import com.abstratt.kirra.mdd.rest.TupleParser;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class RelatedInstanceListResource extends AbstractInstanceListResource {

	@Override
	protected InstanceJSONRepresentation getInstanceJSONRepresentation(Instance instance, Entity entity) {
		RelatedInstanceJSONRepresentation representation = new RelatedInstanceJSONRepresentation();
		new InstanceJSONRepresentationBuilder().build(representation, getReferenceBuilder(), entity, instance);
		String relationshipName = (String) getRequestAttributes().get("relationshipName");
		Entity baseEntity = super.getTargetEntity();
		String objectId = (String) getRequestAttributes().get("objectId");
		Reference baseInstanceRef = getReferenceBuilder().buildInstanceReference(new Instance(baseEntity.getTypeRef(), objectId));
		representation.relatedUri = baseInstanceRef.addSegment(Paths.RELATIONSHIPS).addSegment(relationshipName).addSegment(instance.getObjectId()).toString();
		return representation;
	}
	
	@Override
	protected InstanceJSONRepresentation createInstanceJSONRepresentation() {
		return new RelatedInstanceJSONRepresentation();
	}
	
	protected Entity getTargetEntity() {
		String relationshipName = (String) getRequestAttributes().get("relationshipName");
		return getRelatedEntity(super.getTargetEntity(), this, relationshipName);
	}

	public static Entity getRelatedEntity(Entity baseEntity, AbstractKirraRepositoryResource resource, String relationshipName) {
		ResourceUtils.ensure(baseEntity != null, "Entity not found: " + resource.getEntityName(), Status.CLIENT_ERROR_NOT_FOUND);
		Relationship relationship = baseEntity.getRelationship(relationshipName);
		ResourceUtils.ensure(relationship != null, "Relationship not found: " + relationshipName, Status.CLIENT_ERROR_NOT_FOUND);
		return resource.getRepository().getEntity(relationship.getTypeRef().getEntityNamespace(), relationship.getTypeRef().getTypeName());
	}
	
	@Override
	protected List<Instance> findInstances(String entityNamespace,
			String entityName) {
		Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
		ResourceUtils.ensure(entity != null, "Entity not found: " + getEntityName(), Status.CLIENT_ERROR_NOT_FOUND);
		String relationshipName = (String) getRequestAttributes().get("relationshipName");
		ResourceUtils.ensure(entity.getRelationship(relationshipName) != null, "Relationship not found: " + relationshipName, Status.CLIENT_ERROR_NOT_FOUND);
		String objectId = (String) getRequestAttributes().get("objectId");
		return getRepository().getRelatedInstances(entity.getEntityNamespace(), entity.getName(), objectId, relationshipName, false);
	}
	
	/**
	 * Used for adding a child object to a parent object.  
	 */
	@Post("json")
	public Representation create(Representation requestRepresentation) throws IOException {
		String objectId = (String) getRequestAttributes().get("objectId");
		Entity anchorEntity = getRepository().getEntity(getEntityNamespace(), getEntityName());
		String relationshipName = (String) getRequestAttributes().get("relationshipName");
		Relationship relationship = anchorEntity.getRelationship(relationshipName);
		Instance anchorInstance = getRepository().getInstance(anchorEntity.getEntityNamespace(), anchorEntity.getName(), objectId, false);
		
		JsonNode toCreate = JsonHelper.parse(requestRepresentation.getReader());
		
		Entity targetEntity = getTargetEntity();
		JsonNode existingUri = toCreate.get("uri");
		
		
		if (existingUri != null && !existingUri.isNull()) {
			TupleParser.updateRelationship(anchorEntity, anchorInstance, relationship, existingUri);
			setStatus(Status.SUCCESS_OK);
			getRepository().updateInstance(anchorInstance);
		    return jsonToStringRepresentation(TupleParser.resolveLink(existingUri, relationship.getTypeRef()));
		}
		Instance newRelatedInstance = getRepository().newInstance(targetEntity.getEntityNamespace(), targetEntity.getName());
		TupleParser.updateInstanceFromJsonRepresentation(toCreate, targetEntity, newRelatedInstance);
		if (relationship.getOpposite() != null)
			newRelatedInstance.setSingleRelated(relationship.getOpposite(), anchorInstance);
	    Instance createdInstance = getRepository().createInstance(newRelatedInstance);
	    setStatus(Status.SUCCESS_CREATED);
		return jsonToStringRepresentation(getInstanceJSONRepresentation(createdInstance, targetEntity));
	}
}
