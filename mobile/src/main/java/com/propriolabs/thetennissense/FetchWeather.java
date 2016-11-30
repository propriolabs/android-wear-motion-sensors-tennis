package com.propriolabs.thetennissense;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.URL;

/**
 * Created by Stinson on 5/11/16.
 */
public class FetchWeather {

    private static final String OPEN_WEATHER_MAP_API =
            "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s";

    public static JSONObject getJSON(Context context, double latitude, double longitude){
        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, String.valueOf(latitude), String.valueOf(longitude)));
            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();

            connection.addRequestProperty("x-api-key",
                    context.getString(R.string.open_weather_maps_app_id));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not
            // successful
            if(data.getInt("cod") != 200){
                Log.v("Mistake", Integer.toString(data.getInt("cod")));
                return null;
            }

            return data;
        }catch(Exception e){
            Log.v("Mistake", "exception", e);
            return null;
        }
    }
}
