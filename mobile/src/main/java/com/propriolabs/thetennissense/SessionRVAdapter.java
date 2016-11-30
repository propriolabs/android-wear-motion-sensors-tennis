package com.propriolabs.thetennissense;

/**
 * Created by wgmueller on 2/13/16.
 */
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class SessionRVAdapter extends RecyclerView.Adapter<SessionRVAdapter.SessionViewHolder> {

    private HashMap<String, Long> sessionIds;
    private List<Session> sessions;

    private SessionActivity activity;

    SessionRVAdapter(List<Session> sessions, HashMap<String, Long> sessionIds, SessionActivity activity){
        this.sessions = sessions;
        this.sessionIds = sessionIds;
        this.activity = activity;
    }

    public class SessionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CardView cv;
        TextView sessionDuration;
        TextView sessionDate;
        TextView sessionStrokes;
        TextView sessionUser;
        TextView sessionCalories;
        TextView sessionRallies;
        TextView sessionMeanRally;
        TextView sessionWeather;
        TextView sessionWind;
        TextView sessionTemperature;
        TextView sessionAddress;
        TextView sessionMatch;



        ImageView sessionPhoto;
        Long sessionId;
        String userName;
        String sessionStorage;
        Button optionsButton;
        boolean sessionAnalyzed;
        int sessionNumStrokes;
        String sessionDisplayStrokes;


        SessionViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv2);
            sessionDuration = (TextView)itemView.findViewById(R.id.session_duration);
            sessionDate = (TextView)itemView.findViewById(R.id.session_date);
            sessionStrokes = (TextView)itemView.findViewById(R.id.session_strokes);
            sessionCalories = (TextView)itemView.findViewById(R.id.session_calories);
            sessionRallies = (TextView)itemView.findViewById(R.id.session_rallies);
            sessionMeanRally = (TextView)itemView.findViewById(R.id.session_meanrally);
            sessionPhoto = (ImageView)itemView.findViewById(R.id.session_photo);
            sessionUser = (TextView)itemView.findViewById(R.id.session_user);
            optionsButton = (Button)itemView.findViewById(R.id.button);
//            sessionWeather = (TextView)itemView.findViewById(R.id.weather);
            sessionWind = (TextView)itemView.findViewById(R.id.wind);
            sessionTemperature = (TextView)itemView.findViewById(R.id.temperature);
            sessionAddress = (TextView)itemView.findViewById(R.id.address);
            sessionMatch = (TextView)itemView.findViewById(R.id.match);



            cv.setOnClickListener(this);
            optionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(v.getContext(),v);
                    //Inflating the Popup using xml file
                    popup.getMenuInflater().inflate(R.menu.session, popup.getMenu());

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            Log.v("test","You Clicked : " + item.getTitle());
                            return true;
                        }
                    });

                    popup.show();//showing popup menu
                }
            });
        }

        @Override
        public void onClick(final View view) {
            if(sessionStorage.equals("Cloud") && sessionAnalyzed && sessionNumStrokes > 0) {
                TextView tv = (TextView) view.findViewById(R.id.session_date);
                Log.v("SessionRVAdapter", "clicked on:" + sessionId.toString());
                Intent newIntent = new Intent(view.getContext(), AnalysisActivity.class);
                newIntent.putExtra("session", sessionId.toString());
                view.getContext().startActivity(newIntent);
            }
            else if(sessionStorage.equals("Cloud") && !sessionAnalyzed) {
                TextView tv = (TextView) view.findViewById(R.id.session_date);
                Log.v("SessionRVAdapter", "clicked on:" + sessionId.toString() + ((TextView) view.findViewById(R.id.session_strokes)).getText());
                activity.analyzeData(sessionId.toString());
            }
        }
    }




    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public SessionViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Log.v("SessionRVAdapter","onCreateViewHolder:" + i);
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stats_cardview, viewGroup, false);
        return new SessionViewHolder(v);
    }




    @Override
    public void onBindViewHolder(final SessionViewHolder sessionViewHolder, final int i) {
        Log.v("SessionRVAdapter","onBindViewHolder:" + i);
        sessionViewHolder.sessionId = Long.parseLong(sessions.get(i).sessionId);
        sessionViewHolder.sessionDuration.setText(sessions.get(i).startstop.toString());
        sessionViewHolder.sessionDate.setText(sessions.get(i).date);
        sessionViewHolder.sessionStorage = sessions.get(i).storage;
        sessionViewHolder.sessionAnalyzed = sessions.get(i).analyzed;
        sessionViewHolder.sessionNumStrokes = sessions.get(i).numStrokes;
        sessionViewHolder.sessionDisplayStrokes = sessions.get(i).displayStrokes.toString();
        sessionViewHolder.userName = sessions.get(i).userName;


        Log.v("SessionRVAdapter","onBindViewHolder:" + sessionViewHolder.sessionId);

        sessionViewHolder.sessionStrokes.setText(sessions.get(i).displayStrokes);
        sessionViewHolder.sessionCalories.setText(sessions.get(i).calories);
        sessionViewHolder.sessionRallies.setText(sessions.get(i).rally_count);
        sessionViewHolder.sessionMeanRally.setText(sessions.get(i).hit_per_rally);
        sessionViewHolder.sessionPhoto.setImageResource(sessions.get(i).photoId);
        sessionViewHolder.sessionUser.setText(sessions.get(i).userName);
//        sessionViewHolder.sessionWeather.setText(sessions.get(i).weather);
        sessionViewHolder.sessionWind.setText(sessions.get(i).wind);
        sessionViewHolder.sessionTemperature.setText(sessions.get(i).temperature);
        sessionViewHolder.sessionAddress.setText(sessions.get(i).address);
        sessionViewHolder.sessionMatch.setText(sessions.get(i).match);





        if(sessions.get(i).storage.equals("Local")) {
            sessionViewHolder.cv.setCardBackgroundColor(Color.parseColor("#00a6f5"));
            sessionViewHolder.optionsButton.setVisibility(View.INVISIBLE);
            sessionViewHolder.optionsButton.setText("Options");
        }
        else if(sessions.get(i).storage.equals("Cloud")) {
            sessionViewHolder.cv.setCardBackgroundColor(Color.parseColor("#d3d3d3"));
            sessionViewHolder.optionsButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        Log.v("SessionRVAdapter","getItemCount:" + sessions.size());
        return sessions.size();
    }
}