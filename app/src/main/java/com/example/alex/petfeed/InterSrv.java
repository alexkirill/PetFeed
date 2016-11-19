package com.example.alex.petfeed;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by alex on 10.11.2016.
 */
public class InterSrv extends IntentService {


    public InterSrv() {
        super("interSrv");
    }

    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int tm = intent.getIntExtra("time", 0);
        String label = intent.getStringExtra("label");

    }

    public void onDestroy() {
        super.onDestroy();

    }
}
