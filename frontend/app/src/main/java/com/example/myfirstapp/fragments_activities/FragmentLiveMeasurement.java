package com.example.myfirstapp.fragments_activities;

import static com.example.myfirstapp.backend.storageData.DataRecordingControl.FLAG_ENABLE_STORAGE_DATA;
import static java.lang.Thread.sleep;

import android.graphics.Color;
import android.os.Bundle;

import com.example.myfirstapp.backend.liveProcessing.ModuleACC;
import com.example.myfirstapp.backend.liveProcessing.ModuleECG;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myfirstapp.R;
import com.example.myfirstapp.backend.Protocol;
import com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection;
import com.example.myfirstapp.backend.storageData.DataRecordingControl;
import com.scwang.wave.MultiWaveHeader;

import java.util.concurrent.atomic.AtomicBoolean;


public class FragmentLiveMeasurement extends Fragment {

    public static ConstraintLayout countDownBox, checkSensorAndLiveMeasurementLayoutSleepBuddy, noConnectionBox, liveDataLayoutSleepBuddy;

    AtomicBoolean doRunViewLiveData;

    private TextView waitForConnectionTxtView, countDownTxtView;

    private TextView subFeature1ValueTxtView,
                    subFeature2ValueTxtView,
                    subFeature3ValueTxtView,
                    subFeature4ValueTxtView,
                    subFeature5ValueTxtView,
                    subFeature6ValueTxtView,
                    subFeature7ValueTxtView,
                    subFeature8ValueTxtView,
                    subFeature9ValueTxtView;

    private Button toConnectionTxtView;

    public static TextView txtStatusSensorSleepBuddy;
    public static ImageView imgConfirmStatusSensorSleepBuddy;
    public static Button btnCheckSensorStartSleepBuddy;
    public static int[] drawablesCheckSensor;
    public SwitchMaterial liveDataSwitchSleepBuddy;

    public static boolean isLiveDataEnable = true;

    private Protocol protocol = new Protocol();

    private DataRecordingControl dataRecordingControl = DataRecordingControl.getInstance();

    public static int gintNumberByteInFile = 0;

    public static Chronometer chronometerPlotData;

    BluetoothBLEConnection mBluetoothBLEConnection = BluetoothBLEConnection.getInstance();

    public static boolean isDeviceMeasuring = false;

    public static CountDownTimer countDownTimer;
    static int countDownProgress;
    static boolean timerIsRunning = false;

    MultiWaveHeader multiWaveHeader;

