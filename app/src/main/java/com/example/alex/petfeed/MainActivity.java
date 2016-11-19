package com.example.alex.petfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {

    boolean isActivityInFront = true;
    boolean isFeeding = false;
    String current_ip = "";

    SharedPreferences preferences;

    static String UDP_BROADCAST = "UDPBroadcast";
    public final static String BROADCAST_IP = "b_ip";
    public final static String BROADCAST_MSG = "b_msg";
    BroadcastReceiver br;

    public String broadcast_IP = "";
    public String broadcast_MSG = "";

    private Timer mTimer;
    private mTimerTask mTimerTask;


    public void doFeed(View v) throws InterruptedException {
        if(!isFeeding) {
            isFeeding = true;
            final GifImageView fb = (GifImageView) v.findViewById(R.id.feedButton);
            fb.setImageResource(R.drawable.animatedbutton_blue);
            final GifDrawable drawable = (GifDrawable) fb.getDrawable();

            final String portion = preferences.getString("portion", "1");

            Thread feedtread = new Thread(new Runnable() {
                public void run() {
                    //  Connect.doFeed(current_ip, portion);
                }
            });
            feedtread.start();
            boolean b;
            Thread animate = new Thread(new Runnable() {
                public void run() {
                    long start = System.currentTimeMillis();
                    long end = start + Integer.parseInt(portion) * 2000; // 1 seconds * 1000 ms/sec
                    while (System.currentTimeMillis() < end) {
                        if (!drawable.isPlaying()) {
                            drawable.start();
                        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startService( new Intent(this, InterSrv.class) );//intent.putExtra("time", 3).putExtra("label", "Call 1")

        startService(new Intent(this, UDPListenerService.class));
        regBroadcastRes();
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
        }

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
        TextView dev_state = (TextView) findViewById(R.id.dev_state);
        String recieved_IP = "";
        Integer count_test = 0;
        Integer attempt = 5;

        public mTimerTask(SharedPreferences preferences) {
            this.preferences = preferences;
            this.connect = new Connect();
            this.connect.setPreferences(preferences);
        }
        public void testDevice(){
            recieved_IP  = connect.isConnectLocal(broadcast_IP);
            try {
                sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
          if(isActivityInFront){
              testDevice();
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(recieved_IP.isEmpty()){
                        if(count_test < attempt){
                            count_test++;
                        }else{
                            dev_state.setText("Devise not found in LAN");
                            current_ip = "";
                        }
                    }else{
                        dev_state.setText("Devise is ready on IP: " + recieved_IP);
                        current_ip = recieved_IP;
                        count_test = 0;
                    }
                }
            });
          }
        }
    }
}
