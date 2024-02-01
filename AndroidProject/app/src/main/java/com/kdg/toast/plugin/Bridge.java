package com.kdg.toast.plugin;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class Bridge extends Application {
    static int summarySteps;
    static int steps;
    static int initialSteps;
    static Activity myActivity;
    static Context appContext;
    Date currentDate;
    static final String STEPS = "steps";
    static final String SUMMARY_STEPS = "summarySteps";
    static final String DATE = "currentDate";
    static final String INIT_DATE = "initialDate";

    public static final Intent[] POWERMANAGER_INTENTS = new Intent[]{
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            // ... (outros intents aqui)
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
    };

    public static void ReceiveActivityInstance(Activity tempActivity) {
        myActivity = tempActivity;
        String[] perms = new String[1];
        perms[0] = Manifest.permission.ACTIVITY_RECOGNITION;
        if (ContextCompat.checkSelfPermission(myActivity, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("PEDOMETER", "Permission isn't granted!");
            ActivityCompat.requestPermissions(Bridge.myActivity,
                    perms,
                    1);
        }
    }

    public static void StartService() {
        if (myActivity != null) {
            final SharedPreferences sharedPreferences = myActivity.getSharedPreferences("service_settings", MODE_PRIVATE);
            if (!sharedPreferences.getBoolean("auto_start", false)) {
                for (final Intent intent : POWERMANAGER_INTENTS) {
                    if (myActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        start();
                        break;
                    }
                }
            }
        } else {
            start();
        }
    }

    private static void start() {
        showModelRunningNotification();
        myActivity.startForegroundService(new Intent(myActivity, PedometerService.class));
    }

    private static void showModelRunningNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";
            String description = "Channel Description";
            int importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("channel_id", name, importance);
            channel.setDescription(description);
            NotificationManagerCompat notificationManager = myActivity.getSystemService(NotificationManagerCompat.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(myActivity, "channel_id")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Modelo em Execução")
                .setContentText("O modelo TFLite está sendo executado em segundo plano.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myActivity);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }

    public static void StopService() {
        Intent serviceIntent = new Intent(myActivity, PedometerService.class);
        myActivity.stopService(serviceIntent);
    }


    public static int GetCurrentSteps() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Date currentDate = Calendar.getInstance().getTime();
        editor.putString(DATE, currentDate.toString());
        int walkedSteps = sharedPreferences.getInt(STEPS, 0);
        int allSteps = sharedPreferences.getInt(SUMMARY_STEPS, 0);
        summarySteps = walkedSteps + allSteps;
        Log.i("PEDOMETER", "FROM BRIDGE CLASS - GetCurrentSteps:" + summarySteps);
        return summarySteps;
    }

    public static String SyncData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        int stepsToSend = GetCurrentSteps();
        String firstDate = sharedPreferences.getString(INIT_DATE, "");
        String lastDate = sharedPreferences.getString(DATE, "");
        String data = firstDate + '#' + lastDate + '#' + stepsToSend;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEPS, 0);
        editor.putInt(SUMMARY_STEPS, 0);
        steps = 0;
        summarySteps = 0;
        initialSteps = 0;
        Date currentDate = Calendar.getInstance().getTime();
        editor.putString(INIT_DATE, currentDate.toString());
        editor.putString(DATE, currentDate.toString());
        editor.apply();
        Log.i("PEDOMETER", "SyncData: " + steps + ' ' + summarySteps + data);
        return data;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Bridge.appContext = getApplicationContext();
    }
}
