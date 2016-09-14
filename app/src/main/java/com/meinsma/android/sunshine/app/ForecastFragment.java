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

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        final int DAYS = 7;
        final String UNITS = "metric";
        final String FORMAT = "json";
        final String APPID = BuildConfig.OPEN_WEATHER_MAP_API_KEY;

        private String getReadableDateString(long time){
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low) {

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            return roundedHigh + "/" + roundedLow;
        }

        private double temperatureInUnit(double temperature) {

            String unit = getSharedPreferences().getString(getString(R.string.pref_unit_key), getString(R.string.pref_unit_default));

            if(unit.equals("imperial")) {
                return (temperature * 1.8) + 32;
            }

            return temperature;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {

                String day;
                String description;
                String highAndLow;
    
                JSONObject dayForecast = weatherArray.getJSONObject(i);
    
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);
    
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
    
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureInUnit(temperatureObject.getDouble(OWM_MAX));
                double low = temperatureInUnit(temperatureObject.getDouble(OWM_MIN));
    
                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {

            if(params.length == 0) return null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            try {
                urlConnection = (HttpURLConnection) buildUrl(params).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();

                if (inputStream == null) return null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) return null;

                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG, forecastJsonStr);

            } catch(IOException ioe) {
                Log.e(LOG_TAG, "Error ", ioe);
                return null;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, DAYS);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null) {
                weekForecastAdapter.clear();
                for(String dayForecastStr: strings) {
                    weekForecastAdapter.add(dayForecastStr);
                }
            }
        }

        private URL buildUrl(String... params) {

            URL result = null;

            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String DAYS_PARAM = "cnt";
            final String UNITS_PARAM = "units";
            final String FORMAT_PARAM = "mode";
            final String APPID_PARAM = "appid";

            Uri buildUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(DAYS))
                    .appendQueryParameter(UNITS_PARAM, UNITS)
                    .appendQueryParameter(FORMAT_PARAM, FORMAT)
                    .appendQueryParameter(APPID_PARAM, APPID)
                    .build();

            try {
                result = new URL(buildUri.toString());
            } catch(MalformedURLException mue) {
               Log.e(LOG_TAG, "Error asking weather api.", mue);
            } finally {
                Log.v(LOG_TAG, "Built Uri: " + buildUri);
                return result;
            }
        }

        private double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex) throws JSONException {
            JSONObject obj = new JSONObject(weatherJsonStr);
            return obj.getJSONArray("list").getJSONObject(dayIndex).getJSONObject("main").getDouble("temp_max");
        }
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String location = getSharedPreferences().getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherTask.execute(location + ",NL");
        getActivity().setTitle(getString(R.string.app_name) + " " + location);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getActivity());
    }
}
