package com.github.nmuzhichin.jsonrpc.model.response;

import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

/**
 * Helper class for creating response
 */
public abstract class ResponseUtils {
    private ResponseUtils() {
        // Use static methods
    }

    /**
     * Create response by value type.
     */
    public static Response createResponse(final Long requestId, final Object value) {
        return value instanceof Error
                ? new ErrorResponse(requestId, (Error) value)
                : new SuccessResponse(requestId, value);
    }
}
