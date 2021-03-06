package com.abstratt.kirra.mdd.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.EnumerationLiteral;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.NamedElement;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Operation.OperationKind;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.ParameterSet;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaBuilder;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDUtil;

/**
 * Builds Kirra schema elements based on UML elements.
 */
public class KirraMDDSchemaBuilder implements SchemaBuildingOnUML, SchemaBuilder {
    @Override
    public Schema build() {
        IRepository repository = RepositoryService.DEFAULT.getFeature(IRepository.class);
        Package[] topLevelPackages = repository.getTopLevelPackages(null);
		Collection<Package> applicationPackages = KirraHelper.getApplicationPackages(topLevelPackages);
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (Package current : applicationPackages) {
            List<Entity> entities = new ArrayList<Entity>();
            List<Service> services = new ArrayList<Service>();
            List<TupleType> tupleTypes = new ArrayList<TupleType>();
            for (Type type : current.getOwnedTypes())
                if (KirraHelper.isEntity(type))
                    entities.add(getEntity((Class) type));
                else if (KirraHelper.isService(type))
                    services.add(getService((BehavioredClassifier) type));
                else if (KirraHelper.isTupleType(type))
                    tupleTypes.add(getTupleType((Classifier) type));
            if (!entities.isEmpty() || !services.isEmpty() || !tupleTypes.isEmpty())
                namespaces.add(buildNamespace(current, entities, services, tupleTypes));
        }
        Map<TypeRef, Collection<TypeRef>> subTypes = collectSubtypes(namespaces);
        Schema schema = new Schema();
        schema.setNamespaces(namespaces);
        applySubtypes(schema, subTypes);
        schema.setApplicationName(KirraHelper.getApplicationName(repository));
        schema.setApplicationLabel(KirraHelper.getApplicationLabel(repository));
        schema.setApplicationLogo(KirraHelper.getApplicationLogo(repository));
        if (!namespaces.isEmpty())
            schema.setBuild(namespaces.get(0).getTimestamp());
        return schema;
    }

	private void applySubtypes(Schema schema, Map<TypeRef, Collection<TypeRef>> subTypesPerSuperType) {
		subTypesPerSuperType.forEach((superType, subTypes) -> schema.findNamespace(superType.getEntityNamespace()).findEntity(superType.getTypeName()).setSubTypes(subTypes));
	}

	private Map<TypeRef, Collection<TypeRef>> collectSubtypes(List<Namespace> namespaces) {
		Map<TypeRef, Collection<TypeRef>> subTypesPerSuperType = new LinkedHashMap<>(); 
        namespaces.forEach(namespace ->
        	namespace.getEntities().forEach(subType -> subType.getSuperTypes().forEach( superType ->
        		subTypesPerSuperType.computeIfAbsent(superType, s -> new LinkedHashSet<TypeRef>()).add(subType.getTypeRef())
			))
        );
        return subTypesPerSuperType;
	}

    private Namespace buildNamespace(Package umlPackage, List<Entity> entities, List<Service> services, List<TupleType> tupleTypes) {
        Namespace namespace = new Namespace(KirraHelper.getNamespaceName(umlPackage));
        namespace.setLabel(KirraHelper.getLabel(umlPackage));
        namespace.setDescription(KirraHelper.getDescription(umlPackage));
        namespace.setTimestamp(MDDUtil.getGeneratedTimestamp(umlPackage));
        namespace.setEntities(entities);
        namespace.setServices(services);
        namespace.setTupleTypes(tupleTypes);
        return namespace;
    }

