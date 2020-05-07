package com.goncharenko;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
// синхронный запрос с сохранение 5и картинок = 7600ms
public class BingSynchronous {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                .build();

        String baseUrl = "https://www.bing.com";
        String firstUrl = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=5&mkt=en-US";


        Request request = new Request.Builder()
                .url(firstUrl)
                .build();

        long start = System.nanoTime();
        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            Collection<String> imageUrls = extractImageUrls(json, baseUrl);
            for (String imageUrl : imageUrls) {
                downloadImageFile(client, imageUrl);
            } response.body().close();
        } catch (IOException e) {
            System.err.println("Failed to get today images: " + e.getMessage());
            return;
        }

        long end = System.nanoTime();

        System.out.println(String.format("All requests: %.0f ms", (end - start) / 1_000_000f));
    }
    private static Collection<String> extractImageUrls(String imagesJson, String baseUrl) {
        Collection<String> result = new ArrayList<>();

        Gson gson = new Gson();

        BingResponse bingResponse = gson.fromJson(imagesJson, BingResponse.class);
        for (BingImage bingImage : bingResponse.images) {
            String fullUrl = baseUrl + bingImage.url;
            result.add(fullUrl);
            System.out.println(fullUrl);
        }
        return result;
    }

    private static void downloadImageFile(OkHttpClient client, String imageUrl) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        String fileName = request.url().queryParameter("id");

        try (Response response = client.newCall(request).execute()) {
            InputStream inputStream = response.body().byteStream();
            saveFile(inputStream, fileName);
        }

        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    private static void saveFile(InputStream inputStream, String fileName) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName))) {
            byte[] buffer = new byte[16_384];

            int readCount;
            do {
                readCount = inputStream.read(buffer);
                if (readCount >= 0) {
                    outputStream.write(buffer, 0, readCount);
                }
            } while (readCount != -1);
        }
    }

    private static HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor.Logger logger = new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                long threadId = Thread.currentThread().getId();
                System.out.println(String.format("[%d] %s", threadId, message));
            }
        };

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(logger);
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return interceptor;
    }
}
