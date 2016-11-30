package com.propriolabs.thetennissense;


/**
 * Created by wgmueller on 2/13/16.
 */
class Session {
    String userId;
    String sessionId;
    String startstop;
    String date;
    String storage;
    int photoId;
    boolean analyzed;
    int numStrokes;
    String displayStrokes;
    String userName;
    String calories;
    String rally_count;
    String hit_per_rally;
    String meanRally;
    String weather;
    String wind;
    String temperature;
    String address;
    String match;

    Session(String userId, String sessionId, String startstop, String date, String storage,int photoId,boolean analyzed, int numStrokes, String displayStrokes, String userName, String calories, String rally_count, String hit_per_rally, String meanRally, String weather, String wind, String temperature, String address, String match) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.startstop = startstop;
        this.date = date;
        this.storage= storage;
        this.photoId = photoId;
        this.analyzed = analyzed;
        this.numStrokes = numStrokes;
        this.displayStrokes = displayStrokes;
        this.userName = userName;
        this.calories = calories;
        this.rally_count = rally_count;
        this.hit_per_rally = hit_per_rally;
        this.meanRally = meanRally;
        this.weather = weather;
        this.wind = wind;
        this.temperature = temperature;
        this.address = address;
        if (match == "true") {
            this.match = "Status: Match";
        }
        else if (match == "") {
            this.match = "";
        }
        else {
            this.match = "Status: Practice";

        }
    }
}