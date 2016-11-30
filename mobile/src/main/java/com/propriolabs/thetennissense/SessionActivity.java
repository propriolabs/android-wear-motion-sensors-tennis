package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SessionActivity extends Activity {

    // class member instance of menu.  need to hold onto this because we change it
    private Menu mOptionsMenu;

    // class member instance of card item_new and recyclerView
    private RecyclerView rv;

    private SessionRVAdapter adapter;

    private boolean global = false;

    private List<Session> sessions2 = new ArrayList<>();

    private HashMap<String,Long> dateToMs = new HashMap<String,Long>();

    private File directory;

    private String googleUserId;

    private String googleUserName;

    private String  myStats = "";
    private boolean signedintoTwitter = false;

    private Socket mSocket;

    public static boolean checkMaster(String[] theArray, String targetValue) {
        for(String s: theArray){
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    public static String toCamelCase(String inputString) {
        String result = "";
        if (inputString.length() == 0) {
            return result;
        }
        char firstChar = inputString.charAt(0);
        char firstCharToUpperCase = Character.toUpperCase(firstChar);
        result = result + firstCharToUpperCase;
        for (int i = 1; i < inputString.length(); i++) {
            char currentChar = inputString.charAt(i);
            char previousChar = inputString.charAt(i - 1);
            if (previousChar == ' ') {
                char currentCharToUpperCase = Character.toUpperCase(currentChar);
                result = result + currentCharToUpperCase;
            } else {
                char currentCharToLowerCase = Character.toLowerCase(currentChar);
                result = result + currentCharToLowerCase;
            }
        }
        return result;
    }

    private void compose(final String yoyo) {
        TweetComposer.Builder builder = new TweetComposer.
                Builder(this)
                .text(yoyo);
        builder.show();
    }

    private void setUpSocket() {
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
            mSocket = IO.socket(getString(R.string.data_server),opts);
            mSocket.on("analyze_results", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.v(SessionActivity.class.toString(), "posted results...");
                }

            });
            mSocket.on("userStats", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    try {
                        JSONArray sessions = obj.getJSONArray("data");
                        JSONArray meta = obj.getJSONArray("meta");

                        int rally_count = 0;
                        long hit_per_rally = 0;
                        for (int i = 0; i < sessions.length(); i++) {
                            JSONObject bucket = sessions.getJSONObject(i);
                            JSONObject meta_bucket = meta.getJSONObject(i);
                            String temperature = "";
                            String weather = "";
                            String wind = "";
                            String address = "";
                            if (meta_bucket.getInt("temperature") != -273) {
                                temperature = "Temperature: " + Integer.toString(meta_bucket.getInt("temperature")) + "° C";
                                weather = "Weather: " + toCamelCase(meta_bucket.getString("weather"));
                                wind = "Wind Speed: " + Integer.toString(meta_bucket.getInt("wind")) + " mph";
                                address = "Location: " + meta_bucket.getString("address").replaceAll("\\d", "");
                            }
                            else {
                                temperature = "";
                                weather = "";
                                wind = "";
                                address = "";
                            }
                            Boolean match = meta_bucket.getBoolean("match");
                            SimpleDateFormat sdf_clean = new SimpleDateFormat("EEE, MMM d, ''yy");
                            String sessionId = bucket.getString("session");
                            String userName = bucket.getString("userName");
                            String calories = Integer.toString(bucket.getInt("calories")) + " calories";
                            boolean analyzed = false;
                            int strokesCounted = 0;
                            analyzed = true;
                            JSONObject agg = bucket.getJSONObject("aggregate");
                            strokesCounted += agg.getInt("Serves");
                            strokesCounted += agg.getInt("Forehands");
                            strokesCounted += agg.getInt("Backhands");
                            JSONArray rallies = bucket.getJSONArray("rallies");
                            rally_count = rallies.length();
                            if (rally_count>0) {
                                hit_per_rally = (long)strokesCounted/rally_count;
                            }
                            DecimalFormat df = new DecimalFormat("#.#");
                            String meanRally = "";
                            if (hit_per_rally > 1) {
                                meanRally = df.format(hit_per_rally) + " hits per rally";
                            }
                            else {
                                meanRally = df.format(hit_per_rally) + " hit per rally";
                            }

                            Date date_start = new Date(Long.parseLong(sessionId));
                            dateToMs.put(sdf_clean.format(date_start), Long.parseLong(sessionId));
                            long endTime = bucket.getLong("max_time");
                            Date date_end = new Date(endTime);
                            long diffMs = date_end.getTime() - date_start.getTime();
                            long diffMin = Math.round(diffMs / 60000);
                            String duration = "0";
                            if (diffMin < 1) {
                                duration = "under a minute";
                            }
                            else if (diffMin == 1) {
                                duration = "1 minute";
                            }
                            else {
                                duration = Long.toString(diffMin) + " minutes";
                            }
                            SimpleDateFormat sdf_hhmm = new SimpleDateFormat("hh:mm");
                            SimpleDateFormat sdf_hhmma = new SimpleDateFormat("hh:mm a");
                            String startstop = sdf_hhmm.format(date_start) + " - " + sdf_hhmma.format(date_end);
                            String date = sdf_clean.format(date_start);
                            String strokesOverTime = "";
                            if (strokesCounted==1) {
                                strokesOverTime = Integer.toString(strokesCounted) + " stroke in " + duration;
                            }
                            else {
                                strokesOverTime = Integer.toString(strokesCounted) + " strokes in " + duration;
                            }
                            if (i==0 && agg.getInt("Forehands")!=1 && agg.getInt("Backhands")!=1) {
                                myStats = "Just played tennis for " + duration + ", and hit " + Integer.toString(agg.getInt("Forehands")) + " forehands and " + Integer.toString(agg.getInt("Backhands")) + " backhands via @tennis_sense";
                            }
                            else if (i==0 && agg.getInt("Forehands")==1 && agg.getInt("Backhands")!=1) {
                                myStats = "Just played tennis for " + duration + ", and hit " + Integer.toString(agg.getInt("Forehands")) + " forehand and " + Integer.toString(agg.getInt("Backhands")) + " backhands via @tennis_sense";

                            }
                            else if (i==0 && agg.getInt("Forehands")!=1 && agg.getInt("Backhands")==1) {
                                myStats = "Just played tennis for " + duration + ", and hit " + Integer.toString(agg.getInt("Forehands")) + " forehands and " + Integer.toString(agg.getInt("Backhands")) + " backhand via @tennis_sense";

                            }
                            else if (i==0 && agg.getInt("Forehands")==1 && agg.getInt("Backhands")==1) {
                                myStats = "Just played tennis for " + duration + ", and hit " + Integer.toString(agg.getInt("Forehands")) + " forehand and " + Integer.toString(agg.getInt("Backhands")) + " backhand via @tennis_sense";

                            }
                            sessions2.add(new Session(googleUserId, sessionId, startstop, date,
                                    "Cloud", R.mipmap.ic_tennis, analyzed,
                                    strokesCounted, strokesOverTime,
                                    userName, calories, Integer.toString(rally_count) + " rallies", meanRally, meanRally, weather, wind, temperature, address, Boolean.toString(match)
                                    ));
                        }
                        initializeAdapter();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });
            mSocket.on("GlobalStats", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];

                    try {
                        JSONArray sessions = obj.getJSONArray("data");


                        int rally_count = 0;
                        long hit_per_rally = 0;
                        for (int i = 0; i < sessions.length(); i++) {
                            JSONObject bucket = sessions.getJSONObject(i);
                            SimpleDateFormat sdf_clean = new SimpleDateFormat("EEE, MMM d, ''yy");
                            String sessionId = bucket.getString("session");
                            String userName = bucket.getString("userName");
                            String calories = Integer.toString(bucket.getInt("calories")) + " calories";
                            boolean analyzed = false;
                            int strokesCounted = 0;
                            analyzed = true;
                            JSONObject agg = bucket.getJSONObject("aggregate");
                            strokesCounted += agg.getInt("Serves");
                            strokesCounted += agg.getInt("Forehands");
                            strokesCounted += agg.getInt("Backhands");
                            JSONArray rallies = bucket.getJSONArray("rallies");
                            rally_count = rallies.length();
                            if (rally_count>0) {
                                hit_per_rally = (long)strokesCounted/rally_count;
                            }
                            DecimalFormat df = new DecimalFormat("#.#");
                            String meanRally = "";
                            if (hit_per_rally > 1) {
                                meanRally = df.format(hit_per_rally) + " hits per rally";
                            }
                            else {
                                meanRally = df.format(hit_per_rally) + " hit per rally";
                            }

                            Date date_start = new Date(Long.parseLong(sessionId));
                            dateToMs.put(sdf_clean.format(date_start), Long.parseLong(sessionId));
                            long endTime = bucket.getLong("max_time");
                            Date date_end = new Date(endTime);
                            long diffMs = date_end.getTime() - date_start.getTime();
                            long diffMin = Math.round(diffMs / 60000);
                            String duration = "0";
                            if (diffMin < 1) {
                                duration = "under a minute";
                            }
                            else if (diffMin == 1) {
                                duration = "1 minute";
                            }
                            else {
                                duration = Long.toString(diffMin) + " minutes";
                            }
                            SimpleDateFormat sdf_hhmm = new SimpleDateFormat("hh:mm");
                            SimpleDateFormat sdf_hhmma = new SimpleDateFormat("hh:mm a");
                            String startstop = sdf_hhmm.format(date_start) + " - " + sdf_hhmma.format(date_end);
                            String date = sdf_clean.format(date_start);
                            String strokesOverTime = "";
                            if (strokesCounted==1) {
                                strokesOverTime = Integer.toString(strokesCounted) + " stroke in " + duration;
                            }
                            else {
                                strokesOverTime = Integer.toString(strokesCounted) + " strokes in " + duration;
                            }
                            sessions2.add(new Session(googleUserId, sessionId, startstop, date,
                                    "Cloud", R.mipmap.ic_tennis, analyzed,
                                    strokesCounted, strokesOverTime,
                                    userName, calories, Integer.toString(rally_count) + " rallies", meanRally, meanRally, "", "", "", "", ""
                            ));
                        }

                        initializeAdapter();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });
            if(!mSocket.connected()) {
                mSocket.connect();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public void analyzeData(final String sessionId) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("session", sessionId);
            userObj.put("googleUserId", googleUserId);
            mSocket.emit("analyze", userObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }




    // Function to populate local files
    private void populateLocalFiles() {
        Resources res = getResources();
        final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
        if (master) {

            File[] listOfFiles = directory.listFiles();

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a MM/dd/yyyy");

            dateToMs.clear();
            sessions2.clear();

            if (listOfFiles != null) {
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        String[] parts = listOfFile.getName().split("_");
                        String ms = parts[2].split("\\.")[0];
                        Date date = new Date(Long.parseLong(ms));
                        dateToMs.put(sdf.format(date), Long.parseLong(ms));
                        String duration = "Duration Not Computed";
                        long minutes = 0;
                        try {
                            minutes = countLines(listOfFile.getAbsolutePath()) / (16 * 60);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (minutes < 1) {
                            duration = "Less than a minute";
                        } else {
                            duration = Long.toString(minutes) + " minutes";
                        }
                        sessions2.add(new Session(googleUserId,
                                ms, duration, sdf.format(date), "Local",
                                R.mipmap.ic_tennis, false, 0, "Local Data Storage for Master User", googleUserName, "", "", "", "", "", "", "", "", ""));
                    }
                }
            }
        }
    }


    @Override
    protected void onResume() {
        Log.v("SessionActivity", "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.v("SessionActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.v("SessionActivity", "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v("SessionActivity", "onStop");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.v("SessionActivity","onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.v("SessionActivity", "onRestart");
        checkLoggedIn();

        if(mSocket == null || !mSocket.connected()){
            setUpSocket();
        }

        setContentView(R.layout.activity_matchstats);

        rv = (RecyclerView)findViewById(R.id.rv);
        rv.removeAllViews();

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        initializeAdapter();

        directory = getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS + "/proprio");

        dateToMs.clear();
        sessions2.clear();

        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        googleUserId = preferences.getString("googleUserId", null);
        googleUserName = preferences.getString("googleUserName", null);

        // local files (from offline collect) we add to the sessionlist first
        populateLocalFiles();
        if (isInternetConnected()) {
            populateCloudSessions(googleUserId);
        }

        final Button me = (Button) findViewById(R.id.personal);
        final Button everybody = (Button) findViewById(R.id.global);
        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.setBackgroundColor(getResources().getColor(R.color.accent));
                everybody.setBackgroundColor(getResources().getColor(R.color.primaryText));
                global = false;
                rv.removeAllViews();
                dateToMs.clear();
                sessions2.clear();
                populateLocalFiles();
                if (isInternetConnected()) {
                    populateCloudSessions(googleUserId);
                }
            }
        });

        everybody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                everybody.setBackgroundColor(getResources().getColor(R.color.accent));
                me.setBackgroundColor(getResources().getColor(R.color.primaryText));
                global = true;
                rv.removeAllViews();
                directory = getApplicationContext().getExternalFilesDir(
                        Environment.DIRECTORY_DOCUMENTS + "/proprio");
                dateToMs.clear();
                sessions2.clear();
                populateLocalFiles();
                if (isInternetConnected()) {
                    populateGlobalCloudSessions(googleUserId);
                }
            }
        });


        super.onRestart();
    }

    // Method to kick us back to login if user isn't signed in
    private void checkLoggedIn() {
        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        final String googleUserId = preferences.getString("googleUserId", null);

        if(googleUserId == null) {
            Intent newIntent = new Intent(getApplicationContext(), LandingSignIn.class);
            newIntent.putExtra("SIGN_OUT",true);
            startActivity(newIntent);
        }
    }

    private void populateCloudSessions(final String googleUserId) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("userId", googleUserId);
            mSocket.emit("userStats", userObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void populateGlobalCloudSessions(final String googleUserId) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("userId", googleUserId);
            mSocket.emit("GlobalStats", userObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void goToLandingPage() {
        Intent LandingPage = new Intent(this, LandingSignIn.class);
        startActivity(LandingPage);
        finish();
        return;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("SessionActivity", "onCreate");
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        googleUserId = preferences.getString("googleUserId", null);

        Resources res = getResources();
        final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
        if(!isInternetConnected() && !master) {
            goToLandingPage();
        }
        checkLoggedIn();

        if (mSocket == null || !mSocket.connected()) {
            setUpSocket();
        }

        setContentView(R.layout.activity_matchstats);

        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Image~" + "MatchStats");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        rv = (RecyclerView) findViewById(R.id.rv);
        rv.removeAllViews();

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);


        directory = getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS + "/proprio");

        dateToMs.clear();
        sessions2.clear();




        populateLocalFiles();
        if (isInternetConnected()) {
            populateCloudSessions(googleUserId);
        }


        signedintoTwitter = preferences.getString("twitterUserName", null) != null;

        final Button me = (Button) findViewById(R.id.personal);
        final Button everybody = (Button) findViewById(R.id.global);
        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Click","Just me!");
                me.setBackgroundColor(getResources().getColor(R.color.accent));
                rv.removeAllViews();
                everybody.setBackgroundColor(getResources().getColor(R.color.primaryText));
                dateToMs.clear();
                sessions2.clear();
                populateLocalFiles();
                if (isInternetConnected()) {
                    populateCloudSessions(googleUserId);
                }
                if (signedintoTwitter) {
                    findViewById(R.id.tweetlastsession).setVisibility(View.VISIBLE);
                    findViewById(R.id.tweetexplainer).setVisibility(View.VISIBLE);

                }
            }
        });

        everybody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Click","Everybody");
                everybody.setBackgroundColor(getResources().getColor(R.color.accent));
                me.setBackgroundColor(getResources().getColor(R.color.primaryText));
                rv.removeAllViews();
                directory = getApplicationContext().getExternalFilesDir(
                        Environment.DIRECTORY_DOCUMENTS + "/proprio");
                dateToMs.clear();
                sessions2.clear();
                if (isInternetConnected()) {
                    Log.v("Get", "all the data");
                    populateGlobalCloudSessions(googleUserId);
                }
                findViewById(R.id.tweetlastsession).setVisibility(View.GONE);
                findViewById(R.id.tweetexplainer).setVisibility(View.GONE);

            }
        });

        signedintoTwitter = preferences.getString("twitterUserName", null) != null;
        if (signedintoTwitter) {
            findViewById(R.id.tweetlastsession).setVisibility(View.VISIBLE);
            findViewById(R.id.tweetexplainer).setVisibility(View.VISIBLE);
            final ImageButton tweetlastsession = (ImageButton) findViewById(R.id.tweetlastsession);
            tweetlastsession.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.tweetlastsession:
                            compose(myStats);
                            break;
                    }
                }
            });
        }
        initializeAdapter();
    }



    private void initializeAdapter(){
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                adapter = new SessionRVAdapter(sessions2, dateToMs, SessionActivity.this);
                rv.setAdapter(adapter);
                TableRow tr = (TableRow)findViewById(R.id.booya);
                tr.setVisibility(View.VISIBLE);

                TextView tv = (TextView)findViewById(R.id.emptyMessage);
                if (tv != null) {
                    if (sessions2.isEmpty()) {
                        tv.setVisibility(View.VISIBLE);
                        tv.setText("After you play your first match, check out your stats here. Get out and play!");
                    } else {
                        tv.setVisibility(View.GONE);
                        tv.setText("");
                    }
                }
            }
        }));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_my, menu);
        menu.findItem(R.id.sign_in_out).setTitle("Sign Out");
        if(!isInternetConnected()) {
            mOptionsMenu.getItem(5).setEnabled(false);
        }
        return true;
    }

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
        else if (id == R.id.mainapp) {
            Intent newIntent = new Intent(getApplicationContext(), CollectData.class);
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



/*
    private void pushData(final String sec){
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS +
                R.string.proprio_data_directory) + "/" + sec);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                JSONObject jsonObj = new JSONObject(line);
                mSocket.emit("message",jsonObj);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
*/

    private boolean isInternetConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    // Method to count lines in file.  Need this for local sessions we want to know the number of
    // samples for.
    private static int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
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




