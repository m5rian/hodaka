package com.github.m5rian.hodaka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class Utilities {

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    public static byte[] urlToByteArray(String url) {
        byte[] byteArray = new byte[0];
        try {
            final URL uri = new URL(url);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = uri.openStream()) {
                byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
                int n;

                while ((n = is.read(byteChunk)) > 0) {
                    baos.write(byteChunk, 0, n);
                }

                byteArray = baos.toByteArray();
            } catch (IOException e) {
                System.err.printf("Failed while reading bytes from %s: %s", uri.toExternalForm(), e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("bruh");
            e.printStackTrace();
        }

        return byteArray;
    }

}
