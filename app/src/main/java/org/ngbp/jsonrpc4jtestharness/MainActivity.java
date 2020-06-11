package org.ngbp.jsonrpc4jtestharness;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    JsonRpcServer server;
    IAtsc3Query ats3Query;
    public static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ats3Query = new Atsc3QueryImpl();
        JsonRpcServer server = new JsonRpcServer(mapper, ats3Query, IAtsc3Query.class);

        String inputJsonRpcReqeust = "{\n" +
                "\"jsonrpc\": \"2.0\",\n" +
                "\"method\": \"org.atsc.query.service\",\n" +
                "\"id\": 55 }";

        InputStream inputStream = new ByteArrayInputStream(inputJsonRpcReqeust.getBytes());
        OutputStream outputStream = new ByteArrayOutputStream();

        try {
            server.handleRequest(inputStream, outputStream);
            String outputJsonResponse = new String(((ByteArrayOutputStream)outputStream).toByteArray());
            Log.i("handleRequest", String.format("\n\n\ninputJsonRpcRequest:\n%s\n\noutputJsonRpcResponse:\t\n %s\n\n\n", inputJsonRpcReqeust, outputJsonResponse));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
