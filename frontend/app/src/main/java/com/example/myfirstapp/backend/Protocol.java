package com.example.myfirstapp.backend;

import static com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection.convertToHex;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.checkStatusSensor;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.startSleepBuddy;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.stopSleepBuddy;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.isDeviceMeasuring;

import android.util.Log;

import com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection;
import com.example.myfirstapp.backend.protocolDriver.checksumCalculator;
import com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement;

import java.util.Arrays;

public class Protocol implements BLEDataProtocolListener {

    BluetoothBLEConnection mBluetoothBLEConnectionProtocol = BluetoothBLEConnection.getInstance();

    /************************************************************************************************/
    public void startSleepBuddyOnButton()
    {
        byte[] startSleepBuddyBuff = new byte[] {0x01,startSleepBuddy,0x01,0x00,0x00,0x00,0x1a,0x09};
        int size = startSleepBuddyBuff.length;
        short crcFinal;

        crcFinal = checksumCalculator.gap_crc(startSleepBuddyBuff,0,size-2);

        startSleepBuddyBuff[size - 2] = (byte) ((crcFinal >> 8) & 0xFF);
        startSleepBuddyBuff[size - 1] = (byte) ((crcFinal) & 0xFF);

        mBluetoothBLEConnectionProtocol.writeBytesToSleepBuddy(startSleepBuddyBuff);
        Log.i("sendCommandToDevice", "startSleepBuddyOnButton: " +  Arrays.toString(startSleepBuddyBuff));
    }

    public void stopSleepBuddyOnButton()
    {
        byte[] stopSleepBuddyBuff = new byte[] {0x01,stopSleepBuddy,0x01,0x00,0x00,0x00,0x1a,0x09};
        int size = stopSleepBuddyBuff.length;
        short crcFinal;

        crcFinal = checksumCalculator.gap_crc(stopSleepBuddyBuff,0,size-2);

        stopSleepBuddyBuff[size - 2] = (byte) ((crcFinal >> 8) & 0xFF);
        stopSleepBuddyBuff[size - 1] = (byte) ((crcFinal) & 0xFF);

        mBluetoothBLEConnectionProtocol.writeBytesToSleepBuddy(stopSleepBuddyBuff);
        Log.i("sendCommandToDevice", "stopSleepBuddyOnButton: " +  Arrays.toString(stopSleepBuddyBuff));
    }

    public void checkStatusSensorButton()
    {
        mBluetoothBLEConnectionProtocol.getInstance().setDataListener(this);

        byte[] checkStatusSensorBuff = new byte[] {0x01, checkStatusSensor,0x01,0x00,0x00,0x00,0x1a,0x09};
        int size = checkStatusSensorBuff.length;
        short crcFinal;
        crcFinal = checksumCalculator.gap_crc(checkStatusSensorBuff,0,size-2);

        checkStatusSensorBuff[size - 2] = (byte) ((crcFinal >> 8) & 0xFF);
        checkStatusSensorBuff[size - 1] = (byte) ((crcFinal) & 0xFF);

        mBluetoothBLEConnectionProtocol.writeBytesToSleepBuddy(checkStatusSensorBuff);
        Log.i("sendCommandToDevice", "checkStatusSensorButton: " +  Arrays.toString(checkStatusSensorBuff));
    }




    @Override
    public void onRespondUpdateFromBluetooth(byte[] data)
    {
        if (!isDeviceMeasuring)
        {
            // check status of sensor
            if (data[0] == 0x02 && data[1] == checkStatusSensor)
            {
                if (data[6] == 0x01){
                    FragmentLiveMeasurement.checkSensor_Success_Sleepbuddy();
                } else if (data[6] == 0x00){
                    FragmentLiveMeasurement.checkSensor_Fail_SleepBuddy();
                }
            }

            Log.e("onDataUpdate", "fragmentProtocol:   " + Arrays.toString(convertToHex(data)));
        }
    }

}
