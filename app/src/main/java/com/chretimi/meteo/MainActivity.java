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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    LayoutInflater inflater;
    ViewPager vp;

    private static final int FINE_COARSE_LOCATION_REQUEST = 48674;

    private SharedPreferences prefs;

    private ArrayList<String[]> favoriteCities = new ArrayList<>();
    private ArrayList<String[]> displayCities = new ArrayList<>();

    private CityAdapter customAdapter;

    private static Context context;

    private AsyncTask<String, Integer, ArrayList<City>> cityFinder;

    private City citySelected;

    private ScreenSlidePagerAdapter slideAdapter;

    private FloatingActionButton favoriteButton;

    private SwipeRefreshLayout pullToRefresh;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main_drawer);

        Toolbar mTopToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_lightgrey_24dp);


        cityFinder =  new LoadCitiesTask().execute();

        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Reference ViewPager defined in activity
        vp=(ViewPager)findViewById(R.id.viewPager);

        prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);


        pullToRefresh = findViewById(R.id.refresh_layout);

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pullToRefresh.setRefreshing(false);
            }
        });

        Map<String, String> map = (Map<String, String>) prefs.getAll();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if(entry.getValue() instanceof String){
                displayCities.add(new String[]{ entry.getKey(), entry.getValue()});
            }
        }

        favoriteButton = findViewById(R.id.favorite_floating);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = vp.getCurrentItem();
                Fragment frag = slideAdapter.getItem(currentItem);
                String id = frag.getArguments().getString("id");
                String city = frag.getArguments().getString("city");
                Log.d("Favorite", id + ":" + city + ", " + vp.getCurrentItem());
                boolean isFavorite = false;
                if(prefs.contains(id+"b")){
                    isFavorite = prefs.getBoolean(id+"b", false);
                }
                Log.d("City", city + " favorite ?" + isFavorite);
                isFavorite = isFavorite ? false:true; // Inverse
                SharedPreferences.Editor editor = prefs.edit();
                if(isFavorite){
                    favoriteButton.setImageResource(R.drawable.ic_is_favorite_lightgrey_24dp);
                    editor.putString(id, city);
                    editor.putBoolean(id+"b", true);
                }else{
                    favoriteButton.setImageResource(R.drawable.ic_can_favorite_lightgrey_24dp);
                    editor.remove(id+"b");
                    editor.remove(id);
                    removeFarovriteCity(id);
                    slideAdapter.notifyDataSetChanged();
                }
                editor.apply();
            }
        });

        //set the adapter that will create the individual pages
        slideAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        vp.setAdapter(slideAdapter);
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                Fragment frag = slideAdapter.getItem(position);
                String id = frag.getArguments().getString("id");
                Log.d("Start refresb", "select " + id);
                setRefreshing(true);
                updateFavoriteButton(id);
            }
        });
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                toggleRefreshing(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });
    }

    public void  updateFavoriteButton(String id){
        boolean isFavorite = false;
        if(prefs.contains(id+"b")){
            isFavorite = prefs.getBoolean(id+"b", false);
        }
        if(isFavorite){
            favoriteButton.setImageResource(R.drawable.ic_is_favorite_lightgrey_24dp);
        }else{
            favoriteButton.setImageResource(R.drawable.ic_can_favorite_lightgrey_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void addCity(City city) {
        addCity(city.getId(), city.toString());
    }

    private void addCity(String id, String cityName) {
        displayCities.add(new String[]{ id, cityName});
    }

    private void addCityToFarovrite(City city) {
        addCity(city);
        addCityToFarovrite(city.getId(), city.getName());
    }

    private void addCityToFarovrite(String id, String cityName) {
        favoriteCities.add(new String[]{ id, cityName});
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(id, cityName);
        editor.putBoolean(id+"b", true);
        editor.apply();
    }

    private void removeFarovriteCity(String id) {
        for(int i = 0; i < favoriteCities.size(); i++){
            if(favoriteCities.get(i)[0].equals(id)){
                Object o = favoriteCities.get(i);
                favoriteCities.remove(o);
            }
        }
    }
    private void removeCity(String id) {
        for(int i = 0; i < displayCities.size(); i++){
            if(displayCities.get(i)[0].equals(id)){
                Object o = displayCities.get(i);
                displayCities.remove(o);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_search, menu);
        Log.d("onCreateOptionsMenu", "ah");

        ArrayList<City> cities = new ArrayList<>();
        customAdapter = new CityAdapter(MainActivity.this, android.R.layout.select_dialog_item, cities);
        Log.d("Adapter", "created");

        final MenuItem mSearchItem = menu.findItem(R.id.m_search);
        final MenuItem mLocationButton = menu.findItem(R.id.m_locate);

        final SearchView searchView =
                (SearchView) mSearchItem.getActionView();

        SearchView.SearchAutoComplete globSearchView = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        Log.d("cityFinder", globSearchView==null ? "null" : "pasnull");
        if (globSearchView != null) {
            globSearchView.setAdapter(customAdapter);//setting the adapter data into the AutoCompleteTextView
            globSearchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    citySelected = customAdapter.getItem(position);
                    Log.d("pos", citySelected.getName());
                    Log.d("id", citySelected.getId());
                    addCity(citySelected);
                    setRefreshing(true);
                    vp.getAdapter().notifyDataSetChanged();
                    int lastId = vp.getAdapter().getCount() - 1;
                    mSearchItem.collapseActionView();
                    vp.setCurrentItem(lastId, true);
                }
            });

            /**
             * Unset the var whenever the user types. Validation will
             * then fail. This is how we enforce selecting from the list.
             */
            globSearchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    citySelected = null;
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        mLocationButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setRefreshing(true);
                final LocationListener mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(final Location location) {
                        Log.i("Location fixed", location.toString());
                        OWMForecastFinder forecastFinder = OWMForecastFinder.getInstance();
                        forecastFinder.getForecastsLocation(location);
                        forecastFinder.setLocationForecastEventListener(new OWMForecastFinder.OnLocationForecastEventListener(){
                            @Override
                            public void onEvent(String cityId, String cityName, List<ForecastDay> OWMForecast) {
                                addCity(cityId, cityName);
                                vp.getAdapter().notifyDataSetChanged();
                                int lastId = vp.getAdapter().getCount() - 1;
                                vp.setCurrentItem(lastId, true);
                            }
                        });
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
                return false;
            }
        });
        return true;
    }

    public void toggleRefreshing(boolean enabled) {
        if (pullToRefresh != null) {
            pullToRefresh.setEnabled(enabled);
        }
    }

    public void setRefreshing(boolean b) {
        if(this.pullToRefresh != null){
            this.pullToRefresh.setRefreshing(b);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putString("id", displayCities.get(position)[0]);
            bundle.putString("city", displayCities.get(position)[1]);
            ScreenSlidePageFragment frag = new ScreenSlidePageFragment();
            frag.setArguments(bundle);
            return frag;
        }

        @Override
        public int getCount() {
            return displayCities.size();
        }

    }

    public static Context getAppContext() {
        return context;
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

    private void toastErrorLog(String error) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, getString(R.string.error) + error, duration);
        toast.show();
    }

    private class LoadCitiesTask extends AsyncTask<String, Integer, ArrayList<City>> {
        protected ArrayList<City> doInBackground(String... str) {
            final InputStream in;
            ArrayList<City> cities = new ArrayList<>();
            try {
                in = getAssets().open( "city.list.csv" );
                CSVReader csvReader = new CSVReader(new InputStreamReader(in), ';');
                List<String[]> rawCities;
                rawCities = csvReader.readAll();
                for(String[] city : rawCities){
                    cities.add(new City(city[0],city[1],city[2]));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("Found", cities.size() + "cities");
            customAdapter.updateCities(cities);
            return cities;
        }

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