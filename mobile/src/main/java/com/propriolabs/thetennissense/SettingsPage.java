package com.propriolabs.thetennissense;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appinvite.AppInviteInvitation;
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
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SettingsPage extends Activity implements
        View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private TwitterLoginButton loginButton;
    private String googleUserId = null;
    private static final String TAG = "SettingsPage";
    private static final int REQUEST_INVITE = 0;
    private boolean firstTime = true;
    /* RequestCode for resolutions involving sign-in */
    private static final int RC_SIGN_IN = 1;
    /* RequestCode for resolutions to get GET_ACCOUNTS permission on M */
    private static final int RC_PERM_GET_ACCOUNTS = 2;
    private static final int Twitter_Sign_In = 140;
    /* Keys for persisting instance variables in savedInstanceState */
    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final String KEY_SHOULD_RESOLVE = "should_resolve";
    /* Client for accessing Google APIs */
    private GoogleApiClient mGoogleApiClient;
    /* View to display current status (signed-in, signed-out, disconnected, etc) */
    private TextView mStatus;
    private Menu mOptionsMenu;
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
        Manage the menu bar.  Basically if activate is true, the user is signing in and if it's
        false the user is signing out.
     */
    private void enableMenu(final boolean activate) {
        // Enable or disable all buttons but the last.
        // Last is assumed to be sign in/out so it's never disabled.
        for(int i=0; i< mOptionsMenu.size()-1; i++) {
            mOptionsMenu.getItem(i).setEnabled(activate);
        }

        // We do need to change the text of the button though depending on whether it's a sign in
        // or out action.

        // Assume sign out...
        mOptionsMenu.findItem(R.id.sign_in_out).setTitle("Sign in with Google");

        // But if it's a sign in, change the text.
        if(activate) {
            mOptionsMenu.findItem(R.id.sign_in_out).setTitle("Sign Out");
        }
    }

    /*
        Make the local proprio directory for data storage if it doesn't already exist
     */
    private void makeDir() {
        directory.mkdir();
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

    public static boolean checkMaster(String[] theArray, String targetValue) {
        for(String s: theArray){
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAllPermissions();
        final SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        googleUserId = preferences.getString("googleUserId", null);

        Resources res = getResources();
        final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
        if(!isInternetConnected() && !master) {
            goToLandingPage();
        }
        setContentView(R.layout.activity_settings);
        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
            mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE);
        }

        setUpSocket();
        getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS + "/proprio").mkdir();

        // Make the local proprio directory for data storage if it doesn't already exist
        makeDir();
        // Set up button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.mainapp).setOnClickListener(this);
        findViewById(R.id.invite_button).setOnClickListener(this);

        // Large sign-in
        ((SignInButton) findViewById(R.id.sign_in_button)).setSize(SignInButton.SIZE_WIDE);

        // Start with sign-in button disabled until sign-in either succeeds or fails
        findViewById(R.id.sign_in_button).setEnabled(false);

        // Set up view instances
        mStatus = (TextView) findViewById(R.id.status);

        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterSession session = result.data;
                Log.v(TAG, session.toString());
                Log.v(TAG, "Twitter account @" + session.getUserName() + " logged in!");
                SharedPreferences preferences =
                        getSharedPreferences("PROPRIO_PREFS", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("twitterUserName", session.getUserName());
                editor.putLong("twitterUserId", session.getUserId());
                editor.apply();
                findViewById(R.id.twitter_login_button).setVisibility(View.GONE);
            }

            @Override
            public void failure(TwitterException exception) {
                Log.d("TwitterKit", "Login with Twitter failure", exception);
            }
        });


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

        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Image~" + "Settings");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        /*
            If this is true, the user clicked the sign out menu item_new in some activity and we need
            to set the flag that we need to sign out once initialized.
        */
        if (getIntent().getBooleanExtra("SIGN_OUT", false)) {
            Log.v(TAG, "user clicked sign out");

            shouldSignOut = true;
        }

    }

    /*
        Called after onCreate but not by onCreate apparently
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Grab a reference to the menu while we can because we'll need to change it
        mOptionsMenu = menu;
        // Populate menu with defaults
        getMenuInflater().inflate(R.menu.menu_my, menu);
        // We assume the user is signed out so we disable all menu items at first.
        // Except for the last item_new, which we assume is the "Sign in with Google" button
        for(int i=0; i< menu.size()-1; i++) {
            menu.getItem(i).setEnabled(false);
        }
        return true;
    }

    /*
        When user selects menu item_new
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Stash the id of the item_new which will map up to one in the menu_my
        int id = item.getItemId();

        // If CollectData (which is the New Session Page), start it up.  We ASSUME the user is
        // signed in here because they were ABLE to click the button.
        // Button only gets enabled when user signs in.
        if (id == R.id.mainapp) {
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
        // If Sign In/Out menu item_new, we need to figure which action the user is taking.
        // One way is to check the title of the menu item_new at the time they clicked it.
        else if(id == R.id.sign_in_out) {
            if(item.getTitle().equals("Sign Out")) {
                onSignOutClicked();
            }
            else {
                onSignInClicked();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpSpinner(final Spinner spinner, final String prefType,
                              final String defaultValue) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
              SharedPreferences.Editor editor = preferences.edit();
              String val = (String)parent.getItemAtPosition(position);
              Log.v(prefType, val);
              editor.putString(prefType,val);
              editor.commit();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {

          }
        });
        SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        String spinVal = preferences.getString(prefType, defaultValue);
        spinner.setSelection(((ArrayAdapter<String>) spinner.getAdapter())
                .getPosition(spinVal));
    }

    /*
        This is the meat of the action that occurs when the user signs in or out.
        This needs to get refactored as it's too long.
     */
    private void updateUI(final boolean isSignedIn) {
        // User signed in

        final SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        preferences.edit()
                .putBoolean("settings_complete",true).apply();
        final boolean firstTime = preferences.getBoolean("settings_complete", false);

        if(isSignedIn) {
            // Turn on menu
            enableMenu(isSignedIn);
//            findViewById(R.id.google_icon).setVisibility(View.VISIBLE);
            findViewById(R.id.divider33).setVisibility(View.VISIBLE);
            findViewById(R.id.status).setVisibility(View.VISIBLE);
            findViewById(R.id.proftable).setVisibility(View.VISIBLE);
            findViewById(R.id.bench_pic).setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.status)).setVisibility(View.VISIBLE);

            StringBuffer status = new StringBuffer();

            if(firstTime) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                // If someone IS logged in...
                if (currentPerson != null) {
                    // Stash the google name and id but show the name
                    String name = currentPerson.getDisplayName();
                    String id = currentPerson.getId();
                    String currentAccount = Plus.AccountApi.getAccountName(mGoogleApiClient);

                    try{
                        JSONObject userInfo = new JSONObject();
                        userInfo.put("userId",id);
                        userInfo.put("userName",name);
                        userInfo.put("email",currentAccount);
                        mSocket.emit("new_user",userInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // If someone ISN'T logged in...
                else {
                    // If getCurrentPerson returns null there is generally some error with the
                    // configuration of the application (invalid Client ID, Plus API not enabled, etc).
                    Log.w(TAG, getString(R.string.error_null_person));
                }
            }

            if( !isInternetConnected() ) {
                findViewById(R.id.sign_in_button).setEnabled(false);
                status.append("Internet Disconnected. ");
                mOptionsMenu.getItem(5).setEnabled(false);
                ((TableLayout)findViewById(R.id.proftable)).setVisibility(View.VISIBLE);
            }
            else {
//                status.append("Internet Connected. ");
            }


            String initialHand = preferences.getString("initialHand", "Righty");
            String initialBezel = preferences.getString("initialBezel", "Away from Wrist");
            String name = preferences.getString("googleUserName","none");

            status.append(getString(R.string.signed_in_fmt, name));

            Spinner handspinner = (Spinner) findViewById(R.id.choosehand);
            ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.spinnerHand, R.layout.utils_spinner);
            handspinner.setAdapter(adapter);
            if(initialHand=="Lefty") {
                setUpSpinner(handspinner, "hand", "Lefty");
            }
            else {
                setUpSpinner(handspinner, "hand", "Righty");
            }

            Spinner bezelspinner = (Spinner) findViewById(R.id.choosebezel);
            ArrayAdapter adapter2 = ArrayAdapter.createFromResource(this, R.array.spinnerBezel, R.layout.utils_spinner);
            bezelspinner.setAdapter(adapter2);
            if(initialBezel=="Towards Wrist") {
                setUpSpinner(bezelspinner, "bezel", "Towards Wrist");
            }
            else {
                setUpSpinner(bezelspinner, "bezel", "Away from Wrist");
            }

            Spinner genderspinner = (Spinner) findViewById(R.id.choosegender);
            ArrayAdapter adapter3 = ArrayAdapter.createFromResource(this, R.array.spinnerGender, R.layout.utils_spinner);
            genderspinner.setAdapter(adapter3);
            setUpSpinner(genderspinner, "gender", "Male");

            Spinner heightspinner = (Spinner) findViewById(R.id.chooseheight);
            ArrayAdapter adapter4 = ArrayAdapter.createFromResource(this, R.array.spinnerHeight, R.layout.utils_spinner);
            heightspinner.setAdapter(adapter4);
            setUpSpinner(heightspinner, "height", "5\' 9\"");

            Spinner agespinner = (Spinner) findViewById(R.id.chooseage);
            ArrayAdapter adapter5 = ArrayAdapter.createFromResource(this, R.array.spinnerAge, R.layout.utils_spinner);
            agespinner.setAdapter(adapter5);
            setUpSpinner(agespinner, "age", "25");

            Spinner ratingspinner = (Spinner) findViewById(R.id.chooserating);
            ArrayAdapter adapter6 = ArrayAdapter.createFromResource(this, R.array.spinnerUSTA, R.layout.utils_spinner);
            ratingspinner.setAdapter(adapter6);
            setUpSpinner(ratingspinner, "USTArating", "Unknown");

            Spinner activityfeedspinner = (Spinner) findViewById(R.id.chooseprivacy);
            ArrayAdapter adapter7 = ArrayAdapter.createFromResource(this, R.array.spinnerPrivacy, R.layout.utils_spinner);
            activityfeedspinner.setAdapter(adapter7);
            setUpSpinner(activityfeedspinner, "Privacy", "No");
            Resources res = getResources();
            final Boolean master = checkMaster(res.getStringArray(R.array.masterUsers), googleUserId);
            if (master) {
                findViewById(R.id.admin).setVisibility(View.VISIBLE);
                findViewById(R.id.admin2).setVisibility(View.VISIBLE);
                Spinner labelspinner = (Spinner) findViewById(R.id.chooselabel);
                ArrayAdapter adapter8 = ArrayAdapter.createFromResource(this, R.array.spinnerLabelMode, R.layout.utils_spinner);
                labelspinner.setAdapter(adapter8);
                setUpSpinner(labelspinner, "labelmode", "No");
            }


            // Set sign in/out button visibility for a signed in user
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.goplay).setVisibility(View.VISIBLE);

            mStatus.setText(status.toString());
        }
        else {
            // Show signed-out message and clear email field
//            findViewById(R.id.google_icon).setVisibility(View.VISIBLE);
            findViewById(R.id.status).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.proftable).setVisibility(View.GONE);
            findViewById(R.id.bench_pic).setVisibility(View.VISIBLE);
            if( isInternetConnected() ) {
                enableMenu(isSignedIn);

                SharedPreferences.Editor edit = preferences.edit();
                edit.remove("googleUserId");
                edit.remove("googleUserName");
                edit.commit();

                mStatus.setText(R.string.signed_out);

                // Set button visibility
                findViewById(R.id.sign_in_button).setEnabled(true);
                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                findViewById(R.id.goplay).setVisibility(View.GONE);
            }
            else {
                findViewById(R.id.sign_in_button).setEnabled(false);
                findViewById(R.id.mainapp).setEnabled(false);
                ((TextView)findViewById(R.id.status)).
                        setText("Internet Disconnected and User " +
                                "Signed Out.\nPlease connect and sign in to use app.");
                mOptionsMenu.getItem(5).setEnabled(false);

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
        final Boolean signedintoTwitter = preferences.getString("twitterUserName", null) != null;
        if (!signedintoTwitter) {
            findViewById(R.id.twitter_login_button).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.twitter_login_button).setVisibility(View.INVISIBLE);
        }
    }

    private void setUpSocket() {
        if(mSocket == null || !mSocket.connected()) {
            Log.v(SettingsPage.class.toString(), "setting up socket.");
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
                        Log.d(SettingsPage.class.toString(), "mSocket connected");
                    }

                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(SettingsPage.class.toString(), "mSocket disconnected");
                    }

                });
                mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(SettingsPage.class.toString(), "mSocket connect error: " + args[0].toString());
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
            Log.v(SettingsPage.class.toString(),"Socket already connected.");
        }
    }

    private void showSignedInUI() {
        updateUI(true);
    }

    private void showSignedOutUI() {
//        updateUI(false);
        goToLandingPage();
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


    private void onInviteClicked() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
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
        }

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Check how many invitations were sent and log a message
                // The ids array contains the unique invitation ids for each invitation sent
                // (one for each contact select by the user). You can use these for analytics
                // as the ID will be consistent on the sending and receiving devices.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                //Log.d(TAG, getString(R.string.sent_invitations_fmt, ids.length));
            } else {
                // Sending failed or it was canceled, show failure message to the user
                Log.v(TAG, getString(R.string.send_failed));
            }
        }

        if (requestCode == Twitter_Sign_In) {
            loginButton.onActivityResult(requestCode, resultCode, data);
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
            Log.d(TAG, "Werid resolving state...");
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
            case R.id.mainapp:
                goToMainApp();
                break;
            case R.id.invite_button:
                onInviteClicked();
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
        mStatus.setText(R.string.signing_in);
    }
    // [END on_sign_in_clicked]

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

    private void goToLandingPage() {
        Intent LandingPage = new Intent(this, LandingSignIn.class);
        startActivity(LandingPage);
        finish();
        return;
    }


    private void goToMainApp() {

        Intent Collect = new Intent(this, CollectData.class);
        startActivity(Collect);

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
