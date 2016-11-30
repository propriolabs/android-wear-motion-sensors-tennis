package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class FaqActivity extends Activity {

    // class member instance of menu.  need to hold onto this because we change it
    private Menu mOptionsMenu;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("FaqActivity", "onCreate");

        checkLoggedIn();

        setContentView(R.layout.activity_faq);

        TextView textView = (TextView)findViewById(R.id.feedbackText1);
        textView.setClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());


        final SharedPreferences preferences = getSharedPreferences("PROPRIO_PREFS", 0);
        String name = preferences.getString("googleUserName","none");

        String text = "Yes, " + name + ", of course. Please feel free to reach out directly to" +
                "<a href='mailto:matt@propriolabs.com'> our CEO</a>. We hope you enjoy Tennis Sense!";
        textView.setText(Html.fromHtml(text));

        super.onCreate(savedInstanceState);
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

        return super.onOptionsItemSelected(item);
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}




