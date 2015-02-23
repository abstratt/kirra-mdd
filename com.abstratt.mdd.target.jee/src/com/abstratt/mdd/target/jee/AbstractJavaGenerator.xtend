package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import java.util.Arrays
import java.util.Deque
import java.util.LinkedList
import java.util.List
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AnyReceiveEvent
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.EnumerationLiteral
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.LinkEndData
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Pseudostate
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.SignalEvent
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.TimeEvent
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction
import org.eclipse.uml2.uml.Variable
import org.eclipse.uml2.uml.Vertex

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*

abstract class AbstractJavaGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    private Deque<String> selfReference = new LinkedList(Arrays.asList("this"));

    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }

    def generateComment(Element element) {
        if (!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).
                split('<br>').map['''* «it»'''].join('\n')
            '''
                /**
                 «reformattedParagraphs»
                 */
            '''
        }
    }

    def String packagePrefix(Classifier contextual) {
        contextual.nearestPackage.toJavaPackage
    }

    def String toJavaPackage(Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper) {
        return items.generateMany(mapper, '\n')
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper, String separator) {
        return items.map[mapper.apply(it)].join(separator)
    }

    def CharSequence generateActivity(Activity activity) {
        '''
            «generateActivityRootAction(activity)»
        '''
    }

    def dispatch CharSequence generateAction(Action toGenerate) {
        if (toGenerate.cast)
            toGenerate.sourceAction.generateAction
        else
            generateActionProper(toGenerate)
    }

    def generateActivityRootAction(Activity activity) {
        val rootActionGenerated = generateAction(activity.rootAction)
        '''
            «rootActionGenerated»
        '''
    }

    def dispatch CharSequence generateAction(Void input) {
        throw new NullPointerException;
    }

    def dispatch CharSequence generateAction(InputPin input) {
        generateActionProper(input.sourceAction)
    }

    def CharSequence generateActionProper(Action toGenerate) {
        doGenerateAction(toGenerate)
    }

    def generateStatement(Action statementAction) {
        val isBlock = statementAction instanceof StructuredActivityNode
        val generated = generateAction(statementAction)
        if (isBlock)
            // actually a block
            return generated

        // else generate as a statement
        '''«generated»;'''
    }

    def dispatch CharSequence doGenerateAction(Action action) {

        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence doGenerateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') '''return «generateAction(action.value)»''' else '''«action.variable.name» = «generateAction(
            action.value)»'''
    }
    
    def dispatch CharSequence doGenerateAction(TestIdentityAction action) {
        '''«generateTestidentityAction(action)»'''
    }
    
    def generateTestidentityAction(TestIdentityAction action) {
        '''«generateAction(action.first)» == «generateAction(action.second)»'''
    }
        
    def dispatch CharSequence doGenerateAction(DestroyLinkAction action) {
        generateDestroyLinkAction(action)
    }
    
    def generateDestroyLinkAction(DestroyLinkAction action) {
        val endData = action.endData.head
        '''
        «endData.value.generateAction».«endData.end.otherEnd.name» = null;
        «endData.value.generateAction» = null'''
    }
    
    def generateUnsetLinkEnd(List<LinkEndData> sides) {
        val thisEnd = sides.get(0).end
        val otherEnd = sides.get(1).end
        val thisEndAction = sides.get(0).value
        val otherEndAction = sides.get(1).value
        '''
        «generateLinkDestruction(otherEndAction, thisEnd, thisEndAction, otherEnd, true)»
        «generateLinkDestruction(thisEndAction, otherEnd, otherEndAction, thisEnd, false)»'''
    }
    
    def CharSequence generateLinkDestruction(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        '''
        «generateAction(otherEndAction)».«thisEnd.name»
        «IF thisEnd.multivalued»
        .remove(«generateAction(thisEndAction)»)
        «ELSE»
        = null
        «ENDIF»
        «IF addSemiColon && otherEnd.navigable»;«ENDIF»'''
    }
    
    def dispatch CharSequence doGenerateAction(CreateLinkAction action) {
        generateCreateLinkAction(action) 
    }
    
    def generateCreateLinkAction(CreateLinkAction action) {
        generateSetLinkEnd(action.endData)
    }
    
    def generateSetLinkEnd(List<LinkEndData> sides) {
        val thisEnd = sides.get(0).end
        val otherEnd = sides.get(1).end
        val thisEndAction = sides.get(0).value
        val otherEndAction = sides.get(1).value
        '''
        «generateLinkCreation(otherEndAction, thisEnd, thisEndAction, otherEnd, true)»
        «generateLinkCreation(thisEndAction, otherEnd, otherEndAction, thisEnd, false)»'''
    }
    
    def CharSequence generateLinkCreation(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        '''
        «generateAction(otherEndAction)».«thisEnd.name»«IF thisEnd.multivalued».add(«ELSE» = «ENDIF»«generateAction(thisEndAction)»«IF thisEnd.multivalued»)«ENDIF»«IF addSemiColon && otherEnd.navigable»;«ENDIF»'''
    }    
    

    def dispatch CharSequence doGenerateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }

    protected def CharSequence generateCallOperationAction(CallOperationAction action) {
        val operation = action.operation
        val classifier = action.operation.class_

        // some operations have no class
        if (classifier == null || classifier.package.hasStereotype("ModelLibrary"))
            generateBasicTypeOperationCall(classifier, action)
        else {
            val target = if(operation.static) generateClassReference(classifier) else generateAction(action.target)
            '''«target».«operation.name»(«action.arguments.map[generateAction].join(', ')»)'''
        }
    }

    def CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation
        val operator = switch (operation.name) {
            case 'add': '+'
            case 'subtract': '-'
            case 'multiply': '*'
            case 'divide': '/'
            case 'minus': '-'
            case 'and': '&&'
            case 'or': '||'
            case 'not': '!'
            case 'lowerThan': '<'
            case 'greaterThan': '>'
            case 'lowerOrEquals': '<='
            case 'greaterOrEquals': '>='
            case 'notEquals': '!=='
            case 'equals': '=='
            case 'same': '=='
        }
        if (operator != null)
            switch (action.arguments.size()) {
                // unary operator
                case 0: '''«operator»(«generateAction(action.target)»)'''
                case 1: '''«generateAction(action.target)» «operator» «generateAction(action.arguments.head)»'''
                default: '''Unsupported operation «action.operation.name»'''
            }
        else if (classifier == null) '''Unsupported null target operation "«operation.name»"''' else
            switch (classifier.name) {
                case 'Date':
                    switch (operation.name) {
                        case 'year': '''(«generateAction(action.target)».getYear() + 1900L)'''
                        case 'month': '''«generateAction(action.target)».getMonth()'''
                        case 'day': '''«generateAction(action.target)».getDate()'''
                        case 'today':
                            'new Date()'
                        case 'now':
                            'new Date()'
                        case 'transpose': '''new Date(«generateAction(action.target)».getTime() + «generateAction(
                            action.arguments.head)»)'''
                        case 'differenceInDays': '''(«generateAction(action.arguments.head)».getTime() - «generateAction(
                            action.target)») / (1000*60*60*24)'''
                        default: '''Unsupported Date operation «operation.name»'''
                    }
                case 'Duration': {
                    val period = switch (operation.name) {
                        case 'days': '* 1000 * 60 * 60 * 24'
                        case 'hours': '* 1000 * 60 * 60'
                        case 'minutes': '* 1000 * 60'
                        case 'seconds': '* 1000'
                        case 'milliseconds': ''
                        default: '''Unsupported Duration operation: «operation.name»'''
                    }
                    '''«generateAction(action.arguments.head)»«period» /*«operation.name»*/'''
                }
                case 'Memo': {
                    switch (operation.name) {
                        case 'fromString': generateAction(action.arguments.head)
                        default: '''Unsupported Memo operation: «operation.name»'''
                    }
                }
                case 'Collection': {
                    switch (operation.name) {
                        case 'size': '''«generateAction(action.target)».size()'''
                        case 'forEach': generateCollectionForEach(action)
                        default: '''«if (operation.getReturnResult != null) 'null' else ''» /*Unsupported Collection operation: «operation.name»*/'''
                    }
                }
                case 'System': {
                    switch (operation.name) {
                        case 'user': '''null /* TBD */'''
                        default: '''Unsupported System operation: «operation.name»'''
                    }
                }
                default: '''Unsupported classifier «classifier.name» for operation «operation.name»'''
            }
    }
    
    def CharSequence generateCollectionForEach(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».forEach(«closure.generateActivityAsExpression»)'''
    }

    def dispatch CharSequence doGenerateAction(StructuredActivityNode node) {
        val container = node.eContainer

        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''

        // default path, generate as a statement
        generateStructuredActivityNodeAsBlock(node)
    }
    
    def dispatch CharSequence doGenerateAction(SendSignalAction action) {
        generateSendSignalAction(action)
    }
    
    def generateSendSignalAction(SendSignalAction action) {
        val target = action.target
        val methodName = action.signal.name.toFirstLower
        '''«generateAction(target)».«methodName»(«action.arguments.map[generateAction].join(', ')»)'''
    }
    
    def dispatch CharSequence doGenerateAction(ReadStructuralFeatureAction action) {
        generateReadStructuralFeatureAction(action)
    }
    
    def generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        val feature = action.structuralFeature as Property
        if (action.object == null) {
            val clazz = (action.structuralFeature as Property).class_
            '''«clazz.name».«feature.name»'''
        } else {
            '''«generateAction(action.object)».«feature.name»'''
        }
    }

    def dispatch CharSequence doGenerateAction(AddStructuralFeatureValueAction action) {
        generateAddStructuralFeatureValueAction(action)
    }

    def generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        val target = action.object
        val value = action.value
        val featureName = action.structuralFeature.name

        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
    }
    
    def dispatch CharSequence doGenerateAction(ValueSpecificationAction action) {
        '''«action.value.generateValue»'''
    }
    

    def dispatch CharSequence doGenerateAction(CreateObjectAction action) {
        generateCreateObjectAction(action)
    }

    def generateCreateObjectAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }

    def generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        '''«generateVariables(node)»«node.findTerminals.map[generateStatement].join('\n')»'''
    }

    def generateVariables(StructuredActivityNode node) {
        generateVariableBlock(node.variables)
    }

    def generateVariableBlock(Iterable<Variable> variables) {
        if(variables.empty) '' else variables.map['''«type.toJavaType» «name»;'''].join('\n') + '\n'
    }

    def dispatch CharSequence doGenerateAction(ReadVariableAction action) {
        generateReadVariableValueAction(action)
    }

    def generateReadVariableValueAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def dispatch CharSequence doGenerateAction(ReadSelfAction action) {
        generateReadSelfAction(action)
    }

    def CharSequence generateReadSelfAction(ReadSelfAction action) {
        generateSelfReference()
    }

    def generateSelfReference() {
        selfReference.peek()
    }

    def toJavaType(Type type) {
        switch (type.kind) {
            case Entity:
                type.name
            case Enumeration:
                if (type.namespace instanceof Package) type.name else type.namespace.name + '.' + type.name
            case Primitive:
                switch (type.name) {
                    case 'Integer': 'Long'
                    case 'Double': 'Double'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Memo': 'String'
                    case 'Boolean': 'Boolean'
                    default: 'UNEXPECTED TYPE: «type.typeName»'
                }
            default: '''UNEXPECTED KIND: «type.kind»'''
        }
    }

    def generateClassReference(Classifier classifier) {
        classifier.name
    }
    
    def dispatch generateState(State state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «IF (state.entry != null)»
            @Override void onEntry(«entity.name» instance) {
                «(state.entry as Activity).generateActivity»
            }«
            ENDIF»
            «IF (state.exit != null)»
            @Override void onExit(«entity.name» instance) {
                «(state.exit as Activity).generateActivity»
            }
            «ENDIF»
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        '''.toString.trim
    }
    
    def dispatch generateState(Pseudostate state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        '''.toString.trim 
    }
    
    def generateStateEventHandler(Vertex state, StateMachine stateMachine, Class entity) {
        '''
        @Override void handleEvent(«entity.name» instance, «stateMachine.name»Event event) {
            «IF (!state.outgoings.empty)»
            switch (event) {
                «state.findTriggersPerEvent.entrySet.generateMany[pair |
                    val event = pair.key
                    val triggers = pair.value
                    '''
                    case «event.generateEventName» :
                        «triggers.generateMany
                            [ trigger |
                            val transition = trigger.eContainer as Transition
                            '''
                            «IF (transition.guard != null)»
                            if («transition.guard.generatePredicate») {
                                «transition.generateTransition»
                                break;
                            }
                            «ELSE»
                            «transition.generateTransition»
                            «ENDIF»
                            '''
                        ]»
                        break;
                    ''' 
                ]»
            }
            «ELSE»
            // this is a final state
            «ENDIF»     
        }                       
        '''
    }
    
    def generateTransition(Transition transition) {
        '''
        «IF (transition.effect != null)»
        «(transition.effect as Activity).generateActivity»
        «ENDIF»
        doTransitionTo(instance, «transition.target.name»);
        '''
    }
    
    def generateStateMachine(StateMachine stateMachine, Class entity) {
        val stateAttribute = entity.findStateProperties.head
        if (stateAttribute == null)
            return ''
        val triggersPerEvent = stateMachine.findTriggersPerEvent
        val eventNames = triggersPerEvent.keySet.map[it.generateEventName]
        
        selfReference.push("instance")
        val generated = '''
        enum «stateMachine.name» {
            «stateMachine.vertices.generateMany([generateState(it, stateMachine, entity)],',\n')»;
            void onEntry(«entity.name» instance) {
                // no entry behavior by default
            }
            void onExit(«entity.name» instance) {
                // no exit behavior by default
            }
            /** Each state implements handling of events. */
            abstract void handleEvent(«entity.name» instance, «stateMachine.name»Event event);
            /** 
                Performs a transition.
                @param instance the instance to update
                @param newState the new state to transition to 
            */
            final void doTransitionTo(«entity.name» instance, «stateMachine.name» newState) {
                instance.«stateAttribute.name».onExit(instance);
                instance.«stateAttribute.name» = newState;
                instance.«stateAttribute.name».onEntry(instance);
            }
        }
        
        enum «stateMachine.name»Event {
            «eventNames.join(',\n')»
        }
        '''
        selfReference.pop()
        return generated
    }

    def generateEvent(Class entity, Property stateAttribute, Event event, List<Trigger> triggers) {
        '''«event.generateEventName»'''
    }

    def generateEventName(Event e) {
        switch (e) {
            CallEvent : e.operation.name.toFirstUpper
            SignalEvent : e.signal.name
            TimeEvent : '_time'
            AnyReceiveEvent : '_any'
            default : unsupportedElement(e)
        }
    }
    
    def generatePredicate(Constraint predicate) {
        val predicateActivity = predicate.specification.resolveBehaviorReference as Activity
        predicateActivity.generateActivityAsExpression        
    }
    
    def generateActivityAsExpression(Activity toGenerate) {
        val statements = toGenerate.rootAction.findStatements
        if (statements.size != 1)
            throw new IllegalArgumentException("Single statement activity expected")
        val singleStatement = statements.head
        if (!(singleStatement instanceof AddVariableValueAction)) {
            val returnsValue = toGenerate.closureReturnParameter != null
            val optionalSemicolon = if (returnsValue) '' else ';'  
            return '''
                («toGenerate.closureInputParameters.generateMany([name], ', ')») -> {
                    «singleStatement.generateAction»«optionalSemicolon»
                }'''
        }
        singleStatement.sourceAction.generateAction
    }

    def static CharSequence unsupportedElement(Element e) {
        unsupportedElement(e, if (e instanceof NamedElement) e.qualifiedName else null)
    }
    
    def static CharSequence unsupportedElement(Element e, String message) {
        '''<UNSUPPORTED: «e.eClass.name»> «if (message != null) '''(«message»)''' else ''»>'''
    }

    def generateValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString : switch (value.type.name) {
                case 'String' : '''"«value.stringValue»"'''
                case 'Integer' : '''«value.stringValue»L'''
                case 'Double' : '''«value.stringValue»'''
                case 'Boolean' : '''«value.stringValue»'''
                default : '''UNKNOWN: «value.stringValue»'''
            }
            LiteralBoolean : '''«value.booleanValue»'''
            LiteralNull : switch (value) {
                case value.isVertexLiteral : '''"«value.resolveVertexLiteral.name»"'''
                default : 'null'
            }
            OpaqueExpression case value.behaviorReference : (value.resolveBehaviorReference as Activity).generateActivityAsExpression
            InstanceValue case value.instance instanceof EnumerationLiteral: '''"«value.instance.name»"'''
            default : unsupportedElement(value)
        }
    }
    
    def generateDefaultValue(Type type) {
        switch (type) {
            StateMachine : '''«type.name».«type.initialVertex.name»'''
            Enumeration : '''«type.name».«type.ownedLiterals.head.name»'''
            Class : switch (type.name) {
                case 'Boolean' : 'false'
                case 'Integer' : '0L'
                case 'Double' : '0.0'
                case 'Date' : 'new Date()'
            }
            default : null
        }
    }
    

}