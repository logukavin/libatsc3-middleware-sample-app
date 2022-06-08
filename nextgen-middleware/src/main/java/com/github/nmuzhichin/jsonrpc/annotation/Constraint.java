package com.github.nmuzhichin.jsonrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rule for a parameter validation.
 * MAY consists as part of the {@link JsonRpcParam} or set in the parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Constraint {
    /**
     * Value of the constraint.
     * MAY be empty.
     */
    String value() default "";

    /**
     * Type of the constraint.
     *
     * @see Type
     */
    Type type() default Type.NONE;

    /**
     * Constraint type
     */
    enum Type {
        /**
         * Value MUST BE more or equals minimal.
         * Value type MUST BE Numeric
         */
        MIN,

        /**
         * Value MUST BE less or equals maximum.
         * Value type MUST BE Numeric
         */
        MAX,

        /**
         * Value MUST BE matches of the pattern.
         * Value type MUST BE CharSequence
         */
        PATTERN,

        /**
         * Value MUST BE present
         */
        NOT_NULL,

        /**
         * Value MUST BE strict of the value (work for primitive, boxing and String)
         */
        STRICT,

        /**
         * Constraint doesn't used.
         */
        NONE;
    }
}
