package villo.com.ar.powersupplynotifier.helpers;

import android.view.inputmethod.InputMethodSession;
import android.widget.TextView;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import villo.com.ar.powersupplynotifier.Constants;
import villo.com.ar.powersupplynotifier.R;
import villo.com.ar.powersupplynotifier.model.UpsCallback;
import villo.com.ar.powersupplynotifier.model.UpsResponse;
import villo.com.ar.powersupplynotifier.model.UpsValues;

/**
 * Created by villo on 18/1/16.
 */
public class FetchNewValuesHelper {

    public static void fetchNewValues(final UpsCallback upsCallback) {
        OkHttpClient client = new OkHttpClient();
        client.setAuthenticator(new Authenticator() {
            @Override
            public Request authenticate(Proxy proxy, Response response) throws IOException {
                String credential = Credentials.basic("admin", "admin");
                return response.request().newBuilder().header("Authorization", credential).build();
            }

            @Override
            public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                return null;
            }
        });
        Request request = new Request.Builder()
                .url("http://192.168.1.1/ext/cgi-bin/tomatoups.cgi")
                .build();

        client.setReadTimeout(10, TimeUnit.SECONDS);
        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setFollowRedirects(true);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                UpsResponse response = new UpsResponse();

                response.setErrorMessage("Network problem.");
                upsCallback.onFailure(response, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String bodyString = response.body().string();

                    Document doc = Jsoup.parse(bodyString);

                    final String upsName = doc.select(".even td").get(Constants.UPS_NAME_INDEX).text().trim();
                    final String status = doc.select(".even td").get(Constants.STATUS_INDEX).text().trim();
                    final String charge = doc.select(".even td").get(Constants.CHARGE_INDEX).text().trim();
                    final String voltage = doc.select(".even td").get(Constants.VOLTAGE_INDEX).text().trim();
                    final String usagePercentage = doc.select(".even td").get(Constants.USAGE_PERCENTAGE_INDEX).text().trim();
                    final String temperature = doc.select(".even td").get(Constants.TEMPERATURE_INDEX).text().trim();
                    final String remainingTime = doc.select(".even td").get(Constants.REMAINING_TIME_INDEX).text().trim();

                    UpsResponse upsResponse = new UpsResponse();

                    if (upsName.equalsIgnoreCase("Not Found")) {
                        upsResponse.setInfoMessage("UPS Not Found, disconnected USB Cable or not configured in router.");
                    } else {
                        UpsValues values = new UpsValues();

                        values.setName(upsName);
                        values.setStatus(status);
                        values.setCharge(charge);
                        values.setVoltage(voltage);
                        values.setUsagePercentage(usagePercentage);
                        values.setTemperature(temperature);
                        values.setRemainingTime(remainingTime);
                        upsResponse.setValues(values);

                        if (status.equalsIgnoreCase("ONLINE")) {
                            upsResponse.setInfoMessage("Hay luz, todo ok.");
                        }
                        else {
                            upsResponse.setInfoMessage("En bateria, en breve no habrá luz.");
                        }
                    }
                    upsCallback.onResponse(upsResponse);
                }
            }
        };
        client.newCall(request).enqueue(callback);
    }

}