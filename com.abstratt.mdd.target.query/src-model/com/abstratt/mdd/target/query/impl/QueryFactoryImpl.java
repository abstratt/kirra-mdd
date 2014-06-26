/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.abstratt.mdd.target.query.Join;
import com.abstratt.mdd.target.query.PropertyReference;
import com.abstratt.mdd.target.query.Query;
import com.abstratt.mdd.target.query.QueryFactory;
import com.abstratt.mdd.target.query.QueryPackage;
import com.abstratt.mdd.target.query.VariableReference;

/**
 * <!-- begin-user-doc --> An implementation of the model <b>Factory</b>. <!--
 * end-user-doc -->
 * 
 * @generated
 */
public class QueryFactoryImpl extends EFactoryImpl implements QueryFactory {
    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @deprecated
     * @generated
     */
    @Deprecated
    public static QueryPackage getPackage() {
        return QueryPackage.eINSTANCE;
    }

    /**
     * Creates the default factory implementation. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    public static QueryFactory init() {
        try {
            QueryFactory theQueryFactory = (QueryFactory) EPackage.Registry.INSTANCE.getEFactory("http://abstratt.com/mdd/1.0.0/query");
            if (theQueryFactory != null) {
                return theQueryFactory;
            }
        } catch (Exception exception) {
            EcorePlugin.INSTANCE.log(exception);
        }
        return new QueryFactoryImpl();
    }

    /**
     * Creates an instance of the factory. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    public QueryFactoryImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public EObject create(EClass eClass) {
        switch (eClass.getClassifierID()) {
        case QueryPackage.QUERY:
            return createQuery();
        case QueryPackage.JOIN:
            return createJoin();
        case QueryPackage.PROPERTY_REFERENCE:
            return createPropertyReference();
        case QueryPackage.VARIABLE_REFERENCE:
            return createVariableReference();
        default:
            throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Join createJoin() {
        JoinImpl join = new JoinImpl();
        return join;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public PropertyReference createPropertyReference() {
        PropertyReferenceImpl propertyReference = new PropertyReferenceImpl();
        return propertyReference;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Query createQuery() {
        QueryImpl query = new QueryImpl();
        return query;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public VariableReference createVariableReference() {
        VariableReferenceImpl variableReference = new VariableReferenceImpl();
        return variableReference;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public QueryPackage getQueryPackage() {
        return (QueryPackage) getEPackage();
    }

} // QueryFactoryImpl