    @Override
    public Entity getEntity(Class umlClass) {
        if (!KirraHelper.isEntity(umlClass))
            throw new KirraException(umlClass.getName() + " is not an entity", null, KirraException.Kind.SCHEMA);
        final Entity entity = new Entity();
        entity.setNamespace(getNamespace(umlClass));
        setName(umlClass, entity);
        entity.setProperties(getEntityProperties(umlClass));
        entity.setRelationships(getEntityRelationships(umlClass));
        org.eclipse.uml2.uml.Property mnemonic = KirraHelper.getMnemonic(umlClass);
        if (mnemonic != null) {
        	entity.setMnemonicSlot(mnemonic.getName());
        	if (KirraHelper.isRelationship(mnemonic)) 
        		entity.getRelationship(mnemonic.getName()).setMnemonic(true);
        	else 
        		entity.getProperty(mnemonic.getName()).setMnemonic(true);
        }
        entity.setOperations(getEntityOperations(umlClass));
        entity.setConcrete(KirraHelper.isConcrete(umlClass));
        entity.setInstantiable(KirraHelper.isInstantiable(umlClass));
        entity.setTopLevel(KirraHelper.isTopLevel(umlClass));
        entity.setStandalone(KirraHelper.isStandalone(umlClass));
        entity.setRole(KirraHelper.isRole(umlClass, false));
        entity.setUser(KirraHelper.isUser(umlClass));
        entity.setUserVisible(KirraHelper.isUserVisible(umlClass));
        entity.setOrderedDataElements(KirraHelper.getOrderedDataElements(umlClass));
        Stream<Classifier> superEntities = umlClass.getGenerals().stream().filter(g -> KirraHelper.isEntity(g)); 
        List<TypeRef> superTypes = superEntities.map(superEntity -> KirraHelper.convertType(superEntity)).collect(Collectors.toList());
        entity.setSuperTypes(superTypes);
        return entity;
    }
    
    @Override
    public Operation getEntityOperation(org.eclipse.uml2.uml.Operation umlOperation) {
        if (!KirraHelper.isEntityOperation(umlOperation))
            throw new IllegalArgumentException();
        if (!KirraHelper.isAction(umlOperation) && !KirraHelper.isFinder(umlOperation) && !KirraHelper.isConstructor(umlOperation))
            throw new IllegalArgumentException();

        Operation entityOperation = basicGetOperation(umlOperation);
        entityOperation.setKind(KirraHelper.getOperationKind(umlOperation));
        entityOperation.setInstanceOperation(entityOperation.getKind() == OperationKind.Action && !umlOperation.isStatic());
        return entityOperation;
    }

    @Override
    public Property getEntityProperty(org.eclipse.uml2.uml.Property umlAttribute) {
        Property entityProperty = new Property();
        setName(umlAttribute, entityProperty);
        entityProperty.setMultiple(umlAttribute.isMultivalued());
        entityProperty.setHasDefault(KirraHelper.hasDefault(umlAttribute));
        entityProperty.setInitializable(KirraHelper.isInitializable(umlAttribute));
        entityProperty.setEditable(KirraHelper.isEditable(umlAttribute));
        entityProperty.setRequired(KirraHelper.isRequired(umlAttribute, !entityProperty.isEditable() && entityProperty.isInitializable()));
        Type umlType = umlAttribute.getType();
        setTypeInfo(entityProperty, umlType);
        entityProperty.setDerived(KirraHelper.isDerived(umlAttribute));
        entityProperty.setAutoGenerated(KirraHelper.isAutoGenerated(umlAttribute));
        entityProperty.setUnique(KirraHelper.isUnique(umlAttribute));
        entityProperty.setUserVisible(KirraHelper.isUserVisible(umlAttribute));
        return entityProperty;
    }

    @Override
    public Relationship getEntityRelationship(org.eclipse.uml2.uml.Property umlAttribute) {
        Relationship entityRelationship = new Relationship();
        setName(umlAttribute, entityRelationship);

        org.eclipse.uml2.uml.Property otherEnd = umlAttribute.getOtherEnd();
        if (otherEnd != null && KirraHelper.isRelationship(otherEnd)) {
            entityRelationship.setOpposite(otherEnd.getName());
            entityRelationship.setOppositeRequired(KirraHelper.isRequired(otherEnd));
            entityRelationship.setOppositeReadOnly(KirraHelper.isReadOnly(otherEnd));
        }

        entityRelationship.setStyle(KirraHelper.getRelationshipStyle(umlAttribute));
        entityRelationship.setAssociationName(KirraHelper.getAssociationName(umlAttribute));
        entityRelationship.setAssociationNamespace(KirraHelper.getAssociationNamespace(umlAttribute));
        entityRelationship.setPrimary(KirraHelper.isPrimary(umlAttribute));
        entityRelationship.setNavigable(umlAttribute.isNavigable());
        entityRelationship.setRequired(!umlAttribute.isDerived() && umlAttribute.getLower() > 0);
        entityRelationship.setHasDefault(KirraHelper.hasDefault(umlAttribute));
        entityRelationship.setInitializable(KirraHelper.isInitializable(umlAttribute));
        entityRelationship.setEditable(KirraHelper.isEditable(umlAttribute));
        entityRelationship.setMultiple(umlAttribute.isMultivalued());
        setTypeInfo(entityRelationship, umlAttribute.getType());
        entityRelationship.setDerived(KirraHelper.isDerived(umlAttribute));
        entityRelationship.setUserVisible(KirraHelper.isUserVisible(umlAttribute));
        return entityRelationship;
    }

