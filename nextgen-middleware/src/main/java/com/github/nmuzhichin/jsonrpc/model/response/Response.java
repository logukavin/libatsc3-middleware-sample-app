package com.github.nmuzhichin.jsonrpc.model.response;

import com.github.nmuzhichin.jsonrpc.model.Identifiable;
import com.github.nmuzhichin.jsonrpc.model.Versionable;
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

import java.util.Optional;

/**
 * When a rpc call is made, the Server MUST reply with a Response,
 * except for in the case of Notifications.
 *
 * @see ErrorResponse
 * @see SuccessResponse
 */
public interface Response extends Versionable, Identifiable {
    /**
     * <ul>
     *     result
     * <li>This member is REQUIRED on success.</li>
     * <li>This member MUST NOT exist if there was an error invoking the method.</li>
     * <li>The value of this member is determined by the method invoked on the Server.</li>
     * </ul>
     * <ul>
     *     error
     * <li>This member is REQUIRED on error.</li>
     * <li>This member MUST NOT exist if there was no error triggered during invocation.</li>
     * <li>The value for this member MUST be an Object as defined in section 5.1.</li>
     * </ul>
     * Either the result member or error member MUST be included, but both members MUST NOT be included.
     */
    Object getBody();

    /**
     * Helper method for returned the optional with the casted result.
     */
    <T> Optional<T> getSuccess(Class<T> casted);

    /**
     * Helper method for returned the optional with the error result.
     */
    Optional<Error> getError();

    /**
     * Check that a response result is error
     */
    boolean isError();

    /**
     * Check that a response result is success
     */
    boolean isSuccess();
}
