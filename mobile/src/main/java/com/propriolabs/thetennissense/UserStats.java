package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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

/**
 * Created by Stinson on 4/27/16.
 */
public class UserStats extends Activity implements
        View.OnClickListener {


    @Override
    public void onClick(View v) {
    }

    private static final String TAG = "PROFILEPAGE";

    private Socket mSocket;
    private boolean signedintoTwitter = false;
    private Menu mOptionsMenu;
    private String  myStats = "";
    private List<Session> sessions2 = new ArrayList<>();

    private HashMap<String,Long> dateToMs = new HashMap<String,Long>();

    private String googleUserId;

    private JSONObject information;

    public static boolean checkMaster(String[] theArray, String targetValue) {
        for(String s: theArray){
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    private void goToMainApp() {

        Intent Collect = new Intent(this, CollectData.class);
        startActivity(Collect);

        finish();
        return;
    }

    private void goToProgressViz() {

        Intent Viz = new Intent(this, ProgressActivity.class);
        startActivity(Viz);

        finish();
        return;
    }


    private boolean isInternetConnected() {
    ConnectivityManager cm =
            (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null &&
            activeNetwork.isConnectedOrConnecting();

    return isConnected;
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


    private void populateUserStats(JSONObject information){
        try {
            final String hours = Integer.toString(information.getInt("minutes")/60);
            final String total_time = "Playing Time: " + hours + " hours and " + Integer.toString(information.getInt("minutes") % 60) + " minutes";
            final String strokes = "Hits: " + Integer.toString(information.getInt("strokes")) + " strokes";
            final String max_rally = "Longest Rally: " + Integer.toString(information.getInt("max_rally")) + " strokes";
            myStats = "Check out my stats on @tennis_sense: over " + hours + " hours of playing #tennis, " + Integer.toString(information.getInt("strokes")) + " total strokes, and longest rally was " + Integer.toString(information.getInt("max_rally")) + " hits" ;

            runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                TextView lastplayed = (TextView)findViewById(R.id.playingtime);
                lastplayed.setText(Html.fromHtml(total_time));
                findViewById(R.id.playingtime).setVisibility(View.VISIBLE);
                TextView strokecount = (TextView)findViewById(R.id.strokecount);
                strokecount.setText(Html.fromHtml(strokes));
                findViewById(R.id.strokecount).setVisibility(View.VISIBLE);
                TextView rally = (TextView)findViewById(R.id.longestrally);
                rally.setText(Html.fromHtml(max_rally));
                findViewById(R.id.longestrally).setVisibility(View.VISIBLE);
            }
        }));

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void populateUserRanks(JSONObject information){
        try {
            final String ranking_hit = information.getString("hitrank");
            final String ranking_max_rally = information.getString("maxrallyrank");
            final String ranking_mean_rally = information.getString("meanrallyrank");

            runOnUiThread(new Thread(new Runnable() {
                @Override
                public void run() {
                    TextView hitrank = (TextView)findViewById(R.id.hitrank);
                    hitrank.setText(Html.fromHtml(ranking_hit));
                    TextView maxrallyrank = (TextView)findViewById(R.id.maxrallyrank);
                    maxrallyrank.setText(Html.fromHtml(ranking_max_rally));
                    TextView meanrallyrank = (TextView)findViewById(R.id.meanrallyrank);
                    meanrallyrank.setText(Html.fromHtml(ranking_mean_rally));
                    findViewById(R.id.hitrank).setVisibility(View.VISIBLE);
                    findViewById(R.id.maxrallyrank).setVisibility(View.VISIBLE);
                    findViewById(R.id.meanrallyrank).setVisibility(View.VISIBLE);
                }
            }));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
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
                    Log.v(UserStats.class.toString(), "posted results...");
                }

            });
            mSocket.on("userStats", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    information = new JSONObject();
                    try {
                        information.put("minutes", 0);
                        information.put("strokes", 0);
                        information.put("max_rally", 0);
                        JSONArray sessions = obj.getJSONArray("data");
                        for (int i = 0; i < sessions.length(); i++) {
                            JSONObject bucket = sessions.getJSONObject(i);
                            String sessionId = bucket.getString("session");
                            int strokesCounted = 0;
                            JSONObject agg = bucket.getJSONObject("aggregate");
                            strokesCounted += agg.getInt("Serves");
                            strokesCounted += agg.getInt("Forehands");
                            strokesCounted += agg.getInt("Backhands");
                            information.put("strokes", strokesCounted + information.getInt("strokes"));
                            Date start_date = new Date(Long.parseLong(sessionId));
                            long endTime = bucket.getLong("max_time");
                            Date end_date = new Date(endTime);
                            long diffMin = Math.round((end_date.getTime() - start_date.getTime())/60000);
                            int maximum_rally = bucket.getInt("max_rally");
                            if (maximum_rally > information.getInt("max_rally")) {
                                information.put("max_rally", maximum_rally);
                            }
                            Integer session_duration = (int) diffMin;
                            information.put("minutes", session_duration + information.getInt("minutes"));
                        }

                        populateUserStats(information);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });
            mSocket.on("userRanks", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    information = new JSONObject();
                    try {
                        if(!obj.isNull("rankdata")) {
                            JSONObject ranks = obj.getJSONObject("rankdata");
                            if (!ranks.isNull("hits")) {
                                int hitrank = ranks.getInt("hits");
                                int maxrallyrank = ranks.getInt("maxrally");
                                int meanrallyrank = ranks.getInt("meanrally");

                                String hits = "#" + Integer.toString(hitrank) + " for number of hits";
                                String maxrally = "#" + Integer.toString(maxrallyrank) + " for longest rally length";
                                String meanrally = "#" + Integer.toString(meanrallyrank) + " for average rally length";

                                information.put("hitrank", hits);
                                information.put("maxrallyrank", maxrally);
                                information.put("meanrallyrank", meanrally);


                                populateUserRanks(information);
                            }
                            else {
                                information.put("hitrank", "Get outside and play a match with Tennis Sense to get on the rankings");
                                information.put("maxrallyrank", "");
                                information.put("meanrallyrank", "");
                                populateUserRanks(information);

                            }
                        }
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

    private void populater(final String googleUserId) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("userId", googleUserId);
            mSocket.emit("userStats", userObj);
            mSocket.emit("userRanks", userObj);

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void compose(final String yoyo) {
        TweetComposer.Builder builder = new TweetComposer.
                Builder(this)
                .text(yoyo);
        builder.show();
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
        SharedPreferences preferences =
                getSharedPreferences("PROPRIO_PREFS", MODE_PRIVATE);
        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Image~" + "UserStats");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setContentView(R.layout.activity_userstats);
        googleUserId = preferences.getString("googleUserId", null);
        Resources res = getResources();
        final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
        if(!isInternetConnected() && !master) {
            goToLandingPage();
        }
        String name = preferences.getString("googleUserName","none");
        String text = name;
        TextView textView = (TextView)findViewById(R.id.username);
        textView.setText(Html.fromHtml(text));
        signedintoTwitter = preferences.getString("twitterUserName", null) != null;
        if (signedintoTwitter) {
            findViewById(R.id.tweetuserdetails).setVisibility(View.VISIBLE);
        }

        setUpSocket();
        if (isInternetConnected()) {
            populater(googleUserId);
        }
        // Set up button click listeners
        final Button checkbutton = (Button) findViewById(R.id.mainapp2);
        checkbutton.setOnClickListener(new View.OnClickListener() {
               public void onClick(View v) {
                   switch (v.getId()) {
                       case R.id.mainapp2:
                           goToMainApp();
                           break;
                   }
               }
        });

        // Set up button click listeners
//        final Button performance = (Button) findViewById(R.id.performanceovertime);
//        performance.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                switch (v.getId()) {
//                    case R.id.performanceovertime:
//                        goToProgressViz();
//                        break;
//                }
//            }
//        });

        final ImageButton tweet = (ImageButton) findViewById(R.id.tweet);
        tweet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.tweet:
                        compose(myStats);
                        break;
                }
            }
        });

        // Set icon in upper left
        getActionBar().setIcon(R.drawable.logo);

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


    /*
        When user selects menu item_new
     */
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
        // If SessionActivity, start it up.  Same assumption about signing in as above.
        else if (id == R.id.sessions) {
            Intent newIntent = new Intent(getApplicationContext(), SessionActivity.class);
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
    protected void onResume() {
        Log.v("FaqActivity","onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.v("FaqActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.v("FaqActivity","onStop");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.v("FaqActivity","onStart");
        super.onStart();
    }


}
