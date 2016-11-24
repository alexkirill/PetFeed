package com.example.alex.petfeed;

/**
 * Created by alex on 09.11.2016.
 */

        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.preference.CheckBoxPreference;
        import android.preference.Preference;
        import android.preference.PreferenceActivity;
        import android.preference.PreferenceFragment;
        import android.preference.PreferenceManager;
        import android.preference.SwitchPreference;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.widget.Toolbar;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.LinearLayout;

        import java.util.Arrays;
        import java.util.HashSet;
        import java.util.List;
        import java.util.Set;
        import java.util.Timer;
        import java.util.TimerTask;

public class Preferences extends PreferenceActivity {

    boolean isActivityInFront = true;
    static Preference submitwifi;
    private Timer mTimer;
    private SutupModeCheck SutupModeCheck;
    Toolbar mToolbar;

    private void setTimers() {
        //create timers
        mTimer = new Timer();
        SutupModeCheck = new SutupModeCheck(this);
        mTimer.schedule(SutupModeCheck, 100, 5000);
    }
    private  void delTimer(){
        mTimer.cancel();
        mTimer = null;
    }
    private  void showAlert(String title, String message, String btnText){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                //.setIcon(R.drawable.ic_android_cat)
                .setCancelable(false)
                .setNegativeButton(btnText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public void submitWifiToDevice(View v){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Context context = this;
        final String ssid =  prefs.getString("wifi_ssid", "");
        final String pass =  prefs.getString("wifi_pass", "");
        final String ip =    prefs.getString("wifi_ip", "");
        final String gw =    prefs.getString("wifi_gw", "");
        final String sn =    prefs.getString("wifi_sn", "");
        final Boolean staticIP = prefs.getBoolean("allow_stat_ip", false);

        final Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    if(!ssid.isEmpty() && !pass.isEmpty()){
                        Set<String> statics = new HashSet<String>(Arrays.asList(ip, gw, sn));
                        if(!staticIP && !statics.contains("")){
                            Connect.DeviceToWifi(context, ssid, pass, "", "", "");
                        }else{
                            Connect.DeviceToWifi(context, ssid, pass, ip, gw, sn);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
    }
    @Override
    public void onBuildHeaders(List<Header> target) {
           loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.preferences_toolbar, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        mToolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        isActivityInFront = true;
        setTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityInFront = false;
        submitwifi = null; //for timer cancelation
        delTimer();
    }

    public static class SettingsFragment extends PreferenceFragment {

        SwitchPreference cloud_remote;
        SwitchPreference direct_remote;

        public void submitWifiToDevice(){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String ssid =  prefs.getString("wifi_ssid", "");
            String pass =  prefs.getString("wifi_pass", "");
            String ip =    prefs.getString("wifi_ip", "");
            String gw =    prefs.getString("wifi_gw", "");
            String sn =    prefs.getString("wifi_sn", "");
            Boolean staticIP = prefs.getBoolean("allow_stat_ip", false);

            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        Connect.DeviceToWifi(getActivity(), "K7", "8113082silopt", "10.0.0.195", "10.0.0.1", "255.255.255.0");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t1.start();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("area");
            if ("wifi".equals(settings)) {
                addPreferencesFromResource(R.xml.wifi_settings);
                submitwifi = findPreference("submitwifi");
                /*
                submitwifi = findPreference("submitwifi");
                View v_submitwifi = submitwifi.getView(null, null);
                Button submitButton = (Button)v_submitwifi.findViewById(R.id.submitButton);
                submitButton.setText("Hello");
                  */


            } else if ("cloud".equals(settings)) {
                addPreferencesFromResource(R.xml.cloud_settings);
                //inverse dependency
                cloud_remote = (SwitchPreference) findPreference("allow_cloud_ctrl");
                direct_remote = (SwitchPreference) findPreference("allow_direct_ctrl");
                cloud_remote.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        direct_remote.setChecked(false);
                        return false;
                    }
                });
                direct_remote.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        cloud_remote.setChecked(false);
                        return false;
                    }
                });
                // end inverse dependency
            } else if ("time".equals(settings)) {
                addPreferencesFromResource(R.xml.time_settings);
            } else if ("general".equals(settings)) {
                addPreferencesFromResource(R.xml.general_settings);
            }
        }

        @Override
        public void onStop(){
            super.onStop();
        }
    }
    @Override
    protected boolean isValidFragment(String fragmentName) {
        boolean result =  Preferences.class.getName().equals(fragmentName);
        return true;
    }

    //timer class
    class SutupModeCheck extends TimerTask {
        boolean alreadyWasShown = false;
        Context context;
        Button submit_button;

        public SutupModeCheck(Context context){
            this.context = context;

        }
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(submitwifi != null){
                        if(Connect.isDeviceInSetupMode(context)) {
                            submitwifi.setSummary("* You'll be temporary disconect from your network");
                        }else{
                            if(!alreadyWasShown){
                                showAlert("Attention!", "Device not in Setup Mode. Press Setup button on device", "Ok");
                                alreadyWasShown = true;
                            }

                            submitwifi.setSummary("                                                                        ");

                        }

                    }
                }
            });
        }
    }
}