package com.example.myfirstapp.backend.bluetoothDriver;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.myfirstapp.R;

import java.util.ArrayList;

// Adapter for holding devices found through scanning.
public class MyListAdapterBLE extends ArrayAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;
    private int mSelectedItem = 0xff;

    public MyListAdapterBLE(@NonNull Context context, int resource) {
        super(context, resource);
        mInflator = LayoutInflater.from(context);
        mLeDevices = new ArrayList<>();
    }


    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public void removeDevice(int position) {
        if (mLeDevices.contains(getDevice(position))) {
            mLeDevices.remove(position);
            notifyDataSetChanged();
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    public int getCount() {
        return mLeDevices.size();
    }

    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public void setSelectedItem(int position) {
        mSelectedItem = position;
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        // General ListView optimization code.
        if (convertView == null)
        {
            convertView = mInflator.inflate(R.layout.fragment_bluetooth_listbluetooth_element_sleepbuddy, null);

            viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.listbluetoothAddress);
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.listbluetoothName);

            convertView.setTag(viewHolder);
        } else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device = mLeDevices.get(position);
        if (device != null){
            final String deviceName = device.getName();
            // set name
            if (deviceName != null && deviceName.length() > 0){
                viewHolder.deviceName.setText(deviceName);
            } else{
                viewHolder.deviceName.setText("unknown_device");
            }
            //set address
            viewHolder.deviceAddress.setText(device.getAddress());

            Log.i("viewHolder", "device=  " + deviceName + "    address =   " + device.getAddress());
        }


        return convertView;
    }


    private class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
    }

}
