package com.x1unix.moonwalker;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Grabber {
    private String referrer;
    private OkHttpClient client;

    public Grabber(String referrer) {
        this.referrer = referrer;
        this.client = this.getClient();
    }

    public void getPlayerScriptByKinopoiskId(String kpId, final Callback callback) {
        String url = "http://moonwalk.co/player_api?kp_id=" + kpId;
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public static boolean isSuccessful(Response response) {
        int code = response.code();
        return (code >= 200) && (code < 400);
    }

    private OkHttpClient getClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return new OkHttpClient().newBuilder()
                .addInterceptor(new RequestInterceptor(referrer))
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}