package com.chretimi.meteo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private List<ForecastDay> currForecasts;

    private String cityId = "3014728";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setRefreshing(true);

        final SharedPreferences prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

        ArrayList<String> citiesList = new ArrayList<String>();
        citiesList.add("Lyon");
        citiesList.add("Grenoble");


        Set<String> set = new HashSet<String>();

        set.addAll(citiesList);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("3014728", "Grenoble");
        editor.putString("2996944", "Lyon");
        editor.commit();


        Intent intent = getIntent();
        String value = intent.getStringExtra("userLogin");

        final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                ActionBar ab = getSupportActionBar(); // TODO utiliser cette merde
                /*if (slideOffset != 0 && drawerView..getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
                    NavigationView nv= (NavigationView) findViewById(R.id.nav_view);
                    Menu menu=nv.getMenu();



                    Map<String, String> map = (Map<String, String>) prefs.getAll();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        System.out.println(entry.getKey() + "/" + entry.getValue());

                        MenuItem city = menu.add(Menu.NONE, Integer.parseInt(entry.getKey()), Menu.NONE, entry.getValue());

                        city.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                cityId = "" + item.getItemId();
                                updateForecasts();
                                mDrawerLayout.closeDrawers();
                                return true;
                            }
                        });
                    }

                    MenuItem lyon = menu.add(Menu.NONE, 2996944, Menu.NONE, "Lyon");
                    MenuItem grenoble = menu.add(Menu.NONE, 3014728, Menu.NONE, "Grenoble");
                    lyon.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            cityId = "" + item.getItemId();
                            updateForecasts();
                            mDrawerLayout.closeDrawers();
                            return true;
                        }
                    });

                    grenoble.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            cityId = "" + item.getItemId();
                            updateForecasts();
                            mDrawerLayout.closeDrawers();
                            return true;
                        }
                    });
                }*/
            }

            @Override
            public void onDrawerOpened(View drawerView) {

                NavigationView nv= (NavigationView) findViewById(R.id.nav_view);
                Menu menu=nv.getMenu();

                Map<String, String> map = (Map<String, String>) prefs.getAll();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    System.out.println(entry.getKey() + "/" + entry.getValue());

                    MenuItem city = menu.add(Menu.NONE, Integer.parseInt(entry.getKey()), Menu.NONE, entry.getValue());

                    city.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            cityId = "" + item.getItemId();
                            updateForecasts();
                            mDrawerLayout.closeDrawers();
                            return true;
                        }
                    });
                }


                Switch onOffSwitch = (Switch)  findViewById(R.id.app_notify_switch);
                onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.i("Switch State=", ""+isChecked);
                        if(isChecked){
                            Context context = getApplicationContext();
                            setRecurringAlarm(context);
                            Log.i("RecurringAlarm ", "set");
                        }
                    }

                });
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Called when a drawer has settled in a completely closed state.
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // Called when the drawer motion state changes. The new state will be one of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
            }
        });

        updateForecasts();

        pullToRefresh.setRefreshing(false);

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateForecasts();
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    private void setRecurringAlarm(Context context) {

        // we know mobiletuts updates at right around 1130 GMT.
        // let's grab new stuff at around 11:45 GMT, inexactly
        Calendar updateTime = Calendar.getInstance();
        updateTime.set(Calendar.HOUR_OF_DAY, 8);
        updateTime.set(Calendar.MINUTE, 0);
        updateTime.set(Calendar.SECOND, 0);

        Log.i("time:", updateTime.toString() + Calendar.getInstance());

        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
                0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(
                Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                updateTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, recurringDownload);
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

    private void updateForecasts() { // TODO async task

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
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