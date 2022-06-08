package com.github.nmuzhichin.jsonrpc.api;

/**
 * Listen result of invocation.
 * May mutate result state (ex. null -> Optional).
 */
public interface MutateListener {
    /**
     * Mutate result state.
     */
    Object mutateResultState(Object result);

    /**
     * Listen result before the result mutation.
     */
    default void beforeMutate(Object result) { }

    /**
     * Listen result after the result mutation.
     */
    default void afterMutate(Object result) { }
}
