package com.example.myfirstapp.backend;

import java.util.ArrayList;
import java.util.List;

public class GlobalMessage {
    private static GlobalMessage instance = new GlobalMessage();
    private GlobalMessage(){}

    public static GlobalMessage getInstance() {
        return instance;
    }

    private List<ConnectionListener> connectionListenerList = new ArrayList<>();

    public void registerConnectionListener(ConnectionListener connectionListener){
        connectionListenerList.add(connectionListener);
    }

    public List<ConnectionListener> getConnectionListenerList(){
        return connectionListenerList;
    }

}

