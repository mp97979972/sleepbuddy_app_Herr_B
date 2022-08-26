package com.example.myfirstapp.backend.protocolDriver;

public class protocolConstant {

    /*define all protocol we use for each device*/
    public final static byte FWCommit               = 0x16;
    public final static byte startSleepBuddy        = 0x20;
    public final static byte stopSleepBuddy         = 0x21;
    public final static byte checkStatusSensor      = 0x23;

    public final static byte HEADER_BATTERY                 = 0x02;
    public final static byte HEADER_ADC_SENSOR_FEATURE      = 0x04;
    public final static byte HEADER_ACC_SENSOR_FEATURE      = 0x05;

}
