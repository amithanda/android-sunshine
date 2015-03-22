package com.example.android.sunshine.app;

/**
 * Created by amithanda on 8/20/14.
 */

import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mforecastArrayAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            AsyncWeatherAPICaller asyncCaller = new AsyncWeatherAPICaller();
            asyncCaller.execute();
            Log.e("onOptionsItemSelected", "Completed asyncCaller");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = {
                "Today-Sunny-88/63",
                "Tomorrow-Foggy-70/46",
                "Weds-Sunny-76/56",
                "Thurs-Rain-55/55",
                "Fri-Cloudy-45/56",
                "Sat-Cloudy-45/56",
                "Sun-Cloudy-45/56",
        };

        List<String> forecast_entries = new ArrayList<String>(
                Arrays.asList(forecastArray));

        mforecastArrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forcast,
                R.id.list_item_forecast_textview,
                forecast_entries);
        ListView listview = (ListView) rootView.findViewById(
                R.id.listview_forcast);
        listview.setAdapter(mforecastArrayAdapter);


        //Add the clickListener
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = mforecastArrayAdapter.getItem(position);
                Toast.makeText(parent.getContext(), item, Toast.LENGTH_SHORT).show();
                Intent weatherDetailIntent = new Intent(parent.getContext(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, item);
                startActivity(weatherDetailIntent);
            }
        });


        return rootView;
    }

    public class AsyncWeatherAPICaller extends AsyncTask<Void, Void, String[]> {
        private String LOG_TAG = AsyncWeatherAPICaller.class.getSimpleName().toString();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
                Log.e(LOG_TAG, resultStrs[i]);
            }

            return resultStrs;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String[] weatherData = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Log.e(LOG_TAG, "Trying to call the api");
                Uri.Builder builder = new Uri.Builder();
                String NUM_DAYS = "7";
                //builder.buildUpon("http://api.openweathermap.org/data/2.5/forecast/daily");
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q", "94043")
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("cnt", NUM_DAYS);


                String myUrl = builder.build().toString();
                URL url = new URL(myUrl);
                Log.e(LOG_TAG, "URL to call is " + url);
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                Log.e(LOG_TAG, "Calling Connect");
                urlConnection.connect();
                Log.e(LOG_TAG, "After Calling Connect");
                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    Log.i(LOG_TAG, "Found Null Inputstream");
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    Log.i(LOG_TAG, "Found null buffer length");
                    return null;
                }
                forecastJsonStr = buffer.toString();
                try {
                    weatherData = getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(NUM_DAYS));
                    Log.i(LOG_TAG, forecastJsonStr);


                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                    return null;
                }


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error - IO Exception", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
                //return forecastJsonStr;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
                //return null;
            }
            Log.i(LOG_TAG, "Weather data is");
            for (String data : weatherData) {
                Log.i(LOG_TAG, data);
            }
            return weatherData;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            //super.onPostExecute(strings);
            if (strings != null) {
                Log.i(LOG_TAG, "onPostExecute Weather data is");
                for (String data : strings) {
                    Log.i(LOG_TAG, data);
                }
                mforecastArrayAdapter.clear();
                for (String dayForecast : strings) {
                    mforecastArrayAdapter.add(dayForecast);
                }
            } else Log.e(LOG_TAG, "Null Result received from AsyncTask");


        }
    }
}
