package com.chretimi.meteo;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
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
import java.util.Map;

public class OWMForecastFinder {

    private static final String APIKEY = "473f587c081395c0757c0324bedd6c31";

    private static OWMForecastFinder single_instance=null;

    private Map<Integer, OnForecastEventListener> mListener =new HashMap<>();

    private OnLocationForecastEventListener locListener;

    private Map<String, ForecastCache> cachedForecast = new HashMap<String, ForecastCache>();

    synchronized public static OWMForecastFinder getInstance(){
        if (single_instance == null)
        {
            single_instance = new OWMForecastFinder();
        }
        return single_instance;
    }

    private OWMForecastFinder(){
    }

    public void getForecastsCityID(String cityId){
        if(!cachedForecast.containsKey(cityId) || cachedForecast.get(cityId).isExpired()){
            String url = "http://api.openweathermap.org/data/2.5/forecast?id=" + cityId + "&APPID=" + APIKEY + "&units=metric";
            updateForecasts(url, false);
        }else{
            Log.d("Forecast through cache", cityId);
            for (OnForecastEventListener notifListener : mListener.values()){
                notifListener.onEvent(cityId, cachedForecast.get(cityId).getForecastDays());
            }
        }
    }

    // TODO cache
    public void getForecastsLocation(Location location){

        String lat = Double.toString(location.getLatitude());
        String lon = Double.toString(location.getLongitude());

        String url = "http://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&APPID=" + APIKEY + "&units=metric";
        updateForecasts(url, true);
    }


    private void updateForecasts(String url, boolean usingLoc) {

        Log.e("ForecastUpdate called", url);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.getAppContext());

        final List<Forecast> forecasts = new ArrayList<>();
        final boolean locationMode = usingLoc;
        // Request a string response from the provided URL.

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response){

                        try {
                            JSONObject json = new JSONObject(response);
                            String cod = json.getString("cod");

                            if (!cod.equals("200")) {
                                Log.e("Http error : ", cod);
                                toastErrorLog(getString(R.string.connection_impossible));
                                return;
                            }

                            JSONArray list = json.getJSONArray("list");
                            for (int i = 0; i < list.length(); i++) {
                                int timeStamp = list.getJSONObject(i).getInt("dt");
                                Date weatherDate = new Date((long) timeStamp * 1000);
                                int weatherId = list.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getInt("id");
                                int temp = list.getJSONObject(i).getJSONObject("main").getInt("temp");


                                Forecast f = new Forecast(weatherDate, weatherId, temp);
                                forecasts.add(f);
                            }
                            List<ForecastDay> newForecastsDays = groupForecasts(forecasts);
                            String currCity = json.getJSONObject("city").getString("name");
                            String cityId = json.getJSONObject("city").getString("id"); // Update au cas où la dernière requêtes vient d'info gps
                            Log.i("Weather", "Got weather for " + cityId + ", " + json.getJSONObject("city").getString("name"));
                            if(locationMode){
                                locListener.onEvent(cityId, currCity, newForecastsDays);
                            }else{
                                cachedForecast.put(cityId, new ForecastCache(newForecastsDays));
                                for (OnForecastEventListener notifListener : mListener.values()){
                                    notifListener.onEvent(cityId, cachedForecast.get(cityId).getForecastDays());
                                }
                            }

                        } catch (JSONException e) {
                            toastErrorLog(getString(R.string.json_error));
                            Log.e("ParsingForecast", "JSONException", e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error){
                toastErrorLog(getString(R.string.connection_impossible));
                Log.e("ConnexionError", "VolleyError", error);
            }
        });
        queue.add(stringRequest);
    }

    private String getString(int resId) {
        return MainActivity.getAppContext().getString(resId);
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

    public void addForecastEventListener(int id, OnForecastEventListener eventListener) {
        this.mListener.put(id, eventListener);
    }

    public void setLocationForecastEventListener(OnLocationForecastEventListener eventListener) {
        this.locListener = eventListener;
    }

    public interface OnForecastEventListener{
        void onEvent(String cityId, List<ForecastDay> OWMForecast);
    }

    public interface OnLocationForecastEventListener{
        void onEvent(String cityId, String cityName, List<ForecastDay> OWMForecast);
    }

    private void toastErrorLog(String error) {
        Context context = MainActivity.getAppContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, getString(R.string.error) + " : " + error, duration);
        toast.show();
    }
}
