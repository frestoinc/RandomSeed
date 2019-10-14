package com.example.randomseed;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

/**
 * SAMPLE ACTIVITY ON HOW TO BIND AND CALL {@link LoggerService} Service.
 * WE BIND THE SERVICE ON START OF ACTIVITY AND UN-BIND WHEN ACTIVITY DESTROYED.
 */
public class SampleActivity extends AppCompatActivity implements View.OnClickListener {

    private LoggerService loggerService;
    private TextView textView;
    private int count = 0;
    private byte[] preData = null;
    private boolean isServiceBinded = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            loggerService = ((LoggerService.Binder) service).getService();
            isServiceBinded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (loggerService != null) {
                loggerService.unregister();
            }
            isServiceBinded = false;
            loggerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.plant(new Timber.DebugTree());
        setContentView(R.layout.activity_main);
        findViewById(R.id.fab).setOnClickListener(this);
        textView = findViewById(R.id.text);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            getData();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, LoggerService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBinded) {
            unbindService(serviceConnection);
            isServiceBinded = false;
        }
    }

    private void getData() {
        String postString = textView.getText().toString();
        try {
            if (loggerService == null || loggerService.getDataBytes() == null) {
                return;
            }
            loggerService.registerListener();
            new Handler().postDelayed(() -> {
                try {
                    count = count + 1;
                    byte[] postData = loggerService.getDataBytes();
                    boolean b = preData != null && preData == postData;
                    runOnUiThread(() -> textView.setText(String.format("%s%s", postString,
                            String.format("%s)B:%s\nData: %s\nLength: %sbytes\n\n", count, b, new String(postData), postData.length))));
                    preData = postData;
                    NestedScrollView nsv = findViewById(R.id.nsv);
                    runOnUiThread(() -> nsv.fullScroll(View.FOCUS_DOWN));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> textView.setText(String.format("%s%s", postString,
                            String.format("%s) Data: %s\n", count, ErrorHelper.parseMessage(e)))));
                }
            }, 1500);

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> textView.setText(String.format("%s%s", postString,
                    String.format("%s) Data: %s\n", count, ErrorHelper.parseMessage(e)))));
        }

    }
}
