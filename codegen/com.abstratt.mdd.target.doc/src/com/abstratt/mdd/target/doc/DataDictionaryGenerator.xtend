package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.google.common.base.Function
import java.util.Set
import org.apache.commons.lang3.StringUtils
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.StateMachine

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import com.abstratt.kirra.mdd.target.base.AbstractGenerator

class DataDictionaryGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generate() {
        val entities = this.entities
        val testClasses = repository.getTopLevelPackages(null).map[ownedTypes.filter[it.testClass]].flatten().map[it as Class].toSet
        val entityPackages = entities.map[package].toSet
        val applicationLabel = KirraHelper.getApplicationLabel(repository)
        '''
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          «generateBootstrapLinks»
          <title>«applicationLabel» - Data Dictionary</title>
        </head>
        <body>
        <div class="container-fluid">
        <h1>«applicationLabel»</h1>
        «generateRow[entityPackages.generatePackageIndex()]»
        «entityPackages.generateMany[ appPackage | generateRow[generateEntityIndex(appPackage)]]»
        <h2>Entities</h2>
        «entities.generateMany[ entity |
            '''
            «generateRow[generateEntity(entity)]»
            '''
        ]»
        «IF !testClasses.empty»
        <h2>Scenarios</h2>
        «testClasses.generateMany[ testClass |
            '''
            «generateRow[generateTestClass(testClass)]»
            '''
        ]»
        «ENDIF»
        </div>
        </body>
        </html>
        '''
    }
    
    def CharSequence generatePackageIndex(Set<Package> packages) {
        '''
        
        <div>
        <h2>Packages</h2>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th>Namespace</th>
                    <th>Entities</th>
                </tr>
            </thead>
            <tbody>
                «packages.generateMany[package_ | '''
                <tr>
                    <td><a href="#«package_.qualifiedName»">«package_.qualifiedName»</a></td>
                    <td>«package_.ownedTypes.filter[entity].generateMany([ entity |
                        '''<a href="#«entity.qualifiedName»">«entity.name»</a>'''
                    ], ', ')»
                </tr>
                ''']»
            </tbody>
        </table>
        </div>
        '''
    }
    
    def CharSequence generateRow(Function<Void, CharSequence> wrapped) {
        '''
        <div class="row">
        <div class="col-sm-9 col-md-12 main">        
        «wrapped.apply(null)»
        </div>
        </div>
        '''
    }
    
    def generateBootstrapLinks() {
        '''
        <script   src="https://code.jquery.com/jquery-1.12.3.min.js"   integrity="sha256-aaODHAgvwQW1bFOGXMeX+pC4PZIPsvn2h1sArYOhgXQ="   crossorigin="anonymous"></script>  
        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
        <!-- Optional theme -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">
        <!-- Latest compiled and minified JavaScript -->
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>
        '''
    }
    
    def CharSequence generateEntityIndex(Package appPackage) {
        
        val entities = appPackage.ownedTypes.filter[entity]
        '''
        <div>
        <h3>Package: «appPackage.qualifiedName»</h3>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th>Entity</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                «entities.generateMany[entity | '''
                <tr>
                    <td><a href="#«entity.qualifiedName»">«entity.name»</a></td>
                    <td>«entity.description»</td>
                </tr>
                ''']»
            </tbody>
        </table>
        <h4>Class diagram</h4>
        <img src="«appPackage.toJavaPackage»-classes.png"></img>
        <h4>State diagram</h4>
        <img src="«appPackage.toJavaPackage»-state.png"></img>
        </div>
        '''
    }
    
    def CharSequence generateEntity(Class entity) {
    	val entityRelationships = entity.entityRelationships
    	val entityProperties = entity.properties
    	val entityActions = entity.actions
    	val entityQueries = entity.queries
    	val stateMachine = entity.findStateProperties().head?.type as StateMachine
    	
        '''
        <a name="«entity.qualifiedName»"></a>
        <h3>Entity: «entity.name» (from «entity.package.name»)</h3>
        «IF !StringUtils.isBlank(entity.description)»<blockquote>«entity.description»</blockquote>«ENDIF»
        «IF !entityProperties.empty»
        <h4>Properties</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Property</th>
                    <th width="10%">Type</th>
                    <th width="10%">Required</th>
                    <th width="10%">Initializable</th>
                    <th width="10%">Editable</th>
                    <th width="50%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityProperties.generateMany[property | '''
                <tr>
                    <td>«property.asLabel»</td>
                    <td>«property.type.name»</a></td>
                    <td>«if (property.required) 'yes' else 'no'»</a></td>
                    <td>«if (property.initializable) 'yes' else 'no'»</a></td>
                    <td>«if (property.editable) 'yes' else 'no'»</a></td>
                    <td>
                    <table>
                    <tr><td>
                    «property.generateDescription»
                    </td></tr>
                    «IF property.derived»
                    <tr><th>
                    Computed value
                    </th></tr>
                    <tr><td>
                    <pre>«new PseudoCodeActivityGenerator().generateDerivation(property)»</pre>
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityRelationships.empty»
        <h4>Relationships</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Relationhip</th>
                    <th width="5%">Related Entity</th>
                    <th width="5%">Required</th>
                    <th width="5%">Multiple</th>
                    <th width="75%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityRelationships.generateMany[relationship | '''
                <tr>
                    <td>«relationship.asLabel»</td>
                    <td><a href="#«relationship.type.qualifiedName»">«relationship.type.name»</a></td>
                    <td>«if (relationship.required) 'yes' else 'no'»</a></td>
                    <td>«if (relationship.multiple) 'yes' else 'no'»</a></td>
                    <td>
                    «relationship.generateDescription»
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityActions.empty»
        <h4>Actions</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Action</th>
                    <th width="20%">Parameters</th>
                    <th width="70%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityActions.generateMany[action | '''
                <tr>
                    <td>«action.asLabel»</td>
                    <td>«IF action.parameters.inputParameters.empty»-«ELSE»«action.parameters.inputParameters.generateMany(['''<p>«name» («type.name»)</p>'''])»«ENDIF»</td>
                    <td>
                    <table>
                    <tr><td>
                    «action.generateDescription»
                    </td></tr>
                    «IF stateMachine != null && !stateMachine.findStatesForCalling(action).empty»
                    <tr><th>
                    Valid state(s)
                    </th></tr>
                    <tr><td>
                    «stateMachine.findStatesForCalling(action).map[state | state.name].join(', ')»
                    </td></tr>
                    «ENDIF»
                    «IF !action.methods.empty && action.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    <pre>
                    «action.methods.generateMany[ behavior |
                        new PseudoCodeActivityGenerator().generateActivity(behavior as Activity)
                    ].toString.trim()»
                    </pre>
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityQueries.empty»
        <h4>Queries</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Query</th>
                    <th width="20%">Parameters</th>
                    <th width="70%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityQueries.generateMany[query | '''
                <tr>
                    <td>«query.asLabel»</td>
                    <td>«IF query.parameters.inputParameters.empty»-«ELSE»«query.parameters.inputParameters.generateMany(['''<p>«name» («type.name»)</p>'''])»«ENDIF»</td>
                    <td>
                    <table>
                    <tr><td>
                    «query.generateDescription»
                    </td></tr>
                    «IF !query.methods.empty && query.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    <pre>
                    «query.methods.generateMany[ behavior |
                        new PseudoCodeActivityGenerator().generateActivity(behavior as Activity)
                    ].toString.trim()»
                    </pre>
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»        
        '''
    }
    
    def CharSequence generateTestClass(Class testClass) {
    	val testCases = testClass.operations.filter[testCase]
        '''
        «IF !testCases.empty»
        <a name="«testClass.qualifiedName»"></a>
        <h3>Test class: «testClass.name» (from «testClass.package.name»)</h3>
        «IF !StringUtils.isBlank(testClass.description)»<blockquote>«testClass.description»</blockquote>«ENDIF»
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="25%">Test case</th>
                    <th width="75%">Description</th>
                </tr>
            </thead>
            <tbody>
                «testCases.generateMany[testCase | '''
                <tr>
                    <td>«testCase.asLabel»</td>
                    <td>
                    <table>
                    <tr><td>
                    «testCase.generateDescription»
                    </td></tr>
                    «IF !testCase.methods.empty && testCase.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    <pre>
                    «testCase.methods.generateMany[ behavior |
                        new PseudoCodeActivityGenerator().generateActivity(behavior as Activity)
                    ].toString.trim()»
                    </pre>
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        '''
    }
    
    def CharSequence generateDescription(Element element) {
        val description = element.description
        if (description?.length > 0) description else '-' 
    }
    
    def String asLabel(NamedElement element) {
        KirraHelper.getLabel(element)
    }
    
}