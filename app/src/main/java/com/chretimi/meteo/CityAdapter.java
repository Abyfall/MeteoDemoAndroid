package com.chretimi.meteo;

import android.content.Context;
import android.util.Log;
import android.widget.Filter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CityAdapter extends ArrayAdapter<City> {

    private ArrayList<City> cities;
    private ArrayList<City> itemsAll;
    private ArrayList<City> suggestions;
    private int viewResourceId;

    public CityAdapter(Context context, int viewResourceId, ArrayList<City> items) {
        super(context, viewResourceId, items);
        this.cities = items;
        this.itemsAll = (ArrayList<City>) items.clone();
        this.suggestions = new ArrayList<City>();
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
                Log.d("Filter ", constraint.toString());
                suggestions.clear();
                for (City city : itemsAll) {
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
            ArrayList<City> filteredList = (ArrayList<City>) results.values;
            if(results != null && results.count > 0) {
                clear();
                for (City c : filteredList) {
                    add(c);
                }
                notifyDataSetChanged();
            }
        }
    };

}