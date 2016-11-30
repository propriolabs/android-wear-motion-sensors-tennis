package com.propriolabs.thetennissense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;


import com.gc.materialdesign.views.ButtonFlat;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ogaclejapan.smarttablayout.SmartTabLayout;


public class OnboardingActivity extends FragmentActivity {

    private ViewPager pager;
    private SmartTabLayout indicator;
    private ButtonFlat skip;
    private ButtonFlat next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding);

        GoogleAnalyticsTracker application = (GoogleAnalyticsTracker) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Image~" + "Onboarding");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        pager = (ViewPager) findViewById(R.id.pager);
        indicator = (SmartTabLayout) findViewById(R.id.indicator);
        skip = (ButtonFlat) findViewById(R.id.skip);
        next = (ButtonFlat) findViewById(R.id.next);

        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new OnboardingFragment1();
                    case 1:
                        return new OnboardingFragment2();
                    case 2:
                        return new OnboardingFragment3();
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };

        pager.setAdapter(adapter);
        indicator.setViewPager(pager);

        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    skip.setVisibility(View.GONE);
                    next.setText("Done");
                } else {
                    skip.setVisibility(View.GONE);
                    next.setText("Next");
                }
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pager.getCurrentItem() == 2) { // The last screen
                    finishOnboarding();
                } else {
                    pager.setCurrentItem(
                            pager.getCurrentItem() + 1,
                            true
                    );
                }

            }
        });
    }

    private void finishOnboarding() {
        // Get the shared preferences
        SharedPreferences preferences =
                getSharedPreferences("PROPRIO_PREFS", MODE_PRIVATE);

        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete",true).apply();
//        Send Hand and Bezel to Shared to pop up on profile page
        SharedPreferences.Editor editor = preferences.edit();

        boolean righthanded = ((RadioButton) findViewById(R.id.right)).isChecked();
        boolean away = true;
        Log.v("Righty?", Boolean.toString(righthanded));
        Log.v("Direction Away?", Boolean.toString(away));

        String hand = "Lefty";
        String bezel = "Towards Wrist";

        if(righthanded){
            hand = "Righty";
        }
        if(away){
            bezel = "Away from Wrist";
        }
        Log.v("HAND", Boolean.toString(righthanded));
        Log.v("BEZEL", Boolean.toString(away));
        editor.putString("initialHand", hand);
        editor.putString("initialBezel", bezel);
        editor.commit();
        // Launch the main Activity, called LandingSignIn
        Intent main = new Intent(this, LandingSignIn.class);
        startActivity(main);

        // Close the OnboardingActivity
        finish();
    }

}