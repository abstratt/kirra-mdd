package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.core.util.MDDExtensionUtils.AccessCapability
import com.abstratt.mdd.target.jee.JAXRSAccessControlGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.util.List
import org.eclipse.core.runtime.CoreException
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class JAXRSAccessControlGeneratorTest extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }
    
    val source = '''
            model crm;
            abstract class Person
                attribute number : String;
            end;
            role class Customer specializes Person
                allow Representative create, update;
                allow Admin delete;
                attribute accounts : Account[*];
            end;
            role class Representative specializes Personnel
            end;
            role class Admin specializes Personnel
            end;
            abstract role class Personnel specializes Person
            end;
            
            class Account
                allow Admin all;
                allow Representative create, update;
                allow Customer read, update, call { System#user() == self.customer } ;
                attribute number : String;
                derived attribute totalPurchases : Double := { 0.0 };
                reference customer : Customer opposite accounts;
                attribute comments : String
                    allow Admin read
                    allow Representative
                    allow Customer none;
                operation close()
                    allow Personnel
                    allow Customer { System#user() == self.customer };  
            end;
            
            end.
        '''

    private def CharSequence generateAnnotation(List<String> contextNames, AccessCapability requiredCapability) {
        parseAndCheck(source)
        val contexts = contextNames.map[repository.findNamedElement(it, null, null)]
        val allRoleClasses = repository.findInAnyPackage(it | it instanceof Class && (it as Class).role)
		val generated = new JAXRSAccessControlGenerator(repository).generateEndpointAnnotation(requiredCapability, allRoleClasses, contexts)
		return generated
    } 
    
    def void checkAnnotation(List<String> contextNames, AccessCapability requiredCapability, CharSequence expected) throws CoreException {
		val actual = generateAnnotation(contextNames, requiredCapability)
        AssertHelper.assertStringsEqual(expected.toString, actual.toString)
    }
    
    def void testCondition_Operation() {
        parseAndCheck(source)
        val contextNames = #['crm::Account', 'crm::Account::close']
        val contexts = contextNames.map[repository.findNamedElement(it, null, null)]
        val requiredCapability = AccessCapability.Read
        val allRoleClasses = repository.findInAnyPackage(it | it instanceof Class && (it as Class).role)
		val actual = new JAXRSAccessControlGenerator(repository).generateAccessChecks('self', requiredCapability, allRoleClasses, contexts, 'throw new Error();')
		val expected = '''
		User user = new UserService().findByUsername(securityContext.getUserPrincipal().getName());
		if (securityContext.isUserInRole("Customer")) {
			Customer asCustomer = new CustomerService().findByUser(user);
			if (asCustomer == null || !(asCustomer == self.getCustomer())) {
				throw new Error();
		    }
		} 
		'''
		AssertHelper.assertStringsEqual(expected.toString, actual.toString)
    }
    
    def void testCondition_ReadAttribute1() {
        parseAndCheck(source)
        val contextNames = #['crm::Account', 'crm::Account::close']
        val contexts = contextNames.map[repository.findNamedElement(it, null, null)]
        val requiredCapability = AccessCapability.Read
        val allRoleClasses = repository.findInAnyPackage(it | it instanceof Class && (it as Class).role)
		val actual = new JAXRSAccessControlGenerator(repository).generateAccessChecks('self', requiredCapability, allRoleClasses, contexts, 'throw new Error();')
		val expected = '''
		User user = new UserService().findByUsername(securityContext.getUserPrincipal().getName());
		if (securityContext.isUserInRole("Customer")) {
			Customer asCustomer = new CustomerService().findByUser(user);
			if (asCustomer == null || !(asCustomer == self.getCustomer())) {
				throw new Error();
		    }
		} 
		'''
		AssertHelper.assertStringsEqual(expected.toString, actual.toString)
    }
    
    def void testCondition_Entity() {
        parseAndCheck(source)
        val contextNames = #['crm::Account']
        val contexts = contextNames.map[repository.findNamedElement(it, null, null)]
        val requiredCapability = AccessCapability.Call
        val allRoleClasses = repository.findInAnyPackage(it | it instanceof Class && (it as Class).role)
		val actual = new JAXRSAccessControlGenerator(repository).generateAccessChecks('self', requiredCapability, allRoleClasses, contexts, 'throw new Error();')
		val expected = '''
		User user = new UserService().findByUsername(securityContext.getUserPrincipal().getName());
		if (securityContext.isUserInRole("Customer")) {
			Customer asCustomer = new CustomerService().findByUser(user);
			if (asCustomer == null || !(asCustomer == self.getCustomer())) {
				throw new Error();
		    }
		} 
		'''
		AssertHelper.assertStringsEqual(expected.toString, actual.toString)
    }    
    

    def void testEntity() throws CoreException {
        checkAnnotation(#['crm::Account'], null,
            '''
                @RolesAllowed({"Admin", "Representative", "Customer"})
            '''
        )
    }
    
    def void testEntity_Create() throws CoreException {
    	checkAnnotation(#['crm::Account'], AccessCapability.Create,
            '''
                @RolesAllowed({"Admin", "Representative"})
            '''
        )
    }

    def void testEntity_Delete() throws CoreException {
    	checkAnnotation(#['crm::Account'], AccessCapability.Delete,
            '''
                @RolesAllowed({"Admin"})
            '''
        )
    }

    def void testOperation() throws CoreException {
    	checkAnnotation(#['crm::Account', 'crm::Account::close'], AccessCapability.Call,
            '''
                @RolesAllowed({"Admin", "Personnel", "Customer"})
            '''
		)
    }

    def void testAttribute_Read() throws CoreException {
    	checkAnnotation(#['crm::Account', 'crm::Account::comments'], AccessCapability.Read,
            '''
                @RolesAllowed({"Admin", "Representative"})
            '''
		)
    }
    
    def void testAttribute_Update() throws CoreException {
    	checkAnnotation(#['crm::Account', 'crm::Account::comments'], AccessCapability.Update,
            '''
                @RolesAllowed({"Representative"})
            '''
		)
    }
    
    def void testAttribute_Read2() throws CoreException {
    	checkAnnotation(#['crm::Account', 'crm::Account::totalPurchases'], AccessCapability.Read,
            '''
                @RolesAllowed({"Admin", "Representative", "Customer"})
            '''
		)
    }
	
}