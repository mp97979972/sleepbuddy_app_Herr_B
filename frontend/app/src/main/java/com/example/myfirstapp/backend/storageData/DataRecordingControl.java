package com.example.myfirstapp.backend.storageData;

import android.os.Environment;
import android.util.Log;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataRecordingControl {

    public static final boolean FLAG_ENABLE_STORAGE_DATA = false;

    private static final DataRecordingControl dataRecord = new DataRecordingControl();
    private BufferedWriter writers;
    private File directory;
    private File file;
    private boolean isRecording = false;
    public String nameOfFilteWrite ;

    public DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");


    private DataRecordingControl(){
        // init path of file, before save data
    }

    public static DataRecordingControl getInstance(){
        return dataRecord;
    }

    public void initDirectory(File file){
        String path = "SleepBuddy_Data";

        /*
        #note: we can choose where we save the data in tablet
        */
        /*1. in cache data of app, only read with computer*/
        directory = new File(file,path);

        /*2. in internal memory of device, we can read without computer*/
        //directory = new File(Environment.getExternalStorageDirectory(),path);

        if (!directory.exists()){
            directory.mkdirs();
        }
    }

    private void initFiles()
    {
        String formatTime = simpleDateFormat.format(Calendar.getInstance().getTime());

        file = new File(directory, "SleepBuddy_Measure" +"_" + formatTime +  ".txt");

        nameOfFilteWrite = file.getName();
    }

    private void initWriters(){
        try{
            writers = new BufferedWriter(new FileWriter(file,false));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecording(){
        initFiles();
        initWriters();
        isRecording = true;
    }

    public void stopRecording(){
        try {
            writers.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRecording = false;
    }

    public void writeDataToFile(String data){
        try {
            writers.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording(){
        return isRecording;
    }

    private void writeHeaderOfFile(){
        String NameOfFile = "here is the Header of sleepBuddy File \n\r";

        String SensorConfig =   "ADS: sampleRate 1000Hz, downsampling 200Hz \n\r" +
                                "ACC: sampleRate 400Hz, downsampling 200Hz \n\r";


        writeDataToFile(NameOfFile);
        writeDataToFile(SensorConfig);
    }

    public File getDirectory(){
        return directory;
    }
}
