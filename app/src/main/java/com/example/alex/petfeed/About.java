package com.example.alex.petfeed;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by alex on 14.11.2016.
 */

public class About extends AppCompatActivity {

    boolean isActivityInFront = true;
    String device_name;
    String version;
    String DID;
    String DHEX;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.about_text));

        //read shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
         device_name = preferences.getString("device_name", "NULL");
         version = preferences.getString("version", "NULL");
         DID = preferences.getString("did", "NULL");
         DHEX = preferences.getString("dhex", "NULL");

        TextView textview_name = (TextView) findViewById(R.id.devisename_label);
        TextView textview_version = (TextView) findViewById(R.id.version_label);
        TextView textview_id = (TextView) findViewById(R.id.id_label);
        textview_name.setText(device_name);
        textview_version.setText(version);
        textview_id.setText(DID);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        return super.onOptionsItemSelected(item);
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
