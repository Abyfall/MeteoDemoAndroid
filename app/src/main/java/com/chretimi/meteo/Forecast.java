package com.chretimi.meteo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

class Forecast {

    private final int hour;
    private final String date;
    private final int forecast;
    private final Date rawDate;
    static final List<Forecast> forecasts = new ArrayList<>();
    private final int temp;

    public Forecast(Date date, int forecast, int temp){

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        this.hour = calendar.get(Calendar.HOUR_OF_DAY);

        this.temp = temp;

        this.rawDate = date;
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, dd MMM, HH");
        this.date = formatter.format(date) + "h";
        this.forecast = forecast;
    }

    public String getDate(){
        return date;
    }

    public static List<Forecast> getForecasts(){
        return forecasts;
    }

    public int getLogo() {
        int id = R.drawable.ic_none_24dp;
        switch (forecast/100){
            case 8:
                if (forecast == 800)
                    id = R.drawable.ic_021_sun;
                if (forecast == 801 || forecast == 802)
                    id = R.drawable.ic_021_cloudy;
                if (forecast == 803 || forecast == 804)
                    id = R.drawable.ic_021_cloud;
                break;
            case 2:
                id = R.drawable.ic_021_storm;
                break;
            case 5:
                id = R.drawable.ic_021_rain;
                break;
            case 6:
                id = R.drawable.ic_021_snowing_1;
                break;
            default:
                id = R.drawable.ic_none_24dp;
                break;
        }
        return id;
    }

    public Date getRawDate() {
        return rawDate;
    }

    public int getHour() {
        return hour;
    }

    public String getTextTemp() {
        return "" + temp + "°C";
    }
}
