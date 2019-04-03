package com.chretimi.meteo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AddCity extends AppCompatActivity {

    String[] fruits = {"Apple", "Banana", "Cherry", "Date", "Grape", "Kiwi", "Mango", "Pear"};
    private List<String[]> cities;
    private City citySelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);
        List<String[]> rawCities = new ArrayList<String[]>();
        ArrayList<City> cities = new ArrayList<>();

        //List<City> cities = new ArrayList<City>();
        try {
            //String csvfileString = this.getApplicationInfo().dataDir + File.separatorChar + "assets/";
            final InputStream in = getAssets().open( "city.list.csv" );

            CSVReader csvReader =new CSVReader(new InputStreamReader(in), ';');
            rawCities = csvReader.readAll();

            Log.d("Cities found :", "" + rawCities.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> citiesName = new LinkedList<>();
        for(String[] city : rawCities){
            cities.add(new City(city[0],city[1],city[2]));
        }
        //Creating the instance of ArrayAdapter containing list of fruit names
        /*final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, citiesName);*/

        final CityAdapter customAdapter = new CityAdapter(this, android.R.layout.select_dialog_item, cities);
        //Getting the instance of AutoCompleteTextView
        AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        actv.setThreshold(4);//will start working from first character
        actv.setAdapter(customAdapter);//setting the adapter data into the AutoCompleteTextView
        actv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                citySelected = customAdapter.getItem(position     );
                Log.d("pos", citySelected.getName());
                Log.d("id", citySelected.getId());
                Intent intent = new Intent();
                intent.putExtra("city_id", citySelected.getId());
                intent.putExtra("city_name", citySelected.getName());
                intent.putExtra("city_country", citySelected.getCountry());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        /**
         * Unset the var whenever the user types. Validation will
         * then fail. This is how we enforce selecting from the list.
         */
        actv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                citySelected = null;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
