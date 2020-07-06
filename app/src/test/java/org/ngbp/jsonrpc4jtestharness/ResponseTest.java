package org.ngbp.jsonrpc4jtestharness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.model.response.Response;
import com.github.nmuzhichin.jsonrpc.model.response.errors.AbstractError;
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;
import com.github.nmuzhichin.jsonrpc.model.response.errors.MeaningError;
import com.github.nmuzhichin.jsonrpc.model.response.errors.StacktraceError;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;
import com.github.nmuzhichin.jsonrpc.serialize.ResponseSerializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.github.nmuzhichin.jsonrpc.model.response.errors.Error.Predefine.INVALID_PARAMS;

public class ResponseTest {
    //    private static final Logger log = LoggerFactory.getLogger(ResponseTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        objectMapper.registerModule(new JsonRpcModule());
    }

    @Test
    public void serializeDeserialize() throws IOException {
//        ResponseSerializer

        final Response response = Response.createResponse(1L, "new");

        final String json = objectMapper.writeValueAsString(response);
        final String json2 = "{\"jsonrpc\":\"2.0\",\"method\":\"doSomething\",\"params\":{\"notify\":\"Hello, world!\"}}";

        final Response readValue = objectMapper.readValue(json2, Response.class);

        Assert.assertNotNull(readValue);
        Assert.assertNotNull(readValue.getBody());
//        Assert.assertEquals(1L, readValue.getId());

//        log.info(readValue.getBody().toString());
    }
}

//    @Test
//    public void serializeDeserializeV2() throws IOException {
//        final Response response = ResponseUtils.createResponse(1L, new MeaningError(INVALID_PARAMS));
//
//        final String json = objectMapper.writeValueAsString(response);
//
//        final Response readValue = objectMapper.readValue(json, Response.class);
//
//        Assert.assertNotNull(readValue);
//        Assert.assertNotNull(readValue.getBody());
////        Assert.assertEquals(1L, readValue.getId());
//        Assert.assertEquals(MeaningError.class, readValue.getBody().getClass());
//
////        log.info(readValue.getBody().toString());
//    }
//
//    @Test
//    public void serializeDeserializeV3() throws IOException {
//        final Response response = ResponseUtils.createResponse(1L, null);
//
//        final String json = objectMapper.writeValueAsString(response);
//
//        final Response readValue = objectMapper.readValue(json, Response.class);
//
//        Assert.assertNotNull(readValue);
//        Assert.assertNull(readValue.getBody());
////        Assert.assertEquals(1L, readValue.getId());
//
////        log.info(String.valueOf(readValue.getBody()));
//    }
//
////    @Test
////    void serializeDeserializeV4() throws IOException {
////        final MockModel mockModel = new MockModel();
////        mockModel.setAge(123L);
////        mockModel.setName("Hello");
////        final Response response = ResponseUtils.createResponse(1L, mockModel);
////
////        final String json = objectMapper.writeValueAsString(response);
////
////        final Response readValue = objectMapper.readValue(json, Response.class);
////
////        Assert.assertNotNull(readValue);
////        Assert.assertNotNull(readValue.getBody());
//////        Assert.assertEquals(1L, readValue.getId());
////
////        log.info(readValue.getBody().toString());
////    }
//
//    @Test
//    public void serializeDeserializeV5() throws IOException {
//        final Response response = ResponseUtils.createResponse(1L, "{\"time\":" + "\"" + LocalDateTime.now() + "\"}");
//
//        final String json = objectMapper.writeValueAsString(response);
//
//        final Response readValue = objectMapper.readValue(json, Response.class);
//
//        Assert.assertNotNull(readValue);
//        Assert.assertNotNull(readValue.getBody());
////        Assert.assertEquals(1L, readValue.getId());
//
////        log.info(readValue.getBody().toString());
//    }
//
//    @Test
//    public void serializeDeserializeV6() throws IOException {
//        final Response response =
//                ResponseUtils.createResponse(1L, new StacktraceError(INVALID_PARAMS, new RuntimeException()));
//
//        final String json = objectMapper.writeValueAsString(response);
//
//        final Response readValue = objectMapper.readValue(json, Response.class);
//
//        Assert.assertNotNull(readValue);
//        Assert.assertNotNull(readValue.getBody());
////        Assert.assertEquals(Optional.of(1L), readValue.getId());
//        Assert.assertEquals(StacktraceError.class, readValue.getBody().getClass());
//
////        log.info(readValue.getBody().toString());
//    }
//
//    @Test
//    public void serializeDeserializeV7() throws IOException {
//        final Response response =
//                ResponseUtils.createResponse(1L, new StacktraceError(INVALID_PARAMS, new RuntimeException()));
//
//        final String json = objectMapper.writeValueAsString(response);
//
//        final Response responseRead = objectMapper.readValue(json, Response.class);
//
//        Assert.assertNotNull(responseRead);
//        Assert.assertNotNull(responseRead.getBody());
////        Assert.assertEquals(Optional.of(1L), responseRead.getId());
//        Assert.assertEquals(StacktraceError.class, responseRead.getBody().getClass());
//
//        final Optional<Error> error = responseRead.isError() ? responseRead.getError() : Optional.empty();
//
//        error.ifPresent(it -> ((AbstractError<?>) it).dropData());
//
//        error.ifPresent(err -> Assert.assertNull(err.getData()));
//
////        log.info(responseRead.getBody().toString());
//    }
//}