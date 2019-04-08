package com.chretimi.meteo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ScreenSlidePageFragment extends Fragment {

    private String id;
    private String cityName;
    private OWMForecastFinder forecastFinder;
    private List<ForecastDay> currForecasts = new ArrayList<ForecastDay>();
    private ForecastsAdapter adapter;
    private ActionBar actionBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.page, container, false);

        id = getArguments().getString("id");
        cityName = getArguments().getString("city");

        Log.d("onCreateView ", cityName + " : " + id + " called");

        // Create adapter passing in the sample user data
        adapter = new ForecastsAdapter(currForecasts);

        forecastFinder = OWMForecastFinder.getInstance();
        forecastFinder.addForecastEventListener(Integer.parseInt(id), new OWMForecastFinder.OnForecastEventListener(){
            public void onEvent(String cityId, List<ForecastDay> OWMForecast){
                if(cityId.equals(id)){
                    Log.d("Frag "+id+" got", cityId + " it's me :)");
                    currForecasts = OWMForecast;
                    updateDisplay();
                    ((MainActivity) getActivity()).setRefreshing(false);
                }else{
                    Log.d("Frag "+id+" got", cityId + " skipping");
                }
            }
        });
        forecastFinder.getForecastsCityID(id);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d("onViewCreated " + cityName, "called");

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        // Lookup the recyclerview in activity layout
        final RecyclerView rvForecast = (RecyclerView) getView().findViewById(R.id.forecastRecycler);

        // Attach the adapter to the recyclerview to populate items
        rvForecast.setAdapter(adapter);
        // Set layout manager to position the items
        rvForecast.setLayoutManager(new LinearLayoutManager(MainActivity.getAppContext()));
    }

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);
        if (visible && isResumed())
        {
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!getUserVisibleHint())
        {
            return;
        }

        if(actionBar != null){
            actionBar.setTitle(cityName);
        }

    }

    private void updateDisplay() {

        adapter.updateForecastDays(currForecasts);
        Log.e("adapter " + cityName, "notified");
    }
}
