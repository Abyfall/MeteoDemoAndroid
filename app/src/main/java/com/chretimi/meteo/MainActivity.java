package com.chretimi.meteo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String pageData[];          //Stores the text to swipe.
    LayoutInflater inflater;    //Used to create individual pages
    ViewPager vp;               //Reference to class to swipe views
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private SharedPreferences prefs;

    private ArrayList<String[]> favoriteCities = new ArrayList<>();

    private CityAdapter customAdapter;

    private static Context context;

    private AsyncTask<String, Integer, ArrayList<City>> cityFinder;

    private City citySelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_main);

        cityFinder =  new LoadCitiesTask().execute();

        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Reference ViewPager defined in activity
        vp=(ViewPager)findViewById(R.id.viewPager);

        prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("3021435", "Die" + ", " + "FR");
        editor.putString("3014728", "Grenoble" + ", " + "FR");
        editor.putString("2996944", "Lyon" + ", " + "FR");
        editor.commit();

        //set the adapter that will create the individual pages
        vp.setAdapter(new ScreenSlidePagerAdapter(getSupportFragmentManager()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_search, menu);
        Log.d("onCreateOptionsMenu", "ah");

        ArrayList<City> cities = new ArrayList<>();
        customAdapter = new CityAdapter(MainActivity.this, android.R.layout.select_dialog_item, cities);
        Log.d("Adapter", "created");

        MenuItem mSearchItem = menu.findItem(R.id.m_search);

        SearchView searchView =
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
                    prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(citySelected.getId(), citySelected.getName() + ", " + citySelected.getCountry());
                    editor.commit();
                    //updateForecastsCity();
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
        return true;
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
            prefs = getSharedPreferences("cities", Context.MODE_PRIVATE);
            Map<String, String> map = (Map<String, String>) prefs.getAll();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                favoriteCities.add(new String[]{ entry.getKey(), entry.getValue()});
            }
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putString("id", favoriteCities.get(position)[0]);
            bundle.putString("city", favoriteCities.get(position)[1]);
            ScreenSlidePageFragment frag = new ScreenSlidePageFragment();
            frag.setArguments(bundle);
            return frag;
        }

        @Override
        public int getCount() {
            return favoriteCities.size();
        }
    }

    public static Context getAppContext() {
        return MainActivity.context;
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
            customAdapter.updateCities(cities);
            return cities;
        }

    }
}