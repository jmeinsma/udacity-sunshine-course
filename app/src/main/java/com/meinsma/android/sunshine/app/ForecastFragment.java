package com.meinsma.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.text.format.Time;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.content.Intent.EXTRA_TEXT;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> weekForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /*String[] data = {"Maandag 27 juni, regenachtig, 20 graden",
                "Dinsdag 28 juni, zonnig, 22 graden",
                "Woensdag 29 juni, onweer, 28 graden",
                "Donderdag 30 juni, prachtig, 24 graden",
                "Vrijdag 01 juli, nog mooier, 25 graden",
                "Zaterdag 02 juli, zonnig, 20 graden",
                "Zondag 03 juli, zonnig, 22 graden",
                "Maandag 04 juli, mooi weer, 23 graden",
                "Dinsdag 05 juli, beetje regen, 20 graden"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));*/

        weekForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        listView.setAdapter(weekForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String forecast = weekForecastAdapter.getItem(position);
                //Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                Intent detailViewIntent = new Intent(getActivity(), DetailActivity.class).putExtra(EXTRA_TEXT, forecast);
                startActivity(detailViewIntent);
            }
        });

        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity(), weekForecastAdapter);
        String location = getSharedPreferences().getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherTask.execute(location + ",NL");
        getActivity().setTitle(getString(R.string.app_name) + " " + location);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getActivity());
    }
}
