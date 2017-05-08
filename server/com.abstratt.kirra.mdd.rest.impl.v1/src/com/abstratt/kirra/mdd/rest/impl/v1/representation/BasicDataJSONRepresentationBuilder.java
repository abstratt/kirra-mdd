package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.abstratt.kirra.TopLevelElement;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.mdd.rest.KirraReferenceBuilder;

public abstract class BasicDataJSONRepresentationBuilder<T extends TupleJSONRepresentation> {
    public void build(T representation, KirraReferenceBuilder refBuilder, TopLevelElement element, Tuple instance) {
        if (element != null && element.getTypeRef() != null)
            representation.typeName = element.getTypeRef().getFullName();
        representation.values = new HashMap<String, Object>();
        for (Entry<String, Object> entry : instance.getValues().entrySet()) {
            Object value = convertValue(entry.getValue());
            representation.values.put(entry.getKey(), value);
        }
    }

	private Object convertValue(Object value) {
		if (value instanceof Tuple) {
		    Tuple childTuple = (Tuple) value;
		    TupleJSONRepresentation childRepr = new TupleJSONRepresentation();
		    new TupleJSONRepresentationBuilder().build(childRepr, null, null, childTuple);
		    return childRepr;
		} else if (value instanceof Collection<?>) {
        	List<?> values = (List<?>) ((Collection) value).stream().map(it -> convertValue(it)).collect(Collectors.toList());
			return values;
		} else {
		    return value;
		}
	}

}
