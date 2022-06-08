package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.annotation.Constraint;
import com.github.nmuzhichin.jsonrpc.internal.exceptions.ValidationException;
import org.apache.commons.lang3.ClassUtils;

import java.util.regex.Pattern;

abstract class ParameterValidate {
    private ParameterValidate() {
        // Static method
    }

    static void validate(final ParameterMetadata parameterMetadata, final Object value) {
        checkOnNull(parameterMetadata, value);
        checkOnType(parameterMetadata, value);
        checkOnStrictType(parameterMetadata, value);
        checkOnMinMax(parameterMetadata, value);
        checkOnPattern(parameterMetadata, value);
    }

    private static void checkOnPattern(final ParameterMetadata parameterMetadata, final Object value) {
        if (ClassUtils.isAssignable(value.getClass(), CharSequence.class)) {

            if (parameterMetadata.containsConstraintType(Constraint.Type.PATTERN)) {
                final String pattern = parameterMetadata.getConstraintByType(Constraint.Type.PATTERN);
                if (!Pattern.matches(pattern, value.toString())) {
                    throw new ValidationException(value + " must be meet the pattern " + pattern);
                }
            }
        }
    }

    private static void checkOnMinMax(final ParameterMetadata parameterMetadata, final Object value) {
        if (ClassUtils.isAssignable(value.getClass(), Number.class, true)) {

            if (parameterMetadata.containsConstraintType(Constraint.Type.MAX)) {
                final String max = parameterMetadata.getConstraintByType(Constraint.Type.MAX);
                if (Long.parseLong(value.toString()) > Long.parseLong(max)) {
                    throw new ValidationException(value + " must be equals or less " + max);
                }
            }

            if (parameterMetadata.containsConstraintType(Constraint.Type.MIN)) {
                final String min = parameterMetadata.getConstraintByType(Constraint.Type.MIN);
                if (Long.parseLong(value.toString()) < Long.parseLong(min)) {
                    throw new ValidationException(value + " must be more or equals " + min);
                }
            }
        }
    }

    private static void checkOnStrictType(final ParameterMetadata parameterMetadata, final Object value) {
        if (parameterMetadata.containsConstraintType(Constraint.Type.STRICT)) {

            final String strictVal = parameterMetadata.getConstraintByType(Constraint.Type.STRICT);
            if (!value.toString().equals(strictVal)) {
                throw new ValidationException(value + " must be strict equals " + strictVal);
            }
        }
    }

    private static void checkOnType(final ParameterMetadata parameterMetadata, final Object value) {
        if (!parameterMetadata.getParameterClass().isAssignableFrom(value.getClass())) {
            throw new ValidationException(value.getClass() + " is not assign from " + parameterMetadata.getParameterClass());
        }
    }

    private static void checkOnNull(final ParameterMetadata parameterMetadata, final Object value) {
        if (value == null) {
            if (parameterMetadata.containsConstraintType(Constraint.Type.NOT_NULL)) {
                throw new ValidationException(parameterMetadata.getParameterName() + " must be present.");
            }
        }
    }
}
