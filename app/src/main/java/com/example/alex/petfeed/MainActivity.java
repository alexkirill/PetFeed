package com.example.alex.petfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {

    boolean isActivityInFront = true;
    boolean isFeeding = false;
    String current_address = "";
    String current_port = "80";

    SharedPreferences preferences;

    static String UDP_BROADCAST = "UDPBroadcast";
    public final static String BROADCAST_IP = "b_ip";
    public final static String BROADCAST_MSG = "b_msg";
    BroadcastReceiver br;

    public String broadcast_IP = "";
    public String broadcast_MSG = "";

    private Timer mTimer;
    private mTimerTask mTimerTask;

    AnimationDrawable connectionAnimate;
    ImageView connectionImage;
    ImageView buttonImage;
    TextView dev_state;


    public void doFeed(View v) {
        if(!isFeeding && !current_address.isEmpty()) {
            isFeeding = true;
            final String[] response = new String[1];
            response[0] = "";
            final GifImageView fb = (GifImageView) v.findViewById(R.id.feedButton);
            fb.setImageResource(R.drawable.animatedbutton_blue);
            final GifDrawable drawable = (GifDrawable) fb.getDrawable();

            final String portion = preferences.getString("portion", "1");

            Thread feedtread = new Thread(new Runnable() {
                public void run() {
                    response[0] =  Connect.doFeed(current_address, current_port, portion);
                }
            });
            feedtread.start();
            Thread animate = new Thread(new Runnable() {
                public void run() {
                    int cycleTime = 3000;
                    try {
                        sleep(Integer.parseInt(portion) * cycleTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    fb.post(new Runnable() {
                        @Override
                        public void run() {
                            fb.setImageResource(R.drawable.button_blue);
                        }
                    });
                    isFeeding = false;
                }
            });
            animate.start();
        }
    }
    public void getWhoAmI(String address, String port){
        Map hash = new HashMap<String, String>();
        hash = Connect.whoAmI(address, port);
        if(!hash.isEmpty()){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("device_name", hash.get("device_name").toString());
            editor.putString("version", hash.get("version").toString());
            editor.putString("did", hash.get("did").toString());
            editor.putString("dhex", hash.get("dhex").toString());
            editor.commit();
        }
    }
    private void setTimers() {
        //read shared preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mTimer = new Timer();
        mTimerTask = new mTimerTask(preferences);
        mTimer.schedule(mTimerTask, 100, 3000);
    }
    private  void delTimer(){
        mTimer.cancel();
        mTimer = null;
    }
    public void showDetailStat(View v){
        if(dev_state.getVisibility() == View.VISIBLE){
            dev_state.setVisibility(View.INVISIBLE);
        }else{
            dev_state.setVisibility(View.VISIBLE);
        }
    }
    private void startConnectionAnimate(){
        if(String.valueOf(connectionImage.getTag()) != "connection"){
            connectionImage.setImageResource(0);
            connectionImage.setBackgroundResource(R.drawable.connection);
            connectionAnimate = (AnimationDrawable) connectionImage.getBackground();
        }
        connectionAnimate.start();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startService(new Intent(this, UDPListenerService.class));
        regBroadcastRes();

        dev_state = (TextView) findViewById(R.id.dev_state);
        dev_state.setVisibility(View.INVISIBLE);
        buttonImage = (ImageView) findViewById(R.id.feedButton);
        connectionImage = (ImageView) findViewById(R.id.connection);
        connectionImage.setBackgroundResource(R.drawable.connection);
        connectionAnimate = (AnimationDrawable) connectionImage.getBackground();


    }

    @Override
    public void onResume() {
        super.onResume();
        isActivityInFront = true;
        setTimers();
        startConnectionAnimate();
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityInFront = false;
        delTimer();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent = null;
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            intent = new Intent(this, Preferences.class);
        }else if(id == R.id.action_about){
            intent = new Intent(this, About.class);
        }else if(id == R.id.action_schedule){
            intent = new Intent(this, Schedule.class);
        }/*else if(id == R.id.action_home){
            intent = new Intent(this, MainActivity.class);
        }*/

        startActivity(intent);
        return true;
    }

    public void regBroadcastRes(){
        // создаем BroadcastReceiver
        br = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                broadcast_IP  = intent.getStringExtra(BROADCAST_IP);
                broadcast_MSG = intent.getStringExtra(BROADCAST_MSG);
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(UDP_BROADCAST);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);

    }

    //timer class
    class mTimerTask extends TimerTask {

        SharedPreferences preferences;
        Connect connect;
        String local_IP = "";
        Map cloud_options = new HashMap<String, String>();
        Map remote_options = new HashMap<String, String>();
        Integer count_test = 0;
        Integer attempt = 5;

        public mTimerTask(SharedPreferences preferences) {
            this.preferences = preferences;
            this.connect = new Connect();
            this.connect.setPreferences(preferences);
        }
        public void testLan(){
            local_IP  = connect.ConnectLocal(broadcast_IP);
            getWhoAmI(local_IP, "80");
        }
        private void testCloud() {
            cloud_options = connect.ConnectCloud();
        }
        private void testRemote() {
            remote_options = connect.ConnectRemote();
            if(!remote_options.isEmpty()){
                getWhoAmI(remote_options.get("ip").toString(), remote_options.get("port").toString());
            }
        }

        public void prepareFeed(String adress, String port, String msg,  int status_image){
            current_address = adress;
            current_port = port;
            dev_state.setText(msg);
            stopAnimateButton();
            connectionImage.setImageResource(status_image);
            if(current_address.isEmpty()){
                buttonImage.setImageResource(R.drawable.button_blue_init);
            }else{
                buttonImage.setImageResource(R.drawable.button_blue);
            }
        }
        private void stopAnimateButton(){
            if(connectionAnimate.isRunning()){
                connectionAnimate.stop();
                connectionImage.setBackgroundResource(0);
            }
        }
        public void run() {
          if(isActivityInFront && !isFeeding){
              testLan();
              testRemote();
              testCloud();
              try {
                  sleep(2500);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
            if(!isFeeding) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!local_IP.isEmpty()) {
                            String msg = "Ready on local IP: " + local_IP;
                            prepareFeed(local_IP, "80", msg, R.drawable.lan_network);
                            count_test = 0;
                        } else if (remote_options.size() > 0) {
                            String ip = remote_options.get("ip").toString();
                            String port = remote_options.get("port").toString();
                            String msg = "Ready on remote IP: " + ip + ":" + port;
                            prepareFeed(ip, port, msg, R.drawable.remote_network);
                            count_test = 0;
                        } else if (cloud_options.size() > 0) {
                            String host = cloud_options.get("host").toString();
                            String hash = cloud_options.get("hash").toString();
                            String msg = "Ready on Cloud: " + host;
                            prepareFeed(host+"?apphash="+hash, "80", msg, R.drawable.cloud_network);
                            count_test = 0;
                        } else {
                            if (count_test < attempt) {
                                count_test++;
                            } else {
                                prepareFeed("", "80", "Devise not found in LAN", R.drawable.no_connection);
                            }
                        }
                    }
                });
            }
          }
        }
    }

}