    @Override
    public List<Relationship> getEntityRelationships(Class modelClass) {
        List<Relationship> entityRelationships = new ArrayList<Relationship>();
        for (org.eclipse.uml2.uml.Property attribute : KirraHelper.getRelationships(modelClass)) {
        	Relationship relationship = getEntityRelationship(attribute);
			relationship.setInherited(KirraHelper.isInherited(attribute, modelClass));
			relationship.setDefiner(KirraHelper.convertType(attribute.getClass_()));
			entityRelationships.add(relationship);
        }
        return entityRelationships;
    }

    @Override
    public String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass) {
        return SchemaManagementOperations.getNamespace(umlClass);
    }

    @Override
    public Service getService(BehavioredClassifier serviceClassifier) {
        final Service service = new Service();
        setName(serviceClassifier, service);
        service.setNamespace(getNamespace(serviceClassifier));
        service.setOperations(getServiceOperations(serviceClassifier));
        return service;
    }

    @Override
    public Operation getServiceOperation(org.eclipse.uml2.uml.BehavioralFeature umlOperation) {
        if (!KirraHelper.isServiceOperation(umlOperation))
            throw new IllegalArgumentException();
        Operation serviceOperation = basicGetOperation(umlOperation);
        serviceOperation.setKind(umlOperation instanceof org.eclipse.uml2.uml.Operation ? Operation.OperationKind.Retriever
                : Operation.OperationKind.Event);
        serviceOperation.setInstanceOperation(false);
        return serviceOperation;
    }

    @Override
    public TupleType getTupleType(Classifier umlClass) {
        if (!KirraHelper.isTupleType(umlClass))
            throw new KirraException(umlClass.getName() + " is not a tuple type", null, KirraException.Kind.SCHEMA);
        final TupleType tupleType = new TupleType();
        tupleType.setNamespace(getNamespace(umlClass));
        setName(umlClass, tupleType);
        tupleType.setProperties(getTupleProperties(umlClass));
        return tupleType;
    }

    List<Operation> getEntityOperations(Class umlClass) {
        List<Operation> entityOperations = new ArrayList<Operation>();
        for (org.eclipse.uml2.uml.Operation operation : umlClass.getAllOperations())
            if (operation.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isEntityOperation(operation)) {
				Operation entityOperation = getEntityOperation(operation);
				entityOperation.setInherited(KirraHelper.isInherited(operation, umlClass));
				entityOperation.setDefiner(KirraHelper.convertType(operation.getClass_()));
				entityOperations.add(entityOperation);
			}
        return entityOperations;
    }

    List<Property> getEntityProperties(Class umlClass) {
        List<Property> entityProperties = new ArrayList<Property>();
        for (org.eclipse.uml2.uml.Property attribute : KirraHelper.getProperties(umlClass)) {
            Property entityProperty = getEntityProperty(attribute);
            entityProperty.setInherited(KirraHelper.isInherited(attribute, umlClass));
            entityProperty.setDefiner(KirraHelper.convertType(attribute.getClass_()));
			entityProperties.add(entityProperty);
        }
        return entityProperties;
    }
    
    List<Property> getTupleProperties(Classifier dataType) {
        List<Property> tupleProperties = new ArrayList<Property>();
        for (org.eclipse.uml2.uml.Property attribute : KirraHelper.getTupleProperties(dataType)) {
			Property tupleProperty = getEntityProperty(attribute);
			tupleProperty.setInherited(attribute.getClass_() != null && attribute.getClass_() != dataType);
			tupleProperty.setInherited(KirraHelper.isInherited(attribute, dataType));
			tupleProperties.add(tupleProperty);
		}
        return tupleProperties;
    }

    List<Operation> getServiceOperations(BehavioredClassifier serviceClass) {
        List<Operation> entityOperations = new ArrayList<Operation>();
        for (org.eclipse.uml2.uml.Interface provided : serviceClass.getImplementedInterfaces()) {
            for (org.eclipse.uml2.uml.Operation operation : provided.getAllOperations())
                if (operation.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isServiceOperation(operation))
                    entityOperations.add(getServiceOperation(operation));
            for (org.eclipse.uml2.uml.Reception reception : provided.getOwnedReceptions())
                if (reception.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isServiceOperation(reception))
                    entityOperations.add(getServiceOperation(reception));
        }
        return entityOperations;
    }

    private Operation basicGetOperation(org.eclipse.uml2.uml.BehavioralFeature umlOperation) {
        Operation basicOperation = new Operation();
        setName(umlOperation, basicOperation);
        if (umlOperation instanceof org.eclipse.uml2.uml.Operation) {
        	org.eclipse.uml2.uml.Operation asOperation = (org.eclipse.uml2.uml.Operation) umlOperation; 
            setTypeInfo(basicOperation, ((org.eclipse.uml2.uml.Operation) umlOperation).getType());
            basicOperation.setMultiple(asOperation.getReturnResult() != null && asOperation.getReturnResult().isMultivalued());
        }

        basicOperation.setOwner(KirraHelper.convertType((Type) umlOperation.getOwner()));
        List<ParameterSet> parameterSets = new LinkedList<>();
        KirraHelper.getParameterSets(umlOperation).forEach(parameterSet -> {
            final ParameterSet newSet = new ParameterSet();
            newSet.setOwner(basicOperation);
            newSet.setParameters(KirraHelper.getParameters(parameterSet));
            setName(parameterSet, newSet);
            parameterSets.add(newSet);
        });
        Set<String> allParameterSetNames = parameterSets.stream().map(it -> it.getName()).collect(Collectors.toSet());
        basicOperation.setParameterSets(parameterSets);
        List<Parameter> operationParameters = new ArrayList<Parameter>();
        for (org.eclipse.uml2.uml.Parameter parameter : KirraHelper.getParameters(umlOperation)) {
            final Parameter operationParameter = new Parameter();
            operationParameter.setOwner(basicOperation.getOwner());
            setName(parameter, operationParameter);
            operationParameter.setUserVisible(true);
            operationParameter.setRequired(KirraHelper.isRequired(parameter));
            operationParameter.setHasDefault(KirraHelper.hasDefault(parameter));
            operationParameter.setMultiple(KirraHelper.isMultiple(parameter));
            operationParameter.setDirection(KirraHelper.getParameterDirection(parameter));
            operationParameter.setEffect(KirraHelper.getParameterEffect(parameter));
            List<String> parameterParameterSets = parameter.getParameterSets().stream().map(it->it.getName()).collect(Collectors.toList());
            operationParameter.setParameterSets(parameterParameterSets);
            operationParameter.setInAllSets(allParameterSetNames.isEmpty() || allParameterSetNames.equals(new LinkedHashSet<>(parameterParameterSets)));
            setTypeInfo(operationParameter, parameter.getType());
            operationParameters.add(operationParameter);
        }
        basicOperation.setParameters(operationParameters);
        return basicOperation;
    }

    private void setName(org.eclipse.uml2.uml.NamedElement sourceElement, NamedElement<?> targetElement) {
        targetElement.setName(KirraHelper.getName(sourceElement));
        targetElement.setSymbol(KirraHelper.getSymbol(sourceElement));
        targetElement.setLabel(KirraHelper.getLabel(sourceElement));
        targetElement.setDescription(KirraHelper.getDescription(sourceElement));
    }

    private void setTypeInfo(com.abstratt.kirra.TypedElement<?> typedElement, Type umlType) {
        if (umlType == null)
            return;
        if (KirraHelper.isEnumeration(umlType))
            typedElement.setEnumerationLiterals(KirraHelper
                    .getEnumerationLiterals(umlType)
                    .stream()
                    .map(it -> getEnumerationLiteral(it))
                    .collect(Collectors.toMap(it -> it.getName(), it -> it)));
        typedElement.setTypeRef(KirraHelper.convertType(umlType));
    }
    
    private EnumerationLiteral getEnumerationLiteral(org.eclipse.uml2.uml.NamedElement element) {
        EnumerationLiteral literal = new EnumerationLiteral();
        setName(element, literal);
        return literal;
    }

}
