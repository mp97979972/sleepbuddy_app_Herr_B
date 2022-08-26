package com.example.myfirstapp.backend;

/* use to send state of Bluetooh connection*/
public interface ConnectionListener {

    void onConnected();

    void onDisconnected();


}
