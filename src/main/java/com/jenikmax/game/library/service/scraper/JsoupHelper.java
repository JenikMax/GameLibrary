package com.jenikmax.game.library.service.scraper;

import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class JsoupHelper {

    private JsoupHelper() {}

    private static OkHttpClient client(ScraperConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build();
    }

    public static Document fetchDocument(String url, ScraperConfig config) throws IOException {
        OkHttpClient client = client(config);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            String html = response.body().string();
            return Jsoup.parse(html);
        }
    }

    public static byte[] fetchBytes(String url, ScraperConfig config) throws IOException {
        OkHttpClient client = client(config);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            return response.body().bytes();
        }
    }
}
