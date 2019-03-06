package com.chretimi.meteo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForecastDay {


    private String date;
    int forecast;
    List<Forecast> forecasts = new ArrayList<>();

    public ForecastDay(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE dd MMMM");
        String lowerDate = formatter.format(date);
        this.date = lowerDate.substring(0, 1).toUpperCase() + lowerDate.substring(1);
    }

    public void addForecast(Forecast f){
        this.forecasts.add(f);
    }

    public Forecast getMorning() {
        if(forecasts.size()<3)
            return forecasts.get(0); // TODO create a real solution
        return forecasts.get(2); // Should be the first unless if day already started
    }

    public Forecast getAfternoon() {
        if(forecasts.size()<5)
            return forecasts.get(0); // TODO create a real solution
        return forecasts.get(4); // Should be the first unless if day already started
    }

    public Forecast getEvening() {
        if(forecasts.size()<7)
            return forecasts.get(0); // TODO create a real solution
        return forecasts.get(6); // Should be the first unless if day already started
    }

    public String getDate() {
        return date;
    }
}
