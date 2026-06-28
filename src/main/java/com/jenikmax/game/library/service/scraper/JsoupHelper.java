package com.jenikmax.game.library.service.scraper;

import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsoupHelper {

    private final OkHttpClient client;

    public JsoupHelper(OkHttpClient client) {
        this.client = client;
    }

    public Document fetchDocument(String url, ScraperConfig config) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            return Jsoup.parse(response.body().byteStream(), null, url);
        }
    }

    public byte[] fetchBytes(String url, ScraperConfig config) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            return response.body().bytes();
        }
    }
}
