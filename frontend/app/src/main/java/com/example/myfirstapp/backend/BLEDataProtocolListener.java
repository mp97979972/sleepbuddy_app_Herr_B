package com.example.myfirstapp.backend;

/*use to check respond Command from SleepBuddy */
public interface BLEDataProtocolListener {
    void onRespondUpdateFromBluetooth(byte[] data);
}
