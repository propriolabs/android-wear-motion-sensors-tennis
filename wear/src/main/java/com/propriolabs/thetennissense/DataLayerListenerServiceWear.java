package com.propriolabs.thetennissense;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class DataLayerListenerServiceWear extends WearableListenerService {

    final private String WEARABLE_START_PATH = "/PHONE2WEAR";
    final private String STATS_UPDATE = "/STATS_UPDATE";
    final private String START_OR_STOP_LOGGING = "START_STOP";

    @Override
    public void onCreate() {
        Log.v("proprio-wear-service", "onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.v("proprio-wear-service", "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v("proprio-wear-service", "onDataChanged");
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for(DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri != null ? uri.getPath() : null;

            Log.v("proprio-wear-service", path);

            if (STATS_UPDATE.equals(path)) {
                final DataMap map =
                        DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                Intent messageIntent = new Intent();
                messageIntent.setAction(Intent.ACTION_SEND);
                messageIntent.putExtra("forehand", map.getString("forehand", "0"));
                messageIntent.putExtra("backhand", map.getString("backhand", "0"));
                messageIntent.putExtra("serve", map.getString("serve", "0"));
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(messageIntent);
            }
        }
        super.onDataChanged(dataEvents);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String messagePath = messageEvent.getPath();
        final String messageData = new String(messageEvent.getData());

        Log.v("proprio-wear-service",messagePath);

        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);

        // Mobile saying start/stop session
        if(messagePath.equalsIgnoreCase(WEARABLE_START_PATH)) {
            // Time to change state of sending data.
            final long l = Long.parseLong(messageData);
            messageIntent.putExtra(START_OR_STOP_LOGGING, l);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
        }
        // Mobile MyActivityPhone saying it's ready to rock
        else if(messagePath.equalsIgnoreCase("/PING")) {
            Log.v("proprio-wear-service","supposed to start app");
            Intent startIntent = new Intent(this, sensorActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("FROM_MOBILE", true);
            startActivity(startIntent);
            Log.v("proprio-wear-service", "Ready to Rock");
        }
        else if(messagePath.equals("/NO_INTERNET")) {
            Log.v("proprio-wear-service","No Internet");
            messageIntent.putExtra("/NO_INTERNET", messageData);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
        }
        super.onMessageReceived(messageEvent);
    }
}