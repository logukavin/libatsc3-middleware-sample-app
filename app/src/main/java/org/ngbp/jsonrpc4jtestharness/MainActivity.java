package org.ngbp.jsonrpc4jtestharness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder;
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.model.response.Response;
import com.github.nmuzhichin.jsonrpc.normalizer.JacksonNormalization;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.MockService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.MockServiceImpl;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.SomeService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.SomeServiceImpl;
import org.ngbp.jsonrpc4jtestharness.mappers.RequestRpcMapper;
import org.ngbp.jsonrpc4jtestharness.mappers.ResponseRpcMapper;
import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.models.sample_models.request.Properties;
import org.ngbp.jsonrpc4jtestharness.models.sample_models.response.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    IAtsc3Query ats3Query;
    public static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               stopService();
            }
        });
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService();
            }
        });
        ats3Query = new Atsc3QueryImpl();
        JsonRpcServer server = new JsonRpcServer(mapper, ats3Query, IAtsc3Query.class);
        test();
        sampleForParsingSingleRequest(server);
        sampleForParsingBatchRequest(server);
    }

    private void test() {
        final Map<String, Object> params = new LinkedHashMap<>(1 << 2, 1f);
        params.put("name", "Octavianus Augustus");
        params.put("age", 76L);

        final CompleteRequest request = new CompleteRequest("2.0", 54321L, "strictCall", params);

        final RpcConsumer consumer = new ConsumerBuilder()
                .valueNormalizer(new JacksonNormalization(new ObjectMapper()))
                .resultListener(Optional::ofNullable)
                .build();

        consumer.getProcessor().process(new MockServiceImpl(), MockService.class);

        final Response response = consumer.execution(request);
        Log.d("TEST", response.toString());
    }

    private void sampleForParsingSingleRequest(JsonRpcServer server) {
//        Map params to RPCRequest
        RequestRpcMapper requestMapper = new RequestRpcMapper<Properties>();
        InputStream inputStream = requestMapper.mapModelToInputStream(null, "org.atsc.query.service");

        Log.d("Test", "Request : " + requestMapper.mapModelToSingleRpcRequest(null, "org.atsc.query.service").toString());

        OutputStream outputStream = new ByteArrayOutputStream();

        try {
            server.handleRequest(inputStream, outputStream);
            String outputJsonResponse = new String(((ByteArrayOutputStream) outputStream).toByteArray());

            ResponseRpcMapper rpcMapper = new ResponseRpcMapper<Service>();
            JsonRpcResponse jsonRpcResponse = rpcMapper.mapSingleJsonRpcToResponseModel(outputJsonResponse);

            Log.d("Test", "Response : " + jsonRpcResponse.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sampleForParsingBatchRequest(JsonRpcServer server) {
//        Create params for batch request
        List<Properties> batchRequest = new ArrayList<>();

        Properties properties1 = new Properties();
        List<String> listOfProperties1 = new ArrayList<>();
        listOfProperties1.add("123");
        listOfProperties1.add("456");
        properties1.setProperties(listOfProperties1);

        Properties properties2 = new Properties();
        List<String> listOfProperties2 = new ArrayList<>();
//        listOfProperties2.add("789");
//        listOfProperties2.add("101112");
        properties2.setProperties(listOfProperties2);

        Properties properties3 = new Properties();
        List<String> listOfProperties3 = new ArrayList<>();
        listOfProperties3.add("131415");
        listOfProperties3.add("161718");
        properties3.setProperties(listOfProperties3);

        batchRequest.add(properties1);
        batchRequest.add(properties2);
        batchRequest.add(properties3);
//        Map params to BatchRPCRequest
        RequestRpcMapper requestMapperBatch = new RequestRpcMapper<Properties>();
        InputStream inputStreamBatch = requestMapperBatch.mapModelListToInputStream(batchRequest, "org.atsc.query.service2");

        Log.d("Test", "Request Batch : " + requestMapperBatch.mapModelsToBatchRpcRequest(batchRequest, "org.atsc.query.service2").toString());

        OutputStream outputStreamBatch = new ByteArrayOutputStream();

        try {
            server.handleRequest(inputStreamBatch, outputStreamBatch);
            String outputJsonResponseBatch = new String(((ByteArrayOutputStream) outputStreamBatch).toByteArray());

            ResponseRpcMapper responceMapperBatch = new ResponseRpcMapper<List<String>>();
            List<JsonRpcResponse> jsonRpcResponseBatch = responceMapperBatch.mapBatchJsonRpcToResponseModelList(outputJsonResponseBatch);

            Log.d("Test", "Response Batch: " + jsonRpcResponseBatch.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.START);
        serviceIntent.putExtra("inputExtra", "Foreground RPC Service Example in Android");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.STOP);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onDestroy() {
        stopService();
        super.onDestroy();
    }
}
