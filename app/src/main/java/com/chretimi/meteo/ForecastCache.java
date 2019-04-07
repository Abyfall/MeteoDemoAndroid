package com.chretimi.meteo;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForecastCache {

    private static final long MAX_AGE = TimeUnit.HOURS.toMillis( 1 );
    private Date date;
    private List<ForecastDay> forecastDays;

    public ForecastCache(List<ForecastDay> forecastDays){
        date = new Date();
        this.forecastDays = forecastDays;
    }

    public List<ForecastDay> getForecastDays() {
        return forecastDays;
    }

    public boolean isExpired(){
        Date expireDate = new Date(this.date.getTime() + MAX_AGE);
        return new Date().after(expireDate);
    }
}
