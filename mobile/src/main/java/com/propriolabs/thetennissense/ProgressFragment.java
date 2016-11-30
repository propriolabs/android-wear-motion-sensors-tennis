package com.propriolabs.thetennissense;

/**
 * Created by Stinson on 5/10/16.
 */

        import java.net.URISyntaxException;

        import android.app.Activity;
        import android.app.Fragment;
        import android.content.Context;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.view.ViewStub;
        import android.webkit.WebChromeClient;
        import android.webkit.WebSettings;
        import android.webkit.WebView;
        import android.webkit.WebViewClient;

        import com.github.nkzawa.emitter.Emitter;
        import com.github.nkzawa.socketio.client.IO;
        import com.github.nkzawa.socketio.client.Socket;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

public class ProgressFragment extends Fragment
{
    // save a reference so custom methods
    // can access views
    private View topLevelView;

    private WebView webview;
    private String output;
    private View stub;
    private String data;

    private Activity activity;

    private Socket mSocket;

    private void setUpSocket() {
        try {
            mSocket = IO.socket(getString(R.string.data_server));

            mSocket.on("userStats", new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        JSONArray sessions = obj.getJSONArray("data");
                        JSONArray thedata = new JSONArray();

                        for (int i = 0; i < sessions.length(); i++) {
                            JSONObject bucket = sessions.getJSONObject(i);
                            JSONObject agg = bucket.getJSONObject("aggregate");
                            long time = Long.parseLong(bucket.getString("session"));
                            int forehands = agg.getInt("Forehands");
                            int backhands = agg.getInt("Backhands");
                            int serves = agg.getInt("Serves");
                            JSONArray myArray = new JSONArray();
                            myArray.put(time);
                            myArray.put(forehands);
                            myArray.put(backhands);
                            myArray.put(serves);
                            thedata.put(myArray);
                        }
                        data = thedata.toString();
                        Log.v("ProgressFrament", data);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initPieChart();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            mSocket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container,
                savedInstanceState);

        Log.v("ProgressFragment", "onCreateView");

        final SharedPreferences preferences = this.getActivity().getSharedPreferences("PROPRIO_PREFS", Context.MODE_PRIVATE);

        final String userID = preferences.getString("googleUserId", "none");
        Log.v("ProgressFragment: userId", userID);
        activity = getActivity();


        if( mSocket == null || !mSocket.connected()) {
            setUpSocket();
        }



        boolean attachToRoot = false;
        topLevelView = inflater.inflate(
                R.layout.fragment_activity,
                container,
                attachToRoot);

        // call now or after some condition is met
//        initPieChart();


        mSocket.emit("userStats", userID);
        return topLevelView;
    }

    @Override
    public void onDestroy() {
        Log.v("ActivityFragment","onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.v("ActivityFragment","onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v("ActivityFragment", "onCreate");
        super.onCreate(savedInstanceState);
    }

    // initialize the WebView and the pie chart
    public void initPieChart()
    {
        View stub = topLevelView.findViewById(
                R.id.pie_chart_stub);

        if (stub instanceof ViewStub)
        {
            ((ViewStub)stub).setVisibility(View.VISIBLE);

            webview = (WebView)topLevelView.findViewById(
                    R.id.pie_chart_webview);

            WebSettings webSettings =
                    webview.getSettings();

            webSettings.setJavaScriptEnabled(true);

            webview.setWebChromeClient(
                    new WebChromeClient());

//            webview.setWebViewClient(new WebViewClient()
//            {
//                @Override
//                public void onPageFinished(
//                        WebView view,
//                        String url)
//                {
//                    // after the HTML page loads,
//                    // load the pie chart
//                    loadViz(data);
//                }
//            });

            // note the mapping
            // from  file:///android_asset
            // to PieChartExample/assets
            webview.loadUrl("file:///android_asset/" +
                    "html/test.html");
        }
    }

    public void loadViz(String input)
    {

//    The format here is [[session, forehands, backhands, serves] , ...]
        String text = "[[1398709800000,780,136, 403], [1398450600000,812,220, 500],[1399401000000,784,154, 200],[1399228200000,786,135, 200],[1399573800000,802,131, 200],[1399487400000,773,166, 200],[1399314600000,787,146, 200],[1399919400000,1496,309, 200],[1399833000000,767,138, 200],[1399746600000,797,141, 200],[1399660200000,796,146, 812],[1398623400000,779,143, 200],[1399055400000,794,140, 200],[1398969000000,791,140, 200],[1398882600000,825,107, 200], [1399141800000,786,136, 200], [1398537000000,773,143, 200], [1398796200000,783,154, 130], [1400005800000,1754,284, 200]]";
        // pass the array to the JavaScript function
        webview.loadUrl("javascript:displayViz(" +
                input + ")");
    }
}

