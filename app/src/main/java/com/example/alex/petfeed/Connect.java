package com.example.alex.petfeed;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;

/**
 * Created by alex on 09.11.2016.
 */

public class Connect {
    static SharedPreferences preferences;

    static String setupNetworkSSID = "PetFeed";
    static String setupnetworkPass = "12345678";
    static String onLivePage = "whoami";

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

        sleep(12000);
        performGetVoid("http://192.168.4.1/wifisave?s="+ssid+"&p="+pass+"&ip="+ip+"&gw="+gw+"&sn="+sn);
        sleep(2000);
        performGetVoid("http://192.168.4.1/close");
        sleep(2000);
        //coonect back
        /*
        List<WifiConfiguration> newlist = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : newlist ) {
            if(i.SSID != null && i.SSID.equals("\"" + oldSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                break;
            }
        }
        */
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
}

    public static String doFeed(String address, String port, String portion){
      if (address.equals(getCloudAdres())){
          HashMap<String, String> params = new HashMap<String, String>();
          params.put("version", getVersion());
          params.put("did", getDID());
          params.put("dhex", getDHEX());
          params.put("portion", portion);
          String host = "http://" + address + "/dofeed/set"; // allways with web page
          String response = performPostCall(host, params);
          return response;
      }else{
          return performGetCall("http://" + address + ":" + port + "/dofeed?portion=" + portion);
      }
    }

    //check connection to the device
    public Map<String, String> ConnectRemote(){
         Map hash = new HashMap<String, String>();
        if(preferences.getBoolean("allow_direct_ctrl", false)){
            String remote_IP = preferences.getString("ext_ip", "");
            String remote_port = preferences.getString("ext_port", "");
            if(getDeviceOnWeb(remote_IP, remote_port)){
                hash.put("ip", remote_IP);
                hash.put("port", remote_port);
            }
        }
        return hash;
    }
    public Map<String, String> ConnectCloud(){
        Map hash = new HashMap<String, String>();
        Boolean allow_cloud_ctrl = preferences.getBoolean("allow_cloud_ctrl", false);
        String version = preferences.getString("version", "");
        String did = preferences.getString("did", "");
        String dhex = preferences.getString("dhex", "");
        if(allow_cloud_ctrl && !version.isEmpty() && !did.isEmpty() && !dhex.isEmpty()){
            String cloud_adr = preferences.getString("cloud_adr", "");
            String deviceHash = preferences.getString("dhex", "");;
            if(getDeviceOnCloud(cloud_adr)){
                hash.put("host", cloud_adr);
                hash.put("hash", deviceHash);
            }
        }
        return hash;
    }
    public String ConnectLocal(String broadcast_IP) {
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
            String host = "http://"+ ip +"/" + onLivePage; // allways with web page
            result = performGetVoid(host);
        }
        return result;
    }
    public boolean getDeviceOnCloud(String address){
        boolean result = false;
        if (!address.isEmpty()){
            String host = "http://"+ address; // allways with web page
            result = performGetVoid(host);
        }
        return result;
    }
    public boolean getDeviceOnWeb(String address, String port){
        boolean result = false;
        if (!address.isEmpty()){
            String host = "http://"+ address +":" + port + "/" + onLivePage; // allways with web page
            result = performGetVoid(host);
        }
        return result;
    }

    public static Map<String, Integer>  getDeviceTime(String address, String port){
        Map hash = new HashMap<String, Integer>();
        Integer hour = 0, minute = 0;
        String response;
        String host = "http://" + address + ":" + port + "/gettime"; // allways with web page
        response = performGetCall(host);
        if(!response.isEmpty()){

            try {
                JSONObject obj = new JSONObject(response);
                hour =  Integer.parseInt(obj.getString("hour"));
                minute =  Integer.parseInt(obj.getString("minute"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        hash.put("hour", hour);
        hash.put("minute", minute);
        return hash;
    }
    public static boolean setDeviceTime(String address, String port, String hour, String minute){
        String year = "2017", month = "1", day = "1";
        Calendar c = Calendar.getInstance();
        year = Integer.toString(c.get(Calendar.YEAR));
        month = Integer.toString(c.get(Calendar.MONTH) + 1); //month started from 0
        day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("year", year.substring((year.length()-2), year.length()));//year must be last 2 digits "17"
        params.put("month", month);
        params.put("day", day);
        params.put("hour", hour);
        params.put("minute", minute);
        String host = "http://" + address + ":" + port + "/settime"; // allways with web page
        String response = performPostCall(host, params);
        if(!response.isEmpty()){
            return true;
        }else{
            return false;
        }

    }
    public LinkedHashMap<String, Map<String, String>> getDeviceSchedule(String address, String port){
        LinkedHashMap  hash = new LinkedHashMap<String, Map<String, String>>();
        String response;
        String host = "http://" + address + ":" + port + "/getschedule"; // allways with web page
        response = performGetCall(host);
        if(!response.isEmpty()){

            try {
                JSONObject object = new JSONObject(response);
                Iterator<?> keys = object.keys();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    if ( object.get(key) instanceof JSONObject ) {
                        JSONObject obj =  (JSONObject) object.get(key);
                        Map nestedHash = new HashMap<String, String>();
                        nestedHash.put("hour", obj.getString("hour"));
                        nestedHash.put("minute", obj.getString("minute"));
                        nestedHash.put("portion", obj.getString("portion"));
                        hash.put(key, nestedHash);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }
    public static boolean setDeviceSchedule(String address, String port, String serialazedParams){
        HashMap params = new HashMap<String, String>();
        params.put("schedule", serialazedParams);
        String host = "http://" + address + ":" + port + "/setschedule"; // allways with web page
        String response = performPostCall(host, params);
        if(!response.isEmpty()){
            return true;
        }else{
            return false;
        }
    }
    public static Map<String, String> whoAmI(String address, String port) {
        Map hash = new HashMap<String, String>();
        if(!address.isEmpty() && !port.isEmpty()) {
            String response;
            String host = "http://" + address + ":" + port + "/" + onLivePage; // allways with web page
            response = performGetCall(host);
            if (!response.isEmpty()) {
                JSONObject json_response = null;
                try {
                    json_response = new JSONObject(response);
                    hash.put("device_name", json_response.getString("device_name"));
                    hash.put("version", json_response.getString("version"));
                    hash.put("repository_host", json_response.getString("repository_host"));
                    hash.put("repository_interval", json_response.getString("repository_interval"));
                    hash.put("did", json_response.getString("did"));
                    hash.put("dhex", json_response.getString("dhex"));
                    hash.put("tank", json_response.getString("tank"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return hash;
    }

    //http request
    private static boolean performGetVoid(String requestURL) {
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
    private static String performGetCall(String requestURL) {
        URL url;
        String response = "";

        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("GET");
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
    private static String performPostCall(String requestURL, HashMap<String, String> postDataParams) {

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
    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    private String getStaticIP() {
        String ip = preferences.getString("wifi_ip", "");
        return ip;
    }
    private static String getCloudAdres() {
        String ip = preferences.getString("cloud_adr", "");
        return ip;
    }
    private static String getVersion() {
        String ip = preferences.getString("version", "");
        return ip;
    }
    private static String getDID() {
        String ip = preferences.getString("did", "");
        return ip;
    }
    private static String getDHEX() {
        String ip = preferences.getString("dhex", "");
        return ip;
    }
}
