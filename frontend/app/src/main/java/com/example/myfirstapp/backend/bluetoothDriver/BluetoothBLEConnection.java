package com.example.myfirstapp.backend.bluetoothDriver;

import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.FWCommit;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.isDeviceMeasuring;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.myfirstapp.backend.BLEDataProcessListener;
import com.example.myfirstapp.backend.ConnectionListener;
import com.example.myfirstapp.backend.BLEDataProtocolListener;
import com.example.myfirstapp.backend.GlobalMessage;
import com.example.myfirstapp.backend.protocolDriver.checksumCalculator;
import com.example.myfirstapp.backend.receiveData.DataHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class BluetoothBLEConnection implements BLEDataProcessListener {
    public static final UUID DEFAULT_UART_SERVICE_UUID  =   UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID DEFAULT_TX_UUID            =   UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID DEFAULT_RX_UUID            =   UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CLIENT_DESCRIPTOR_CONFIG   =   UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final static String TAG = "Bluetooth Connection";

    private static final int ConnectionTimeOut = 8000;

    private static BluetoothBLEConnection bleConnection = null;



    public static BluetoothBLEConnection getInstance(){
        if (bleConnection == null)
            bleConnection = new BluetoothBLEConnection();
        return bleConnection;
    }

    //avoid to create another BLEConnection in another class
    private BluetoothBLEConnection(){}

    private static BluetoothGatt mBluetoothGatt;
    private static BluetoothGattService mBluetoothGattService;

    public static boolean mConnected = false;

    private int length_data_input = 0;

    public static ArrayBlockingQueue<Byte> inPutByteQueue = new ArrayBlockingQueue(10000);// receive data from Device via Bluetooth

    public static ArrayBlockingQueue<Byte> outPutByteQueue = new ArrayBlockingQueue(3000);//send data from Bluetooth via Device

    public void setDataListener(BLEDataProtocolListener dataListener) {
        this.dataListenerFromBluetooth.add(dataListener);
    }

    private List<BLEDataProtocolListener> dataListenerFromBluetooth = new ArrayList<>();

    private  byte[] protocolGetFWCommit_Send      = new byte[] { 0x01, FWCommit, 0x00, 0x00, 0x00, 0x00, (byte) 0x82, 0x6f};//length Payload = 0, length Command = 8

    /**
     * BluetoothGatt Service Register
     * with Bluetooth Gatt CallBack
     */
    private final BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            List<ConnectionListener> listeners = GlobalMessage.getInstance().getConnectionListenerList();

            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                mBluetoothGatt.discoverServices();
                mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                if (!listeners.isEmpty()){
                    for (ConnectionListener listener : listeners){
                        listener.onConnected();
                    }
                }

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        /*waiting the connection stable: 10 seconds*/
                        int numberCycle = 0;
                        while (numberCycle < 10) {
                            numberCycle++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        /*read number commit of firmware*/
                        bleConnection.writeBytesToSleepBuddy(protocolGetFWCommit_Send);
                        mConnected = true;

                        if (isDeviceMeasuring)// if the reconnection happens while measuring
                        {
                            int tryRestart = 0;
                            while (!DataHandler.getInstance().isSensorDataInStreaming())
                            {
                                //take commit number again
                               startSBAfterInterruptConnection();

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                tryRestart = tryRestart+1;
                            }

                            //take commit number again: for sure
                            isDeviceMeasuring = false;
                            bleConnection.writeBytesToSleepBuddy(protocolGetFWCommit_Send);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            isDeviceMeasuring = true;
                        }
                    }
                }).start();
            }



            if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                mConnected = false;

                DataHandler.getInstance().setSensorDataInStreaming(false);

                if (!listeners.isEmpty())
                    for (ConnectionListener listener : listeners)
                        listener.onDisconnected();


                new Thread(new Runnable()
                {
                    @Override
                    public void run() {
                        int time = 0;
                        if (mBluetoothGatt != null)
                        {
                            while (!mConnected && time < ConnectionTimeOut)
                            {
                                mBluetoothGatt.connect();
                                try {
                                    Thread.sleep(1000);
                                    time += 1000;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }
        }



        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                return;
            }

            // Register Characteristics
            mBluetoothGattService = mBluetoothGatt.getService(DEFAULT_UART_SERVICE_UUID);
            if (mBluetoothGattService != null)
            {
                Log.i(TAG, "Service characteristic UUID found: " + mBluetoothGattService.getUuid().toString());
            } else
            {
                Log.i(TAG, "Service characteristic not found for UUID: ");
            }

            BluetoothGattCharacteristic characteristicRead = mBluetoothGattService.getCharacteristic(DEFAULT_TX_UUID);

            // Register Descriptor to Notify on CharacteristicsRead
            mBluetoothGatt.setCharacteristicNotification(characteristicRead, true);

            BluetoothGattDescriptor descriptor = characteristicRead.getDescriptor(CLIENT_DESCRIPTOR_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic == null) {
                Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request");
                return;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            readGattCharacteristic(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

    };



    /*******************************************Receive data from Bluetooth Device******************************************************/
    public void readGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (DEFAULT_TX_UUID.equals(characteristic.getUuid()))
        {
            byte[] data = characteristic.getValue();

            /*save data in queue to data process*/
            if (data != null)
            {
                if (inPutByteQueue.size() < 9000){
                    insertDataToArrayRead(inPutByteQueue, data); 
                }
                
                Log.d(TAG, "readBytes:   " + Arrays.toString(convertToHex(data)));
            }

            /* set notification for some task(protocol task) , that need to check respond*/
            for (BLEDataProtocolListener listener:dataListenerFromBluetooth)
            {
                listener.onRespondUpdateFromBluetooth(data);
            }
        }

        return;
    };


    private byte[] getDataFromQueue(ArrayBlockingQueue<Byte> array, int len){
        Log.d("getDataFromQueue", "-----------------------" + len);
        byte[] data = new byte[len];
        for (int i=0; i<len;i++) {
            try {
                data[i] = array.poll();
            } catch (IllegalStateException illegal) {
                Log.e("dataQueue", "InsertData get error");
            } catch (NullPointerException nULL) {
                Log.e("dataQueue", "InsertData get null");
            }
        }
        return data;
    }

    @Override
    public void onDataReceivedFromBluetooth()
    {
        if (inPutByteQueue.size()>200)
        {
            DataHandler.getInstance().onDataTransfer(getDataFromQueue(inPutByteQueue,inPutByteQueue.size()));
        }
    }




    /********************************************Write data to device via bluetooth***************************************************/
    public void writeCharacteristicsCommand(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null)
        {
            Log.i(TAG, "NO BLUETOOTH CONNECTION");
            return;
        }

        byte[] writeData;
        int check;

        writeData = new byte[outPutByteQueue.size()];
        check = getDataFromArray(outPutByteQueue, writeData, 0, outPutByteQueue.size());

        if (check == 0)
            return;

        if (DEFAULT_RX_UUID.equals(characteristic.getUuid()))
        {
            characteristic.setValue(writeData);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothGatt.writeCharacteristic(characteristic);

            Log.d(TAG, "Send Data to Device: " + Arrays.toString(writeData));
        }
    }



    /**
     * Get Data From a Byte Array
     * and move to an ArrayBlockingQueue for safety of Thread function
     */
    private static int getDataFromArray(ArrayBlockingQueue<Byte> paramArrayBlockingQueue, byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
        int i2 = paramArrayBlockingQueue.size();
        int i1 = paramInt2;
        if (i2 < paramInt2)
            i1 = i2;
        int index = 0;
        if (i1 <= 0)
            return 0;
        while (index < i1&&!paramArrayBlockingQueue.isEmpty()) {
            paramArrayOfByte[paramInt1 + index] = paramArrayBlockingQueue.poll();
            index++;
        }
        return i1;
    }



    public void writeBytesToSleepBuddy(byte[] arrayByte) {
        if (mBluetoothGattService != null)
        {
            if ((arrayByte != null)&&(mConnected))
            {
                insertDataToArrayWrite(outPutByteQueue, arrayByte);

                Log.d(TAG, "writeBytes:   " + Arrays.toString(convertToHex(arrayByte)) + "  " + mConnected);

                BluetoothGattCharacteristic characteristicWrite = mBluetoothGattService.getCharacteristic(DEFAULT_RX_UUID);

                writeCharacteristicsCommand(characteristicWrite);
            }
        }else {
            Log.e(TAG, "mBluetoothGattService null:   " + Arrays.toString(arrayByte));
        }
    }
    /*************************************************************************************************/




    /*************************************************************************************************/
    private void insertDataToArrayRead(ArrayBlockingQueue<Byte> array, byte[] data) {
        length_data_input = length_data_input + data.length;
        byte[] convertByte = new byte[data.length];
        System.arraycopy(data, 0, convertByte, 0, data.length);
        for (byte b : convertByte) {
            try {
                array.add(b);
            } catch (IllegalStateException illegal) {
                Log.w(TAG, "InsertData get error");
            } catch (NullPointerException nULL) {
                Log.e(TAG, "InsertData get null");
            }
        }
    }

    private void insertDataToArrayWrite(ArrayBlockingQueue<Byte> array, byte[] command) {
        Log.d(TAG, "insertDataToArray:   " + Arrays.toString(convertToHex(command)));

        byte[] convertByte = new byte[command.length];
        System.arraycopy(command, 0, convertByte, 0, command.length);
        for (byte b : convertByte) {
            try {
                array.add(b);
            } catch (IllegalStateException illegal) {
                Log.w(TAG, "InsertData get error");
            } catch (NullPointerException nULL) {
                Log.e(TAG, "InsertData get null");
            }
        }
    }

    public static  String[] convertToHex(byte [] data){
        String[] hexData = new String[data.length];
        for (int i=0;i<hexData.length;i++)
        {
            hexData[i] = Integer.toHexString(data[i]).toUpperCase();
            if (hexData[i].length()>2)
            {
                hexData[i] = hexData[i].substring(hexData[i].length()-2);
            }
        }
        return hexData;
    }

    /*************************************************************************************************/






    /*************************************************************************************************/
    public void connectToDevice(Context context,BluetoothDevice device)
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            mBluetoothGatt =  device.connectGatt(context,true, mGattCallBack,BluetoothDevice.TRANSPORT_LE) ;
        }else
        {
            mBluetoothGatt = device.connectGatt(context,true,mGattCallBack);
        }

        if (mBluetoothGatt.connect())
        {
            DataHandler.getInstance().startDataProcessThread();
        }
    };


    public void disconnectToDevice()
    {
        if (DataHandler.getInstance().isDataProcessRunning()){
            DataHandler.getInstance().stopDataProcessThread();
        }

        List<ConnectionListener> listeners = GlobalMessage.getInstance().getConnectionListenerList();
        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.close();
            mBluetoothGatt = null;

            if (!listeners.isEmpty()){
                for (ConnectionListener listener : listeners){
                    listener.onDisconnected();
                }
            }

        }
    };
    /*************************************************************************************************/

    public boolean ismConnected(){
        return mConnected;
    }


    public void startSBAfterInterruptConnection(){

        byte[] checkStatusSensorBuff = new byte[] {0x01,0x20,0x01,0x00,0x00,0x00,0x1a,0x09};
        int size = checkStatusSensorBuff.length;
        short crcFinal;

        crcFinal = checksumCalculator.gap_crc(checkStatusSensorBuff,0,size-2);

        checkStatusSensorBuff[size - 2] = (byte) ((crcFinal >> 8) & 0xFF);
        checkStatusSensorBuff[size - 1] = (byte) ((crcFinal) & 0xFF);

        writeBytesToSleepBuddy(checkStatusSensorBuff);

        Log.d("sendCommandToDevice", "startSleepBuddyOnButton: " +  Arrays.toString(checkStatusSensorBuff));
    }


}
