package com.oligon.bienentracker.weather;

import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.ui.activities.NewEntryActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherTask extends AsyncTask<Double, String, ContentValues> {
    @Override
    protected ContentValues doInBackground(Double[] params) {
        try {
            InputStream is = null;
            int len = 1000;
            try {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("weather")
                        .appendQueryParameter("lat", params[0].toString())
                        .appendQueryParameter("lon", params[1].toString())
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("lang", "de")  // TODO: add language support
                        .appendQueryParameter("APPID", "e165ae07025b765c428990e8220d9b31");

                Log.d("BienenTracker", "URL: " + builder.toString());

                URL url = new URL(builder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                conn.connect();
                int response = conn.getResponseCode();
                Log.d("BienenTracker", "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                Reader reader = new InputStreamReader(is, "UTF-8");
                char[] buffer = new char[len];
                reader.read(buffer);
                ContentValues r = new ContentValues();
                try {
                    JSONObject jsonObj = new JSONObject(new String(buffer));
                    JSONArray weather = jsonObj.getJSONArray("weather");
                    if (weather.length() > 0) {
                        JSONObject w = weather.getJSONObject(0);
                        r.put("id", w.getInt("id"));
                    }
                    r.put("temp", jsonObj.getJSONObject("main").getDouble("temp"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    BeeApplication.getInstance().trackException(e);
                    return null;
                }
                return r;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            BeeApplication.getInstance().trackException(e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(ContentValues r) {
        super.onPostExecute(r);
        if (r != null && r.containsKey("temp"))
            NewEntryActivity.onWeatherSet(r.getAsDouble("temp").floatValue(),
                    r.getAsInteger("id"));
    }
}
