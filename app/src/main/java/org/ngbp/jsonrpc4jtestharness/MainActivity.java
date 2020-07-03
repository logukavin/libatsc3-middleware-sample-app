package org.ngbp.jsonrpc4jtestharness;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;

import org.ngbp.jsonrpc4jtestharness.core.FileUtils;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocketClient;
import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCManager;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCProcessor;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.ReceiverActionCallback;
import org.ngbp.libatsc3.Atsc3Module;
import org.ngbp.libatsc3.ndk.a331.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

public class MainActivity extends AppCompatActivity implements ReceiverActionCallback {

    private static final int FILE_REQUEST_CODE = 133;

    public static final ObjectMapper mapper = new ObjectMapper();
    private RPCManager rpcManager;
    RPCProcessor callWrapper;
    private Double xPos = Double.valueOf(50);
    private Double yPos = Double.valueOf(50);

    private Atsc3Module atsc3Module;

    private EditText stsc3FilePath;
    private View stsc3Open;
    private View stsc3Start;
    private View stsc3Stop;
    private View stsc3Close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapper.registerModule(new JsonRpcModule());
        rpcManager = new RPCManager();
        rpcManager.setCallback(this);
        callWrapper = new RPCProcessor(rpcManager);
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
        findViewById(R.id.connect_to_ws).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWSClient();
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

    final Request request = new CompleteRequest("2.0", 1L, "org.atsc.getFilterCodes", new HashMap<>());
    String json = "";

    private void makeCall() {
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
        callWrapper.processRequest(json);

        initLibAtsc3();
    }

    private void initLibAtsc3() {
        atsc3Module = new Atsc3Module(getApplicationContext());
        atsc3Module.getState().observe(this, this::updateAssc3Buttons);
        atsc3Module.getSltServices().observe(this, services -> {
            services.stream()
                    .filter(service -> "WZTV".equals(service.getShortServiceName()))
                    .findFirst()
                    .ifPresent(service -> atsc3Module.selectService(service));
        });

        stsc3FilePath = findViewById(R.id.atsc3_file_path);
        stsc3FilePath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateAssc3Buttons(TextUtils.isEmpty(s) ? null : Atsc3Module.State.IDLE);
            }
        });

        stsc3Open = findViewById(R.id.atsc3_open);
        stsc3Open.setOnClickListener(v -> atsc3Module.openPcapFile(stsc3FilePath.getText().toString()));

        stsc3Start = findViewById(R.id.atsc3_start);
        stsc3Start.setOnClickListener(v -> {
            //atsc3Module.
        });

        stsc3Stop = findViewById(R.id.atsc3_stop);
        stsc3Stop.setOnClickListener(v -> atsc3Module.stop());

        stsc3Close = findViewById(R.id.atsc3_close);
        stsc3Close.setOnClickListener(v -> atsc3Module.close());

        findViewById(R.id.atsc3_file_choose).setOnClickListener(v -> showFileChooser());

        updateAssc3Buttons(null);
    }

    private void updateAssc3Buttons(Atsc3Module.State state) {
        stsc3Open.setEnabled(state == Atsc3Module.State.IDLE);
        stsc3Start.setEnabled(state == Atsc3Module.State.OPENED || state == Atsc3Module.State.PAUSED);
        stsc3Stop.setEnabled(state == Atsc3Module.State.OPENED);
        stsc3Close.setEnabled(state == Atsc3Module.State.OPENED || state == Atsc3Module.State.PAUSED);
        callWrapper.processRequest(json);
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.ACTION_START);
        serviceIntent.putExtra("inputExtra", "Foreground RPC Service Example in Android");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.ACTION_STOP);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void startWSClient() {
        Thread wsClient = new Thread() {
            @Override
            public void run() {
                MiddlewareWebSocketClient client = new MiddlewareWebSocketClient();
                client.start();
            }
        };
        wsClient.start();
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

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_REQUEST_CODE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "There is no one File Manager registered in system.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            String filePath = FileUtils.getPath(getApplicationContext(), data.getData());
            stsc3FilePath.setText(filePath);
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
