package com.example.myfirstapp.fragments_activities;

import static android.content.Context.MODE_PRIVATE;
import static com.example.myfirstapp.GetNumberCommit.gStr_NumberCommit;
import static com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection.convertToHex;
import static com.example.myfirstapp.backend.protocolDriver.protocolConstant.HEADER_BATTERY;
import static com.example.myfirstapp.fragments_activities.MainScreen.SHARED_PREFS_PROFILE_PICTURE;
import static com.example.myfirstapp.fragments_activities.MainScreen.TEXT_PROFILE_PICTURE;
import static com.example.myfirstapp.fragments_activities.MainScreen.batteryTextView;
import static com.example.myfirstapp.fragments_activities.MainScreen.sleepBuddyImgView;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.myfirstapp.R;
import com.example.myfirstapp.backend.ConnectionListener;
import com.example.myfirstapp.backend.BLEDataProtocolListener;
import com.example.myfirstapp.backend.GlobalMessage;
import com.example.myfirstapp.backend.bluetoothDriver.BluetoothBLEConnection;
import com.github.dhaval2404.imagepicker.ImagePicker;

import java.util.Arrays;


public class FragmentSettings extends PreferenceFragmentCompat implements BLEDataProtocolListener, ConnectionListener {
    Preference profilePicture, deleteProfilePicture, appVersion;
    Dialog deleteDialog;
    ImageView cancelProfilePictureImgView, deleteProfilePictureImgView;
    BluetoothBLEConnection mBluetoothBLEConnection = BluetoothBLEConnection.getInstance();
    private int counterBattery = 0;
    private boolean isBatteryValue_getDone = false;
    private boolean isFWCommit_getDone = false;
    final String SETTINGS_VIEW = "Settings view";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profilePicture                  = findPreference("changeProfilePicture");
        deleteProfilePicture            = findPreference("deleteProfilePicture");
        appVersion                      = findPreference("appVersion");

        mBluetoothBLEConnection.setDataListener(this);
        GlobalMessage.getInstance().registerConnectionListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View settingsView = super.onCreateView(inflater, container, savedInstanceState);

        deleteDialog = new Dialog(getContext());

        profilePicture.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ImagePicker.Companion.with(getActivity())
                        .crop()
                        .maxResultSize(540,540)
                        .start(20);
                Log.d(SETTINGS_VIEW, "Profile Picture changed");
                return true;
            }
        });

        deleteProfilePicture.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREFS_PROFILE_PICTURE, MODE_PRIVATE);

                ((MainScreen)getActivity()).profilePictureString = sharedPreferences.getString(TEXT_PROFILE_PICTURE, "");

                if (((MainScreen)getActivity()).profilePictureString.equals("") || ((MainScreen)getActivity()).profilePictureString.equals("textProfilePicture")) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.no_profile_picture_available, Toast.LENGTH_SHORT).show();
                    return false;
                } else
                {
                    deleteDialog.setContentView(R.layout.popup_delete_profile_picture);

                    cancelProfilePictureImgView = deleteDialog.findViewById(R.id.cancelProfilePictureImgView);
                    deleteProfilePictureImgView = deleteDialog.findViewById(R.id.deleteProfilePictureImgView);
                    deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    deleteDialog.show();
                    cancelProfilePictureImgView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getActivity().getApplicationContext(), R.string.profile_picture_not_deleted, Toast.LENGTH_SHORT).show();
                            deleteDialog.dismiss();
                        }
                    });

                    deleteProfilePictureImgView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((MainScreen)getActivity()).profileImgView.setImageDrawable(((MainScreen)getActivity()).getDrawable(R.drawable.profil_img_dummy));
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(TEXT_PROFILE_PICTURE, "");
                            editor.apply();
                            Toast.makeText(getActivity().getApplicationContext(), R.string.profile_picture_deleted, Toast.LENGTH_SHORT).show();
                            Log.d(SETTINGS_VIEW, "Profile Picture deleted");
                            deleteDialog.dismiss();
                        }
                    });
                    return true;
                }

            }
        });
        appVersion.setSummary(gStr_NumberCommit);

        return settingsView;
    }




    /**********************************************************************************************/
    @Override
    public void onRespondUpdateFromBluetooth(byte[] data)
    {
        if (!isBatteryValue_getDone || !isFWCommit_getDone)
        {
            //updateReceiveData(data);
            Log.e("onDataUpdate", "fragmentProtocol:   " + Arrays.toString(convertToHex(data)));

            if (data[0] == -126 && data[1] == 12 && data[2] == HEADER_BATTERY)
            {
                if (counterBattery == 2)
                {
                    int battery = (data[3]*256+ data[4]);

                    if (battery>100){
                        battery = 100;
                    }

                    if (battery>2)
                    {
                        int finalBattery = battery;
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run() {
                                batteryTextView.setText(finalBattery + "%");
                                sleepBuddyImgView.setVisibility(View.VISIBLE);
                                isBatteryValue_getDone = true;
                            }
                        });
                    }
                    counterBattery = 0;
                } else
                {
                    counterBattery++;
                }

            }
        }
    }




    /**********************************************************************************************/
    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryTextView.setText("");
                sleepBuddyImgView.setVisibility(View.INVISIBLE);

                isBatteryValue_getDone = false;
                isFWCommit_getDone = false;
            }
        });
    }
}
