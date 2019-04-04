package com.chretimi.meteo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int FINE_COARSE_LOCATION_REQUEST = 48674;
    private static final String APIKEY = "473f587c081395c0757c0324bedd6c31";

    private List<ForecastDay> currForecasts;

    private String cityId = "3014728";
    private String currCity = "";
    private SharedPreferences prefs;

    private SearchView.SearchAutoComplete globSearchView;
    private City citySelected;
    private CityAdapter customAdapter;

    private AsyncTask<String, Integer, ArrayList<City>> cityFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.slide_menu);
        setSupportActionBar(toolbar);

        cityFinder =  new LoadCitiesTask().execute();

        prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);


        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Called when a drawer has settled in a completely closed state.
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                {
                    if (newState == DrawerLayout.STATE_SETTLING && !mDrawerLayout.isDrawerOpen(GravityCompat.START)) {

                        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
                        Menu menu = nv.getMenu();
                        menu.clear(); //clear old inflated items.
                        nv.inflateMenu(R.menu.drawer_menu); //inflate new items.

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
                        MenuItem searchItem = menu.findItem(R.id.action_search);
                        SearchView searchView =
                                (SearchView) searchItem.getActionView();

                        globSearchView = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
                        // Configure the search info and add any event listeners...
                        try {
                            cityFinder.get();
                            if(globSearchView != null) {
                                globSearchView.setAdapter(customAdapter);//setting the adapter data into the AutoCompleteTextView
                                globSearchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        citySelected = customAdapter.getItem(position);
                                        Log.d("pos", citySelected.getName());
                                        Log.d("id", citySelected.getId());
                                        prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString(citySelected.getId(), citySelected.getName() + ", " + citySelected.getCountry());
                                        editor.commit();
                                        cityId = citySelected.getId();
                                        updateForecastsCity();
                                        mDrawerLayout.closeDrawers();
                                    }
                                });

                                /**
                                 * Unset the var whenever the user types. Validation will
                                 * then fail. This is how we enforce selecting from the list.
                                 */
                                globSearchView.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        citySelected = null;
                                    }
                                    @Override
                                    public void afterTextChanged(Editable s) {}
                                });
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

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

                        Map<String, String> map = (Map<String, String>) prefs.getAll();
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            System.out.println(entry.getKey() + "/" + entry.getValue());

                            if (menu.findItem(Integer.parseInt(entry.getKey())) == null) {
                                MenuItem city = menu.add(Menu.NONE, Integer.parseInt(entry.getKey()), Menu.NONE, entry.getValue());
                                city.setIcon(R.drawable.ic_remove_black_24dp); // add icon with drawable resource
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
                }
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


    private void updateForecasts(String url) { // TODO async task


        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);

        pullToRefresh.post(new Runnable() {
            @Override
            public void run() {
                pullToRefresh.setRefreshing(true);
            }
        });

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

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


    private class LoadCitiesTask extends AsyncTask<String, Integer, ArrayList<City>> {
        protected ArrayList<City> doInBackground(String... str) {
            final InputStream in;
            ArrayList<City> cities = new ArrayList<>();
            try {
                in = getAssets().open( "city.list.csv" );
                CSVReader csvReader = new CSVReader(new InputStreamReader(in), ';');
                List<String[]> rawCities = new ArrayList<>();
                rawCities = csvReader.readAll();
                List<String> citiesName = new LinkedList<>();
                for(String[] city : rawCities){
                    cities.add(new City(city[0],city[1],city[2]));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("Found", cities.size() + "cities");
            return cities;
        }


        @Override
        protected void onPostExecute(ArrayList<City> result) {
            setDataCreateAdapter(result);
        }
    }

    private void setDataCreateAdapter(ArrayList<City> cities) {
        customAdapter = new CityAdapter(MainActivity.this, android.R.layout.select_dialog_item, cities);
        Log.d("Adapter", "created");
    }

}