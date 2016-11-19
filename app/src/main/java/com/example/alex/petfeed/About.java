package com.example.alex.petfeed;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by alex on 14.11.2016.
 */

public class About extends AppCompatActivity {

    boolean isActivityInFront = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //read shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //PetFeed petfedd = new PetFeed(preferences);
        //Connect connect = new Connect(preferences);

    }

    @Override
    public void onResume() {
        super.onResume();
        isActivityInFront = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityInFront = false;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
