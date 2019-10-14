package com.example.randomseed;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * SERVICE RUNNING ON FOREGROUND.
 * A TIMER WAS SCHEDULED COMPUTE REGISTERED SENSORS READING, OVERWRITING {@link LoggerService.dataBytes} EVERY MINUTE;
 */
public class LoggerService extends Service implements SensorEventListener {

    private static final int DELAY = SensorManager.SENSOR_DELAY_NORMAL;

    private SensorManager sensorManager = null;
    private byte[] dataBytes = new byte[]{0};
    private LoggerAsync loggerAsync = null;
    private Binder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.e("onCreate");
        binder = new Binder();
        setTimer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * The type Binder.
     */
    class Binder extends android.os.Binder {
        LoggerService getService() {
            return LoggerService.this;
        }
    }

    /**
     * Register listener if device supports listed sensor.
     * For list of sensors refer to @see https://source.android.com/devices/sensors/sensor-types.
     */
    public void registerListener() {
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_POSE_6DOF) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_POSE_6DOF), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT), DELAY);
        }
        if (getSensorManager().getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            getSensorManager().registerListener(this,
                    getSensorManager().getDefaultSensor(Sensor.TYPE_PROXIMITY), DELAY);
        }
    }

    private void setTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                registerListener();
                new Handler(getMainLooper()).postDelayed(() -> unregister(), 0);
            }
        }, 0, 60 * 1000);
    }

    private SensorManager getSensorManager() {
        if (sensorManager == null) {
            sensorManager =
                    (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }
        return sensorManager;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (loggerAsync != null) {
            return;
        }
        loggerAsync = new LoggerAsync(this);
        loggerAsync.execute(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //todo
    }

    @Override
    public void onDestroy() {
        unregister();
        super.onDestroy();
    }

    private static class LoggerAsync extends AsyncTask<SensorEvent, Void, byte[]> {

        private String error = null;
        private AtomicReference<LoggerService> service = new AtomicReference<>();
        private byte[] bytes = new byte[]{0};

        LoggerAsync(LoggerService s) {
            service.set(s);
        }

        @Override
        protected byte[] doInBackground(SensorEvent... event) {
            try {
                for (int i = 0; i < event[0].values.length; i++) {
                    Timber.e("Sensor: %s\nValues: %s", event[0].sensor.getStringType(), event[0].values[i]);
                    bytes = floatToByteArray(event[0].values[i]);
                    concatenateByte(bytes);
                    if (bytes.length > 512) {
                        bytes = new byte[]{0};
                    }
                }
                return bytes;
            } catch (Exception e) {
                e.printStackTrace();
                error = ErrorHelper.parseMessage(e);
                return new byte[]{0};
            }
        }

        @Override
        protected void onPostExecute(byte[] b) {
            service.get().loggerAsync = null;
            if (b == null && error != null) {
                Timber.e("Error: %s", this.error);
                return;
            }
            service.get().dataBytes = b;
            new Handler().postDelayed(service.get()::unregister, 1000);
        }

        private static byte[] floatToByteArray(float value) {
            return ByteBuffer.allocate(4).putFloat(value).array();
        }

        private void concatenateByte(byte[] input) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (bytes != null) {
                bos.write(bytes);
            }
            bos.write(input);
            bytes = bos.toByteArray();
        }
    }

    /**
     * Get {@link LoggerService.dataBytes}  byte value[].
     *
     * @return the byte []
     * @throws NoSuchAlgorithmException the no such algorithm exception
     */
    public byte[] getDataBytes() throws NoSuchAlgorithmException {
        return hashBytes(dataBytes);
    }

    private byte[] hashBytes(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        return md.digest(data);
    }


    /**
     * Unregister sensor listener to avoid battery drainage.
     */
    public void unregister() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sensorManager = null;
    }

}
