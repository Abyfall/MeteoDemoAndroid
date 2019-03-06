package com.chretimi.meteo;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<ForecastDay> currForecasts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;


        Intent intent = getIntent();
        String value = intent.getStringExtra("userLogin");

        updateForecasts();

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateForecasts(); // your code
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    /**
     * Group unique forecast as forecastDay (all forecast for a day)
     *
     * @param forecasts an ordered list of forecast to group
     * @return an ordered list of forecast
     */
    private List<ForecastDay> groupForecasts(List<Forecast> forecasts) {
        Calendar calendar = GregorianCalendar.getInstance();
        List<ForecastDay> forecastDays = new ArrayList<>();

        int currDay = -1;
        ForecastDay currForecastDays = null;
        for (Forecast f : forecasts) {
            Date forecastDate = f.getRawDate();
            calendar.setTime(forecastDate);

            int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

            if (dayOfYear != currDay) {
                if (currDay != -1) { // If it's not the first day
                    forecastDays.add(currForecastDays);
                }
                currForecastDays = new ForecastDay(forecastDate);
                currDay = dayOfYear;
            }
            currForecastDays.addForecast(f);
        }

        return forecastDays;
    }


    private void updateDisplay() {

        // Lookup the recyclerview in activity layout
        final RecyclerView rvForecast = (RecyclerView) findViewById(R.id.forecastRecycler);

        List<ForecastDay> days = currForecasts;

        // Create adapter passing in the sample user data
        ForecastsAdapter adapter = new ForecastsAdapter(days);
        // Attach the adapter to the recyclerview to populate items
        rvForecast.setAdapter(adapter);
        // Set layout manager to position the items
        rvForecast.setLayoutManager(new LinearLayoutManager(MainActivity.this));

    }

    private void updateForecasts() {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String cityId = "3014728";
        String apiKey = "473f587c081395c0757c0324bedd6c31";
        String url = "http://api.openweathermap.org/data/2.5/forecast?id=" + cityId + "&APPID=" + apiKey + "&units=metric";


        final List<Forecast> forecasts = new ArrayList<>();

        // Request a string response from the provided URL.

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject json = new JSONObject(response);
                            String cod = json.getString("cod");

                            if (!cod.equals("200")) {
                                toastErrorLog("Http error : " + cod);
                                return;
                            }

                            JSONArray list = json.getJSONArray("list");
                            for (int i = 0; i < list.length(); i++) {
                                int timeStamp = list.getJSONObject(i).getInt("dt");
                                Date weatherDate = new Date((long) timeStamp * 1000);
                                int weatherId = list.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getInt("id");
                                int temp = list.getJSONObject(i).getJSONObject("main").getInt("temp");

                                Log.e("Parse", list.getJSONObject(i).toString());
                                Log.e("Parse", "temp:" + temp + ", id: " + weatherId + " date " + weatherDate);
                                Forecast f = new Forecast(weatherDate, weatherId, temp);
                                forecasts.add(f);
                            }
                            currForecasts = groupForecasts(forecasts);
                            updateDisplay();

                        } catch (JSONException e) {
                            toastErrorLog(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                toastErrorLog(error.getMessage());
            }
        });
        queue.add(stringRequest);
    }

    private void toastErrorLog(String error){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, "Error: " + error, duration);

        toast.show();
    }
}