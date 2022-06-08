package com.github.nmuzhichin.jsonrpc.annotation;

import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the presence of named parameters in the called method.
 * Each method parameter must have this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface JsonRpcParam {
    /**
     * Method parameter name.
     * If not set uses the method name obtained by reflection.
     */
    String value() default "";

    boolean nullable() default false;

    /**
     * If {@code true} is specified,
     * the parameter MUST BE predefined by the {@link RpcConsumer}
     * and MAY not be accepted upon {@link com.github.nmuzhichin.jsonrpc.model.request.Request}.
     */
    boolean predefine() default false;

    /**
     * Array of rules for parameter validation.
     *
     * @see Constraint
     */
    Constraint[] constraints() default {};


}
