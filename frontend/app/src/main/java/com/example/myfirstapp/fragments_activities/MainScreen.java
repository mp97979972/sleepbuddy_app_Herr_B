package com.example.myfirstapp.fragments_activities;

import static com.example.myfirstapp.backend.storageData.DataRecordingControl.FLAG_ENABLE_STORAGE_DATA;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.myfirstapp.R;
import com.example.myfirstapp.backend.storageData.DataRecordingControl;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;
import java.io.File;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainScreen extends AppCompatActivity {
    //create variables
    FragmentManager fragmentManager;
    FragmentSettings fragmentSettings;
    FragmentConnection fragmentConnection;
    FragmentLiveMeasurement fragmentLiveMeasurement;

    static BottomNavigationView bottomNavigationView;

    public static File directory;
    String profilePictureString;

    CircleImageView profileImgView;
    public static TextView batteryTextView;
    public static ImageView sleepBuddyImgView;

    public static final String SHARED_PREFS_PROFILE_PICTURE = "sharedPrefsProfilePicture";
    public static final String TEXT_PROFILE_PICTURE = "textProfilePicture";

    public static final String TAG_MENU_ITEM = "Menu item selected";
    public static final String TAG_ACTIVITY_LIFE_CYCLE = "Activity life cycle";


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Clear the Activity's bundle of the subsidiary fragments' bundles.
        outState.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG_ACTIVITY_LIFE_CYCLE, "onCreate()");
        super.onCreate(savedInstanceState);

        setDisplayMode();
        setContentView(R.layout.activity_mainscreen);

        initViewsAndVariables();

        setProfilePicture();

        createFragments();
        createBottomNavigationViewListener();
        fragmentManager.beginTransaction().show(fragmentConnection).commit();
    }

    @Override
    protected void onResume() {
        Log.d(TAG_ACTIVITY_LIFE_CYCLE, "onResume()");
        super.onResume();
        hideNavigationBar();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //set profile picture with image from image picker and save image as SharedPreference
        if (requestCode == 20)
        {
            Uri uri = data.getData();

            if (uri != null) {
                profileImgView.setImageURI(uri);

                closeCurrentFragment();

                fragmentManager.beginTransaction().show(fragmentSettings);
                bottomNavigationView.setSelectedItemId(R.id.nav_settings);
                SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_PROFILE_PICTURE, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(TEXT_PROFILE_PICTURE, uri.toString());
                editor.apply();
            }

        }
    }

    private void initViewsAndVariables() {
        bottomNavigationView        = findViewById(R.id.bottomNavigationView);
        fragmentManager             = getSupportFragmentManager();
        profileImgView              = findViewById(R.id.profileImgView);
        batteryTextView             = findViewById(R.id.batteryTextView);
        sleepBuddyImgView           = findViewById(R.id.sleepBuddyImgView);

        /*give permissions when the permissions is not already on*/
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                },1
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionX.init(MainScreen.this)
            .permissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
            .request(new RequestCallback() {
                @Override
                public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {

                }
            });
        }

        if (FLAG_ENABLE_STORAGE_DATA){
            directory                   = getExternalCacheDir();
            DataRecordingControl.getInstance().initDirectory(directory);
        }

    }

    private void setProfilePicture() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_PROFILE_PICTURE, MODE_PRIVATE);
        profilePictureString = sharedPreferences.getString(TEXT_PROFILE_PICTURE, "");
        if (!profilePictureString.equals("")) {
            profileImgView.setImageURI(Uri.parse(profilePictureString));
        }

    }

    private void hideNavigationBar() {
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void createFragments() {

        fragmentSettings = new FragmentSettings();
        fragmentConnection = new FragmentConnection();
        fragmentLiveMeasurement = new FragmentLiveMeasurement();
        createSettingsFragment();
        createConnectionFragment();
        createLiveMeasurementFragment();

    }

    private void createSettingsFragment() {
        if (fragmentSettings.isAdded()) {
            return; //or return false/true, based on where you are calling from
        }
        fragmentManager.beginTransaction().add(R.id.container, fragmentSettings).commit();
        fragmentManager.beginTransaction().hide(fragmentSettings).commit();
    }

    private void createConnectionFragment() {
        if (fragmentConnection.isAdded()) {
            return; //or return false/true, based on where you are calling from
        }
        fragmentManager.beginTransaction().add(R.id.container, fragmentConnection).commit();
        fragmentManager.beginTransaction().hide(fragmentConnection).commit();
    }

    private void createLiveMeasurementFragment() {
        if (fragmentLiveMeasurement.isAdded()) {
            return; //or return false/true, based on where you are calling from
        }
        fragmentManager.beginTransaction().add(R.id.container, fragmentLiveMeasurement).commit();
        fragmentManager.beginTransaction().hide(fragmentLiveMeasurement).commit();
    }

    private void createBottomNavigationViewListener() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                closeCurrentFragment();
                int id = item.getItemId();
                switch (id) {
                    case (R.id.nav_connection):
                        fragmentManager.beginTransaction().show(fragmentConnection).commit();
                        Log.d(TAG_MENU_ITEM, "Connection");
                        break;
                    case (R.id.nav_live_measurement):
                        fragmentManager.beginTransaction().show(fragmentLiveMeasurement).commit();
                        Log.d(TAG_MENU_ITEM, "Live-measurement");
                        break;
                    case (R.id.nav_settings):
                        fragmentManager.beginTransaction().show(fragmentSettings).commit();
                        Log.d(TAG_MENU_ITEM, "Settings");
                        break;
                }
                return true;
            }
        });
    }

    public void closeCurrentFragment() {
        for (Fragment current : fragmentManager.getFragments()) {
            if (!current.isHidden()) {
                fragmentManager.beginTransaction().hide(current).commit();
                break;
            }
        }
    }

    public void setDisplayMode() {
            setDarkMode();
    }

    public void setDarkMode() {
        setTheme(R.style.AppTheme_MyDark);
    }

    public void setLightMode() {
        setTheme(R.style.AppTheme_MyLight);
    }

    public void reset() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    @Override
    protected void onStart() {
        Log.d(TAG_ACTIVITY_LIFE_CYCLE, "onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG_ACTIVITY_LIFE_CYCLE, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG_ACTIVITY_LIFE_CYCLE, "onDestroy()");
        super.onDestroy();
    }
}
