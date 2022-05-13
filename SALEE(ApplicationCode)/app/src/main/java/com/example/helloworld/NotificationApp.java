package com.example.helloworld;

import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotificationApp extends Application implements LifecycleObserver {

    // Notification Strings
    public static final String CHANNEL_1_ID = "channel1";

    private static Context context;


    @Override
    public void onCreate(){
        super.onCreate();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // create notifications
        createNotificationChannels();

        // NotificationApp.context = getApplicationContext();

    }

    public static Context getAppContext() {
        return NotificationApp.context;
    }

    private void createNotificationChannels(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Channel 1",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel1.setDescription("Notifications of unusual power consumption");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        ConnectionActivity.resume.set(true);
        /*
        while (ConnectionActivity.eventQueue.size() > 0) {
            HashMap <String, Float> eve = new HashMap<>();

            eve = ConnectionActivity.eventQueue.remove();

            // get timestamp and difference of event
            Map.Entry<String, Float> entry = eve.entrySet().iterator().next();
            String time = entry.getKey();
            Float power = entry.getValue();
            if (power > 0) {
                showDialog(eve, true);
            } else {
                showDialog(eve, false);
            }
        }
        // ConnectionActivity.infoMessagesTwo.append("Check");
        */
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        ConnectionActivity.resume.set(false);
        // ConnectionActivity.infoMessagesTwo.append("Check");
    }
}
