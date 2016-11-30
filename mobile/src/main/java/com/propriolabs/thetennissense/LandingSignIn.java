package com.propriolabs.thetennissense;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
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

import io.fabric.sdk.android.Fabric;


/**
 * Created by Stinson on 4/28/16.
 */
public class LandingSignIn extends Activity implements
        View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "LandingSignin";
    private String TWITTER_KEY = "30Pnb3EHdKij9G4OnPdG5xUZg";
    private String TWITTER_SECRET = "9b8jeqi4lFHQaPRccWovKQvcblO8dFmA19EJETmtahy1u9MTIN";

    private static boolean serverDown = false;
    /* RequestCode for resolutions involving sign-in */
    private static final int RC_SIGN_IN = 1;
    /* RequestCode for resolutions to get GET_ACCOUNTS permission on M */
    private static final int RC_PERM_GET_ACCOUNTS = 2;
    /* Keys for persisting instance variables in savedInstanceState */
    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final String KEY_SHOULD_RESOLVE = "should_resolve";
    /* Client for accessing Google APIs */
    private GoogleApiClient mGoogleApiClient;
    private String googleUserId;
    /* View to display current status (signed-in, signed-out, disconnected, etc) */
    private TextView mStatus;
    private Socket mSocket;
    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;
    private final int ACCOUNT_PERM = 1;
    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;
    // [END resolution_variables]
    private boolean shouldSignOut = false;
    private final static File directory =
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS + R.string.proprio_data_directory);
    /*
        Make the local proprio directory for data storage if it doesn't already exist
     */
    private void makeDir() {
        directory.mkdir();
    }
    private boolean PackageExists(String targetPackage){
        List<ApplicationInfo> packages;
        PackageManager pm;

        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }
    private void checkAllPermissions(){
        String[] perms = new String[]{
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        for(String perm: perms){
            int permissionCheck = ContextCompat.checkSelfPermission(this, perm);

            if(permissionCheck == PackageManager.PERMISSION_GRANTED){
                Log.v("Permission Check", perm + ":SUCCESS");
            }
            else {
                Log.v("Permission Check",perm + ":FAIL");

                ActivityCompat.requestPermissions(this,
                        new String[]{perm},
                        ACCOUNT_PERM);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpSocket();
        setContentView(R.layout.landing);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        checkAllPermissions();
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        SharedPreferences preferences =
                getSharedPreferences("PROPRIO_PREFS", MODE_PRIVATE);

        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Image~" + "Landing");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Crashlytics(), new Twitter(authConfig));
        Fabric.with(this, new TwitterCore(authConfig), new TweetComposer());

        if(!preferences.getBoolean("onboarding_complete",false)){
            Intent onboarding = new Intent(this, OnboardingActivity.class);
            startActivity(onboarding);
            finish();
            return;
        }

        if(!PackageExists("com.google.android.wearable.app")) {
            Log.v("Android Wear", "True");
            Intent NoAndroidWear = new Intent(this, NoAndroidWear.class);
            startActivity(NoAndroidWear);
            finish();
            return;
        }

        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
            mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE);
        }

        getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS + "/proprio").mkdir();

        makeDir();

        // Set up button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Large sign-in
        ((SignInButton) findViewById(R.id.sign_in_button)).setSize(SignInButton.SIZE_WIDE);

        // Start with sign-in button disabled until sign-in either succeeds or fails
        findViewById(R.id.sign_in_button).setEnabled(false);
        ((TextView)findViewById(R.id.status)).
                setText("Welcome to Tennis Sense\nSign in with Google to get started");
        // Set up view instances
        mStatus = (TextView) findViewById(R.id.status);


        // Build GoogleApiClient with access to basic profile
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope(Scopes.EMAIL))
                .addApi(AppIndex.API).build();

        // Set icon in upper left
        getActionBar().setIcon(R.drawable.logo);

        /*
            If this is true, the user clicked the sign out menu item_new in some activity and we need
            to set the flag that we need to sign out once initialized.
        */
        if (getIntent().getBooleanExtra("SIGN_OUT", false)) {
            Log.v(TAG, "user clicked sign out");

            shouldSignOut = true;
        }

    }

    private void appCantWork(final String message) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.sign_in_button).setVisibility(View.INVISIBLE);
                        findViewById(R.id.status).setVisibility(View.VISIBLE);
                        findViewById(R.id.bench_pic).setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.status)).
                                setText(message);
                        findViewById(R.id.status).setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    public static boolean checkMaster(String[] theArray, String targetValue) {
        for(String s: theArray){
            Log.v("master_googleUserId", s);
            Log.v("current_googleUserId", targetValue);
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    private void updateUI(final boolean isSignedIn) {

        final SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);

        if(isSignedIn) {

            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (currentPerson != null) {
                String name = currentPerson.getDisplayName();
                String id = currentPerson.getId();
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString("googleUserName", name);
                editor.putString("googleUserId", id);
                editor.commit();
                googleUserId = id;
            }

            Resources res = getResources();
            final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
            if( isInternetConnected() || master) {
                if (serverDown){
                    appCantWork("Sorry, we are experiencing technical difficulties.  Please try again or come back" +
                            " in a couple hours.");
                }
                else if (!preferences.getBoolean("settings_complete", false)) {
                    goToSettingsApp();
                } else {
                    goToUserStats();
                }
            }
            else {
                String message = "Internet Disconnected.\n" +
                        "Please connect to the Internet to use Tennis Sense.";
                appCantWork(message);
            }
        }
        else {
            // Show signed-out message and clear email field
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.status).setVisibility(View.VISIBLE);
            findViewById(R.id.bench_pic).setVisibility(View.VISIBLE);




            if( isInternetConnected() ) {
                findViewById(R.id.sign_in_button).setEnabled(true);
                if (serverDown){
                    appCantWork("Sorry, we are experiencing technical difficulties.  Please try again or come back" +
                            " in a couple hours.");
                }
            }
            else {
                findViewById(R.id.sign_in_button).setEnabled(false);

                ((TextView)findViewById(R.id.status)).
                        setText("Internet Disconnected.\nPlease connect to the Internet and sign in to use Tennis Sense.");
                findViewById(R.id.status).setVisibility(View.VISIBLE);

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
            }
        }
    }

    /**
     * Check if we have the GET_ACCOUNTS permission and request it if we do not.
     * @return true if we have the permission, false if we do not.
     */

    private void setUpSocket() {
        if(mSocket == null || !mSocket.connected()) {
            Log.v(TAG, "setting up socket.");
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
                        Log.d(TAG, "mSocket connected");
                    }

                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, "mSocket disconnected");
                    }

                });
                mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, "mSocket connect error: " + args[0].toString());
                        serverDown = true;

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
            Log.v(TAG,"Socket already connected.");
        }
    }

    private void showSignedInUI() {
        updateUI(true);
    }

    private void showSignedOutUI() {
        updateUI(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "SettingsPage Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.propriolabs.thetennissense/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "SettingsPage Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.propriolabs.thetennissense/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RESOLVING, mIsResolving);
        outState.putBoolean(KEY_SHOULD_RESOLVE, mShouldResolve);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
            Log.v(TAG, "Signing in...");
        }

    }

    // [END on_activity_result]

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult:" + requestCode);

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            Intent onboarding = new Intent(this, NoAndroidWear.class);
            startActivity(onboarding);

            finish();
            return;
        }
        return;
    }

    // [START on_connected]
    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Log.d(TAG, "onConnected:" + bundle);
        mShouldResolve = false;

        if(shouldSignOut){
            Log.d(TAG, "onConnected:" + "signed out...weird");
            onSignOutClicked();
            shouldSignOut = false;
            return;
        }
        else {
            Log.d(TAG, "onConnected:" + "signed in");
            showSignedInUI();
        }
    }
    // [END on_connected]

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost. The GoogleApiClient will automatically
        // attempt to re-connect. Any UI elements that depend on connection to Google APIs should
        // be hidden or disabled until onConnected is called again.
        Log.w(TAG, "onConnectionSuspended:" + i);
    }

    // [START on_connection_failed]
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    Log.d(TAG, "resolving...");
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                Log.d(TAG, "no resolution");
                showErrorDialog(connectionResult);
            }
        } else {
            Log.d(TAG, "It's signed out");
            showSignedOutUI();
        }
    }
    // [END on_connection_failed]

    private void showErrorDialog(ConnectionResult connectionResult) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, RC_SIGN_IN,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mShouldResolve = false;
                                showSignedOutUI();
                            }
                        }).show();
            } else {
                Log.w(TAG, "Google Play Services Error:" + connectionResult);
                String errorString = apiAvailability.getErrorString(resultCode);
                Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();

                mShouldResolve = false;
                showSignedOutUI();
            }
        }
    }



    // [START on_click]
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInClicked();
                break;
        }
    }
    // [END on_click]

    // [START on_sign_in_clicked]
    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();
        shouldSignOut = false;
        // Show a message to the user that we are signing in.
        findViewById(R.id.status).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mStatus.setText(R.string.signing_in);
    }
    // [END on_sign_in_clicked]

    // [START on_sign_out_clicked]
    private void onSignOutClicked() {
        // Clear the default account so that GoogleApiClient will not automatically
        // connect in the future.
        if (isInternetConnected() && mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        else if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        showSignedOutUI();
    }

    // [END on_sign_out_clicked]

    private void goToSettingsApp() {
        Intent Settings = new Intent(this, SettingsPage.class);
        startActivity(Settings);
        finish();
        return;
    }

    private void goToUserStats() {
        Intent UserStats = new Intent(this, com.propriolabs.thetennissense.UserStats.class);
        startActivity(UserStats);
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
}
