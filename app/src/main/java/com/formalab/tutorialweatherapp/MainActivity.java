package com.formalab.tutorialweatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "ee318df497020f077901717e7b51cced";

    Button btnSearch;
    Button btnLocalisation;
    EditText etCityName;
    ImageView iconWeather;
    TextView tvTemp, tvCity;
    ListView lvDailyWeather;
    LinearLayout MainPart;

    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        btnSearch = findViewById(R.id.btnSearch);
        btnLocalisation = findViewById(R.id.btnLocalisation);
        etCityName = findViewById(R.id.etCityName);
        iconWeather = findViewById(R.id.iconWeather);
        tvTemp = findViewById(R.id.tvTemp);
        tvCity = findViewById(R.id.tvCity);
        lvDailyWeather = findViewById(R.id.lvDailyWeather);
        MainPart = findViewById(R.id.MainPart);

        // Make the Main Part Invisible
        MainPart.setVisibility(View.INVISIBLE);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = etCityName.getText().toString();
                if (city.isEmpty())
                    Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_LONG).show();
                else {
                    loadWeatherByCityName(city);
                }
            }
        });

        btnLocalisation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // When permission granted
                    getLocation();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this
                            , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);

                    showDialog(MainActivity.this,"?????? ????????????????????");
                }
            }
        });

    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {



            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull @org.jetbrains.annotations.NotNull Task<Location> task) {

                Location location = task.getResult();

                if(location != null){
                    Geocoder geocoder = new Geocoder(MainActivity.this,Locale.getDefault());
                    try {
                        List<Address> adress = geocoder.getFromLocation(
                                location.getLatitude(),location.getLongitude(),1);

                        // Search by the name of the city returned
                        loadWeatherByCityName(adress.get(0).getLocality());
                        etCityName.setText(null);

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,"Server Error",Toast.LENGTH_SHORT);
                    }

                }else{
                    Toast.makeText(MainActivity.this,"Please Turn On Your Localisation !!",Toast.LENGTH_SHORT);
                }
            }
        });
    }


    private void loadWeatherByCityName(String city) {
        String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" + API_KEY;
        Ion.with(this)
                .load(apiUrl)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {

                        if (e != null) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "please check your network connection !", Toast.LENGTH_SHORT).show();
                        } else {


                            MainPart.setVisibility(View.VISIBLE);

                            // ???????????????????????????? json
                            JsonObject main = result.get("main").getAsJsonObject();
                            double temp = main.get("temp").getAsDouble();
                            tvTemp.setText(temp + "??C");

                            JsonObject sys = result.get("sys").getAsJsonObject();
                            String country = sys.get("country").getAsString();
                            tvCity.setText(city + ", " + country);

                            JsonArray weather = result.get("weather").getAsJsonArray();
                            String icon = weather.get(0).getAsJsonObject().get("icon").getAsString();
                            loadIcon(icon);

                            JsonObject coord = result.get("coord").getAsJsonObject();
                            double lon = coord.get("lon").getAsDouble();
                            double lat = coord.get("lat").getAsDouble();

                            loadDailyForecast(lon, lat);
                        }
                    }
                });
    }

    private void loadDailyForecast(double lon, double lat) {
        String apiUrl = "https://api.openweathermap.org/data/2.5/forecast/daily?lat="+lat+"&lon="+lon+"&cnt=16&appid=" + API_KEY;
        Ion.with(this)
                .load(apiUrl)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                        } else {
                            List<Weather> weatherList = new ArrayList<>();
                            //String timeZone = result.get("timezone").getAsString();
                            JsonArray daily = result.get("list").getAsJsonArray();
                            for(int i=1;i<daily.size();i++) {
                                Long date = daily.get(i).getAsJsonObject().get("dt").getAsLong();
                                Double temp = daily.get(i).getAsJsonObject().get("temp").getAsJsonObject().get("day").getAsDouble();
                                String icon = daily.get(i).getAsJsonObject().get("weather").getAsJsonArray().get(0).getAsJsonObject().get("icon").getAsString();
                                weatherList.add(new Weather(date, temp, icon));
                            }


                            DailyWeatherAdapter dailyWeatherAdapter = new DailyWeatherAdapter(MainActivity.this, weatherList);
                            lvDailyWeather.setAdapter(dailyWeatherAdapter);
                        }
                    }
                });
    }


    private void loadIcon(String icon) {
        Ion.with(this)
                .load("http://openweathermap.org/img/w/" + icon + ".png")
                .intoImageView(iconWeather);
    }



    public void showDialog(Activity activity, String msg){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog);

        TextView text = (TextView) dialog.findViewById(R.id.text_dialog);
        text.setText(msg);

        Button dialogButton = (Button) dialog.findViewById(R.id.btn_dialog);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }


}