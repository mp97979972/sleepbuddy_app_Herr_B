package com.example.myfirstapp.backend.liveProcessing;

import static com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection.mConnected;

import android.util.Log;


import com.example.myfirstapp.backend.receiveData.DataHandler;

public class liveProcessingPullData extends Thread{

    public static final int LOG_LEVEL_NO_LOG        = 0;
    public static final int LOG_LEVEL_SYSTEM_ONLY   = 1; // init call etc.
    public static final int LOG_LEVEL_ON_UPDATE     = 2; // update of values
    public static final int LOG_LEVEL_ON_CALL       = 3; // every poll
    public static final int LOG_LEVEL_EVERY_SHIT    = 4; // extra stuff (spamming!)
    public int mLogLevel = LOG_LEVEL_ON_UPDATE;

    private DataHandler dataHandler;

    public static final int ads_feature = 0x01;
    public static final int acc_feature = 0x02;

    public void liveProcessingGet_DataHandler_Instance(){
        dataHandler = DataHandler.getInstance();
    }

    private ModuleECG moduleECG = ModuleECG.getInstance();
    private ModuleACC moduleACC = ModuleACC.getInstance();


    boolean has_new_ads = false;
    float ads_rr_interval = 0;
    float ads_ecg_rms = 0;
    float ads_emg_rms = 0;

    boolean has_new_acc = false;
    float acc_x = 0;
    float acc_y = 0;
    float acc_z = 0;
    float snoring_rms = 0;
    float moving_rms_iir = 0;
    float moving_rms_fir = 0;

    private boolean pollChartValueFromQueue(int typeSignale)
    {
        if (mLogLevel >= LOG_LEVEL_ON_CALL) {
            Log.i("liveProcessing", "pollChartValueFromQueue called");
        }

        boolean returnValue  = false; // signals if any update is available

        switch (typeSignale)
        {
            case ads_feature:

                has_new_ads = false;

                ads_rr_interval = -1;
                ads_ecg_rms = 0;
                ads_emg_rms = 0;

                if (!dataHandler.fQueueADS_RR_Interval.isEmpty())   ads_rr_interval = dataHandler.fQueueADS_RR_Interval.poll();
                if (!dataHandler.fQueueADS_ECG_RMS.isEmpty())       ads_ecg_rms = dataHandler.fQueueADS_ECG_RMS.poll();
                if (!dataHandler.fQueueADS_EMG_RMS.isEmpty())       ads_emg_rms = dataHandler.fQueueADS_EMG_RMS.poll();

                if (ads_rr_interval >= 0) { // use rr as update check. No negative values possible!
                    has_new_ads = true;
                    returnValue = true;
                }

                if (mLogLevel >= LOG_LEVEL_EVERY_SHIT) {
                    Log.i("liveProcessing", "QueueSizeADCFeature: " + dataHandler.fQueueADS_RR_Interval.size() + " " + dataHandler.fQueueADS_ECG_RMS.size() + " " + dataHandler.fQueueADS_EMG_RMS.size());
                    Log.i("liveProcessing", "ADCFeature: " + ads_rr_interval + " " + ads_ecg_rms + " " + ads_emg_rms);
                }

                break;

            case acc_feature:

                has_new_acc = false;

                acc_x = 0;
                acc_y = 0;
                acc_z = 0;
                snoring_rms = -1;
                moving_rms_iir = 0;
                moving_rms_fir = 0;

                if (!dataHandler.fQueueACC_AXES_X_5Hz.isEmpty())        acc_x = dataHandler.fQueueACC_AXES_X_5Hz.poll();
                if (!dataHandler.fQueueACC_AXES_Y_5Hz.isEmpty())        acc_y = dataHandler.fQueueACC_AXES_Y_5Hz.poll();
                if (!dataHandler.fQueueACC_AXES_Z_5Hz.isEmpty())        acc_z = dataHandler.fQueueACC_AXES_Z_5Hz.poll();
                if (!dataHandler.fQueueACC_Snoring_RMS.isEmpty())       snoring_rms = dataHandler.fQueueACC_Snoring_RMS.poll();
                if (!dataHandler.fQueueACC_Moving_RMS_IIR.isEmpty())    moving_rms_iir = dataHandler.fQueueACC_Moving_RMS_IIR.poll();
                if (!dataHandler.fQueueACC_Moving_RMS_FIR.isEmpty())    moving_rms_fir = dataHandler.fQueueACC_Moving_RMS_FIR.poll();

                if (snoring_rms >= 0) { // use snore rms as update check, no negative values possible!
                    has_new_acc = true;
                    returnValue = true;
                }

                if (mLogLevel >= LOG_LEVEL_EVERY_SHIT) {
                    Log.i("liveProcessing", "QueueSizeACCFeature: " + dataHandler.fQueueACC_Snoring_RMS.size() + " " + dataHandler.fQueueACC_Moving_RMS_IIR.size() + " " + dataHandler.fQueueACC_Moving_RMS_FIR.size());
                    Log.i("liveProcessing", "ADCFeature: " + acc_x + " " + acc_y + " " + acc_z + "  ||||  " + snoring_rms + "  " + moving_rms_iir + "  " + moving_rms_fir);
                }
                break;

            default:
                break;
        }
        return returnValue;
    }

