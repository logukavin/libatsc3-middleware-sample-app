package com.github.nmuzhichin.jsonrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method with this annotation must participate in processing
 * and be stored in the {@link com.github.nmuzhichin.jsonrpc.api.Context}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonRpcMethod {

    /**
     * The method name for the remote call.
     * If not set uses the method name obtained by reflection.
     */
    String value() default "";

    /**
     * If {@code true} is specified, the method WILL BE cached.
     */
    boolean cacheable() default false;

    /**
     * If {@code true} is specified, the method calls without check on arguments order and validate.
     */
    boolean strictArgsOrder() default false;
}
