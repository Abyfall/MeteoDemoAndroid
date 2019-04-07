package com.chretimi.meteo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CityAdapter extends ArrayAdapter<City> {

    private final Map<String, Integer> alphabetIndexes = new HashMap<String, Integer>() {
        {
            put("a", 16);
            put("b", 11327);
            put("c", 28524);
            put("d", 43844);
            put("e", 51044);
            put("f", 56121);
            put("g", 60985);
            put("h", 70024);
            put("i", 77814);
            put("j", 80383);
            put("k", 83158);
            put("l", 93317);
            put("m", 105445);
            put("n", 120681);
            put("o", 127816);
            put("p", 132979);
            put("q", 146130);
            put("r", 147207);
            put("s", 155151);
            put("t", 179626);
            put("u", 188776);
            put("v", 190819);
            put("w", 197812);
            put("x", 204384);
            put("y", 205264);
            put("z", 207042);
        }
    };
    private ArrayList<City> cities;
    private int viewResourceId;

    public CityAdapter(Context context, int viewResourceId, ArrayList<City> items) {
        super(context, viewResourceId, items);
        this.cities = new ArrayList<>(items.size());
        this.cities.addAll(items);
        this.viewResourceId = viewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.city_auto, null);
        }

        City city = getItem(position);

        TextView name = (TextView) view.findViewById(R.id.cityLabel);
        name.setText(city.toString());

        return view;
    }

    public void updateCities(ArrayList<City> newCities) {
        cities.clear();
        cities.addAll(newCities);
        this.notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    Filter nameFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            String str = ((City)(resultValue)).getName();
            Log.d("convertResultToString", str);
            return str;
        }
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if(constraint != null) {

                ArrayList<City> suggestions = new ArrayList<>();
                Log.d("Filter ", constraint.toString());
                String firstLetter = "" + constraint.toString().toLowerCase().charAt(0);
                int charValue = firstLetter.charAt(0);
                String nextChar = String.valueOf( (char) (charValue + 1));
                int start = alphabetIndexes.get(firstLetter);
                int end = alphabetIndexes.get(nextChar);
                for(int i = start; i < end; i++){
                    City city = cities.get(i);
                    if(city.getName().toLowerCase().startsWith(constraint.toString().toLowerCase())){
                        if(suggestions.size() < 25){
                            suggestions.add(city);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            if (results != null && results.count > 0) {
                // we have filtered results
                addAll((ArrayList<City>) results.values);
            } else {
                // no filter, add entire original list back in
                addAll(cities);
            }
            notifyDataSetChanged();
        }
    };

}