    @Override
    public void run() {

        if (mLogLevel >= LOG_LEVEL_SYSTEM_ONLY) {
            Log.i("liveProcessing", "start processing thread!");
        }

        while (true)
        {
            try {
                if (mLogLevel >= LOG_LEVEL_ON_CALL) {
                    Log.i("liveProcessing", "loop awakes (connected: " + mConnected + ", isDataProcessRunning: " + dataHandler.isDataProcessRunning() + "");
                }

                if (dataHandler.isDataProcessRunning() && dataHandler.isSensorDataInStreaming() && mConnected)
                {

                    /*pull all feature of ads*/
                    pollChartValueFromQueue(ads_feature);
                    /*pull all feature of acc*/
                    pollChartValueFromQueue(acc_feature);

                    if (has_new_acc) {

                        moduleACC.doStuff(acc_x, acc_y, acc_z, snoring_rms, moving_rms_fir, moving_rms_iir);

                        if (mLogLevel >= LOG_LEVEL_ON_UPDATE) {
                            Log.i("LiveProcessing", "----- new acc packet processed -----");
                            Log.i("LiveProcessing", "   acc x / y / z : "
                                    + String.format("%.3f", moduleACC.getAccX()) + " / "
                                    + String.format("%.3f", moduleACC.getAccY()) + " / "
                                    + String.format("%.3f", moduleACC.getAccZ())
                            );

                            Log.i("LiveProcessing", "   rms snore / moveFir / moveIIR : "
                                    + String.format("%.3f", moduleACC.getRmsSnore()) + " / "
                                    + String.format("%.3f", moduleACC.getRmsMove1()) + " / "
                                    + String.format("%.3f", moduleACC.getRmsMove2()));
                        }
                    }
                    if (has_new_ads) {
                        moduleECG.doStuff(ads_rr_interval, ads_ecg_rms, ads_emg_rms);

                        if (mLogLevel >= LOG_LEVEL_ON_UPDATE) {
                            Log.i("LiveProcessing", "----- new adc packet processed -----");
                            Log.i("LiveProcessing", "   rr / rmsEcg / rmsEmg : "
                                    + moduleECG.getRR() + " / "
                                    + String.format("%.6f", moduleECG.getRmsEcg()) + " / "
                                    + String.format("%.6f", moduleECG.getRmsEmg()));
                        }
                    }
                } // end of processing
            } // end of update
            catch (Exception e){
                e.printStackTrace();
                Log.i("liveProcessing", "return out of run-function ...");
                return;
            }

            try {
                Thread.sleep(100);//in milisecond
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
