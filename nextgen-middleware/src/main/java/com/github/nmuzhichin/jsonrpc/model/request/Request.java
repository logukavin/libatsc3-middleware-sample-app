package com.github.nmuzhichin.jsonrpc.model.request;

import com.github.nmuzhichin.jsonrpc.model.Identifiable;
import com.github.nmuzhichin.jsonrpc.model.Versionable;

import java.util.Map;

/**
 * A rpc call is represented by sending a Request object to a Server.
 * <p>
 * The Server MUST reply with the same value in the Response object if included.
 * This member is used to correlate the context between the two objects.
 * <p>
 * A Notification is a Request object without an "id" member.
 * A Request object that is a Notification signifies the Client's lack of interest in the corresponding Response object,
 * and as such no Response object needs to be returned to the client.
 * <p>
 * The Server MUST NOT reply to a Notification, including those that are within a batch request.
 * <p>
 * Notifications are not confirmable by definition, since they do not have a Response object to be returned.
 * As such, the Client would not be aware of any errors (like e.g. "Invalid params","Internal error").
 *
 * @see CompleteRequest
 * @see Notification
 */
public interface Request extends Versionable, Identifiable {

    /**
     * A String containing the name of the method to be invoked.
     * Method names that begin with the word rpc followed by a period character (U+002E or ASCII 46)
     * are reserved for rpc-internal methods and extensions and MUST NOT be used for anything else.
     */
    String getMethod();

    /**
     * A Structured value that holds the parameter values to be used during the invocation of the method.
     * This member MAY be omitted.
     */
    Map<String, Object> getParams();
}
