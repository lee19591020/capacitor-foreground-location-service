package com.fusion5.capacitorforegroundlocationservice;

import android.content.Context;
import android.util.Log;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;


public class CapacitorForegroundLocationService extends Service {

    private static final String CHANNEL_ID = "CapacitorForegroundLocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    CapacitorForegroundLocationServicePlugin plugin = CapacitorForegroundLocationServicePlugin.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void startLocationUpdates(int interval, int distanceFilter) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateDistanceMeters(distanceFilter)
                .setWaitForAccurateLocation(false)
                .setMaxUpdateDelayMillis(interval)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int interval = intent.getIntExtra("interval", 5000);
        int distanceFilter = intent.getIntExtra("distanceFilter", 10);
        String notificationTitle = intent.getStringExtra("notificationTitle");
        String notificationText = intent.getStringExtra("notificationText");
        int importance = intent.getIntExtra("notificationImportance", NotificationManager.IMPORTANCE_HIGH);
        int notificationChannelId = intent.getIntExtra("notificationChannelId", 235);

        if (notificationTitle == null) notificationTitle = "Location Service";
        if (notificationText == null) notificationText = "Tracking location...";
        createNotificationChannel(this, importance);
        Notification notification = getForegroundNotification(this, notificationTitle, notificationText);
        startForeground(notificationChannelId, notification);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    CapacitorForegroundLocationServicePlugin plugin = CapacitorForegroundLocationServicePlugin.getInstance();
                    if (plugin != null) {
                        plugin.broadcastLocation(location);
                    }
                    Log.d("LOCATION", "Location: " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        };
        startLocationUpdates(interval, distanceFilter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void createNotificationChannel(Context context, int importance) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "capacitor_foreground_location_service";
            String channelName = "Foreground Service Channel";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    importance
            );
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
    public Notification getForegroundNotification(Context context, String title, String message) {
        String channelId = "capacitor_foreground_location_service";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use your app icon
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
