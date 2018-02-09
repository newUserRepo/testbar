package com.example.vaadinupload;

import com.vaadin.ui.Label;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * @author Alejandro Duarte
 */
@Service
public class ProcessingService {

    /**
     * This method simulates a processing job
     **/
    @Async // run in a separate thread
    public void processData(File file, String hash, Label labelResult,
                            Consumer<Float> progressListener,
                            Runnable succeededListener) {

        final int STEPS = 20;
        final long largo = file.length();
        final byte[] bytes = new byte[1024];
        MessageDigest messageDigest = null;
        final StringBuilder sb = new StringBuilder();
        try {
            messageDigest = MessageDigest.getInstance(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        int dataRead = 0;
        int acum = 0;
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            while((dataRead = bis.read(bytes,0,1024)) != -1) {
                sleep();
                messageDigest.update(bytes,0,dataRead);
                acum += dataRead;
                float per = (acum/(float)largo);
                System.out.println("percentage: "+per+"%");
                progressListener.accept(per);
            }
            final byte[] bytesDigest = messageDigest.digest();
            for(int f=0; f<bytesDigest.length; f++) {
                sb.append(Integer.toString((bytesDigest[f] & 0xFF ) + 0x100 ,16).substring(1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        for (int i = 1; i <= STEPS; i++) {
//            sleep();
//            Long totalCharCount = file.length();
//            int processedCharCount = totalCharCount.intValue() / STEPS * i;
//            float processedPercentage = (float) processedCharCount / totalCharCount;
//            System.out.println("Percent: "+processedPercentage+"%");
//            progressListener.accept(processedPercentage); // notify progress listener
//        }
        labelResult.setValue("Checksum: "+sb.toString());
        succeededListener.run(); // notify succeeded listener
    }

    private void sleep() {
        try {
            int lower = 500;
            int upper = 3000;
            //Thread.sleep((long) ((Math.random() * (upper - lower)) + lower));
            Thread.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void beep(Consumer<String> label) {
            while (true) {
                try {
                    Thread.sleep(1000);
                    label.accept("Hora actual: "+getHour());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    }

    public String getHour() {
        return DateTimeFormatter
                .ofPattern("hh:mm:ss a")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

}
