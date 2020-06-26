package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.model.response.Response;

import java.util.List;

//public class RequestHandler {
//    public Response getRequest(final List<Request> requests) {
//        /* -- Get something in batch request, example --
//        [
//             {"jsonrpc":"2.0","method":"superAction","id":1234,"params":{"argumentStr":"Caesar", "argumentLong":70L, "myModel":{}}},
//             {"jsonrpc":"2.0","method":"doSomething","params":{"notify":"Hello, world!"}}
//        ]
//        */
//
//        // Request -> Response
//        final Response<?> response = consumer.execution(requests.get(0));
//
//        // Request without an id is a notification and don't return a response.
//        consumer.notify(requests.get(1));
//
//        return respons
//}
