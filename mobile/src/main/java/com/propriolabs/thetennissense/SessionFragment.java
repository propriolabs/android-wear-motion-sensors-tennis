package com.propriolabs.thetennissense;

import android.app.Activity;
import android.app.Fragment;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;

public class SessionFragment extends Fragment
{
    // save a reference so custom methods
    // can access views
    private View topLevelView;

    private View stub;
    private String output;
    private String sessionId;

    // save a reference to show the pie chart
    private WebView webview;

    private Activity activity;

    private Socket mSocket;

    private void setUpSocket() {
        try {
            mSocket = IO.socket(getString(R.string.data_server));

            mSocket.on("session_data", new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        JSONObject jsonObj = obj.getJSONObject("data");
                        output = jsonObj.toString();
                        Log.v("data", jsonObj.toString());
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

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        Log.v("SessionFragment", "onCreateView");

        super.onCreateView(inflater, container, savedInstanceState);
        sessionId = ((AnalysisActivity)getActivity()).getSessionId();

        activity = getActivity();

        Log.v("sessionId", sessionId);

        if( mSocket == null || !mSocket.connected()) {
            setUpSocket();
        }

        boolean attachToRoot = false;
        topLevelView = inflater.inflate(R.layout.sessionsummary, container, attachToRoot);
        //Toast.makeText(topLevelView.getContext(), "date = " + sessionId, Toast.LENGTH_SHORT).show();

        //initPieChart();
        // call now or after some condition is met
        mSocket.emit("get_session", sessionId);
        return topLevelView;
    }

    @Override
    public void onDestroy() {
        Log.v("SessionFragment","onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.v("SessionFragment","onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v("SessionFragment", "onCreate");
        super.onCreate(savedInstanceState);
    }



    // initialize the WebView and the pie chart
    public void initPieChart()
    {

        stub = topLevelView.findViewById(
                R.id.pie_chart_stub);

        if (stub instanceof ViewStub)
        {
            ((ViewStub)stub).setVisibility(View.VISIBLE);

            webview = (WebView)topLevelView.findViewById(R.id.pie_chart_webview);

            WebSettings webSettings =
                    webview.getSettings();

            webSettings.setJavaScriptEnabled(true);

            webview.setWebChromeClient(
                    new WebChromeClient());

            webview.setWebViewClient(new WebViewClient()
            {
                @Override
                public void onPageFinished(
                        WebView view,
                        String url)
                {

                    // after the HTML page loads,
                    // load the pie chart
                    loadPieChart(output);
                }
            });

            // note the mapping
            // from  file:///android_asset
            // to PieChartExample/assets
            webview.loadUrl("file:///android_asset/" +
                    "html/index.html");
        }
    }

    public void loadPieChart(String input)
    {

        int dataset[] = new int[] {};
        try {
            JSONObject aggregate = new JSONObject();
            JSONObject jsonObj = new JSONObject(input);
            aggregate = jsonObj.getJSONObject("aggregate");
            for (int i = 0; i < aggregate.names().length(); i++) {
                dataset = addElement(dataset, (int) aggregate.get(aggregate.names().getString(i)));

            }

        } catch(JSONException e){
            e.printStackTrace();
        }


        Log.v("pie",input);
        // use java.util.Arrays to format
        // the array as text
        String text = Arrays.toString(dataset);

        webview = (WebView)topLevelView.findViewById(R.id.pie_chart_webview);

        //webview.loadUrl("javascript:setHeader(" +
        //      AnalysisActivity.getSessionId() + ")");
        // pass the array to the JavaScript function
        webview.loadUrl("javascript:loadCharts(" +
                input + ")");
        //webview.loadUrl("javascript:loadBarChart(" +
        //       text + ")");
    }

    static int[] addElement(int[] a, int e) {
        a  = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = e;
        return a;
    }


}
