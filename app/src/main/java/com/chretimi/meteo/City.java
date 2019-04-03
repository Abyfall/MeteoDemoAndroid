package com.chretimi.meteo;


public class City {

    private String id;
    private String name;
    private String country;

    public City(String name, String country, String id){
        this.id = id;
        this.name = name;
        this.country = country;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public String toString(){
        return name + ", " + country;
    }


}
