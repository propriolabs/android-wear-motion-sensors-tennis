package com.propriolabs.thetennissense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DataLayerListenerServicePhone extends WearableListenerService {

    private final static String UNSET = "UNKNOWN";

    private Long sessionStartTime = null;

    private Long lastSessionStartTime = null;

    private String activity;

    private String userName;

    private String userId;

    String label;

    private String hand;

    private String bezel;

    private String gender;

    private String age;

    private String height;

    private String rating;

    private String privacy;

    BroadcastReceiver sResultReceiver;

    private GoogleApiClient mGoogleApiClient;

    private Socket mSocket = null;

    private String customTag;

    private static OutputStreamWriter writer;

    private static File directory;

    private File file;

    private boolean serverChecked = false;

    private boolean logLocally = true;

    private int receivedCount = 1;

    private final int SAMPLE_RATE = 10;

    private JSONArray samples = new JSONArray();

    private void sendSensorData(final String data) {

        try {
            final JSONObject map = new JSONObject(data);
            // read your values from map:
            float X = (float) map.getDouble("x");
            float Y = (float) map.getDouble("y");
            float Z = (float) map.getDouble("z");
            float g1 = (float) map.getDouble("g1");
            float g2 = (float) map.getDouble("g2");
            float g3 = (float) map.getDouble("g3");
            float gx = (float) map.getDouble("grav1");
            float gy = (float) map.getDouble("grav2");
            float gz = (float) map.getDouble("grav3");
            float r1 = (float) map.getDouble("r1");
            float r2 = (float) map.getDouble("r2");
            float r3 = (float) map.getDouble("r3");
            float heartrate = (float) map.getDouble("heartrate");

            String manufacturer = map.getString("manufacturer");
            String model = map.getString("model");
            String product = map.getString("product");
            float accRange = (float) map.getDouble("accRange");

            configureOutput(map.getLong("sessionStartTime"));

            long watchTime = map.getLong("watchtime");
            long tabletTime = System.currentTimeMillis();

            JSONObject sensdata = new JSONObject();
                sensdata.put("userName", userName);
                sensdata.put("userId", userId);
                sensdata.put("session", String.valueOf(sessionStartTime));
                sensdata.put("activity", activity);
                sensdata.put("ax", X);
                sensdata.put("ay", Y);
                sensdata.put("az", Z);
                sensdata.put("g1", g1);
                sensdata.put("g2", g2);
                sensdata.put("g3", g3);
                sensdata.put("r1", r1);
                sensdata.put("r2", r2);
                sensdata.put("r3", r3);
                sensdata.put("gx", gx);
                sensdata.put("gy", gy);
                sensdata.put("gz", gz);
                sensdata.put("heartrate",heartrate);
                sensdata.put("watchtime", watchTime);
                sensdata.put("tablettime", tabletTime);
                sensdata.put("label", label);
                sensdata.put("hand", hand);
                sensdata.put("bezel", bezel);
                sensdata.put("notes", customTag);
                sensdata.put("manufacturer", manufacturer);
                sensdata.put("product", product);
                sensdata.put("model", model);
                sensdata.put("age", age);
                sensdata.put("gender", gender);
                sensdata.put("accRange", accRange);
                sensdata.put("heightInches", height);
                sensdata.put("privacy", privacy);
                sensdata.put("rating", rating);

            if (isConnected() && !logLocally) {
                samples.put(sensdata);
                receivedCount++;
                if (receivedCount % SAMPLE_RATE == 0) {
                    mSocket.emit("message", samples);
                    samples = new JSONArray();
                }
            } else {
                saveData(sensdata);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v("proprio-service", "onMessageReceived:" + messageEvent.getPath());

        if(messageEvent.getPath().equals("/PING")) {
            // Gets here on startup of watch app.  Let's start the New Session activity
            // with a flag that says the watch wants to start it.
            Intent startIntent = new Intent(getApplicationContext(), CollectData.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startIntent.setAction("phone.localIntent");
            startIntent.putExtra("FROM_WATCH", true);
            startActivity(startIntent);
        }
        if(messageEvent.getPath().equals("/WATCH_UP")) {
            // Gets here on startup of watch app.  Let's start the New Session activity
            // with a flag that says the watch wants to start it.
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("WATCH_READY", "WATCH_READY");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if (messageEvent.getPath().equals("/DATA")){
            sendSensorData(new String(messageEvent.getData()));
        }
        else if(messageEvent.getPath().equals("/DESTROYED")) {
            // Gets here when watch app is destroyed.
            // Basically on the mobile, we should no longer allow a session to start
            // until the watch is started back up so let's tell the mobile.
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("disable", "no");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if(messageEvent.getPath().equals("/SCORE")) {
            String string = new String(messageEvent.getData());
            Log.v("proprio-service", "onMessageReceived: data " + string);
            JSONObject score = null;
            try {
                score = new JSONObject(string);
                score.put("userId",userId);
                score.put("sessionId",sessionStartTime);
                score.put("datetime",System.currentTimeMillis());
                mSocket.emit("set_score",score);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals("/LABEL")) {
            String string = new String(messageEvent.getData());
            Log.v("proprio-service", "onMessageReceived: data " + string);
            JSONObject label = null;
            try {
                label = new JSONObject(string);
                label.put("userId", userId);
                label.put("sessionId", sessionStartTime);
                label.put("datetime", System.currentTimeMillis());
                mSocket.emit("audio_label", label);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals("/ACCELERATION_LACKING")) {
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("ACCELERATION_LACKING", "ACCELERATION_LACKING");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if(messageEvent.getPath().equals("/GYROSCOPE_LACKING")) {
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("GYROSCOPE_LACKING", "GYROSCOPE_LACKING");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if(messageEvent.getPath().equals("/ROTATION_LACKING")) {
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("ROTATION_LACKING", "ROTATION_LACKING");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if(messageEvent.getPath().equals("/GRAVITY_LACKING")) {
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("GRAVITY_LACKING", "GRAVITY_LACKING");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }
        else if(messageEvent.getPath().equals("/SENSORS_LACKING")) {
            // Gets here when watch app is destroyed.
            // Basically on the mobile, we should no longer allow a session to start
            // until the watch is started back up so let's tell the mobile.
            Intent localIntent = new Intent("phone.localIntent");
            localIntent.putExtra("SENSORS_LACKING", "SENSORS_LACKING");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
        }

        super.onMessageReceived(messageEvent);
    }

    // Honestly not sure when this is called.
    @Override
    public void onPeerConnected (Node peer) {
        Log.d("proprio-service", "onPeerConnected");
        super.onPeerConnected(peer);
    }

    // Honestly not sure when this is called.
    @Override
    public void onPeerDisconnected (Node peer) {
        Log.d("proprio-service", "onPeerDisconnected");
        super.onPeerDisconnected(peer);
    }

    @Override
    public void onCreate() {
        Log.d("proprio-service", "onCreate");

        directory = getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS + "/proprio");

        Log.v("dir",directory.getAbsolutePath());

        // Path to local data location


        // get preferences
        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS",0);

        // get all profile settings
        hand = preferences.getString("hand",UNSET);
        bezel = preferences.getString("bezel",UNSET);
        activity = preferences.getString("activity","tennis");
        userName = preferences.getString("googleUserName",UNSET);
        userId = preferences.getString("googleUserId",UNSET);
        gender = preferences.getString("gender", UNSET);
        age = preferences.getString("age", UNSET);
        height = preferences.getString("height", UNSET);
        customTag = preferences.getString("custom", UNSET);
        rating = preferences.getString("USTArating",UNSET);
        privacy = preferences.getString("Privacy",UNSET);

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            IO.setDefaultSSLContext(sc);
            HttpsURLConnection.setDefaultHostnameVerifier(new RelaxedHostNameVerifier());

            // socket options
            IO.Options opts = new IO.Options();
            opts.forceNew = false;
            opts.reconnection = false;
            opts.secure = true;
            opts.sslContext = sc;
            mSocket = IO.socket(getString(R.string.data_server));
            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d("proprio-service", "socket connected");
                    logLocally = false;
                    serverChecked = true;
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d("proprio-service", "socket disconnected");
                }

            });
            mSocket.on("ping_response", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.v("proprio-service", "socket: ping response success");
                    JSONObject obj = (JSONObject) args[0];
                    try {
                        String message = obj.getString("response");
                        if (message.equals("up")) {
                            logLocally = false;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });

            mSocket.on("data-ack", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("proprio-service", "socket: data received by server");

                }
            });

            mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.v("proprio-service", "socket connection error: " + args[0].toString());
                }
            });

        } catch (URISyntaxException e) {
            Log.d("proprio-service", "didn't connect to server...");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        if(!mSocket.connected()){
            mSocket.connect();
        }
        else {
            logLocally = false;
        }

        //checkDataServer();

        sResultReceiver = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                sResultReceiver,
                new IntentFilter("label.localIntent"));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.v("proprio-service", "Connection established");
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.v("proprio-service", "Connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.v("proprio-service", "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        serverChecked = true;

        super.onCreate();
    }

    private void configureOutput(final Long time) {
        // Use session start time being null as indicator we need to configure output
        if ( sessionStartTime == null && lastSessionStartTime != time) {
            Log.v("proprio-service", "setting session:" + time);
            sessionStartTime = time;
            lastSessionStartTime = time;

            if ( !isConnected() || logLocally ){
                String filename = userName + "_" + activity + "_" + sessionStartTime + ".json";

                file = new File(directory + "/" + filename);
                FileOutputStream stream = null;

                try {
                    stream = new FileOutputStream(file, true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                writer = new OutputStreamWriter(stream);
            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v("proprio-service", "onDataChanged:" + serverChecked + " " + logLocally);

        if( serverChecked ) {

            final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

            for (DataEvent event : events) {
                final Uri uri = event.getDataItem().getUri();
                final String path = uri != null ? uri.getPath() : null;

                Log.v("proprio-service", path);

                if ("/WEAR2PHONE".equals(path)) {
                    Log.v("onDataChanged", "response from sensors");

                    // Must mean we've enabled data sending.
                    // Check and see if new we need a new session.

                    final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    // read your values from map:
                    float X = map.getFloat("x");
                    float Y = map.getFloat("y");
                    float Z = map.getFloat("z");
                    float g1 = map.getFloat("g1");
                    float g2 = map.getFloat("g2");
                    float g3 = map.getFloat("g3");
                    float gx = map.getFloat("grav1");
                    float gy = map.getFloat("grav2");
                    float gz = map.getFloat("grav3");
                    float r1 = map.getFloat("r1");
                    float r2 = map.getFloat("r2");
                    float r3 = map.getFloat("r3");

                    String manufacturer = map.getString("manufacturer");
                    String model = map.getString("model");
                    String product = map.getString("product");
                    float accRange = map.getFloat("accRange");

                    configureOutput(map.getLong("sessionStartTime"));

                    long watchTime = map.getLong("watchtime");
                    long tabletTime = System.currentTimeMillis();

                    JSONObject sensdata = new JSONObject();
                    try {
                        sensdata.put("userName", userName);
                        sensdata.put("userId", userId);
                        sensdata.put("session", String.valueOf(sessionStartTime));
                        sensdata.put("activity", activity);
                        sensdata.put("ax", X);
                        sensdata.put("ay", Y);
                        sensdata.put("az", Z);
                        sensdata.put("g1", g1);
                        sensdata.put("g2", g2);
                        sensdata.put("g3", g3);
                        sensdata.put("r1", r1);
                        sensdata.put("r2", r2);
                        sensdata.put("r3", r3);
                        sensdata.put("gx", gx);
                        sensdata.put("gy", gy);
                        sensdata.put("gz", gz);
                        sensdata.put("watchtime", watchTime);
                        sensdata.put("tablettime", tabletTime);
                        sensdata.put("label", label);
                        sensdata.put("hand", hand);
                        sensdata.put("bezel", bezel);
                        sensdata.put("notes", customTag);
                        sensdata.put("manufacturer", manufacturer);
                        sensdata.put("product", product);
                        sensdata.put("model", model);
                        sensdata.put("age", age);
                        sensdata.put("gender", gender);
                        sensdata.put("accRange", accRange);
                        sensdata.put("heightInches", height);
                        sensdata.put("privacy",privacy);
                        sensdata.put("rating",rating);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (isConnected() && !logLocally) {
                        mSocket.emit("message", sensdata);
                    } else {
                        saveData(sensdata);
                    }

                }
            }
        }
        super.onDataChanged(dataEvents);
    }

    private void checkDataServer() {
        logLocally = true;
        if(mSocket != null && mSocket.connected()) {
            mSocket.emit("ping");
        }
        else{
            Log.v("checkDataServer","not pinging server...");
        }
    }


    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

  /*  Checks if external storage is available for read and write*/
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void saveData(JSONObject data) {
        if (!isExternalStorageWritable()) {
            Log.d("proprio-service", "External Storage Not Writable");
            return;
        }

        String dataJSON = data.toString() + "\n";
        try {
            writer.write(dataJSON);
        } catch (Exception e) {
            Log.d("proprio-service", "Error Saving Data");
            e.printStackTrace();
        }
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("proprio-service", "broadcast received");

                if (intent.getStringExtra("stopSession") != null ){
                    Log.d("proprio-service","stopping session!");
                    Log.d("proprio-service","Last:" + sessionStartTime);
                    try {
                        if(writer != null) {
                            writer.flush();
                            writer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    lastSessionStartTime = sessionStartTime;
                    sessionStartTime = null;
                    intent.removeExtra("stopSession");
                }

                if( intent.getStringExtra("label") != null ) {
                    label = intent.getStringExtra("label");
                }

            }
        };
    }

    @Override
    public void onDestroy() {
        Log.d("proprio-service", "onDestroy");

        Intent localIntent = new Intent("phone.localIntent");
        localIntent.putExtra("destroyed", "destroyed");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);

        mGoogleApiClient.disconnect();
        try {
            if( writer != null ){
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    private TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }
    } };

    public static class RelaxedHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}