package org.ngbp.jsonrpc4jtestharness;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;

import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCManager;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCProcessor;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.TempActivityCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TempActivityCallback {

    public static final ObjectMapper mapper = new ObjectMapper();
    private RPCManager rpcManager;
    private Double xPos = Double.valueOf(50);
    private Double yPos = Double.valueOf(50);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapper.registerModule(new JsonRpcModule());
        rpcManager = RPCManager.newInstance(this);
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

        findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (xPos > 0)
                    xPos = xPos - 10;
                makeCall();
            }
        });
        findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                xPos = xPos + 10;
                makeCall();
            }
        });
        findViewById(R.id.top).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (xPos > 0)
                    yPos = yPos - 10;
                makeCall();
            }
        });
        findViewById(R.id.bottom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                yPos = yPos + 10;
                makeCall();
            }
        });
        makeCall();
    }

    private void makeCall() {
        RPCProcessor callWrapper = new RPCProcessor(rpcManager);
        HashMap<String, Object> propertioes = new HashMap<>();
        propertioes.put("scaleFactor", 10);
        propertioes.put("xPos", xPos);
        propertioes.put("yPos", yPos);

        final Request request = new CompleteRequest("2.0", 1L, "org.atsc.scale-position", propertioes);
        String json = "";
        try {
            json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Object val = callWrapper.processRequest(json);
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

    @Override
    public void updateViewPosition(Double scaleFactor, Double xPos, Double yPos) {
        ConstraintLayout constraintLayout = findViewById(R.id.root);
        View view = findViewById(R.id.testView);
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.connect(view.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT, xPos.intValue() * 10);
        set.connect(view.getId(), ConstraintSet.TOP, constraintLayout.getId(), ConstraintSet.TOP, yPos.intValue() * 10);
        set.applyTo(constraintLayout);
    }
}
