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
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;

import org.ngbp.jsonrpc4jtestharness.core.FileUtils;
import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCProcessor;
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;
import org.ngbp.libatsc3.Atsc3Module;
import org.ngbp.libatsc3.ndk.a331.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_REQUEST_CODE = 133;

    public static final ObjectMapper mapper = new ObjectMapper();

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

        RPCProcessor callWrapper = new RPCProcessor();
        List<String> requestParams = new ArrayList<>();

        final Request request = new CompleteRequest("2.0", 1L, "org.atsc.getFilterCodes", new HashMap<>());
        String json = "";
        try {
            json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        final Request request2 = new CompleteRequest("2.0", 2L, "org.atsc.query.service", new HashMap<>());
        String json2 = "";
        try {
            json2 = mapper.writeValueAsString(request2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        GetFilterCodes val = callWrapper.processRequest(json);
        requestParams.add(json);
        requestParams.add(json2);
        //List<Object> composedResponses =   callWrapper.processRequest(requestParams);

        initLibAtsc3();
    }

    private void initLibAtsc3() {
        atsc3Module = new Atsc3Module(getApplicationContext());
        atsc3Module.setListener(new Atsc3Module.Listener() {
            @Override
            public void onServicesLoaded(@NonNull List<Service> services) {
                Service s = services.stream()
                        .filter(service -> "WZTV".equals(service.getShortServiceName()))
                        .findFirst()
                        .orElse(null);

                if (s != null) {
                    Uri mediaPath = atsc3Module.getMediaUri(s);
                }
            }

            @Override
            public void onStateChanged(Atsc3Module.State state) {
                updateAssc3Buttons(state);
            }
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
