package com.github.nmuzhichin.jsonrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method with this annotation sets
 * a custom behavior for error handling.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonRpcError {
    /**
     * The class of an exception.
     * NOTE: The class MUST has the public default constructor with empty signature.
     */
    Class<? extends Throwable> value();

    /**
     * The behavior for the exception: wrapping or throwing.
     */
    Mode mode() default Mode.WRAP;

    enum Mode {
        /**
         * Wrapping caught exception to specified without throwing.
         */
        WRAP,

        /**
         * Throw the specified exception instead of the caught one.
         */
        THROW
    }
}
