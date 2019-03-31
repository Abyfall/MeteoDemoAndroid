package com.chretimi.meteo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int FINE_COARSE_LOCATION_REQUEST = 48674;
    private static final String APIKEY = "473f587c081395c0757c0324bedd6c31";

    private List<ForecastDay> currForecasts;

    private String cityId = "3014728";
    private String currCity = "";
    private SharedPreferences prefs;
    private List<String[]> cities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.slide_menu);
        setSupportActionBar(toolbar);

        CSVReader reader = null;
        try {
            //String csvfileString = this.getApplicationInfo().dataDir + File.separatorChar + "assets/";
            final InputStream in = getAssets().open( "city.list.csv" );
            reader = new CSVReader(new InputStreamReader(in));
            cities = reader.readAll();
            Log.d("Cities found :", "" + cities.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);


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

            }

            @SuppressLint("RestrictedApi")
            @Override
            public void onDrawerOpened(View drawerView) {


                NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
                Menu menu = nv.getMenu();
                menu.clear(); //clear old inflated items.
                nv.inflateMenu(R.menu.drawer_menu); //inflate new items.

                String[] COUNTRIES = new String[] { "Belgium",
                        "France", "France_", "Italy", "Germany", "Spain" };

                // TODO BETTER TEST
                Switch onOffSwitch = (Switch) findViewById(R.id.app_notify_switch);
                onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.i("Switch State=", "" + isChecked);
                        if (isChecked) {
                            Context context = getApplicationContext();
                            setRecurringAlarm(context);
                            Log.i("RecurringAlarm ", "set");
                        } else {
                            Context context = getApplicationContext();
                            cancelRecurringAlarm(context);
                            Log.i("RecurringAlarm ", "removed");
                        }
                    }

                });

                final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

                MenuItem locationButton = (MenuItem) menu.findItem(R.id.local_weather);
                MenuItem addCityButton = (MenuItem) menu.findItem(R.id.add_city);

                locationButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.d("ALEDD", "Click location");

                        final LocationListener mLocationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(final Location location) {
                                Log.i("Location fixed", location.toString());
                                updateForecastsLocation(location);
                                mDrawerLayout.closeDrawers();
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {

                            }

                            @Override
                            public void onProviderEnabled(String provider) {

                            }

                            @Override
                            public void onProviderDisabled(String provider) {

                            }
                        };

                        requestLocationUpdate(mLocationListener);
                        return true;
                    }
                });

                addCityButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.d("Je vais", "Changer de vue");
                        Intent intent = new Intent(MainActivity.this, AddCity.class);
                        startActivity(intent);
                        return false;
                    }
                });

                Map<String, String> map = (Map<String, String>) prefs.getAll();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    System.out.println(entry.getKey() + "/" + entry.getValue());

                    if (menu.findItem(Integer.parseInt(entry.getKey())) == null) {
                        MenuItem city = menu.add(Menu.NONE, Integer.parseInt(entry.getKey()), Menu.NONE, entry.getValue());

                        city.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                cityId = "" + item.getItemId();
                                updateForecastsCity();
                                mDrawerLayout.closeDrawers();
                                return true;
                            }
                        });
                    }
                }

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

        updateForecastsCity();

        pullToRefresh.setRefreshing(false);

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateForecasts("");
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

    private void cancelRecurringAlarm(Context context) {

        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(recurringDownload);
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

        final TextView cityName = (TextView) findViewById(R.id.label_weather_city);
        cityName.setText(getString(R.string.weather) + " " + currCity);

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

    private void updateForecastsCity(){
        String url = "http://api.openweathermap.org/data/2.5/forecast?id=" + cityId + "&APPID=" + APIKEY + "&units=metric";
        updateForecasts(url);
    }

    private void updateForecastsLocation(Location location){
        String lat = Double.toString(location.getLatitude());
        String lon = Double.toString(location.getLongitude());
        String url = "http://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&APPID=" + APIKEY + "&units=metric";
        updateForecasts(url);
    }


    private void updateForecasts(String query) { // TODO async task


        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);

        pullToRefresh.post(new Runnable() {
            @Override
            public void run() {
                pullToRefresh.setRefreshing(true);
            }
        });

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = query;

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

                                /*
                                Log.d("Parse", list.getJSONObject(i).toString());
                                Log.d("Parse", "temp:" + temp + ", id: " + weatherId + " date " + weatherDate);*/

                                Forecast f = new Forecast(weatherDate, weatherId, temp);
                                forecasts.add(f);
                            }
                            currForecasts = groupForecasts(forecasts);
                            currCity = json.getJSONObject("city").getString("name");
                            cityId = json.getJSONObject("city").getString("id"); // Update au cas où la dernière requêtes vient d'info gps
                            Log.i("Weather", "Got weather for " + cityId + ", " + json.getJSONObject("city").getString("name"));
                            updateDisplay();
                            pullToRefresh.setRefreshing(false);

                        } catch (JSONException e) {
                            toastErrorLog(e.getMessage());
                            pullToRefresh.setRefreshing(false);
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                toastErrorLog(getString(R.string.connection_impossible));
                pullToRefresh.setRefreshing(false);
            }
        });
        queue.add(stringRequest);
    }

    private void toastErrorLog(String error) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, getString(R.string.error) + error, duration);

        toast.show();
    }

    private void requestLocationUpdate(LocationListener locationUpdateListener) {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    FINE_COARSE_LOCATION_REQUEST);
        }else{
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationUpdateListener, null);
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationUpdateListener, null);
        }
    }

}