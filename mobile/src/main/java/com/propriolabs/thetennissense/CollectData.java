package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CollectData extends Activity {

    private GoogleApiClient mGoogleApiClient;

    private BroadcastReceiver mResultReceiver;

    private boolean sending = false;

    private static int count = 0;

    private Intent localIntent;

    private String numForehands = "0";
    private String numBackhands = "0";
    private String numServes = "0";

    private static final String Tag = "Tennis Sense";
    private long currentSession;

    private Menu mOptionsMenu;

    private Long lastDataSent = null;

    private String locationProvider = LocationManager.NETWORK_PROVIDER;

    private LocationManager locationManager;

    private Timer handler = new Timer();

    private Timer dataChecker = new Timer();

    private static boolean fromWatch = false;

    private Location lastKnownLocation = null;

    private static boolean autoStart = false;

    private Chronometer chronometer;

    private Button startButton;

    private TextView statusView;

    private boolean signedIn = false;

    private Socket mSocket = null;

    private String googleUserId = null;

    private String googleUserName = null;
    private String labelmode = "No";

    private String test = "start";
    private String TAG = "CollectData";
    private boolean match = true;



    private String version = BuildConfig.VERSION_NAME;

    private String myAddress;
    private JSONObject weatherdata;

    public static boolean checkMaster(String[] theArray, String targetValue) {
        for(String s: theArray){
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    public void getWeather(double latitude, double longitude) {
        weatherdata = FetchWeather.getJSON(this, latitude, longitude);
        if (weatherdata != null) {
            Log.v(Tag, weatherdata.toString());
        }
        else {
            weatherdata = new JSONObject();

            Log.v(Tag, "Weather API Failed");
            JSONObject main = new JSONObject();
            JSONObject wind = new JSONObject();
            JSONArray weather = new JSONArray();
            JSONObject description = new JSONObject();
            try {
                main.put("temp", 0);
                wind.put("speed", 0);
                description.put("description", "");
                weather.put(description);
                weatherdata.put("main", main);
                weatherdata.put("wind", wind);
                weatherdata.put("weather", weather);
            }
            catch (JSONException e) {
            }
        }
    }

    public void getLocationAddress(double latitude, double longitude) {

        Geocoder geocoder= new Geocoder(this, Locale.ENGLISH);

        try {

            //Place your latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latitude,longitude, 1);

            if(addresses != null) {

                Address fetchedAddress = addresses.get(0);
                String strAddress = new String();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress = fetchedAddress.getAddressLine(i);
                }

                myAddress = strAddress.toString();
                Log.v(Tag, myAddress);

            }

            else
                myAddress = "Unknown location";
                Log.v(Tag, myAddress);

        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            myAddress = "Unknown";
            e.printStackTrace();
//            Toast.makeText(getApplicationContext(),"Could not get address..!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.v(CollectData.class.toString(), "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    private void populateLabels(String[] labels) {
        LinearLayout labelTable = (LinearLayout) findViewById(R.id.btn_layout);

        // first clear everything
        labelTable.removeAllViews();
        labelTable.setVisibility(View.VISIBLE);

        localIntent.putExtra("label", "");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);

        for (String s: labels ) {
            final Button button = new Button(getApplicationContext());
            button.setText(s);
            button.setTextColor(Color.parseColor("#000000"));
            button.setBackgroundColor(Color.RED);
            button.setTextSize(12);
            button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Button b = (Button) v;
                    String buttonText = b.getText().toString();
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            button.setBackgroundColor(Color.GREEN);
                            localIntent.putExtra("label", buttonText);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                            return true; // if you want to handle the touch event

                        case MotionEvent.ACTION_UP:
                            button.setBackgroundColor(Color.RED);
                            localIntent.putExtra("label", "");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                            return true; // if you want to handle the touch event
                    }
                    return false;
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 40);
            button.setLayoutParams(params);
            button.setWidth(200);
            button.setHeight(150);
            labelTable.addView(button);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(CollectData.class.toString(), "onNewIntent");
        super.onNewIntent(intent);
    }

    private void setUpSocket() {
        if(mSocket == null || !mSocket.connected()) {
            Log.v(CollectData.class.toString(),"setting up socket.");
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

                mSocket = IO.socket(getString(R.string.data_server), opts);
                mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        Log.d(CollectData.class.toString(), "mSocket connected");
                    }

                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(CollectData.class.toString(), "mSocket disconnected");
                    }

                });
                mSocket.on("analyze_results", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(CollectData.class.toString(), "mSocket analyze_results");
                        try {
                            JSONObject obj = (JSONObject) args[0];
                            JSONObject jsonObj = obj.getJSONObject("data");

                            setStats(jsonObj.getJSONObject("aggregate").getString("Forehands"),
                                    jsonObj.getJSONObject("aggregate").getString("Backhands"),
                                    jsonObj.getJSONObject("aggregate").getString("Serves"));

                            final PutDataMapRequest putRequest = PutDataMapRequest.
                                    create("/STATS_UPDATE");

                            putRequest.setUrgent();
                            final DataMap map = putRequest.getDataMap();
                            map.putString("forehand", numForehands);
                            map.putString("backhand", numBackhands);
                            map.putString("serve", numServes);
                            Wearable.DataApi.putDataItem(mGoogleApiClient,
                                    putRequest.asPutDataRequest());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(CollectData.class.toString(), "mSocket connect error: " + args[0].toString());
                    }

                });

                if(!mSocket.connected()){
                    mSocket.connect();
                }


            }catch(URISyntaxException e){
                e.printStackTrace();
            }catch(NoSuchAlgorithmException e){
                e.printStackTrace();
            }catch(KeyManagementException e){
                e.printStackTrace();
            }
        }
        else {
            Log.v(CollectData.class.toString(),"Socket already connected.");
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(CollectData.class.toString(), "onRestoreInstanceState");
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void finish(){
        Log.v(CollectData.class.toString(), "finish");
        super.finish();
    }

    private void goToLandingPage() {
        Intent LandingPage = new Intent(this, LandingSignIn.class);
        startActivity(LandingPage);
        finish();
        return;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        count++;
        Log.v(CollectData.class.toString(), "onCreate:" + count);
        final SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        googleUserId = preferences.getString("googleUserId", null);
        labelmode = preferences.getString("labelmode", "No");
        Resources res = getResources();
        final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
        if(!isInternetConnected() && !master) {
            goToLandingPage();
        }

        locationManager = (LocationManager)
                this.getSystemService(Context.LOCATION_SERVICE);

        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

        setUpSocket();

        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();

        Tracker mTracker = application.getDefaultTracker();
        Log.i(CollectData.class.toString(), "Setting screen name: " + "Play");
        mTracker.setScreenName("Image~" + "Play");

        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        boolean tempFromWatch = getIntent().getBooleanExtra("FROM_WATCH", fromWatch);

        googleUserName = preferences.getString("googleUserName", null);

        signedIn = (preferences.getString("googleUserId", null) != null );

        String wrist = preferences.getString("wrist","left");

        setContentView(R.layout.activity_play);

        if(tempFromWatch && !fromWatch){
            autoStart = true;
        }
        else{
            autoStart = false;
        }


        fromWatch = tempFromWatch;

        localIntent = new Intent("label.localIntent");
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        statusView = (TextView) findViewById(R.id.recordingstatus);
        if (lastKnownLocation != null) {
            getLocationAddress(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getWeather(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
            }).start();
        }
        else {
            myAddress = "Unknown location";
        }
        final Spinner activitySelector = (Spinner) findViewById(R.id.spin);
        if (labelmode.equals("Yes")) {
            ((Spinner) findViewById(R.id.spin)).setVisibility(View.VISIBLE);
        }
        else {
            ((Spinner) findViewById(R.id.spin)).setVisibility(View.GONE);
        }
            activitySelector.setFocusableInTouchMode(true);
        activitySelector.requestFocus();
        activitySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int picId = getResources().getIdentifier(parent.getItemAtPosition(position).toString(), "array", getApplicationContext().getPackageName());

                if (labelmode.equals("Yes")) {
                    ((Spinner) findViewById(R.id.spin)).setVisibility(View.VISIBLE);
                    ((RadioGroup) findViewById(R.id.radiogroup)).setVisibility(View.INVISIBLE);
                    ((TableLayout) findViewById(R.id.stroketable)).setVisibility(View.INVISIBLE);
                    populateLabels(getResources().getStringArray(picId));
                }
                SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
                SharedPreferences.Editor edit = preferences.edit();
                edit.putString("activity", parent.getItemAtPosition(position).toString());
                edit.putInt("activity_index", position);
                edit.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        int act = preferences.getInt("activity_index",0);

        activitySelector.setSelection(act);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.v(CollectData.class.toString(), "Connection established");

                        // Set connectivity status in activity

                        if(!fromWatch) {
                            Log.v("proprio-mobile", "Sending create FROM mobile!");
                            findViewById(R.id.startApp).setEnabled(false);
                            findViewById(R.id.startApp).setVisibility(View.VISIBLE);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                                    for (Node node : nodes.getNodes()) {
                                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                                mGoogleApiClient, node.getId(), "/PING", "START_STOP".getBytes()).await();
                                    }
                                }
                            }).start();
                        }
                        else {
                            Log.v(CollectData.class.toString(), "Not Sending create FROM mobile!");
                            findViewById(R.id.startApp).setEnabled(true);
                        }

                        if(!signedIn && !isInternetConnected()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                                    for (Node node : nodes.getNodes()) {
                                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                                mGoogleApiClient, node.getId(), "/NO_INTERNET", "NO_INTERNET".getBytes()).await();
                                    }
                                }
                            }).start();
                            Intent newIntent = new Intent(getApplicationContext(), LandingSignIn.class);
                            newIntent.putExtra("SIGN_OUT", true);
                            startActivity(newIntent);
                        }
                        else if(!signedIn) {
                            Intent newIntent = new Intent(getApplicationContext(), LandingSignIn.class);
                            newIntent.putExtra("SIGN_OUT", true);
                            startActivity(newIntent);
                        }
                        else if(!isInternetConnected()) {
                            // we can still log data locally here.  this is no problem
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.v(CollectData.class.toString(), "Connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.v(CollectData.class.toString(), "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        mResultReceiver = createBroadcastReceiver();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                mResultReceiver,
                new IntentFilter("phone.localIntent"));



        startButton = (Button) findViewById(R.id.startApp);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RadioButton radioMatch = (RadioButton) findViewById(R.id.radioMatch);
                match = radioMatch.isChecked();
                if (!sending) {
                    startButton.setText("End");
                    startButton.setVisibility(View.VISIBLE);
                    startButton.setBackgroundColor(Color.RED);
                    statusView.setText("Recording");
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    currentSession = System.currentTimeMillis();
                    JSONObject newSession = new JSONObject();
                    try {
                        newSession.put("userId", googleUserId);
                        newSession.put("sessionId", currentSession);
                        newSession.put("userName", googleUserName);
                        if (lastKnownLocation != null) {
                            newSession.put("latitude", lastKnownLocation.getLatitude());
                            newSession.put("longitude", lastKnownLocation.getLongitude());
                        }
                        else {
                            newSession.put("latitude", 0);
                            newSession.put("longitude", 0);
                        }
                        newSession.put("match", match);
                        newSession.put("address", myAddress);
                        newSession.put("version", version);
                        if (weatherdata == null) {
                            weatherdata = new JSONObject();
                            JSONObject main = new JSONObject();
                            JSONObject wind = new JSONObject();
                            JSONArray weather = new JSONArray();
                            JSONObject description = new JSONObject();
                            try {
                                main.put("temp", 0);
                                wind.put("speed", 0);
                                description.put("description", "");
                                weather.put(description);
                                weatherdata.put("main", main);
                                weatherdata.put("wind", wind);
                                weatherdata.put("weather", weather);
                            }
                            catch (JSONException e) {
                            }
                        }
                        Log.v(Tag, weatherdata.toString());
                        newSession.put("temperature", weatherdata.getJSONObject("main").getInt("temp") - 273);
                        newSession.put("wind", weatherdata.getJSONObject("wind").getInt("speed"));
                        newSession.put("weather", weatherdata.getJSONArray("weather").getJSONObject(0).getString("description"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.v("Mistake", "exception", e);
                    }
                    mSocket.emit("new_session",newSession);
                    setStats("0", "0", "0");
                    setMenuEnabled(false);


                    if (isInternetConnected()) {
                        handler = new Timer();

                        TimerTask tt = new TimerTask() {
                            @Override
                            public void run() {
                                newAnalyzeData(Long.toString(currentSession));
                            }
                        };

                        handler.scheduleAtFixedRate(tt, 3000, 3000);

                        dataChecker = new Timer();

                        TimerTask serverCheck = new TimerTask() {
                            @Override
                            public void run() {
                                if( lastDataSent != null ) {
                                    if (System.currentTimeMillis() - lastDataSent > 30000) {
                                        runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        TextView dataSent = (TextView) findViewById(R.id.lastDataSent);
                                                        dataSent.setText("Data Server may be down!");
                                                        dataSent.setTextColor(Color.RED);
                                                    }
                                                }
                                        );
                                    }
                                }
                        }
                        };

                        dataChecker.scheduleAtFixedRate(serverCheck, 3000, 3000);
                    }


                } else {
                    Random rand = new Random();
                    int  variablereward = rand.nextInt(100) + 1;
                    if (Integer.parseInt(numForehands) > 5) {
                        if (variablereward > 80) {
                            Toast.makeText(getApplicationContext(), "Awesome work!", Toast.LENGTH_LONG).show();
                        } else if (variablereward > 60 && variablereward < 80) {
                            Toast.makeText(getApplicationContext(), "Nice playing!", Toast.LENGTH_LONG).show();
                        } else if (variablereward > 48 && variablereward < 60) {
                            Toast.makeText(getApplicationContext(), "Great job!", Toast.LENGTH_LONG).show();
                        }
                    }

                    startButton.setText("Play");
                    startButton.setBackgroundColor(getResources().getColor(R.color.accent));
                    statusView.setText("Ready!");
                    chronometer.stop();
                    setConnectivity();

                    setMenuEnabled(true);

                    if (isInternetConnected()) {
                        handler.cancel();
                        dataChecker.cancel();
                    }

                    // We assume DataLayerListenerService IS running here because it's getting
                    // data from the watch.

                    Intent tempIntent = new Intent("label.localIntent");
                    tempIntent.putExtra("stopSession", "stop");
                    LocalBroadcastManager.getInstance
                            (getApplicationContext()).sendBroadcast(tempIntent);
                }

                sending = !sending;


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                        for (Node node : nodes.getNodes()) {
                            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                    mGoogleApiClient, node.getId(), "/PHONE2WEAR", Long.toString(currentSession).getBytes()).await();
                        }
                    }
                }).start();
                mSocket.disconnect();



            }
        });


    }

    private void setStats(final String forehands, final String backhands, final String serves) {

        numForehands = forehands;
        numBackhands = backhands;
        numServes = serves;

        final TextView n_forehands = (TextView) findViewById(R.id.n_forehands);
        final TextView n_backhands = (TextView) findViewById(R.id.n_backhands);
        final TextView n_serves = (TextView) findViewById(R.id.n_serves);

        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                n_forehands.setText(String.valueOf(numForehands));
                n_backhands.setText(String.valueOf(numBackhands));
                n_serves.setText(String.valueOf(numServes));
            }
        }));

        Log.v(CollectData.class.toString(), "Forehands: " + numForehands);
        Log.v(CollectData.class.toString(), "Backhands: " + numBackhands);
        Log.v(CollectData.class.toString(), "Serves: " + numServes);
    }

    private void newAnalyzeData(final String sec) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("session", sec);
            userObj.put("googleUserId", googleUserId);
            userObj.put("googleUserName", googleUserName);
            mSocket.emit("analyze", userObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    `sec` refers to the current session_id -- We should change this to make it obvious.
    private void analyzeData(final String sec) {

        if (sec != null) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    HttpURLConnection urlConnection = null;
                    try {
                        URL url = new URL(getString(R.string.analysis_server) + "/tennis");
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setDoOutput(true);
                        urlConnection.setRequestProperty("Content-Type", "application/json");
                        JSONObject jsonParam = new JSONObject();
                        try {
                            jsonParam.put("session", sec);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        OutputStream rout = new BufferedOutputStream(urlConnection.getOutputStream());
                        rout.write(jsonParam.toString().getBytes());
                        rout.flush();
                        rout.close();

                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        final StringBuilder out = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            out.append(line);
                        }
                        Log.v(CollectData.class.toString(), "API Result: " + out.toString());

                        try {
                            JSONObject jsonObj = new JSONObject(out.toString());

                            setStats(jsonObj.getJSONObject("aggregate").getString("Forehands"),
                                    jsonObj.getJSONObject("aggregate").getString("Backhands"),
                                    jsonObj.getJSONObject("aggregate").getString("Serves"));

                            final PutDataMapRequest putRequest = PutDataMapRequest.
                                    create("/STATS_UPDATE");
                            
                            putRequest.setUrgent();
                            final DataMap map = putRequest.getDataMap();
                            map.putString("forehand", numForehands);
                            map.putString("backhand", numBackhands);
                            map.putString("serve", numServes);
                            Wearable.DataApi.putDataItem(mGoogleApiClient,
                                    putRequest.asPutDataRequest());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
            }).start();
        }
    }





    private void setConnectivity() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isConnected = isInternetConnected();

                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                final int howMany = nodes.getNodes().size();
                StringBuffer message = new StringBuffer();
                final String col;

                if (isConnected) {
                    Log.v(CollectData.class.toString(), "Internet Connected");

                }
                else {
                    message.append("Internet Not Connected ");
                    Log.v(CollectData.class.toString(), "Internet Not Connected");


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mOptionsMenu != null) {
                                mOptionsMenu.getItem(5).setEnabled(false);
                            }
                            if (!labelmode.equals("Yes")) {
                                startButton.setEnabled(false);
                                startButton.setText("Please connect to the Internet");
                                startButton.setVisibility(View.VISIBLE);
                                startButton.setBackgroundColor(Color.GRAY);
                                startButton.setTextColor(Color.BLACK);
                            }
                        }
                    });
                }

                if( howMany >= 1) {
                    Log.v(CollectData.class.toString(), "Watch Connected");
                    message.append("Ready!");
                    col = "BLACK";

                }
                else {
                    Log.v(CollectData.class.toString(), "Watch Not Connected");
                    message.append("Android Wear Not Found");
                    col = "BLACK";
                    runOnUiThread(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startButton.setEnabled(false);
                            startButton.setText("Please connect Android Wear");
                            startButton.setVisibility(View.VISIBLE);
                            startButton.setBackgroundColor(Color.GRAY);
                            startButton.setTextColor(Color.BLACK);
                        }
                    }));

                }

                final String outMessage = message.toString();
                runOnUiThread(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.recordingstatus)).setText(outMessage);
                        ((TextView)findViewById(R.id.recordingstatus)).
                                setTextColor(Color.parseColor(col));
                    }
                }));
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_my, menu);
        menu.findItem(R.id.sign_in_out).setTitle("Sign Out");
        setConnectivity();

        if(fromWatch && signedIn && autoStart) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startButton.setEnabled(true);
                    Timer clicker = new Timer();

                    TimerTask tt = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startButton.callOnClick();
                                }
                            });

                        }
                    };
                    clicker.schedule(tt, 2000);
                }
            });


        }
        else if(fromWatch && signedIn){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startButton.setEnabled(true);
                }
            });
        }
        else {
            Timer playEnabler = new Timer();

            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    startButton.setEnabled(true);
                                }
                            }
                    );
                }
            };

            playEnabler.schedule(tt,2000);
        }

        return true;
    }

    /*
        Method to enable/disable menu while session is inactive/active.
     */
    private void setMenuEnabled(final boolean enabled) {
        for(int i=0; i< mOptionsMenu.size(); i++) {
            mOptionsMenu.getItem(i).setEnabled(enabled);
        }
    }

    //Navigate between classes from the Action Bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.profile_settings) {
            Intent newIntent = new Intent(getApplicationContext(), SettingsPage.class);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.sign_in_out) {
            Intent newIntent = new Intent(getApplicationContext(), LandingSignIn.class);
            newIntent.putExtra("SIGN_OUT",true);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.sessions) {
            Intent newIntent = new Intent(getApplicationContext(), SessionActivity.class);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.profilepage) {
            Intent newIntent = new Intent(getApplicationContext(), UserStats.class);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.feedback) {
            Intent newIntent = new Intent(getApplicationContext(), FaqActivity.class);
            startActivity(newIntent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStop() {
        Log.v(CollectData.class.toString(), "onStop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.v(CollectData.class.toString(), "onRestart");
        super.onRestart();
    }

    @Override
    protected void onStart() {
        Log.v(CollectData.class.toString(), "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.v(CollectData.class.toString(), "onResume");
        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        final String googleUserId = preferences.getString("googleUserId", null);

        if(googleUserId == null) {
            Intent newIntent = new Intent(getApplicationContext(), LandingSignIn.class);
            newIntent.putExtra("SIGN_OUT",true);
            startActivity(newIntent);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(CollectData.class.toString(), "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        count--;
        Log.d(CollectData.class.toString(), "onDestroy:" + count);

        Button button = (Button)findViewById(R.id.startApp);

        if(sending) {
            button.callOnClick();
        }

        test = "end";

        super.onDestroy();
    }

    @Override
    public boolean isFinishing() {
        Log.d(CollectData.class.toString(), "isFinishing:" + super.isFinishing());
        return super.isFinishing();
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(CollectData.class.toString(),"broadcast received");
                Log.d(CollectData.class.toString(),intent.getAction());

                if (intent.getStringExtra("enable") != null ) {
                    Button button = (Button)findViewById(R.id.startApp);
                    button.setEnabled(true);
                    button.setText("Start");
                    button.setBackgroundColor(Color.GREEN);
                }
                else if (intent.getStringExtra("DATA-ACK") != null){
                        lastDataSent = System.currentTimeMillis();
                }
                else if (intent.getStringExtra("WATCH_READY") != null){
                    startButton.setEnabled(true);
                }
                else if (intent.getStringExtra("disable") != null ) {
                    Button button = (Button)findViewById(R.id.startApp);
                    if(sending){
                        button.callOnClick();
                    }
                    button.setEnabled(false);
                    button.setText("Please Restart App");
                    button.setBackgroundColor(Color.RED);
                    fromWatch = false;
                }

                else if (intent.getStringExtra("running") != null && intent.getStringExtra("running").equals("ready")) {
                    Intent testIntent = new Intent("label.localIntent");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(testIntent);
                }
                else {
                        Log.v(CollectData.class.toString(),"createBroadcastReceived: strange case");
                }
            }
        };
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
