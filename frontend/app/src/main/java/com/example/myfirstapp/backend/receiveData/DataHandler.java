package com.example.myfirstapp.backend.receiveData;

import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.HEADER_ACC_SENSOR_FEATURE;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.HEADER_ADC_SENSOR_FEATURE;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.HEADER_BATTERY;
import static com.example.myfirstapp.backend.storageData.DataRecordingControl.FLAG_ENABLE_STORAGE_DATA;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.gintNumberByteInFile;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.isDeviceMeasuring;

import android.util.Log;

import com.example.myfirstapp.backend.BLEDataProcessListener;
//import com.example.myfirstapp.backend.DataListener;
import com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection;
import com.example.myfirstapp.backend.liveProcessing.liveProcessingPullData;
import com.example.myfirstapp.backend.storageData.DataRecordingControl;


import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class DataHandler {
    private static final DataHandler dataHanler = new DataHandler();

    private DataProcess dataProcess;

    //private DataListener dataListenerDataReceive;

    private BLEDataProcessListener bleListener = BluetoothBLEConnection.getInstance();

    private liveProcessingPullData liveProcessingPullData;

    private final int SIZE_OF_BUFFER_MPCHART_QUEUE = 1000;

    public ArrayBlockingQueue<Float> fQueueADS_RR_Interval  = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueADS_ECG_RMS      = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueADS_EMG_RMS      = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);

    public ArrayBlockingQueue<Float> fQueueACC_AXES_X_5Hz = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueACC_AXES_Y_5Hz = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueACC_AXES_Z_5Hz = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueACC_Snoring_RMS = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueACC_Moving_RMS_IIR = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);
    public ArrayBlockingQueue<Float> fQueueACC_Moving_RMS_FIR = new ArrayBlockingQueue<>(SIZE_OF_BUFFER_MPCHART_QUEUE);

    final int RR_Interval       = 0x01;
    final int ECG_RMS           = 0x02;
    final int EMG_RMS           = 0x03;

    final int ACC_X_5Hz         = 0x01;
    final int ACC_Y_5Hz         = 0x02;
    final int ACC_Z_5Hz         = 0x03;
    final int Snoring_RMS       = 0x04;
    final int Moving_RMS_IIR    = 0x05;
    final int Moving_RMS_FIR    = 0x06;


    private DataRecordingControl dataRecordingControl;
    private boolean isDataProcessRunning = false;


    /****************************** Reconnect: control **********************************/
    int nbByte_Reading = 0;
    public boolean isSensorDataInStreaming() {
        return isSensorDataInStreaming;
    }

    public void setSensorDataInStreaming(boolean sensorDataInStreaming) {
        isSensorDataInStreaming = sensorDataInStreaming;
        if(!isSensorDataInStreaming) {
            nbByte_Reading = 0;
        }
    }

    private boolean isSensorDataInStreaming = false;
    /******************************Reconnect**********************************/


    public boolean isDataProcessRunning() {
        return isDataProcessRunning;
    }

    private DataHandler() {
       dataRecordingControl = DataRecordingControl.getInstance();
    }

    public static DataHandler getInstance(){
        Log.d("TAG_Handler", "DataHandler getInstance");
        return dataHanler;
    }

    public void onDataTransfer(byte[] data){
        dataProcess.setDataToQueueInDataProcess(data);
    }

    public void startDataProcessThread(){
        if (!isDataProcessRunning)
        {
            /*start thread data processing*/
            if (dataProcess == null){
                dataProcess = new DataProcess();
            }
            dataProcess.startDataProcess();//in thread function
            dataProcess.start();
            Log.d("TAG_Handler", "start Data Process Thread");
            isDataProcessRunning = true;

            /*start thread running live processing pull data*/
            if (liveProcessingPullData == null)
            {
                liveProcessingPullData = new liveProcessingPullData();
            }
            liveProcessingPullData.liveProcessingGet_DataHandler_Instance();
            liveProcessingPullData.start();

            Log.d("TAG_Handler", "start liveProcessing Thread");
        }
    }

    public void stopDataProcessThread(){
        if (isDataProcessRunning)
        {
            /*stop thread data processing*/
            if (dataProcess != null)
            {
                dataProcess.stopDataProcess();

                try {
                    dataProcess.join(100);
                    dataProcess = null;
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("TAG_Handler", "stopDataProcessThread");
                isDataProcessRunning = false;
            }

            /*stop thread live Processing pull Data*/
            if (liveProcessingPullData != null)
            {
                try {
                    liveProcessingPullData.join(100);
                    liveProcessingPullData = null;
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private String convertDataToString(byte[] data, int length){
        String text = String.valueOf(", " + data[0]);
        for (int i = 1;i<length;i++)
            text = text + ", " + data[i];
        return text;
    }


    private float decodeADS_feature(byte value1, byte value2, byte value3, byte value4, int typeFeature)
    {
        final float ADC_V_REF   = 3.3f; // reference voltage of sleepbuddy
        final float ADC_V_RES   = 24f;  // bit resolution of acc
        final float ADC_V_GAIN  = 40f;  // configured gain of sleepbuddy

        // transformation factor value to physical unit [g]
        final float ADC_VAL_TO_PHYS = (float) ( (1f/ADC_V_GAIN) * (ADC_V_REF / ( Math.pow(2, ADC_V_RES - 1) )));

        // transformation correction factor because of int to float conversison (because of compression of 24 bit int to float with range of -1 to +1)
        final float ADC_FLOAT_CONVERSION_SCALE = 8388608f;

        float rVal  = 0;

        switch (typeFeature)
        {
            case RR_Interval:
                int ecg_rr = ( ((value3 << 8)&0xff00) |  (value4&0xff)); // combine bytes (24 bit uint)
                rVal = ecg_rr / 1000f; // transform rr interval from milliseconds to seconds
                Log.i("ADS_Feature", "RR_Interval: " + rVal + " s");
                break;

            case ECG_RMS:
                int ecg_rms = ( ((value1 << 24)&0xff000000) | ((value2 << 16)&0xff0000) | ((value2 << 8)&0xff00) |  (value4&0xff)); // combine bytes (binary 32 bit float)
                rVal = Float.intBitsToFloat(ecg_rms) * ADC_VAL_TO_PHYS * ADC_FLOAT_CONVERSION_SCALE; // transform to physical unit Volt
                Log.i("ADS_Feature", "ECG_RMS: " + rVal+ " V");
                break;

            case EMG_RMS:
                int emg_rms = ( ((value1 << 24)&0xff000000) | ((value2 << 16)&0xff0000) | ((value2 << 8)&0xff00) |  (value4&0xff)); // combine bytes (binary 32 bit float)
                rVal = Float.intBitsToFloat(emg_rms) * ADC_VAL_TO_PHYS * ADC_FLOAT_CONVERSION_SCALE; // transform to physical unit Volt
                Log.i("ADS_Feature", "EMG_RMS: " + rVal+ " V");
                break;

            default:
                break;
        }
        return  rVal;
    }


    private float decodeACC_feature(byte data_high, byte data_medium_high, byte data_medium_low , byte data_low, int typeFeature)
    {
        final float ACC_G_REF = 2; // configured acc measure range of sleepbuddy (in g)
        final float ACC_G_RES = 16; // acc bit resolution of sleepbuddy

        // transformation factor value to physical unit [V]
        final float ACC_VAL_TO_PHYS = (float) (ACC_G_REF / ( Math.pow(2, ACC_G_RES - 1) )); // transformation factor value to physical unit [g]

        // transformation correction factor because of int to float conversison (because of compression of 16 bit int to float with range of -1 to +1)
        final float ACC_FLOAT_CONVERSION_SCALE = 32768f;

        float rVal ;
        int intrVal;

        float returnValue = 0;

        switch (typeFeature)
        {
            case ACC_X_5Hz:
                intrVal = ((data_medium_low << 8)&0xff00) |  (data_low&0xff);  // combine bytes (16 bit uint)
                if (intrVal>32768) { intrVal = intrVal - 65536; } // reinterpret uint to int
                returnValue = (float) intrVal * ACC_VAL_TO_PHYS; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "ACC-X 5Hz: " + intrVal + "  |  " + returnValue + " g");
                break;

            case ACC_Y_5Hz:
                intrVal = ((data_medium_low << 8)&0xff00) |  (data_low&0xff);  // combine bytes (16 bit uint)
                if (intrVal>32768) { intrVal = intrVal - 65536; } // reinterpret uint to int
                returnValue = (float) intrVal * ACC_VAL_TO_PHYS; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "ACC-Y 5Hz: " + intrVal + "  |  " + returnValue + " g");
                break;

            case ACC_Z_5Hz:
                intrVal = ((data_medium_low << 8)&0xff00) |  (data_low&0xff);  // combine bytes (16 bit uint)
                if (intrVal>32768) { intrVal = intrVal - 65536; } // reinterpret uint to int
                returnValue = (float) intrVal * ACC_VAL_TO_PHYS; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "ACC-Z 5Hz: " + intrVal +  "  |  " + returnValue + " g");
                break;

            case Snoring_RMS:
                rVal = Float.intBitsToFloat(((data_high << 24)&0xff000000) | ((data_medium_high << 16)&0xff0000) | ((data_medium_low << 8)&0xff00) |  (data_low&0xff)); // combine bytes (binary 32 bit float)
                returnValue = rVal * ACC_VAL_TO_PHYS * ACC_FLOAT_CONVERSION_SCALE; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "Snoring_RMS: " + rVal + "  |  " + returnValue + " g");
                break;


            case Moving_RMS_IIR:
                rVal = Float.intBitsToFloat(((data_high << 24)&0xff000000) | ((data_medium_high << 16)&0xff0000) | ((data_medium_low << 8)&0xff00) |  (data_low&0xff)); // combine bytes (binary 32 bit float)
                returnValue = rVal * ACC_VAL_TO_PHYS * ACC_FLOAT_CONVERSION_SCALE; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "Moving_RMS_IIR: " + rVal + "  |  " + returnValue + " g");
                break;

            case Moving_RMS_FIR:
                rVal = Float.intBitsToFloat( ((data_high << 24)&0xff000000) | ((data_medium_high << 16)&0xff0000) | ((data_medium_low << 8)&0xff00) |  (data_low&0xff)); // combine bytes (binary 32 bit float)
                returnValue = rVal * ACC_VAL_TO_PHYS * ACC_FLOAT_CONVERSION_SCALE; // transform to physical unit acceleration in g
                Log.i("ACC Feature", "Moving_RMS_FIR: " + rVal + "  |  " + returnValue + " g");
                break;

            default:
                break;
        }

        return  returnValue;
    }


    public class DataProcess extends Thread
    {
        private boolean isRunningDataProcess;
        private volatile ArrayBlockingQueue<Byte> dataQueueProcess;

        public DataProcess(){
            dataQueueProcess = new ArrayBlockingQueue<>(5000);
            Log.e("TAG_Handler", "initDataProcess");
        }

        public void startDataProcess(){
            isRunningDataProcess = true;
            Log.e("TAG_Handler", "startDataProcess");
        }

        public void stopDataProcess(){
            isRunningDataProcess = false;
            Log.e("TAG_Handler", "stopDataProcess");
        }

        public void setDataToQueueInDataProcess(byte[] queueInput){
            byte[] convert = new byte[queueInput.length];
            System.arraycopy(queueInput,0,convert,0,convert.length);
            Log.e("TAG_Handler", "setDataProcess   " + String.valueOf(queueInput.length));
            for (int i= 0;i<convert.length;i++){
                try{
                    dataQueueProcess.add(convert[i]);
                } catch (IllegalStateException illegal) {
                    Log.e("dataQueueProcess", "InsertData get error");
                } catch (NullPointerException nULL){
                    Log.e("dataQueueProcess", "InsertData get null");
                }
            }
        }


        /********************************************Data decoding**************************************************************/
        int numberCompletePackage           = 0;
        byte number_Package_for_Decoding    = 5;

        int length_buffer_for_Queue         = 1000;
        byte[] dataBuffer_getFromQueue      = new byte[length_buffer_for_Queue];// save all data from bluetooth module
        byte[] dataBuffer_Rest              = new byte[50];                     // the rest of buffer from last processing
        byte[] dataBuffer_Combine           = new byte[length_buffer_for_Queue];// combine 2 buffer(new and the rest)
        byte[] dataBuffer_setDecodeRaw      = new byte[length_buffer_for_Queue];// save all completed data package
        int numberByte_in_dataBuffer_Rest   = 0;
        int index_dataBuffer_setDecodeRaw   = 0;


        public void getDataFromQueueInDataProcess(ArrayBlockingQueue<Byte> queueOutput){
            int len = queueOutput.size();

            if (queueOutput.isEmpty())
            {
                return;
            }else
            {
                //reset buffer
                Arrays.fill(dataBuffer_Combine, (byte)0);

                //step 1: take all data from bluetooth buffer
                for (int i = 0;i<len;i++){
                    dataBuffer_getFromQueue[i] = queueOutput.poll();
                }

                // check Bluetooth receive Data
                nbByte_Reading = nbByte_Reading + len;
                if (nbByte_Reading > 200)
                {
                    isSensorDataInStreaming = true;
                }

                /**********************save raw data before decoding**********************/
                if (FLAG_ENABLE_STORAGE_DATA){
                    if (dataRecordingControl.isRecording())
                    {
                        dataRecordingControl.writeDataToFile(convertDataToString(dataBuffer_getFromQueue,len));
                        gintNumberByteInFile = gintNumberByteInFile + len;
                    }
                }


                /**********************live:convert sub-features to features**********************/
                if (isDeviceMeasuring)
                {
                    //step 2: combine 2 buffer: [buffer_rest] + [getFromQueue]
                    for (int i = 0;i<numberByte_in_dataBuffer_Rest;i++){
                        dataBuffer_Combine[i] = dataBuffer_Rest[i];
                    }

                    for (int i = 0;i<len;i++){
                        dataBuffer_Combine[numberByte_in_dataBuffer_Rest+i] = dataBuffer_getFromQueue[i];
                    }

                    //step 3: decode to find the number of completed data Package from Buffer
                    int length_buffer_combine = numberByte_in_dataBuffer_Rest + len;// update length of Buffer combine: len(received from bluetooth) + numberByte_in_dataBuffer_Rest(the rest from last processing)

                    int j;
                    int beginRest_in_buffer_Combine = length_buffer_combine - 2;

                    numberByte_in_dataBuffer_Rest = 2;//reset a value to init this variable

                    for (j = 0;j<(length_buffer_combine-3);j++)// -3: for case the 2 byte last: -126 -- 12
                    {
                        if ((dataBuffer_Combine[j] == -126) && (dataBuffer_Combine[j+1] == 12) &&
                                ((dataBuffer_Combine[j+2] == HEADER_ADC_SENSOR_FEATURE) || (dataBuffer_Combine[j+2] == HEADER_ACC_SENSOR_FEATURE) || (dataBuffer_Combine[j+2] == 6 )  || (dataBuffer_Combine[j+2] == 7)))
                        {
                            if ((length_buffer_combine-j) < 35)
                            {
                                numberByte_in_dataBuffer_Rest = length_buffer_combine-j;
                                beginRest_in_buffer_Combine = j;
                                j = length_buffer_combine;//jump out this function
                            }else
                            {

                                dataBuffer_setDecodeRaw[index_dataBuffer_setDecodeRaw++] = dataBuffer_Combine[j];
                                dataBuffer_setDecodeRaw[index_dataBuffer_setDecodeRaw++] = dataBuffer_Combine[j+1];
                                dataBuffer_setDecodeRaw[index_dataBuffer_setDecodeRaw++] = dataBuffer_Combine[j+2];

                                for (int k = 0;k<32;k++){
                                    dataBuffer_setDecodeRaw[index_dataBuffer_setDecodeRaw++] = dataBuffer_Combine[j+3+k];
                                }

                                numberCompletePackage++;

                                nbByte_Reading++;
                                if (nbByte_Reading > 5){
                                    isSensorDataInStreaming = true;
                                }
                            }
                        }
                    }

                    /*step 4: if we get enough completed Package, we go to decode feature*/
                    if (numberCompletePackage >= number_Package_for_Decoding)// always process 5 data package
                    {
                        decodeDataBufferInDataProcess(dataBuffer_setDecodeRaw,numberCompletePackage*35);

                        numberCompletePackage = 0;
                        index_dataBuffer_setDecodeRaw = 0;
                    }

                    /*step 5: take number rest in buffer and save into buffer_rest*/
                    Arrays.fill(dataBuffer_Rest, (byte)0);

                    for (int k = 0;k<numberByte_in_dataBuffer_Rest;k++){
                        dataBuffer_Rest[k]  = dataBuffer_Combine[beginRest_in_buffer_Combine + k];
                    }
                }
            }
        }



        public void decodeDataBufferInDataProcess(byte [] dataBuffer, int lengthBuffer)
        {
            byte[] dataFrame = new byte[lengthBuffer];
            System.arraycopy(dataBuffer,0,dataFrame,0,lengthBuffer);

            /*decoding data sensor*/
            for (int i = 0;i<lengthBuffer;i++)
            {
                /*ads feature*/
                if ((dataFrame[i] == -126)&&(dataFrame[i+1] == 12)&&(dataFrame[i+2] == HEADER_ADC_SENSOR_FEATURE))
                {
                    if (fQueueADS_RR_Interval.size()<(SIZE_OF_BUFFER_MPCHART_QUEUE-4))
                    {
                        fQueueADS_RR_Interval.add(decodeADS_feature((byte)0,(byte) 0,dataFrame[i+3],dataFrame[i+4], RR_Interval));
                        fQueueADS_ECG_RMS.add(decodeADS_feature(dataFrame[i+5],dataFrame[i+6],dataFrame[i+7],dataFrame[i+8], ECG_RMS));
                        fQueueADS_EMG_RMS.add(decodeADS_feature(dataFrame[i+9],dataFrame[i+10],dataFrame[i+11],dataFrame[i+12], EMG_RMS));
                    }else {
                        Log.i("DataHandler", "fQueueADSFeature: fullllllllllllllllll");
                    }
                    i = i+34;
                }

                /*acc feature*/
                if ((dataFrame[i] == -126)&&(dataFrame[i+1] == 12)&&(dataFrame[i+2] == HEADER_ACC_SENSOR_FEATURE))
                {
                    if (fQueueACC_Snoring_RMS.size()<(SIZE_OF_BUFFER_MPCHART_QUEUE-4))
                    {
                        fQueueACC_AXES_X_5Hz.add(decodeACC_feature((byte) 0,(byte) 0,dataFrame[i+3],dataFrame[i+4], ACC_X_5Hz));
                        fQueueACC_AXES_Y_5Hz.add(decodeACC_feature((byte) 0,(byte) 0,dataFrame[i+5],dataFrame[i+6], ACC_Y_5Hz));
                        fQueueACC_AXES_Z_5Hz.add(decodeACC_feature((byte) 0,(byte) 0,dataFrame[i+7],dataFrame[i+8], ACC_Z_5Hz));

                        fQueueACC_Snoring_RMS.add(decodeACC_feature(dataFrame[i+9],dataFrame[i+10],dataFrame[i+11],dataFrame[i+12], Snoring_RMS));
                        fQueueACC_Moving_RMS_IIR.add(decodeACC_feature(dataFrame[i+13],dataFrame[i+14],dataFrame[i+15],dataFrame[i+16], Moving_RMS_IIR));
                        fQueueACC_Moving_RMS_FIR.add(decodeACC_feature(dataFrame[i+17],dataFrame[i+18],dataFrame[i+19],dataFrame[i+20], Moving_RMS_FIR));
                    }
                    else {
                        Log.i("DataHandler", "fQueueACCFeature: fullllllllllllllllll");
                    }
                    i = i+34;
                }

                /*sleepBuddy battery value*/
                if ((dataFrame[i] == -126)&&(dataFrame[i+1] == 12)&&(dataFrame[i+2] == HEADER_BATTERY))
                {
                    Log.i("DataHandler", "battery packet: " +  dataFrame[i+3]*255+dataFrame[i+4]);
                    i = i+19;
                }
            }

        }


        @Override
        public void run() {
            while (isRunningDataProcess)
            {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bleListener.onDataReceivedFromBluetooth();

                if (!dataQueueProcess.isEmpty()){
                    getDataFromQueueInDataProcess(dataQueueProcess);
                }
            }
        }
    }
}
