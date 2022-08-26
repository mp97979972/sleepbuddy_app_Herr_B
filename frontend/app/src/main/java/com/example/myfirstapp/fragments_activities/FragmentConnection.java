package com.example.myfirstapp.fragments_activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.myfirstapp.R;
import com.example.myfirstapp.backend.ConnectionListener;
import com.example.myfirstapp.backend.GlobalMessage;
import com.example.myfirstapp.backend.bluetoothDriver.*;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.checkSensorAndLiveMeasurementLayoutSleepBuddy;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.countDownBox;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.countDownTimer;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.imgConfirmStatusSensorSleepBuddy;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.isDeviceMeasuring;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.noConnectionBox;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.timerIsRunning;
import static com.example.myfirstapp.fragments_activities.FragmentLiveMeasurement.txtStatusSensorSleepBuddy;
import static com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection.mConnected;
import static com.example.myfirstapp.fragments_activities.MainScreen.bottomNavigationView;


import java.util.List;


public class FragmentConnection extends Fragment implements ConnectionListener {

    // Declare the View object references
    private Context mContext;
    private static final String TAG_SCAN = "bluetooth_scan";
    private static final String CONNECTION_VIEW = "Connection_view";

    public Button btScanBT, btConnectDisconnectBT;
    public ProgressBar progressBarScanBT;

    public ListView lvScanBTDevice;

    private MyListAdapterBLE myListAdapterBLE;
    private BluetoothAdapter mBluetoothAdapter = null;

    public static String mDeviceName;
    public static String mDeviceAddress;

    BluetoothBLEConnection mBluetoothBLEConnection = BluetoothBLEConnection.getInstance();

    private String[] PERMISSIONS;

    public FragmentConnection() {
        // Required empty public constructor
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_connection, container, false);

        btScanBT                = (Button) view.findViewById(R.id.btScanBT);
        btConnectDisconnectBT   = (Button) view.findViewById(R.id.btConnectDisconnectDevice);
        progressBarScanBT       = (ProgressBar) view.findViewById(R.id.prgbarScanBT);

        lvScanBTDevice          = (ListView) view.findViewById(R.id.lvScanBT);

        /***************************************************************************/
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        GlobalMessage.getInstance().registerConnectionListener(this);

