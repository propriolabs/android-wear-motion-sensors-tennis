package com.propriolabs.thetennissense;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *  This is the main Activity for the wearable.  It's purpose is to stream raw sensor data
 *  back to the mobile device.
 *
 *  The UI's features allow user notification of watch/mobile status, user selection of activity,
 *  user begin/end session, and ultimately simple feedback in the form of the classified actions
 *  within an activity.
 */

public class sensorActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private GoogleApiClient mGoogleApiClient;
    private MessageReceiver messageReceiver;
    private long session = 0L;
    private float[] acceleration = new float[3];
    private float[] rotationRate = new float[3];
    private float[] gravityData = new float[3];
    private float[] rotVectorData = new float[3];
    private float heartRateValue = 0;
    private int forehandsIndex;
    private int backhandsIndex;
    private int servesIndex;
    private DecoView decoView;
    private long lastSampleTime = 0L;
    private boolean sending = false;
    private SpeechRecognizer sr = null;
    private Intent speechIntent = null;
    private boolean fromMobile = false;
    private PutDataMapRequest putRequest = null;
    private final int SPEECH_REQUEST_CODE = 0;

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Log.v("Speech!",spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendMessage(final String message, final String data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), message, data.getBytes()).await();
                }
            }
        }).start();
    }

    private JSONObject labelFromAudio(String cand){
        JSONObject label = new JSONObject();
        try {
            switch (cand) {
                case "4":
                case "four":
                case "for":
                case "Ford":
                case "Thor":
                case "poor":
                case "door":
                    label.put("label","Forehand");
                    return label;
                case "back":
                case "Beck":
                case "Mac":
                case "fact":
                case "fat":
                case "bat":
                case "that":
                case "black":
                    label.put("label","Backhand");
                    return label;
                case "serve":
                case "server":
                case "sure":
                case "surf":
                case "search":
                case "sir:":
                case "3rd:":
                case "third:":
                case "shirt:":
                    label.put("label","Serve");
                    return label;
                default:
                    return null;
            }
        }
        catch (JSONException je)
        {
            return null;
        }
    }

    private JSONObject scoreFromAudio(String cand){
        JSONObject score = new JSONObject();
        try {
            switch (cand) {
                case "love love":
                case "Luv Luv":
                case "Luv all":
                case "love all":
                case "loveall":
                case "Lovell":
                case "level level":
                case "love off":
                    score.put("myGameScore",0);
                    score.put("oppGameScore",0);
                    score.put("type","game");
                    return score;
                case "love fifteen":
                case "love 15":
                case "love-fifteen":
                case "Love's 15":
                case "log fifteen":
                case "Lucky 13":
                case "lug 15":
                case "love bugs":
                case "love 5":
                case "lovebug":
                case "love Buzz":
                case "lovebugs":
                case "Love Bob":
                case "love bug":
                case "Club size":
                case "Club sign":
                    score.put("myGameScore",0);
                    score.put("oppGameScore",15);
                    score.put("type","game");
                    return score;
                case "fifteen love":
                case "14 love":
                case "fifteen-love":
                case "15 lug":
                case "16 love":
                case "15 laws":
                case "15 luv":
                case "5 Love":
                case "five love":
                case "V love":
                case "5 lug":
                case "Live Love":
                    score.put("myGameScore",15);
                    score.put("oppGameScore",0);
                    score.put("type","game");
                    return score;
                case "15 all":
                case "15 hour":
                case "15 Hall":
                case "1500":
                case "15 off":
                case "1515":
                case "15 15":
                case "15/15":
                case "1615":
                case "fifteen-fifteen":
                case "five all":
                case "Fireball":
                    score.put("myGameScore",15);
                    score.put("oppGameScore",15);
                    score.put("type","game");
                    return score;
                case "1530":
                case "15:30":
                case "15-30":
                case "15/30":
                case "5:30":
                case "530":
                case "five thirty":
                case "5-30":
                case "five-thirty":
                case "1630":
                    score.put("myGameScore",15);
                    score.put("oppGameScore",30);
                    score.put("type","game");
                    return score;
                case "Thirty 15":
                case "dirty 15":
                case "30 15":
                case "3015":
                case "1315":
                case "thirty-five":
                case "30 V":
                case "thirty five":
                case "35":
                case "thirty-fifteen":
                    score.put("myGameScore",30);
                    score.put("oppGameScore",15);
                    score.put("type","game");
                    return score;
                case "30 all":
                case "30 off":
                case "30 hour":
                case "30 Hall":
                case "3000":
                case "30-30":
                case "30 30":
                case "30/30":
                case "3013":
                    score.put("myGameScore",30);
                    score.put("oppGameScore",30);
                    score.put("type","game");
                    return score;
                case "34d":
                case "34t":
                case "30/40":
                case "Thirty 40":
                case "Thirty Forty":
                case "3040":
                case "30 40":
                    score.put("myGameScore", 30);
                    score.put("oppGameScore", 40);
                    score.put("type","game");
                    return score;
                case "4013":
                case "40/30":
                case "40 30":
                case "40-30":
                    score.put("myGameScore", 40);
                    score.put("oppGameScore", 30);
                    score.put("type","game");
                    return score;
                case "4015":
                case "40-15":
                case "40 15":
                case "forty-fifteen":
                case "4014":
                case "45":
                case "forty-five":
                    score.put("myGameScore", 40);
                    score.put("oppGameScore", 15);
                    score.put("type","game");
                    return score;
                case "1540":
                case "15-40":
                case "1640":
                case "1340":
                case "1514":
                case "15/40":
                case "5:40":
                case "514":
                case "542":
                    score.put("myGameScore", 15);
                    score.put("oppGameScore", 40);
                    score.put("type","game");
                    return score;
                case "love 40":
                    score.put("myGameScore", 0);
                    score.put("oppGameScore", 40);
                    score.put("type","game");
                    return score;
                case "love 30":
                    score.put("myGameScore", 0);
                    score.put("oppGameScore", 30);
                    score.put("type","game");
                    return score;
                case "40 love":
                    score.put("myGameScore", 40);
                    score.put("oppGameScore", 0);
                    score.put("type","game");
                    return score;
                case "30 love":
                    score.put("myGameScore", 30);
                    score.put("oppGameScore", 0);
                    score.put("type","game");
                    return score;
                case "deuce":
                case "Duce":
                case "Zeus":
                case "Dukes":
                case "juice":
                    score.put("myGameScore", 40);
                    score.put("oppGameScore", 40);
                    score.put("type","game");
                    return score;
                case "add in":
                case "Eden":
                case "Adam":
                case "Madden":
                case "dine-in":
                case "ad in":
                case "the Inn":
                case "the end":
                case "bad in":
                    score.put("myGameScore", 50);
                    score.put("oppGameScore", 40);
                    score.put("type","game");
                    return score;
                case "add out":
                case "ad out":
                case "a doubt":
                case "I doubt":
                case "add app":
                case "badass":
                    score.put("myGameScore", 40);
                    score.put("oppGameScore", 50);
                    score.put("type","game");
                    return score;
                case "games love love":
                case "games of love":
                case "games love la":
                case "James love love":
                case "zero zero":
                case "game zero zero":
                case "games zero zero":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "games loved one":
                case "games love 1":
                case "games love one":
                case "game loved one":
                case "game love 1":
                case "game love one":
                case "James love one":
                case "love 1":
                case "loved ones":
                case "love won":
                case "games zero one":
                case "game zero one":
                case "zero one":
                case "one one":
                case "1 1":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "games love to":
                case "games love too":
                case "games love 2":
                case "games love two":
                case "Games of Love 2":
                case "love two":
                case "Luv two":
                case "zero two":
                case "games zero two":
                case "game zero two":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games love three":
                case "games love 3":
                case "James love 3":
                case "games of three":
                case "love three":
                case "Lab 3":
                case "Lock 3":
                case "loc 3":
                case "love free":
                case "games zero three":
                case "game zero three":
                case "zero three":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "games love four":
                case "games love 4":
                case "games love for":
                case "James love for":
                case "game love four":
                case "game love 4":
                case "games before":
                case "game of 4":
                case "love for":
                case "zero four":
                case "games zero four":
                case "game zero for":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games love 5":
                case "games loved by":
                case "game love 5":
                case "James love 5":
                case "game lovebug":
                case "game love bug":
                case "games lovebug":
                case "games love bug":
                case "games website":
                case "games zero five":
                case "game zero five":
                    score.put("mySetScore", 0);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games 5 love":
                case "James V love":
                case "games by blood":
                case "games 500":
                case "game 5 Love":
                case "game 5 lug":
                case "game five love":
                case "game five zero":
                case "games five zero":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "games 4 love":
                case "names for love":
                case "games for Luv":
                case "games 4 Less":
                case "game for love":
                case "game 4 love":
                case "4 love":
                case "for love":
                case "four zero":
                case "games four zero":
                case "game four zero":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "Games 3 love":
                case "games 300":
                case "Sims 3 loves":
                case "games we love":
                case "game we love":
                case "game three love":
                case "game free love":
                case "3 love":
                case "three love":
                case "free love":
                case "three zero":
                case "games three zero":
                case "game three zero":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "Games 2 of":
                case "games 2 love":
                case "games 200":
                case "used to love":
                case "thing to love":
                case "games 2 of":
                case "game 2 love":
                case "2 love":
                case "too love":
                case "to love":
                case "two zero":
                case "games two zero":
                case "game two zero":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "James One Love":
                case "games One Love":
                case "games one loves":
                case "games with love":
                case "one love":
                case "1 Luv":
                case "1love":
                case "game 1 love":
                case "game one love":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 0);
                    score.put("type","set");
                    return score;
                case "games one one":
                case "one-one":
                case "games 1-1":
                case "James 1 1":
                case "games 101":
                case "game one1 ":
                case "game one one":
                case "game 1 1":
                case "one all":
                case "games one all":
                case "game one all":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "games one two":
                case "game 1 2":
                case "games 1 2":
                case "James 1 2":
                case "game one two":
                case "one two":
                case "1 2":
                case "1 to":
                case "one too":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games one three":
                case "games one 3":
                case "Games 1 3":
                case "games won 3":
                case "James 1:3":
                case "game one three":
                case "game won 3":
                case "game one 3":
                case "one three":
                case "1 3":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "games one four":
                case "games one for":
                case "games won for":
                case "game one for":
                case "game one four":
                case "game 1 for":
                case "game one floor":
                case "1 for":
                case "1 four":
                case "1 4":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games One V":
                case "games one five":
                case "one 5":
                case "1 5":
                case "One V":
                case "game one five":
                case "game One V":
                case "game one 5":
                    score.put("mySetScore", 1);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games five five":
                case "games by five":
                case "James by 5":
                case "games 5 5":
                case "game 5-5":
                case "game 5 V":
                case "games five all":
                case "games by ball":
                case "game five all":
                case "games by Hall":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games five four":
                case "games 5'4":
                case "games by four":
                case "Gamestop 4":
                case "game five four":
                case "game by four":
                case "five four":
                case "five for":
                case "50":
                case "size four":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games five three":
                case "games by three":
                case "games 5 3":
                case "5 3":
                case "five three":
                case "5/3":
                case "5-3":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "games five two":
                case "games by two":
                case "game five two":
                case "game 5-2":
                case "game 5 2":
                case "five two":
                case "5:2":
                case "5 to":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games five one":
                case "games 5 1":
                case "James 5:1":
                case "game five one":
                case "game 5 on":
                case "game 501":
                case "501":
                case "5-1":
                case "five-one":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "games two one":
                case "2:1":
                case "two one":
                case "2 one":
                case "to one":
                case "games to one":
                case "game who won":
                case "game two one":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "games two two":
                case "games too too":
                case "too too":
                case "2 2":
                case "James 2 2":
                case "games tutu":
                case "game two two":
                case "game 2-2":
                case "games Youtube":
                case "games two all":
                case "game two all":
                case "two all":
                case "2 all":
                case "you all":
                case "do all":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games two three":
                case "James 2 3":
                case "Games 2 3":
                case "games to 3":
                case "game two three":
                case "2 3":
                case "to 3":
                case "2-3":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "2 4":
                case "to form":
                case "games two four":
                case "games to four":
                case "games to 4":
                case "games 2 for":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games two five":
                case "games two 5":
                case "games 2 5":
                case "games to buy":
                case "game 2 5":
                case "game two five":
                case "game to buy":
                case "2 5":
                case "two five":
                case "to 5":
                    score.put("mySetScore", 2);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games four five":
                case "4 5":
                case "4-5":
                case "four-five":
                case "game four five":
                case "games for 5":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games four four":
                case "games for for":
                case "games for 4":
                case "fore all":
                case "for all":
                case "games for all":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games four three":
                case "games for 3":
                case "games for free":
                case "game four three":
                case "game for 3":
                case "four three":
                case "for 3":
                case "4:3":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "games four two":
                case "games for 2":
                case "games for two":
                case "things for two":
                case "game for two":
                case "for 2":
                case "for two":
                case "forward to":
                case "four two":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games four one":
                case "games for one":
                case "games for 1":
                case "game four one":
                case "game for one":
                case "for one":
                case "401":
                case "4-1":
                    score.put("mySetScore", 4);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "31":
                case "3-1":
                case "three one":
                case "games 3-1":
                case "games three one":
                case "game three one":
                case "game 3 1":
                case "games 3:1":
                case "James 3:1":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 1);
                    score.put("type","set");
                    return score;
                case "games three two":
                case "Games 3 2":
                case "game 3-2":
                case "game three two":
                case "three two":
                case "3-2":
                case "3 to":
                case "three to":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 2);
                    score.put("type","set");
                    return score;
                case "games three three":
                case "games three all":
                case "games free all":
                case "James 3-3":
                case "game three three":
                case "game 3-3":
                case "three all":
                case "free all":
                case "3 all":
                case "three three":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 3);
                    score.put("type","set");
                    return score;
                case "games three four":
                case "three for":
                case "3 4":
                case "3-4":
                case "Games 3 4":
                case "James 3 4":
                case "game three four":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 4);
                    score.put("type","set");
                    return score;
                case "games three five":
                case "game three five":
                case "game three V":
                case "three five":
                case "3 5":
                case "3five":
                    score.put("mySetScore", 3);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "games five six":
                case "games 5 sex":
                case "games by 6":
                case "game five six":
                case "game five sex":
                case "game by 6":
                case "five six":
                case "5 6":
                    score.put("mySetScore", 5);
                    score.put("oppSetScore", 6);
                    score.put("type","set");
                    return score;
                case "games six five":
                case "games 6-5":
                case "James 6:5":
                case "game 6 5":
                case "game six five":
                case "six five":
                case "6'5":
                case "6-5":
                    score.put("mySetScore", 6);
                    score.put("oppSetScore", 5);
                    score.put("type","set");
                    return score;
                case "sets one zero":
                case "set 1-0":
                case "sets 1 0":
                case "set 1 0":
                    score.put("myMatchScore", 1);
                    score.put("oppMatchScore", 0);
                    score.put("type","match");
                    return score;
                case "sets zero one":
                case "set 0-1":
                case "sets 0 1":
                case "set 0 1":
                case "set 01":
                    score.put("myMatchScore", 0);
                    score.put("oppMatchScore", 1);
                    score.put("type","match");
                    return score;
                case "sets zero two":
                case "set 0-2":
                case "sets 0 2":
                case "set 0 2":
                case "set 02":
                case "702":
                    score.put("myMatchScore", 0);
                    score.put("oppMatchScore", 2);
                    score.put("type","match");
                    return score;
                case "sets two zero":
                case "set 2-0":
                case "sets 2 0":
                case "set 20":
                case "set 2 -":
                case "set to 0":
                case "sets to 0":
                    score.put("myMatchScore", 2);
                    score.put("oppMatchScore", 0);
                    score.put("type","match");
                    return score;
                case "sets 1 1":
                case "set 1-1":
                case "set 1 1":
                case "sets 1-1":
                case "sets 1 all":
                case "sets one all":
                    score.put("myMatchScore", 1);
                    score.put("oppMatchScore", 1);
                    score.put("type","match");
                    return score;
                case "sets 2 1":
                case "set 2-1":
                case "set 2 1":
                case "sets 2-1":
                    score.put("myMatchScore", 2);
                    score.put("oppMatchScore", 1);
                    score.put("type","match");
                    return score;
                case "sets 1 2":
                case "set 1-2":
                case "set 1 2":
                case "sets 1-2":
                    score.put("myMatchScore", 1);
                    score.put("oppMatchScore", 2);
                    score.put("type","match");
                    return score;
                case "sets 2 2":
                case "set 2-2":
                case "set 2 2":
                case "sets 2-2":
                case "sets 2 all":
                case "sets two all":
                case "set to all":
                case "sets to all":
                    score.put("myMatchScore", 2);
                    score.put("oppMatchScore", 2);
                    score.put("type","match");
                    return score;
                default:
                    return null;
            }
        }
        catch (JSONException je)
        {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("proprio-wear-activity", "onCreate");

        putRequest = PutDataMapRequest.create("/WEAR2PHONE");
        putRequest.setUrgent();

        fromMobile = getIntent().getBooleanExtra("FROM_MOBILE", false);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        final String TAG = "speech";

        if(getResources().getBoolean(R.bool.score_with_audio) ||
                getResources().getBoolean(R.bool.label_with_audio)) {

            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.propriolabs.thetennissense");

            speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "onReadyForSpeech");
                }

                public void onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech");
                }

                public void onRmsChanged(float rmsdB) {
                    Log.d(TAG, "onRmsChanged");
                }

                public void onBufferReceived(byte[] buffer) {
                    Log.d(TAG, "onBufferReceived");
                }

                public void onEndOfSpeech() {
                    Log.d(TAG, "onEndofSpeech");
                }

                public void onError(int error) {
                    Log.d(TAG, "error " + error + sending);
                    if (sending) {
                        sr.startListening(speechIntent);
                    } else {
                        sr.stopListening();
                    }
                }

                public void onResults(Bundle results) {
                    ArrayList data = results.getStringArrayList(SpeechRecognizer.
                            RESULTS_RECOGNITION);

                    String displayedScore = "";

                    for (int i = 0; i < data.size(); i++) {
                        Log.d(TAG, "result " + data.get(i));
                        if(getResources().getBoolean(R.bool.score_with_audio)) {
                            final JSONObject scoreJSON = scoreFromAudio((String) data.get(i));
                            if (scoreJSON != null) {
                                try {
                                    Log.v(TAG, "result " + scoreJSON.getString("type"));
                                    if (scoreJSON.getString("type").equals("game")) {
                                        if (String.valueOf(scoreJSON.getInt("myGameScore")).equals("0")) {
                                            displayedScore = "Love-".concat(String.valueOf(scoreJSON.getInt("oppGameScore")));
                                        }
                                        else if (String.valueOf(scoreJSON.getInt("oppGameScore")).equals("0")) {
                                            displayedScore = String.valueOf(scoreJSON.getInt("myGameScore")).concat("-Love");
                                        }
                                        else {
                                            displayedScore = String.valueOf(scoreJSON.getInt("myGameScore")).concat("-").concat(String.valueOf(scoreJSON.getInt("oppGameScore")));
                                        }
                                    }
                                    if (scoreJSON.getString("type").equals("set")) {
                                        displayedScore = String.valueOf(scoreJSON.getInt("mySetScore")).concat("-").concat(String.valueOf(scoreJSON.getInt("oppSetScore")));
                                    }
                                    if (displayedScore.equals("40-40")) {
                                        displayedScore = "Deuce";
                                    }
                                    if (displayedScore.equals("40-50")) {
                                        displayedScore = "Add out";
                                    }
                                    if (displayedScore.equals("50-40")) {
                                        displayedScore = "Add in";
                                    }
                                    sendMessage("/SCORE", scoreJSON.toString());
                                    break;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(getResources().getBoolean(R.bool.label_with_audio)) {
                            final JSONObject labelJSON = labelFromAudio((String) data.get(i));
                            if (labelJSON != null) {
                                    sendMessage("/LABEL", labelJSON.toString());
                                    break;
                            }
                        }
                    }

                    TextView score = (TextView) findViewById(R.id.score);
                    score.setText(displayedScore);

                    if (sending) {
                        sr.startListening(speechIntent);
                    } else {
                        sr.stopListening();
                    }
                }

                public void onPartialResults(Bundle partialResults) {
                    Log.d(TAG, "onPartialResults");
                }

                public void onEvent(int eventType, Bundle params) {
                    Log.d(TAG, "onEvent " + eventType);
                }
            });
        }

        // Start the activity, the intent will be populated with the speech text

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor rotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor heartrate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if(accelerometer == null ){
            Log.v("SENSOR","NO accelerometer");
        }
        if(gravity == null ){
            Log.v("SENSOR","NO gravity");
        }
        if(rotVector == null ){
            Log.v("SENSOR","NO rotVector");
        }
        if(gyroscope == null ) {
            Log.v("SENSOR", "NO gyroscope");
        }
        if(heartrate == null ) {
            Log.v("SENSOR", "NO heart rate");
        }

        setContentView(R.layout.sensor_activity);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.v("proprio-wear-activity", "Connection established");

                        decoView = (DecoView) findViewById(R.id.dynamicArcView);

                        decoView.configureAngles(360, 0);

                        //Create data series track
                        SeriesItem serves = new SeriesItem.Builder(Color.parseColor("#00abff"))
                                .setRange(0, 50, 50)
                                .setInset(new PointF(56f, 56f))
                                .setLineWidth(8f)
                                .build();

                        servesIndex = decoView.addSeries(serves);

                        //Create data series track
                        SeriesItem backhands = new SeriesItem.Builder(Color.parseColor("#ffffff"))
                                .setRange(0, 100, 100)
                                .setInset(new PointF(44f, 44f))
                                .setLineWidth(8f)
                                .build();

                        backhandsIndex = decoView.addSeries(backhands);

                        //Create data series track
                        SeriesItem forehands = new SeriesItem.Builder(Color.parseColor("#e0162b"))
                                .setRange(0, 100, 100)
                                .setInset(new PointF(32f, 32f))
                                .setLineWidth(8f)
                                .build();

                        forehandsIndex = decoView.addSeries(forehands);


                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        setSending(true, false);

                        if (!fromMobile) {
                            sendMessage("/PING", "START_STOP");
                        }
                        else {
                            sendMessage("/WATCH_UP", "START_STOP");
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.v("proprio-wear-activity", "Connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.v("proprio-wear-activity", "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new MessageReceiver(this);

        LocalBroadcastManager.getInstance(getApplicationContext()).
                registerReceiver(messageReceiver, messageFilter);


        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, rotVector, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, heartrate, SensorManager.SENSOR_DELAY_NORMAL);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    protected void onDestroy() {
        Log.v("proprio-wear-activity", "onDestroy");

        sendMessage("/DESTROYED", "START_STOP");

        mSensorManager.unregisterListener(this);

        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.v("proprio-wear-activity", "onStop");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.v("proprio-wear-activity", "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.v("proprio-wear-activity", "onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.v("proprio-wear-activity", "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v("proprio-wear-activity", "onPause");
        super.onPause();
    }

    private void setSending(final boolean internet, final boolean shouldSend) {

        if(getResources().getBoolean(R.bool.score_with_audio) ||
                getResources().getBoolean(R.bool.label_with_audio)) {
            if (shouldSend) {
                sr.startListening(speechIntent);
            } else {
                sr.stopListening();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String BG_STRING = "Connect Mobile";
                int BG_COLOR =
                        ContextCompat.getColor(getApplicationContext(), R.color.background_off);

                boolean isConnected = isConnected();
                if(internet) {
                    Log.v("proprio-wear-activity", "setSending - internet!");
                    if(isConnected) {
                        Log.v("proprio-wear-activity", "setSending - mobile connected!");
                        if(!shouldSend) {
                            BG_STRING = "Ready!";
                            BG_COLOR =
                                    ContextCompat.getColor(
                                            getApplicationContext(), R.color.background_off);

                        }
                        else {
                            BG_STRING = "Recording";
                            BG_COLOR = ContextCompat.getColor(getApplicationContext(),
                                    R.color.background_on);
                        }
                    }
                }
                else {
                    Log.v("proprio-wear-activity", "setSending - no internet!");
                    BG_STRING = "No Internet";
                }
                sending = shouldSend;
                final String BGS = BG_STRING;
                final int BGC = BG_COLOR;
                runOnUiThread(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TextView actionStatus = (TextView) findViewById(R.id.actionStatus);
                        actionStatus.setText(BGS);
                        findViewById(R.id.main).setBackgroundColor(BGC);

                        Chronometer chronometer = (Chronometer)findViewById(R.id.chronometer);

                        if(shouldSend) {
                            ((TextView) findViewById(R.id.numForehandsWatch)).setTextColor(getResources().getColor(R.color.forehands));
                            ((TextView) findViewById(R.id.numBackhandsWatch)).setTextColor(getResources().getColor(R.color.backhands));
                            ((TextView) findViewById(R.id.numServesWatch)).setTextColor(getResources().getColor(R.color.serves));
                            ((TextView) findViewById(R.id.chronometer)).setTextColor(getResources().getColor(R.color.text));
                            ((TextView) findViewById(R.id.digitalClock)).setTextColor(getResources().getColor(R.color.text));
                            ((TextView) findViewById(R.id.score)).setText("Recording");
                            ((TextView) findViewById(R.id.score)).setTextColor(getResources().getColor(R.color.white));

                            decoView.addEvent(new DecoEvent.Builder(0).
                                    setIndex(backhandsIndex).build());

                            decoView.addEvent(new DecoEvent.Builder(0).
                                    setIndex(servesIndex).build());

                            decoView.addEvent(new DecoEvent.Builder(0).
                                    setIndex(forehandsIndex).build());

                            ((TextView)findViewById(R.id.numBackhandsWatch)).
                                    setText("0 Backhands");

                            ((TextView)findViewById(R.id.numServesWatch)).
                                    setText("0 Serves");

                            ((TextView)findViewById(R.id.numForehandsWatch)).
                                    setText("0 Forehands");

                            chronometer.setBase(SystemClock.elapsedRealtime());
                            chronometer.start();
                        } else {
                            chronometer.stop();
                            ((TextView) findViewById(R.id.numBackhandsWatch)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.numForehandsWatch)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.numServesWatch)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.chronometer)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.digitalClock)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.score)).setTextColor(Color.BLACK);
                            ((TextView) findViewById(R.id.score)).setText("Not Recording");
                        }
                    }
                }));
            }
        }).start();

    }

    private boolean isConnected() {
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        int numNodes = nodes.getNodes().size();

        return numNodes >= 1;
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (sending) {

            if (lastSampleTime == 0L) {
                lastSampleTime = System.currentTimeMillis();
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                acceleration[0] = sensorEvent.values[0];
                acceleration[1] = sensorEvent.values[1];
                acceleration[2] = sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                heartRateValue = sensorEvent.values[0];
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                rotationRate[0] = sensorEvent.values[0];
                rotationRate[1] = sensorEvent.values[1];
                rotationRate[2] = sensorEvent.values[2];
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravityData[0] = sensorEvent.values[0];
                gravityData[1] = sensorEvent.values[1];
                gravityData[2] = sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                rotVectorData[0] = sensorEvent.values[0];
                rotVectorData[1] = sensorEvent.values[1];
                rotVectorData[2] = sensorEvent.values[2];
            }

            if (lastSampleTime == 0L || lastSampleTime + 50 < System.currentTimeMillis()) {
                final JSONObject map = new JSONObject();
                try {
                    map.put("x", acceleration[0]);
                    map.put("y", acceleration[1]);
                    map.put("z", acceleration[2]);
                    map.put("g1", rotationRate[0]);
                    map.put("g2", rotationRate[1]);
                    map.put("g3", rotationRate[2]);
                    map.put("heartrate",heartRateValue);
                    map.put("grav1", gravityData[0]);
                    map.put("grav2", gravityData[1]);
                    map.put("grav3", gravityData[2]);
                    map.put("r1", rotVectorData[0]);
                    map.put("r2", rotVectorData[1]);
                    map.put("r3", rotVectorData[2]);
                    map.put("manufacturer", Build.MANUFACTURER);
                    map.put("model", Build.MODEL);
                    map.put("product", Build.PRODUCT);
                    map.put("accRange", accelerometer.getMaximumRange());
                    map.put("watchtime", System.currentTimeMillis());
                    map.put("sessionStartTime", session);
                    lastSampleTime = System.currentTimeMillis();

                    sendMessage("/DATA",map.toString());
                    //Wearable.DataApi.putDataItem(mGoogleApiClient, pdmr.asPutDataRequest());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        else{
            Log.v("proprio-wear-activity","Not Sending Data");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static class MessageReceiver extends BroadcastReceiver {

        private final WeakReference<sensorActivity> activity;

        public MessageReceiver(sensorActivity act){
            this.activity = new WeakReference<>(act);
        }
        @Override
        public void onReceive(Context context, final Intent intent) {
            Log.v("proprio-wear-activity","broadcast received:");

            if (intent.getLongExtra("START_STOP",0L) != 0L ) {
                Log.v("proprio-wear-activity","sending data:" + !activity.get().sending);
                activity.get().session = intent.getLongExtra("START_STOP",0L);
                activity.get().setSending(true, !activity.get().sending);
            }
            else if (intent.getStringExtra("/NO_INTERNET") != null ) {
                Log.v("proprio-wear-activity","No Internet...");
                activity.get().setSending(false, false);
            }
            else if (intent.getStringExtra("forehand") != null) {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        activity.get().decoView.addEvent(new DecoEvent.Builder(
                                Integer.parseInt(intent.getStringExtra("backhand"))).
                                setIndex(activity.get().backhandsIndex).build());

                        activity.get().decoView.addEvent(new DecoEvent.Builder(
                                Integer.parseInt(intent.getStringExtra("serve"))).
                                setIndex(activity.get().servesIndex).build());

                        activity.get().decoView.addEvent(new DecoEvent.Builder(
                                Integer.parseInt(intent.getStringExtra("forehand"))).
                                setIndex(activity.get().forehandsIndex).build());

                        ((TextView)activity.get().findViewById(R.id.numBackhandsWatch)).
                                setText(intent.getStringExtra("backhand") + " Backhands");

                        ((TextView)activity.get().findViewById(R.id.numServesWatch)).
                                setText(intent.getStringExtra("serve") + " Serves");

                        ((TextView)activity.get().findViewById(R.id.numForehandsWatch)).
                                setText(intent.getStringExtra("forehand") + " Forehands");
                    }
                });
            }
        }
    }

}
