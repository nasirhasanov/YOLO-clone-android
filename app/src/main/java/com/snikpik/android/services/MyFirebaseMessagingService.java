package com.snikpik.android.services;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snikpik.android.MainActivity;
import com.snikpik.android.R;

import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_NOTIFICATION_KEY;
import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_USER_SETTINGS;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    @Override
    public void onNewToken(@NonNull String token) {
        SharedPreferences.Editor edit = getSharedPreferences(SHARED_PREFS_NOTIFICATION_KEY, MODE_PRIVATE).edit();
        edit.putString("notification_key", token);
        edit.apply();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser!=null && !currentUser.isAnonymous()) {
            updateTokentoDb(token);
        }
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        SharedPreferences.Editor edit = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE).edit();
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
        int currentValue = prefs.getInt("new_answer_count",0);
        ++currentValue;
        edit.putInt("new_answer_count", currentValue);
        edit.apply();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user !=null && !user.isAnonymous()) {
            createNotification();
        }
    }

    public void createNotification()
    {
        Intent resultIntent = new Intent(this , MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentText(getString(R.string.somebody_answered))
                .setAutoCancel(true)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0});
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
        mNotificationManager.notify(0 /* Request Code */, mBuilder.build());
    }

    public void updateTokentoDb(String token){
       DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser!=null) {
            userDbRef.child(currentUser.getUid()).child("notificationKey").setValue(token);
        }
    }
}