        //Register for broadcasts when a device is discovered
        IntentFilter filter;
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);

        //Register for connect fail
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, filter);

        myListAdapterBLE = new MyListAdapterBLE(getContext(), R.layout.fragment_bluetooth_listbluetooth_element_sleepbuddy);
        lvScanBTDevice.setAdapter(myListAdapterBLE);

        progressBarScanBT.setVisibility(View.INVISIBLE);


        btScanBT.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onClick(View v)
            {
                Log.d(CONNECTION_VIEW, "Scan button clicked");
                if (Build.VERSION.SDK_INT < 31)
                {
                    PERMISSIONS = new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    };
                } else
                {
                    PERMISSIONS = new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                    };
                }

                if (!hasPermissions(getContext(), PERMISSIONS))
                {
                    Toast.makeText(getActivity(), R.string.permission_needed, Toast.LENGTH_SHORT).show();
                    PermissionX.init(getActivity())
                            .permissions(PERMISSIONS)
                            .request(new RequestCallback() {
                                @Override
                                public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
                                    if (allGranted) {
                                        if (hasPermissions(getContext(), PERMISSIONS)) {
                                            Toast.makeText(getActivity(), "Scan Bluetooth!", Toast.LENGTH_SHORT).show();
                                            // if bluetooth is disable, we need to enable bluetooth module
                                            if (!mBluetoothAdapter.isEnabled()) {
                                                mBluetoothAdapter.enable();
                                            }

                                            // clear list view
                                            myListAdapterBLE.clear();
                                            myListAdapterBLE.notifyDataSetChanged();

                                            //scan bluetooth
                                            mBluetoothAdapter.startDiscovery();
                                            progressBarScanBT.setVisibility(View.VISIBLE);
                                            btScanBT.setEnabled(false);
                                        }
                                    }
                                }
                            });
                } else
                {
                    Toast.makeText(getActivity(), "Scan Bluetooth!", Toast.LENGTH_SHORT).show();
                    // if bluetooth is disable, we need to enable bluetooth module
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }

                    lvScanBTDevice.setVisibility(View.VISIBLE);

                    //step1: clear list view
                    myListAdapterBLE.clear();
                    myListAdapterBLE.notifyDataSetChanged();

                    //step2: scan bluetooth
                    Log.d(CONNECTION_VIEW, "start discovery");
                    mBluetoothAdapter.startDiscovery();
                    progressBarScanBT.setVisibility(View.VISIBLE);
                    btScanBT.setEnabled(false);
                }
            }
        });

        btConnectDisconnectBT.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if (btConnectDisconnectBT.getText().equals(getString(R.string.connect_device)))
                {
                    Log.d(CONNECTION_VIEW, "Connect device button clicked");
                    if (myListAdapterBLE.getSelectedItem() == 0xff)
                    {
                        Toast.makeText(getActivity(), R.string.no_device_selected, Toast.LENGTH_SHORT).show();
                    } else
                    {
                        BluetoothDevice deviceWantToConnect = myListAdapterBLE.getDevice(myListAdapterBLE.getSelectedItem());

                        if (deviceWantToConnect != null){
                            mBluetoothBLEConnection.connectToDevice(mContext, deviceWantToConnect);
                            btScanBT.setEnabled(false);
                            progressBarScanBT.setVisibility(View.INVISIBLE);

                            Toast.makeText(getActivity(), "Name: " + "  " + deviceWantToConnect.getName() + "  address:  " + deviceWantToConnect.getAddress(), Toast.LENGTH_SHORT).show();

                            btConnectDisconnectBT.setText(R.string.disconnect_device);
                            myListAdapterBLE.clear();
                            myListAdapterBLE.notifyDataSetChanged();

                            noConnectionBox.setVisibility(View.GONE);

                            ((MainScreen) getActivity()).closeCurrentFragment();
                            ((MainScreen) getActivity()).fragmentManager.beginTransaction().show(((MainScreen) getActivity()).fragmentLiveMeasurement).commit();
                            ((MainScreen) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_live_measurement);

                            countDownTimer.start();
                            countDownBox.setVisibility(View.VISIBLE);
                        }
                    }
                } else if (btConnectDisconnectBT.getText().equals(getString(R.string.disconnect_device)))
                {
                    Log.d(CONNECTION_VIEW, "Disconnect device button clicked");
                    if (mBluetoothBLEConnection.ismConnected()) {
                        if (isDeviceMeasuring) {
                            Toast.makeText(getActivity(), R.string.please_stop_measurement_first, Toast.LENGTH_SHORT).show();
                        } else
                        {
                            mBluetoothBLEConnection.disconnectToDevice();
                            myListAdapterBLE.setSelectedItem(0xff);
                            myListAdapterBLE.notifyDataSetChanged();
                            btScanBT.setEnabled(true);

                            Toast.makeText(getActivity(), R.string.disconnect_device_toast, Toast.LENGTH_SHORT).show();
                            btConnectDisconnectBT.setText(R.string.connect_device);

                            txtStatusSensorSleepBuddy.setText("");
                            noConnectionBox.setVisibility(View.VISIBLE);
                            countDownBox.setVisibility(View.INVISIBLE);
                            checkSensorAndLiveMeasurementLayoutSleepBuddy.setVisibility(View.GONE);
                        }
                    } else{
                        Toast.makeText(getContext(), getString(R.string.wait_for_stable_connection), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        /***************************ListView Bluetooth for Fragment: begin*****************************/
        lvScanBTDevice.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice bluetoothDeviceChoose = myListAdapterBLE.getDevice(position);

                if (bluetoothDeviceChoose != null){
                    // show Information of Device we choose
                    mDeviceName = bluetoothDeviceChoose.getName();
                    mDeviceAddress = bluetoothDeviceChoose.getAddress();
                }
                myListAdapterBLE.setSelectedItem(position);
                myListAdapterBLE.notifyDataSetChanged();
                Log.e("TAG_CHOOSE_BLE ", "position: " + String.valueOf(position) + "   name: " + mDeviceName + "  address: " + mDeviceAddress);
            }
        });

        /***************************ListView Bluetooth for Fragment: end*****************************/

        return view;
    }

    private boolean hasPermissions(Context context, String... PERMISSIONS) {
        if (context != null && PERMISSIONS != null) {
            for (String permission : PERMISSIONS)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //when disconvery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                //Get the Bluetooth Device Object from the Intent
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MAX_VALUE);

                //if it is already paired, skip it
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //if device is different with the device is already in List view, we will add this device into Bluetooth Device list
                            Log.i(TAG_SCAN, "device=  " + device.getAddress() + "    rssi =   " + rssi + "  name:" + device.getName());
                            if (device.getName() != null) {
                                myListAdapterBLE.addDevice(device);
                                myListAdapterBLE.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }

            // when discovery is finished, change the Activity title
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.i(TAG_SCAN, "Please select a device to connect");
                progressBarScanBT.setVisibility(View.INVISIBLE);
                btScanBT.setEnabled(true);
            }
        }
    };


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterServices();

    }

    // unregister Receivers when app is destroyed
    public void unRegisterServices() {
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onConnected() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                bottomNavigationView.getMenu().getItem(0).setIcon(R.drawable.nav_bar_bluetooth_connected);
                imgConfirmStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);
            }
        });

        btConnectDisconnectBT.setText(getString(R.string.disconnect_device));
        btScanBT.setVisibility(View.INVISIBLE);
        lvScanBTDevice.setVisibility(View.INVISIBLE);
        progressBarScanBT.setVisibility(View.INVISIBLE);
        imgConfirmStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);

        Log.d(CONNECTION_VIEW, "ConnectionListener onConnected()");

        noConnectionBox.setVisibility(View.GONE);

        Handler handlerReconnection = new Handler(Looper.getMainLooper());
        handlerReconnection.post(new Runnable() {
            @Override
            public void run() {
                if (!timerIsRunning) {
                    checkSensorAndLiveMeasurementLayoutSleepBuddy.setVisibility(View.VISIBLE);
                    txtStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);
                    imgConfirmStatusSensorSleepBuddy.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                bottomNavigationView.getMenu().getItem(0).setIcon(R.drawable.nav_bar_bluetooth_disconnected);
                if (!isDeviceMeasuring) {
                    if (noConnectionBox.getVisibility() == View.GONE) {
                        checkSensorAndLiveMeasurementLayoutSleepBuddy.setVisibility(View.GONE);
                        noConnectionBox.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        mConnected = false;
        btConnectDisconnectBT.setText(getString(R.string.connect_device));
        btScanBT.setVisibility(View.VISIBLE);

        Log.d(CONNECTION_VIEW, "ConnectionListener onDisconnected()");

    }
}