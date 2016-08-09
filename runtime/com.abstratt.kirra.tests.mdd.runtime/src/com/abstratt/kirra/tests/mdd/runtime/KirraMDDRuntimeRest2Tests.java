package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.ServiceRegistration;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.kirra.rest.common.Paths;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.WebFrontEnd;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import junit.framework.TestCase;

public class KirraMDDRuntimeRest2Tests extends AbstractKirraRestTests {

    Map<String, String> authorized = new HashMap<String, String>();
    
    private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    protected ServiceRegistration<AuthenticationService> authenticatorRegistration;
    
    public KirraMDDRuntimeRest2Tests(String name) {
        super(name);
    }
    
    @Override
    protected URI getWorkspaceURI() throws IOException, HttpException {
    	URI workspaceURI = URI.create("http://localhost" + WebFrontEnd.APP_API2_PATH + "/");
		return workspaceURI;
    }
    
    @Override
    protected void login(String username, String password) throws HttpException, IOException {
    }

    public void testCreateInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute username : String;\n";
        model += "end;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer := 5;\n";
        model += "    attribute attr3 : Integer := 90;\n";
        model += "    attribute attr4 : Integer[0,1] := 80;\n";
        model += "    attribute attr5 : Integer[0,1];\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        URI sessionURI = getWorkspaceURI();

