package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by wgmueller on 2/7/16.
 */
public class AnalysisActivity extends Activity {

    private String sessionId;

    private Menu mOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v("AnalysisActivity", "onCreate");
        Intent intent = getIntent();
        sessionId = intent.getStringExtra("session");
        //Toast.makeText(getApplicationContext(), "date = " + sessionId, Toast.LENGTH_SHORT).show();
        setContentView(R.layout.session_main);
    }

    @Override
    protected void onDestroy(){
        Log.v("AnalysisActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_my, menu);
        menu.findItem(R.id.sign_in_out).setTitle("Sign Out");
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
            Intent newIntent = new Intent(getApplicationContext(), SettingsPage.class);
            newIntent.putExtra("SIGN_OUT",true);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.sessions) {
            Intent newIntent = new Intent(getApplicationContext(), SessionActivity.class);
            startActivity(newIntent);
            return true;
        }
        else if (id == R.id.mainapp) {
            Intent newIntent = new Intent(getApplicationContext(), CollectData.class);
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

    public String getSessionId() {

        return sessionId;
    }

}