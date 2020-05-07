package com.goncharenko;
/*
1. Создать вторую точку входа в приложение (main). Реализовать асинхронную загрузку нескольких изображений,
используя сервис Bing, переделав реализацию синхронной загрузки с занятия.
2. Добавить логирование запросов с выводом Thread ID.
3. измерить время выполнения всех запросов в обоих подходах, используя System.nanoTime.
4. В асинхронной загрузке подменить dispatcher с указанием ExecutorService из стандартных доступных и измерить время выполнения:
    a. Executors.newCachedThreadPool
    b. Executors.newFixedThreadPool
    c. Executors.newSingleThreadExecutor
 */

import com.google.gson.Gson;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class BingAsynchronous {
    static long start;
    static AtomicInteger taskCounter = new AtomicInteger(0);

    public static void main(String[] args) {

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(createLoggingInterceptor())
                //.dispatcher(new Dispatcher(Executors.newFixedThreadPool(5))) //  Response: 1734 ms
                //.dispatcher(new Dispatcher(Executors.newCachedThreadPool())) // Response: 2088 ms
                   .dispatcher(new Dispatcher(Executors.newSingleThreadExecutor())) // Response: 2213 ms
                .build();
        String baseUrl = "https://www.bing.com";
        String firstUrl = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=5&mkt=en-US";
        start = System.nanoTime();
        Request request1 = new Request.Builder()
                .url(firstUrl)
                .build();
        Callback sharedCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.getMessage();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody.string();
                    Collection<String> imageUrls = extractImageUrls(json, baseUrl);
                    for (String imageUrl : imageUrls) {
                        downloadImageFile(client, imageUrl);
                    }
                }
                taskCounter.decrementAndGet();
            }
        };
        taskCounter.incrementAndGet();
        Call call1 = client.newCall(request1);
        call1.enqueue(sharedCallback);
        //client.connectionPool().evictAll();

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
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                try (ResponseBody responseBody = response.body()) {
                    InputStream inputStream = responseBody.byteStream();
                    saveFile(inputStream, fileName);
                }
                taskCounter.decrementAndGet();
                if(taskCounter.get()==0){
                    System.out.println(String.format("Response time : %.0f ms", (System.nanoTime() - start) / 1_000_000f));
                }
            }
        });
        taskCounter.incrementAndGet();
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
        HttpLoggingInterceptor.Logger logger = message -> {

            long threadId = Thread.currentThread().getId();
            System.out.println(String.format(" Thread ID: [%d] %s", threadId, message));
        };
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(logger);
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return interceptor;
    }

    static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
