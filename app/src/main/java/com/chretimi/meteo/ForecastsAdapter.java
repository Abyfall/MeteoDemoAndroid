package com.chretimi.meteo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ForecastsAdapter extends RecyclerView.Adapter<ForecastsAdapter.ViewHolder>{

    // Store a member variable for the contacts
    private final List<ForecastDay> mForecast;

    // Pass in the contact array into the constructor
    public ForecastsAdapter(List<ForecastDay> contacts) {
        mForecast = contacts;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public final TextView nameTextView;

        public final ImageView morningForecastImageView;
        public final ImageView afternoonForecastImageView;
        public final ImageView eveningForecastImageView;

        public final TextView morningTemp;
        public final TextView afternoonTemp;
        public final TextView eveningTemp;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.date);
            morningForecastImageView = (ImageView) itemView.findViewById(R.id.morning_weather);
            afternoonForecastImageView = (ImageView) itemView.findViewById(R.id.afternoon_weather);
            eveningForecastImageView = (ImageView) itemView.findViewById(R.id.evening_weather);


            morningTemp = itemView.findViewById(R.id.morning_temp);
            afternoonTemp = itemView.findViewById(R.id.afternoon_temp);
            eveningTemp =  itemView.findViewById(R.id.evening_temp);
        }
    }


    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public ForecastsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.forecast_fragment_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        ForecastDay currDay = mForecast.get(position);
        Forecast morning = currDay.getMorning();
        Forecast afternoon = currDay.getAfternoon();
        Forecast evening = currDay.getEvening();

        TextView textView = viewHolder.nameTextView;
        textView.setText(currDay.getDate());

        viewHolder.morningForecastImageView.setImageResource(morning.getLogo());
        viewHolder.morningTemp.setText(morning.getTextTemp());

        viewHolder.afternoonForecastImageView.setImageResource(afternoon.getLogo());
        viewHolder.afternoonTemp.setText(afternoon.getTextTemp());

        viewHolder.eveningForecastImageView.setImageResource(evening.getLogo());
        viewHolder.eveningTemp.setText(evening.getTextTemp());
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mForecast.size();
    }

    public void updateForecastDays(List<ForecastDay> newForecast) {
        mForecast.clear();
        mForecast.addAll(newForecast);
        this.notifyDataSetChanged();
    }
}
