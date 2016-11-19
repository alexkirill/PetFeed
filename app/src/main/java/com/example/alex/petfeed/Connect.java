package com.example.alex.petfeed;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;

/**
 * Created by alex on 09.11.2016.
 */

public class Connect {
    SharedPreferences preferences;

    static String setupNetworkSSID = "PetFeed";
    static String setupnetworkPass = "12345678";

    public Connect(){
    }
    public void setPreferences(SharedPreferences preferences){
        this.preferences = preferences;
    }
    public static boolean isDeviceInSetupMode(Context context){
        boolean result = false;
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){ wifiManager.setWifiEnabled(true); }
        wifiManager.startScan();

        List<ScanResult> list = wifiManager.getScanResults();
        for( ScanResult i : list ) {
            if(i.SSID != null && i.SSID.equals(setupNetworkSSID)) {
                result = true;
                break;
            }
        }
        return result;
    }
    public static void DeviceToWifi(Context context, String ssid, String pass, String ip, String gw, String sn) throws InterruptedException {
        //connect to device network
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + setupNetworkSSID + "\"";
        conf.preSharedKey = "\""+ setupnetworkPass +"\"";

        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){ wifiManager.setWifiEnabled(true); }
        //remember current ssid
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String oldSSID = wifiInfo.getSSID();
        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + setupNetworkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                break;
            }
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            URL url;
                   Network network = connectivityManager.getActiveNetwork();
                    //connectivityManager.bindProcessToNetwork(network);
            try {
                url = new URL("http://192.168.4.1/close");
                try {
                    URLConnection conn = network.openConnection(url);
                    conn.setReadTimeout(2000);
                    conn.setConnectTimeout(2000);
                    conn.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        }else
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connectivityManager.getAllNetworks();
            NetworkInfo networkInfo;
            Network network;
            for (int i = 0; i < networks.length; i++){
                network = networks[i];
                networkInfo = connectivityManager.getNetworkInfo(network);
                if ((networkInfo.getType() ==     ConnectivityManager.TYPE_WIFI) && (networkInfo.getState().equals(NetworkInfo.State.CONNECTED))) {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                    break;
                }
            }
        }

        sleep(15000);
        performGetVoid("http://192.168.4.1/wifisave?s="+ssid+"&p="+pass+"&ip="+ip+"&gw="+gw+"&sn="+sn);
        sleep(2000);
        performGetVoid("http://192.168.4.1/close");
        sleep(2000);
        //coonect back
        List<WifiConfiguration> newlist = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : newlist ) {
            if(i.SSID != null && i.SSID.equals("\"" + oldSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                break;
            }
        }
}

    public static void doFeed(String IP, String portion){
        if(!IP.isEmpty()){
            performGetVoid("http://" + IP + "/dofeed?portion=" + portion);
        }
    }
    //check connection to the device
    public boolean isConnectedCloud(){
         return false;
    }
    public String isConnectLocal(String broadcast_IP) {
        String result = "";
         String static_IP = getStaticIP();
        if(getDeviceOnIP(broadcast_IP)){
            result =  broadcast_IP;
        }else if(getDeviceOnIP(static_IP)){
            result = static_IP;
        }
        return result;
    }
    public boolean getDeviceOnIP(String ip){
        boolean result = false;
        if (!ip.isEmpty()){
            String host = "http://"+ ip +"/whoami"; // allways with web page
            result = performGetVoid(host);
        }
            return result;
    }
    //http request
    public static boolean performGetVoid(String requestURL) {
        URL url;
        Boolean response = false;
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(2000);
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                response = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    public static String performGetCall(String requestURL) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(2000);
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("GET");
            conn.connect();
            conn.connect();

            try {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                response =  readStream(in);
            } finally {
               conn.disconnect();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    public static String performPostCall(String requestURL, HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(2000);
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
    private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
    public static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
    //end http request
    //get last known IP from sharedPref
    private String getLastIP() {
        String ip = preferences.getString("lastIP", "");
        return ip;
    }
    private void setLastIP(String ip) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastIP", ip);
        editor.commit();
    }
    private String getStaticIP() {
        String ip = preferences.getString("wifi_ip", "");
        return ip;
    }
}
