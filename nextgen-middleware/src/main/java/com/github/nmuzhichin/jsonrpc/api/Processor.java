package com.github.nmuzhichin.jsonrpc.api;

/**
 * This interface defines an annotation processor
 */
public interface Processor {
    /**
     * Do the annotation processing for a given object.
     */
    void process(Object object, Class<?> objectType);
}
