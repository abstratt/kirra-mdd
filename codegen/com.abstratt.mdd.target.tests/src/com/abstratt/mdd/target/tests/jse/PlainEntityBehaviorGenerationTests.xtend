package com.abstratt.mdd.target.tests.jse

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest


class PlainEntityBehaviorGenerationTests extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }

    protected def buildModel(CharSequence fragment) {
        var source = '''
            model mymodel;
            
                association ManyMyClass1OneMyClass2
                    role MyClass2.related1B;
                    role MyClass1.optionalRelated1;
                end;
                
                association ManyMyClass2OneMyClass1
                    role MyClass2.related1C;
                    role MyClass1.optionalRelated2;
                end;
                
                class MyClass2
                    attribute related1A : MyClass1[*];
                    attribute related1B : MyClass1[*];
                    attribute related1C : MyClass1[0,1];
                end;
            
                class MyClass1
                    reference related : MyClass2[1,1] opposite related1A;
                    attribute optionalRelated1 : MyClass2[0,1];
                    attribute optionalRelated2 : MyClass2[0,*];
                    derived attribute aDerivedAttribute : Integer := { 1 };
                    attribute anAttribute : Integer;
                    attribute anOptionalAttribute : Integer[0,1];
                    query aQuery() : Integer;
                    operation anAction();
                    «fragment»
                end;
            end.
         '''
        parseAndCheck(source)
    }
    
    protected def testBodyGeneration(CharSequence operation, CharSequence expected) {
        testBodyGeneration(operation, expected, 'mymodel::MyClass1::op1')
    }
    protected def testBodyGeneration(CharSequence fragment, CharSequence expected, String operationName) {
        buildModel(fragment)
        val op1 = getOperation(operationName) 
        val generated = (createEntityGenerator()).generateOperationBody(op1)
        AssertHelper.assertStringsEqual(
            expected.toString, generated.toString)
    }
    protected def testBodyGenerationForDerivedAttribute(CharSequence fragment, CharSequence expected, String attributeName) {
        buildModel(fragment)
        val attr1 = getProperty(attributeName) 
        val generated = (createEntityGenerator()).generateDerivedAttributeComputation(attr1)
        AssertHelper.assertStringsEqual(
            expected.toString, generated.toString)
    }
    
    protected def PlainEntityGenerator createEntityGenerator() {
        new PlainEntityGenerator(repository)
    }    

    /** See #179 and related issues. */
    def testPrecondition_RequiredParameter() {
        testBodyGeneration('''
            operation op1(par1 : Integer) 
                precondition (par1) { par1 >= 0 };
            ''',
            '''
            if (!(par1 >= 0L)) {
                // precondition violated 
                throw new ConstraintViolationException();
            }
            '''
        )
    }
    def testPrecondition_OptionalParameter() {
        testBodyGeneration('''
            operation op1(par1 : Integer[0,1])
                precondition (par1) { (par1 ?: 0) >= 0 };
            ''',
            '''
            if (par1 != null) {
                if (!(Optional.ofNullable(par1).orElse(0L)>=0L)) {
                    //precondition violated
                    throw new ConstraintViolationException();
                }
            }
            '''
        )
    }
    
    def testPrecondition_RequiredDerivedAttribute() {
        testBodyGeneration('''
            operation op1() 
                precondition { self.aDerivedAttribute >= 0 };
            ''',
            '''
            if (!(this.getADerivedAttribute() >= 0L)) {
                // precondition violated 
                throw new ConstraintViolationException();
            }
            '''
        )
    }


    def testPrecondition_RequiredAttribute() {
        testBodyGeneration('''
            operation op1() 
                precondition { self.anAttribute >= 0 };
            ''',
            '''
            if (!(this.getAnAttribute() >= 0L)) {
                // precondition violated 
                throw new ConstraintViolationException();
            }
            '''
        )
    }
    
    def testDateToday() {
        testBodyGeneration('''
            operation op1();
            begin
                var today;
                today := Date#today();
            end;
            ''',
            '''Date today=java.sql.Date.valueOf(java.time.LocalDate.now());'''
        )
    }
    def testDateArithmetic() {
        testBodyGeneration('''
            operation op1(refDate : Date);
            begin 
                var dayBefore;
                dayBefore := refDate.transpose(Duration#days(-1));
            end;
            ''',
            '''Date dayBefore = new Date(refDate.getTime()+-1L*(1000*60*60*24)/*days*/);'''
        )
    }
    
    def testIntegerAdd() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer);
            begin
                var result;
                result := val1 + val2;
            end;
            ''',
            '''long result = val1 + val2;'''
        )
    }
    def testIntegerAddOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := !!val1 + !!val2;
            end;
            ''',
            '''long result = Objects.requireNonNull(val1) + Objects.requireNonNull(val2);'''
        )
    }
    def testIntegerMultiplyOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := !!val1 * !!val2;
            end;
            ''',
            '''long result = Objects.requireNonNull(val1) * Objects.requireNonNull(val2);'''
        )
    }
    def testIntegerDivideOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := !!val1 / !!val2;
            end;
            ''',
            '''long result = Objects.requireNonNull(val1) / Objects.requireNonNull(val2);'''
        )
    }
    def testDoubleAdd() {
        testBodyGeneration('''
            operation op1(val1 : Double, val2 : Double);
            begin
                var result;
                result := val1 + val2;
            end;
            ''',
            '''double result = val1 + val2;'''
        )
    }
    def testDoubleAddOptionalValues() {
        testBodyGeneration('''
           operation op1(val1 : Double[0,1], val2 : Double[0,1]);
           begin
               var result;
               result := !!val1 + !!val2;
           end;
           ''',
           '''double result = Objects.requireNonNull(val1) + Objects.requireNonNull(val2);'''
        )
    }
    def testDoubleMultiplyOptionalValues_argumentCastAsRequired() {
        testBodyGeneration('''
            operation op1(val1 : Double[0,1], val2 : Double[0,1]);
            begin
                var result;
                result := (val1 ?: 0) * !!val2;
            end;
            ''',
            '''double result = Optional.ofNullable(val1).orElse(0L) * Objects.requireNonNull(val2);'''
        )
    }
    def testDoubleMultiplyOptionalValueByRequired() {
        testBodyGeneration('''
            operation op1(val1 : Double[0,1], val2 : Double);
            begin
                var result;
                result := !!val1 * val2;
            end;
            ''',
            '''double result = Objects.requireNonNull(val1) * val2;'''
        )
    }
    def testDoubleDivideOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Double[0,1], val2 : Double[0,1]);
            begin
                var result;
                result := (val1 ?: 0) / val2 ?: 1;
            end;
            ''',
            '''double result = Optional.ofNullable(val1).orElse(0L) / Optional.ofNullable(val2).orElse(1L);'''
        )
    }
    def testBooleanSafeAnd_optionalResult() {
        testBodyGeneration('''
            operation op1(val1 : Boolean[0,1], val2 : Boolean[0,1]);
            begin
                var result : Boolean[0,1];
                result := !!val1 and !!val2;
            end;
            ''',
            '''Boolean result = Objects.requireNonNull(val1) && Objects.requireNonNull(val2);'''
        )
    }
    def testBooleanSafeAnd_requiredResult() {
        testBodyGeneration('''
            operation op1(val1 : Boolean[0,1], val2 : Boolean[1]);
            begin
                var result : Boolean[1];
                result := !!val1 and val2;
            end;
            ''',
            '''boolean result = Objects.requireNonNull(val1) && val2;'''
        )
    }
    def testComparison() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer); 
            begin
                var result;
                result := val1 >= val2;
            end;
            ''',
            '''boolean result = val1 >= val2;'''
        )
    }
    def testSafeComparisonBothOptional_castAsRequired() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin
                var result;
                result := !!val1 >= !!val2;
            end;
            ''',
            '''boolean result = Objects.requireNonNull(val1) >= Objects.requireNonNull(val2);'''
        )
    }
    
    def testSafeComparisonBothOptional_nullSafe() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin
                var result;
                result := val1 ?: 0 >= val2 ?: 1;
            end;
            ''',
            '''boolean result = Optional.ofNullable(val1).orElse(0L) >= Optional.ofNullable(val2).orElse(1L);'''
        )
    }
    
    def testSafeComparisonTargetIsOptional() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer);
            begin 
                var result;
                result := !!val1 >= val2;
            end;''',
            '''boolean result = Objects.requireNonNull(val1) >= val2;'''
        )
    }
    def testSafeComparisonArgumentIsOptional_castAsRequired() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer[0,1]); 
            begin
                var result;
                result := val1 >= !!val2;
            end;
            ''',
            '''boolean result=val1>=Objects.requireNonNull(val2);'''
        )
    }
    

    def testUnlink_Required() {
        //TODO-RC shouldn't this be just prevented at compilation time?
        val textuml = '''
            operation op1();
            begin
                self.related := null;
            end;
            '''
        val java = '''
            if (this.related != null) {
                this.related.removeFromRelated1A(this);
                this.setRelated(null);
            }
            '''    
        testBodyGeneration(textuml, java)
    }
    
    def testUnlink_OptionalCurrent() {
        testBodyGeneration('''
        
            operation op1();
            begin
                unlink ManyMyClass1OneMyClass2(related1B := self, optionalRelated1 := self.optionalRelated1);
            end;
            ''', 
            '''
            if (this.optionalRelated1 != null) {
                this.optionalRelated1.removeFromRelated1B(this);
                this.setOptionalRelated1(null);
            }
            '''
        )
    }

    def testReplace_OptionalGiven() {
        testBodyGeneration('''
            operation op1(given : MyClass2);
            begin
                unlink ManyMyClass2OneMyClass1(related1C := self, optionalRelated2 := given);
            end;
            ''', 
            '''
            if (given != null) {
                this.removeFromOptionalRelated2(given);
                given.setRelated1C(null);
            }
            '''
        )
    }

    def testReplace_RequiredGiven() {
        testBodyGeneration('''
            operation op1(given : MyClass2);
            begin
                unlink ManyMyClass2OneMyClass1(related1C := self, optionalRelated2 := given);
            end;
            ''', 
            '''
            this.removeFromOptionalRelated2(given);
            given.setRelated1C(null);
            '''
        )
    }

      
    def testSafeQueryInvocation() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin 
                var result : Integer[0,1];
                result := val1?.aQuery();
            end;''',
            '''Long result = Optional.ofNullable(val1).map(it -> val1.aQuery()).orElse(null);'''
        )
    }
    def testSafeActionInvocation() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                val1?.anAction();
            end;
            ''', 
            '''
            if (val1 != null) {
                val1.anAction();
            }
            '''
        )
    }
    def testAttributeRead() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                var result : Integer;
                result := val1.anAttribute;
            end;
            ''',
            '''long result = val1.getAnAttribute();'''
        )
    }
    def testAttributeReadOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                var result : Integer;
                result := (val1.anOptionalAttribute as Integer);
            end;
            ''',
            '''long result = Objects.requireNonNull(val1.getAnOptionalAttribute());'''
        )
    }
    def testAttributeRead_OptionalSink() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                var result : Integer[0,1];
                result := val1.anAttribute;
            end;
            ''',
            '''Long result = val1.getAnAttribute();'''
        )
    }
    def testAttributeReadOptionalValue_OptionalSink() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                var result : Integer[0,1];
                result := val1.anOptionalAttribute;
            end;
            ''',
            '''Long result = val1.getAnOptionalAttribute();'''
        )
    }
    def testAttributeWrite() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                val1.anAttribute := 1;
            end;
            ''',
            '''val1.setAnAttribute(1L);'''
        )
    }
    def testAttributeWriteOptionalValue_castAsRequired() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1);
            begin
                val1.anAttribute := !!val1.anOptionalAttribute;
            end;
            ''',
            '''val1.setAnAttribute(Objects.requireNonNull(val1.getAnOptionalAttribute()));'''
        )
    }
    
    
    def testSafeAttributeReadOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                var result : Integer;
                result := (val1?.anOptionalAttribute as Integer);
            end;
            ''',
            '''long result = Objects.requireNonNull(Optional.ofNullable(val1).map(it->it.getAnOptionalAttribute()).orElse(null));'''
        )
    }
    
    def testSafeAttributeReadOptionalValue_elvis() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                var result : Integer;
                result := val1?.anOptionalAttribute ?: 33;
            end;
            ''',
            '''long result = Optional.ofNullable(val1).map(it -> it.getAnOptionalAttribute()).orElse(33L);'''
        )
    }
    def testSafeAttributeReadRequiredValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                var result : Integer;
                result := (val1?.anAttribute as Integer);
            end;
            ''',
            '''long result = Objects.requireNonNull(Optional.ofNullable(val1).map(it->it.getAnAttribute()).orElse(null));'''
        )
    }
    def testSafeAttributeWriteOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                val1?.anAttribute := ((val1 as MyClass1).anOptionalAttribute as Integer);
            end;
            ''',
            '''
            if(val1!=null) {
                val1.setAnAttribute(Objects.requireNonNull(Objects.requireNonNull(val1).getAnOptionalAttribute()));
            }
            '''
        )
    }
    
    def testSafeAttributeWriteRequiredValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass1[0,1]);
            begin
                val1?.anAttribute := 1;
            end;
            ''',
            '''
            if (val1 != null) {
                val1.setAnAttribute(1L);
            }
            '''
        )
    }
    
    def testIf() {
        testBodyGeneration('''
            attribute someValue : Integer;
            query op1() : Boolean;
            begin
                if (self.someValue > 0) then
                    return true
                else
                    return false;
            end;
            ''',
            '''
            if (this.getSomeValue() > 0L) {
                return true;
            } else {
                return false;
            }
            '''
        )
    }
    
    def testIfInDerivedAttribute() {
        testBodyGenerationForDerivedAttribute('''
            attribute condition : Boolean;
            derived attribute attr1 : Integer := {
                if (self.condition) then
                    return 1
                else
                    return 2;
            };
            ''',
            '''
            if (this.isCondition()) {
                return 1L;
            } else {
                return 2L;
            }
            ''',
            'mymodel::MyClass1::attr1'
        )
    }
    
}