    final String LIVE_MEASUREMENT_VIEW = "Live Measurement view";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_measurement, container, false);
        countDownBox = view.findViewById(R.id.countDownBox);
        checkSensorAndLiveMeasurementLayoutSleepBuddy = view.findViewById(R.id.checkSensorAndLiveMeasurementLayoutSleepBuddy);
        noConnectionBox = view.findViewById(R.id.noConnectionBox);
        countDownTxtView = view.findViewById(R.id.countdownTxtView);
        toConnectionTxtView = view.findViewById(R.id.toConnectionTxtView);
        liveDataLayoutSleepBuddy = view.findViewById(R.id.liveDataLayoutSleepBuddy);
        multiWaveHeader = view.findViewById(R.id.multiWaveHeader);
        multiWaveHeader.stop();
        doRunViewLiveData = new AtomicBoolean(true);

        subFeature1ValueTxtView = view.findViewById(R.id.subFeature1ValueTxtView);
        subFeature2ValueTxtView = view.findViewById(R.id.subFeature2ValueTxtView);
        subFeature3ValueTxtView = view.findViewById(R.id.subFeature3ValueTxtView);
        subFeature4ValueTxtView = view.findViewById(R.id.subFeature4ValueTxtView);
        subFeature5ValueTxtView = view.findViewById(R.id.subFeature5ValueTxtView);
        subFeature6ValueTxtView = view.findViewById(R.id.subFeature6ValueTxtView);
        subFeature7ValueTxtView = view.findViewById(R.id.subFeature7ValueTxtView);
        subFeature8ValueTxtView = view.findViewById(R.id.subFeature8ValueTxtView);
        subFeature9ValueTxtView = view.findViewById(R.id.subFeature9ValueTxtView);

        toConnectionTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainScreen) getActivity()).closeCurrentFragment();
                ((MainScreen) getActivity()).fragmentManager.beginTransaction().show(((MainScreen) getActivity()).fragmentConnection).commit();
                ((MainScreen) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_connection);

            }
        });

        countDownProgress = 10;

        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long l) {
                countDownProgress--;
                timerIsRunning = true;
                countDownTxtView.setText(Integer.toString(countDownProgress));
            }

            @Override
            public void onFinish() {
                countDownBox.setVisibility(View.GONE);
                timerIsRunning = false;
                checkSensorAndLiveMeasurementLayoutSleepBuddy.setVisibility(View.VISIBLE);
                countDownProgress = 10;
            }
        };

        btnCheckSensorStartSleepBuddy = view.findViewById(R.id.btnCheckSensorStartSleepBuddy);
        waitForConnectionTxtView = view.findViewById(R.id.waitForConnectionTxtView);
        txtStatusSensorSleepBuddy = view.findViewById(R.id.txtConfirmStatusSensorSleepBuddy);
        imgConfirmStatusSensorSleepBuddy = view.findViewById(R.id.imgConfirmStatusSensorSleepBuddy);
        drawablesCheckSensor = new int[2];
        drawablesCheckSensor[0] = R.drawable.check_sensor_check;
        drawablesCheckSensor[1] = R.drawable.check_sensor_fail;
        liveDataSwitchSleepBuddy = view.findViewById(R.id.liveDataSwitchSleepBuddy);
        chronometerPlotData = view.findViewById(R.id.simpleChronometerPlotData);

        liveDataSwitchSleepBuddy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLiveDataEnable = !isLiveDataEnable;
                if(!liveDataSwitchSleepBuddy.isChecked()){
                    subFeature1ValueTxtView.setText("--");
                    subFeature2ValueTxtView.setText("--");
                    subFeature3ValueTxtView.setText("--");
                    subFeature4ValueTxtView.setText("--");
                    subFeature5ValueTxtView.setText("--");
                    subFeature6ValueTxtView.setText("--");
                    subFeature7ValueTxtView.setText("--");
                    subFeature8ValueTxtView.setText("--");
                    subFeature9ValueTxtView.setText("--");
                }
            }
        });

        btnCheckSensorStartSleepBuddy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothBLEConnection.ismConnected()) {
                    if (btnCheckSensorStartSleepBuddy.getText().equals("Start") || btnCheckSensorStartSleepBuddy.getText().equals("Stop")) {
                        if (isDeviceMeasuring) {
                            /*stop sleepbuddy device*/
                            Log.d(LIVE_MEASUREMENT_VIEW, "Stop button clicked");
                            btnCheckSensorStartSleepBuddy.setText("Check Sensor");
                            protocol.stopSleepBuddyOnButton();
                            isDeviceMeasuring = false;

                            /*stop recording*/
                            if (FLAG_ENABLE_STORAGE_DATA){
                                dataRecordingControl.stopRecording();
                            }

                            chronometerPlotData.stop();
                            gintNumberByteInFile = 0;
                            multiWaveHeader.stop();

                            /*stop view live data*/
                            doRunViewLiveData.set(false);
                            viewLiveData();
                            liveDataLayoutSleepBuddy.setVisibility(View.GONE);

                        } else {
                            /*start sleepbuddy device*/
                            Log.d(LIVE_MEASUREMENT_VIEW, "Start button clicked");
                            txtStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);
                            imgConfirmStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);
                            btnCheckSensorStartSleepBuddy.setText("Stop");
                            liveDataLayoutSleepBuddy.setVisibility(View.VISIBLE);
                            protocol.startSleepBuddyOnButton();
                            isDeviceMeasuring = true;

                            /*start recording*/
                            if (FLAG_ENABLE_STORAGE_DATA){
                                dataRecordingControl.startRecording();
                            }


                            chronometerPlotData.setBase(SystemClock.elapsedRealtime());
                            chronometerPlotData.start();
                            multiWaveHeader.setVelocity(3);
                            multiWaveHeader.start();
                            
                            /*start view live data*/
                            liveDataSwitchSleepBuddy.setChecked(false);
                            isLiveDataEnable = false;

                            doRunViewLiveData.set(true);
                            viewLiveData();
                        }
                    } else if (btnCheckSensorStartSleepBuddy.getText().equals("Check Sensor")) {
                        Log.d(LIVE_MEASUREMENT_VIEW, "Check Sensor button clicked");
                        protocol.checkStatusSensorButton();
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.wait_for_stable_connection), Toast.LENGTH_LONG).show();
                }

            }
        });

        return view;
    }

    private void viewLiveData() {
        Thread viewLiveDataThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (doRunViewLiveData.get())
                {
                    if(isLiveDataEnable)
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                subFeature1ValueTxtView.setText(String.format("%.0f ms", ModuleECG.getInstance().getRR())); // current rr distance in ms
                                subFeature2ValueTxtView.setText(String.format("%.3f µV", ModuleECG.getInstance().getRmsEcg())); // current rms of the filtered ecg signal in µV
                                subFeature3ValueTxtView.setText(String.format("%.3f µV", ModuleECG.getInstance().getRmsEmg()));// current rms of the filtered emg signal in µV
                                subFeature4ValueTxtView.setText(String.format("%.3f g", ModuleACC.getInstance().getAccX())); // Lowpass filtered X-Axis (smoothed acc x-axis) in g
                                subFeature5ValueTxtView.setText(String.format("%.3f g", ModuleACC.getInstance().getAccY())); // Lowpass filtered Y-Axis (smoothed acc y-axis) in g
                                subFeature6ValueTxtView.setText(String.format("%.3f g", ModuleACC.getInstance().getAccZ())); // Lowpass filtered Z-Axis (smoothed acc z-axis) in g
                                subFeature7ValueTxtView.setText(String.format("%.1f mg", ModuleACC.getInstance().getRmsSnore())); // RMS of high filtered z-axis in mg
                                subFeature8ValueTxtView.setText(String.format("%.1f mg", ModuleACC.getInstance().getRmsMove1())); // longtime rms of absolute vector of bandpass filtered axes in mg (FIR Method)
                                subFeature9ValueTxtView.setText(String.format("%.1f mg", ModuleACC.getInstance().getRmsMove2())); // shorttime rms of absolute vector of bandpass filtered axes in mg (IIR Method)

                                // if the ecg rms is to low, the rr value is possible wrong
                                float rmsEcg = ModuleECG.getInstance().getRmsEcg();

                                // colorize ecg rms
                                if (rmsEcg < 10) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorVeryCritical));
                                }else if (rmsEcg >= 10 && rmsEcg < 20) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorCritical));
                                }else if (rmsEcg >= 20 && rmsEcg < 35) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorDubious));
                                }else if (rmsEcg >= 35 && rmsEcg < 55) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorAcceptable));
                                }else if (rmsEcg >= 55 && rmsEcg < 75) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorGood));
                                }else if (rmsEcg >= 80) {
                                    subFeature2ValueTxtView.setTextColor(getResources().getColor(R.color.colorSuper));
                                }

                                // colorize ecg rr
                                if (rmsEcg < 15f) { // ecg rms lower than 15 µV (critical)
                                    subFeature1ValueTxtView.setTextColor(getResources().getColor(R.color.grey));
                                }
                                else {
                                    subFeature1ValueTxtView.setTextColor(getResources().getColor(R.color.light_grey));
                                }
                            }
                        });
                    }

                    try
                    {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        viewLiveDataThread.start();
    }


    public static void checkSensor_Success_Sleepbuddy() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                imgConfirmStatusSensorSleepBuddy.setVisibility(View.VISIBLE);
                txtStatusSensorSleepBuddy.setVisibility(View.VISIBLE);
                btnCheckSensorStartSleepBuddy.setText("Start");
                imgConfirmStatusSensorSleepBuddy.setImageResource(drawablesCheckSensor[0]);
                Log.i("sendCommandToDevice", "checkStatusSensor: ok");
                txtStatusSensorSleepBuddy.setText(R.string.you_can_start_measurement);
            }
        });

    }

    public static void checkSensor_Fail_SleepBuddy() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                imgConfirmStatusSensorSleepBuddy.setVisibility(View.VISIBLE);
                txtStatusSensorSleepBuddy.setVisibility(View.VISIBLE);
                imgConfirmStatusSensorSleepBuddy.setImageResource(drawablesCheckSensor[1]);
                Log.i("sendCommandToDevice", "checkStatusSensor: fails");
                txtStatusSensorSleepBuddy.setText(R.string.start_and_reconnect);
                txtStatusSensorSleepBuddy.setVisibility(View.VISIBLE);

            }
        });


    }
}