package com.kavurivinay.esense;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseEventListener;
import io.esense.esenselib.ESenseManager;
import io.esense.esenselib.ESenseSensorListener;


public class MainActivity extends AppCompatActivity /*Activity*/ implements ESenseConnectionListener {
    ESenseManager manager;
    ESenseConfig deviceConfig = new ESenseConfig(ESenseConfig.AccRange.valueOf("G_16"), ESenseConfig.GyroRange.valueOf("DEG_2000"), ESenseConfig.AccLPF.valueOf("DISABLED"), ESenseConfig.GyroLPF.valueOf("DISABLED"));
    boolean gatherData = false;
    final int PERMISSION_REQUEST_RECORD_AUDIO = 200;
    private final String TAG = "ESenseActivity";
    private static final String FILE_NAME = "data.json";
    static String FILE_NAME_MIC = null;
    private MediaRecorder recorder;
    long startTime;
    long timeSensing;
    private boolean record = false;
    ArrayList<Double> ax = new ArrayList<>();
    ArrayList<Double> ay = new ArrayList<>();
    ArrayList<Double> az = new ArrayList<>();

    ArrayList<Double> gx = new ArrayList<>();
    ArrayList<Double> gy = new ArrayList<>();
    ArrayList<Double> gz = new ArrayList<>();

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            Log.d("record","test");
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                prepareRecording();
                Log.d("record","This has started recording");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        Intent intent = registerReceiver(mBluetoothScoReceiver, intentFilter);
        if (intent == null) {
            Log.e(TAG, "Failed to register bluetooth sco receiver...");
            return;
        }

        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
            prepareRecording();
        }

        // Ensure the SCO audio connection stays active in case the
        // current initiator stops it.
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.startBluetoothSco();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionToRecordAccepted = true;
                } else {
                    permissionToRecordAccepted = true;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        //if (!permissionToRecordAccepted ) finish();
    }

    private void prepareRecording() {
        Log.d("Record","I reached prepareRecording()");
        recorder = new MediaRecorder();
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(FILE_NAME_MIC);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("Record", "prepare() failed");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        Log.d("Dir", "Saved to " + getExternalCacheDir().getAbsolutePath() + "/" + FILE_NAME_MIC);
        Toast.makeText(MainActivity.this, "Saved to " + getExternalCacheDir().getAbsolutePath() + "/" + FILE_NAME_MIC, Toast.LENGTH_LONG).show();
    }


    private void timeStart() {
        startTime = System.currentTimeMillis();
    }

    private long timeEnd() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Random rand = new Random();
        int r = rand.nextInt(100000);
        FILE_NAME_MIC = getExternalCacheDir().getAbsolutePath();
        FILE_NAME_MIC += "/audio"+r+".3gp";
        final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
        }


    }
    /*
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if(view!=null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();

        findViewById(R.id.writeFile).setOnClickListener(handleClick);
        findViewById(R.id.connect_btn).setOnClickListener(handleClick);
        findViewById(R.id.disconnect_btn).setOnClickListener(handleClick);
        findViewById(R.id.check_con_btn).setOnClickListener(handleClick);
        findViewById(R.id.regist_sensor_listener).setOnClickListener(handleClick);
        findViewById(R.id.unregist_sensor_listener).setOnClickListener(handleClick);
        findViewById(R.id.advertisment_get).setOnClickListener(handleClick);
        findViewById(R.id.regist_evt_listener).setOnClickListener(handleClick);
        findViewById(R.id.unregist_evt_listener).setOnClickListener(handleClick);
        findViewById(R.id.btnReadConfig).setOnClickListener(handleClick);
        findViewById(R.id.recordMic).setOnClickListener(handleClick);
        findViewById(R.id.stopRecord).setOnClickListener(handleClick);
        //hideKeyboard();

    }

    private View.OnClickListener handleClick = new View.OnClickListener() {
        public void onClick(View view) {
            Button btn = (Button) view;
            switch (btn.getId()) {
                case R.id.writeFile:
                    if (!gatherData) {
                        TextView tv = (TextView) findViewById(R.id.main_txt);
                        tv.setText("No data has been gathered.");
                    } else {
                        File file = new File(getFilesDir(), FILE_NAME);
                        FileWriter fileWriter;
                        BufferedWriter bufferedWriter;
                        if (!file.exists()) {
                            try {
                                file.createNewFile();
                                fileWriter = new FileWriter(file.getAbsoluteFile());
                                bufferedWriter = new BufferedWriter(fileWriter);
                                bufferedWriter.write("{}");
                                bufferedWriter.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        JSONObject data = new JSONObject();

                        //getting data here
                        JSONArray accx = new JSONArray();
                        JSONArray accy = new JSONArray();
                        JSONArray accz = new JSONArray();
                        JSONArray gyx = new JSONArray();
                        JSONArray gyy = new JSONArray();
                        JSONArray gyz = new JSONArray();

                        for (int i = 0; i < ax.size(); i++) {
                            accx.put(ax.get(i));
                            accy.put(ay.get(i));
                            accz.put(az.get(i));
                            gyx.put(gx.get(i));
                            gyy.put(gy.get(i));
                            gyz.put(gz.get(i));
                        }
                        try {
                            data.put("Accelerometer X", accx);
                            data.put("Accelerometer Y", accy);
                            data.put("Accelerometer Z", accz);
                            data.put("Gyroscope X", gyx);
                            data.put("Gyroscope Y", gyy);
                            data.put("Gyroscope Z", gyz);
                            data.put("Time", timeSensing);
                            fileWriter = new FileWriter(file.getAbsoluteFile());
                            BufferedWriter bw = new BufferedWriter(fileWriter);
                            bw.write(data.toString());
                            Log.d("Dir", "Saved to " + getFilesDir() + "/" + FILE_NAME);
                            Toast.makeText(MainActivity.this, "Saved to " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
                            bw.close();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.recordMic:
                    record = false;
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.startBluetoothSco();

                    recorder.start();

                    break;
                case R.id.stopRecord:
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.stopBluetoothSco();

                    stopRecording();

                    break;
                case R.id.connect_btn:
                    EditText nameText = findViewById(R.id.txtName);
                    String name = nameText.getText().toString();
                    Log.d("MyApp", name);
                    manager = new ESenseManager(name, MainActivity.this.getApplicationContext(), MainActivity.this);
                    manager.connect(10000);
                    break;
                case R.id.disconnect_btn:
                    manager.disconnect();
                    break;
                case R.id.check_con_btn:
                    TextView tv2 = (TextView) findViewById(R.id.rtn_is_connect);
                    tv2.setText("" + manager.isConnected());
                    Log.d("MyApp", "isconnect button hit, " + manager.isConnected());
                    break;
                case R.id.regist_sensor_listener:
                    gatherData = true;
                    ESSL essl = new ESSL();
                    manager.registerSensorListener(essl, 100);
                    recorder.start();
                    timeStart();
                    break;
                case R.id.unregist_sensor_listener:
                    timeSensing = timeEnd();
                    manager.unregisterSensorListener();
                    recorder.stop();
                    break;
                case R.id.advertisment_get:
                    manager.getAdvertisementAndConnectionInterval();
                    break;
                case R.id.regist_evt_listener:
                    ESEL esel = new ESEL();
                    manager.registerEventListener(esel);
                    break;
                case R.id.unregist_evt_listener:
                    manager.unregisterEventListener();
                    break;
                case R.id.btnReadConfig:
                    manager.getSensorConfig();
                    break;

            }
        }
    };

    public void onConnected(final ESenseManager manager) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onConnect");
                manager.setSensorConfig(deviceConfig);
                manager.setAdvertisementAndConnectiontInterval(100, 150, 30, 50);

            }
        });
    }

    public void onDisconnected(ESenseManager manager) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDisconnect");
            }
        });
    }

    public void onDeviceFound(ESenseManager manager) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDeviceFound");
            }
        });
    }

    public void onDeviceNotFound(ESenseManager manager) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDeviceNotFound");
            }
        });
    }

    public class ESEL implements ESenseEventListener {

        public void onButtonEventChanged(final boolean pressed) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView tv = (TextView) findViewById(R.id.main_txt);
                    tv.setText("onButtonEventChanged");
                }
            });
        }

        public void onAdvertisementAndConnectionIntervalRead(final int minAdvertsingInterval, final int maxAdvertsingInterval, final int minConnectionInterval, final int maxConnectionInterval) {
            Log.i(TAG, "onAdvertisementAndConnectionIntervalRead() min=" + minAdvertsingInterval + ", max=" + maxAdvertsingInterval);
        }

        public void onDeviceNameRead(String deviceName) {
            Log.i(TAG, "onDeviceNameRead() called & value is : " + deviceName);
        }

        @Override
        public void onSensorConfigRead(ESenseConfig config) {
            deviceConfig = config;
            Log.i(TAG, "Acc Range: " + config.getAccRange() + " Gyro Range: " + config.getGyroRange());
            Log.i(TAG, "ACC LPF: " + config.getAccLPF() + " Gyro LPF: " + config.getGyroLPF());
        }

        @Override
        public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
            Log.i(TAG, "Acc Offset: " + offsetX + " | " + offsetY + " | " + offsetZ);
        }

        @Override
        public void onBatteryRead(double voltage) {
            Log.i(TAG, "onBatteryRead() voltage is " + voltage);
        }
    }

    public class ESSL implements ESenseSensorListener {

        public ESSL() {
        }

        private void update(final double acc[], final double gyro[]) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DecimalFormat f = new DecimalFormat("##.00");
                    ax.add(acc[0]);
                    ay.add(acc[1]);
                    az.add(acc[2]);

                    gx.add(gyro[0]);
                    gy.add(gyro[1]);
                    gz.add(gyro[2]);
                }
            });
        }

        public void onSensorChanged(final ESenseEvent evt) {

            //if(){
            // If we know the device configuration we can convert the raw data
            double[] acc_g;
            double[] gyro_deg;
            acc_g = evt.convertAccToG(deviceConfig);
            gyro_deg = evt.convertGyroToDegPerSecond(deviceConfig);
            update(acc_g, gyro_deg);
            /*} else {
                short[] acc;
                short[] gyro;
                acc = evt.getAccel();
                gyro = evt.getGyro();

                double[] acc_double = new double[acc.length];
                double[] gyro_double = new double[gyro.length];

                for (int j=0;j<acc.length;j++) {
                    acc_double[j] = (double)acc[j];
                    gyro_double[j] = (double)gyro[j];
                }

                updateUI(acc_double, gyro_double);

            }*/
        }
    }

}
