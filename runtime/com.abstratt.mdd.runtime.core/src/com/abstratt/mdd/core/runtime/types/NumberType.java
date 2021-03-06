package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public abstract class NumberType<T extends Number> extends PrimitiveType<T> {
    protected NumberType(T value) {
		super(value);
	}

	private static final long serialVersionUID = 1L;
    
    
    public abstract NumberType<?> add(ExecutionContext context, NumberType<?> another);

    public final double asDouble() {
        return value.doubleValue();
    }
    
    protected RealType asReal() {
        return new RealType(asDouble());
    }
    
    public RealType asDouble(ExecutionContext context) {
    	return asReal();
    }
    
    public IntegerType asInteger(ExecutionContext context) {
    	return new IntegerType(value.longValue());
    }

    public abstract NumberType<?> divide(ExecutionContext context, NumberType<?> number);

    @Override
    public BooleanType equals(ExecutionContext context, Type other) {
    	if (other == null) 
    		return BooleanType.FALSE;
        return BooleanType.fromValue(other != null && asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) == 0);
    }

    @Override
    public BooleanType greaterThan(ExecutionContext context, ComparableType other) {
    	if (other == null) 
    		return BooleanType.FALSE;
        return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) > 0);
    }

    @Override
    public BooleanType lowerThan(ExecutionContext context, ComparableType other) {
    	if (other == null) 
    		return BooleanType.FALSE;
        return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) < 0);
    }

    public abstract NumberType<?> multiply(ExecutionContext context, NumberType<?> number);

    public abstract NumberType<?> subtract(ExecutionContext context);

    public abstract NumberType<?> subtract(ExecutionContext context, NumberType<?> another);
}