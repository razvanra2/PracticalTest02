package ro.pub.cs.systems.eim.practicaltest02.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;
import cz.msebera.android.httpclient.util.EntityUtils;
import ro.pub.cs.systems.eim.practicaltest02.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.WeatherForecastInformation;

public class CommunicationThread extends Thread {

    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");

            String currency = bufferedReader.readLine();

            if (currency == null || currency.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }
            HashMap<String, Double> data = serverThread.getData();

            Double bitcoinInformation = null;
            if (data.containsKey(currency)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                bitcoinInformation = data.get(currency);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";


                HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + currency + ".json");
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    pageSourceCode = EntityUtils.toString(httpGetEntity);
                }


                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else {
                    Log.i(Constants.TAG, pageSourceCode);
                }

                JSONObject content = new JSONObject(pageSourceCode);
                Log.i("obj", pageSourceCode);
                JSONArray weatherArray = content.getJSONArray(Constants.WEATHER);
                JSONObject weather;
                String condition = "";
                for (int i = 0; i < weatherArray.length(); i++) {
                    weather = weatherArray.getJSONObject(i);
                    condition += weather.getString(Constants.MAIN) + " : " + weather.getString(Constants.DESCRIPTION);

                    if (i < weatherArray.length() - 1) {
                        condition += ";";
                    }
                }

                JSONObject main = content.getJSONObject(Constants.MAIN);
                String temperature = main.getString(Constants.TEMP);
                String pressure = main.getString(Constants.PRESSURE);
                String humidity = main.getString(Constants.HUMIDITY);

                JSONObject wind = content.getJSONObject(Constants.WIND);
                String windSpeed = wind.getString(Constants.SPEED);

                bitcoinInformation = 5.0;

                serverThread.setData(currency, bitcoinInformation);
            }

            if (bitcoinInformation == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }
            String result = null;

            printWriter.println(result);
            printWriter.flush();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } catch (JSONException jsonException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
            if (Constants.DEBUG) {
                jsonException.printStackTrace();
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}