        GetMethod getTemplateInstance = new GetMethod(sessionURI.resolve(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName())).toString());

        ObjectNode template = (ObjectNode) executeJsonMethod(200, getTemplateInstance);

        ((ObjectNode) template.get("values")).put("attr1", "foo");
        ((ObjectNode) template.get("values")).put("attr3", 100);
        ((ObjectNode) template.get("values")).put("attr4", (String) null);
        PostMethod createMethod = new PostMethod(sessionURI.resolve(Paths.INSTANCES_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName())).toString());
        createMethod.setRequestEntity(new StringRequestEntity(template.toString(), "application/json", "UTF-8"));

        ObjectNode created = (ObjectNode) executeJsonMethod(201, createMethod);
        TestCase.assertEquals("foo", created.get("values").get("attr1").asText());
        TestCase.assertEquals(5, created.get("values").get("attr2").asLong());
        TestCase.assertEquals(100, created.get("values").get("attr3").asLong());
        TestCase.assertNull(created.get("values").get("attr4"));
        TestCase.assertNull(created.get("values").get("attr5"));
    }


    public void testGetEntity() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute username : String;\n";
        model += "end;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer;\n";
        model += "    operation op1();\n";
        model += "    operation op2();\n";
        model += "    static query op3() : MyClass1;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        URI sessionURI = getWorkspaceURI();
        
        URI entityUri = sessionURI.resolve(Paths.ENTITY_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));
        GetMethod getEntity = new GetMethod(entityUri.toString());
        executeMethod(200, getEntity);

        ObjectNode jsonEntity = (ObjectNode) JsonHelper.parse(new InputStreamReader(getEntity.getResponseBodyAsStream()));

        TestCase.assertEquals("MyClass1", jsonEntity.get("name").textValue());
        TestCase.assertEquals("mypackage", jsonEntity.get("namespace").textValue());
        TestCase.assertEquals(StringUtils.removeEnd(entityUri.toString(), "/"), StringUtils.removeEnd(jsonEntity.get("uri").textValue(), "/"));

        TestCase.assertNotNull(jsonEntity.get("extentUri"));
    }

    public void testGetEntityList() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1 attribute a : String; end;\n";
        model += "class MyClass2 attribute a : Integer; end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        
        URI sessionURI = getWorkspaceURI();
        URI entitiesUri = sessionURI.resolve(Paths.ENTITIES_PATH.replace("{application}", getName()));
        
        GetMethod getDomainTypes = new GetMethod(entitiesUri.toASCIIString());
        executeMethod(200, getDomainTypes);

        ArrayNode entities = (ArrayNode) JsonHelper.parse(new InputStreamReader(getDomainTypes.getResponseBodyAsStream()));
        TestCase.assertEquals(3, entities.size());

        
        List<Object> elementList = IteratorUtils.toList(entities.elements(), entities.size());
		List<JsonNode> myPackageEntities = elementList.stream().map(it -> ((JsonNode) it)).filter(it -> "mypackage".equals(it.get("namespace").textValue())).collect(Collectors.toList());
		assertEquals(2, myPackageEntities.size());
		TestCase.assertEquals("MyClass1", myPackageEntities.get(0).get("name").textValue());
		TestCase.assertEquals("MyClass2", myPackageEntities.get(1).get("name").textValue());
		
        for (JsonNode jsonNode : myPackageEntities) {
            TestCase.assertEquals("mypackage", jsonNode.get("namespace").textValue());
            TestCase.assertNotNull(jsonNode.get("uri"));
            executeMethod(200, new GetMethod(jsonNode.get("uri").toString()));
        }
    }

    public void testGetInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute username : String;\n";
        model += "end;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        URI sessionURI = getWorkspaceURI();

        Instance created = RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Instance>() {
            @Override
            public Instance run(Resource<?> resource) {
                Repository repository = resource.getFeature(Repository.class);
                Instance instance = repository.newInstance("mypackage", "MyClass1");
                instance.setValue("attr1", "The answer is");
                instance.setValue("attr2", "42");
                Instance created = repository.createInstance(instance);
                return created;
            }
        });
        
        ObjectNode jsonInstance = executeJsonMethod(200,
                new GetMethod(sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.getObjectId()).replace("{application}", getName())).toString()));

        TestCase.assertNotNull(jsonInstance.get("uri"));
        executeMethod(200, new GetMethod(jsonInstance.get("uri").toString()));
        TestCase.assertNotNull(jsonInstance.get("entityUri"));
        executeMethod(200, new GetMethod(jsonInstance.get("entityUri").asText()));

        ObjectNode values = (ObjectNode) jsonInstance.get("values");
        TestCase.assertEquals("The answer is", values.get("attr1").textValue());
        TestCase.assertEquals(42L, values.get("attr2").asLong());
    }

    public void testGetInstanceList() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute username : String;\n";
        model += "end;\n";
        model += "class MyClass1 attribute a : Integer[0,1]; end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
            @Override
            public Object run(Resource<?> resource) {
                Repository repository = resource.getFeature(Repository.class);
                repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
                repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
                return null;
            }
        });

        URI sessionURI = getWorkspaceURI();
        URI instanceListUri = sessionURI.resolve(Paths.INSTANCES_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));

        GetMethod getInstances = new GetMethod(instanceListUri.toString());
        executeMethod(200, getInstances);

        ArrayNode instances = (ArrayNode) JsonHelper.parse(new InputStreamReader(getInstances.getResponseBodyAsStream())).get("contents");
        TestCase.assertEquals(2, instances.size());

        for (JsonNode jsonNode : instances) {
            TestCase.assertNotNull(jsonNode.get("uri"));
            executeMethod(200, new GetMethod(jsonNode.get("uri").asText()));
            TestCase.assertNotNull(jsonNode.get("entityUri"));
            executeMethod(200, new GetMethod(jsonNode.get("entityUri").asText()));
        }
    }

    public void testUpdateInstance() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        URI sessionURI = getWorkspaceURI();
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName())).toString()));
        
        ((ObjectNode) jsonInstance1.get("values")).set("attr1", new TextNode("value 1a"));
        ((ObjectNode) jsonInstance1.get("values")).set("attr2", new TextNode("value 2a"));

        PutMethod putMethod = new PutMethod(jsonInstance1.get("uri").textValue());
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        TestCase.assertEquals("value 1a", updated.get("values").get("attr1").asText());
        TestCase.assertEquals("value 2a", updated.get("values").get("attr2").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        TestCase.assertEquals("value 1a", retrieved.get("values").get("attr1").asText());
        TestCase.assertEquals("value 2a", retrieved.get("values").get("attr2").asText());
    }
    
    public void testUpdateInstance_ClearValue() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        URI sessionURI = getWorkspaceURI();

        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName())).toString()));
        
        ((ObjectNode) jsonInstance1.get("values")).set("attr1", new TextNode(""));

        PutMethod putMethod = new PutMethod(jsonInstance1.get("uri").textValue());
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        TestCase.assertEquals("", updated.get("values").get("attr1").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        TestCase.assertEquals("", retrieved.get("values").get("attr1").asText());
    }
    
    public void testUpdateInstance_SetLink() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        URI sessionURI = getWorkspaceURI();
        URI uri1 = sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", created.get(0).getTypeRef().getFullName()).replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        URI uri2 = sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", created.get(1).getTypeRef().getFullName()).replace("{objectId}", created.get(1).getObjectId()).replace("{application}", getName()));        
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(uri1.toString()));
        ObjectNode links = jsonNodeFactory.objectNode();
        jsonInstance1.put("links", links);
        
        ObjectNode myClass2 = jsonNodeFactory.objectNode();
        links.put("myClass2", myClass2);
        myClass2.set("uri", new TextNode(uri2.toString()));

        PutMethod putMethod = new PutMethod(uri1.toString());
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        assertNotNull(updated.get("links"));
        assertNotNull(updated.get("links").get("myClass2"));
        TestCase.assertEquals(uri2.toString(), updated.get("links").get("myClass2").get("uri").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        assertNotNull(retrieved.get("links"));
        assertNotNull(retrieved.get("links").get("myClass2"));
        TestCase.assertEquals(uri2.toString(), retrieved.get("links").get("myClass2").get("uri").asText());
    }
    
    public void testUpdateInstance_UnsetLink() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        URI sessionURI = getWorkspaceURI();
        URI uri1 = sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", created.get(0).getTypeRef().getFullName()).replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        URI uri2 = sessionURI.resolve(Paths.INSTANCE_PATH.replace("{entityName}", created.get(1).getTypeRef().getFullName()).replace("{objectId}", created.get(1).getObjectId()).replace("{application}", getName()));        
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(uri1.toString()));
        ObjectNode links = jsonNodeFactory.objectNode();
        jsonInstance1.put("links", links);
        
        ObjectNode myClass2 = jsonNodeFactory.objectNode();
        links.put("myClass2", myClass2);
        myClass2.set("uri", new TextNode(uri2.toString()));

        PutMethod putMethod1 = new PutMethod(uri1.toString());
        putMethod1.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated1 = (ObjectNode) executeJsonMethod(200, putMethod1);
        ((ObjectNode)updated1.get("links")).set("myClass2", jsonNodeFactory.nullNode());
        
        PutMethod putMethod2 = new PutMethod(uri1.toString());
        putMethod2.setRequestEntity(new StringRequestEntity(updated1.toString(), "application/json", "UTF-8"));
        ObjectNode updated2 = (ObjectNode) executeJsonMethod(200, putMethod2);
        
        assertNotNull(updated2.get("links"));
        assertNull(updated2.get("links").get("myClass2"));
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        assertNotNull(retrieved.get("links"));
        assertNull(retrieved.get("links").get("myClass2"));
    }    

	private List<Instance> testUpdateInstanceSetup() throws IOException, CoreException {
		String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String[0,1];\n";
        model += "    attribute attr2 : String[0,1];\n";
        model += "    attribute myClass2 : MyClass2[0,1];\n";
        model += "end;\n";
        model += "class MyClass2\n";
        model += "    attribute attr2 : String[0,1];\n";
        model += "end;\n";
        model += "end.";
        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        return RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<List<Instance>>() {
        	@Override
        	public List<Instance> run(Resource<?> resource) {
        		List<Instance> created = new LinkedList<>();
        		Repository repository = resource.getFeature(Repository.class);
        		Instance instance1 = repository.newInstance("mypackage", "MyClass1");
        		instance1.setValue("attr1", "value1");
        		created.add(repository.createInstance(instance1));
        		Instance instance2 = repository.newInstance("mypackage", "MyClass2");
        		instance2.setValue("attr2", "value2");
        		created.add(repository.createInstance(instance2));
        		return created;
        	}
        });
	}


    public void testGetTemplateInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute username : String;\n";
        model += "end;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer := 5;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        URI sessionURI = getWorkspaceURI();

        URI templateUri = sessionURI.resolve(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));
		GetMethod getTemplateInstance = new GetMethod(templateUri.toString());
        executeMethod(200, getTemplateInstance);

        ObjectNode jsonInstance = (ObjectNode) JsonHelper.parse(new InputStreamReader(getTemplateInstance.getResponseBodyAsStream()));

        TestCase.assertNull(((ObjectNode) jsonInstance.get("values")).get("attr1"));
        TestCase.assertEquals(5, ((ObjectNode) jsonInstance.get("values")).get("attr2").asLong());
    }

    @Override
    protected void tearDown() throws Exception {
        if (authenticatorRegistration != null)
            authenticatorRegistration.unregister();
        super.tearDown();
    }
}