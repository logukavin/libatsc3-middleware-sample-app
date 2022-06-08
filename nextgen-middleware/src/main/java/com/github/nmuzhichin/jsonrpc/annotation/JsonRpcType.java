package com.github.nmuzhichin.jsonrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Auxiliary annotation for documentation generation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonRpcType {
    /**
     * Custom type with a predefined title.
     */
    String value() default "";
}
