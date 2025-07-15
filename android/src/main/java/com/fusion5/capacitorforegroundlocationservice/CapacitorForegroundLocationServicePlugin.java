package com.fusion5.capacitorforegroundlocationservice;

import static com.fusion5.capacitorforegroundlocationservice.GeoUtils.calculateDistance;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@CapacitorPlugin(
        name = "CapacitorForegroundLocationService",
        permissions = {
                @Permission(
                        alias = "android12",
                        strings = {
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }
                ),
                @Permission(
                        alias = "greater13",
                        strings = {
                                Manifest.permission.FOREGROUND_SERVICE,
                                Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                        }
                )
        }
)
public class CapacitorForegroundLocationServicePlugin extends Plugin {

    private String TAG = "CapacitorForegroundLocationService";
    private boolean isAppInForeground = true;
    int interval;
    int distanceFilter;
    String notificationTitle;
    String notificationText;

    @PluginMethod
    public void getStoredValue(PluginCall call) {
        try {
            SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String goefenceData = prefs.getString("geofenceData", null);
            String userData = prefs.getString("userData", null);
            String logsEndpoint = prefs.getString("logsEndpoint", null);
            String endPoint = prefs.getString("endPoint", null);
            boolean allowNotification = Boolean.TRUE.equals(prefs.getBoolean("allowNotification", false));
            JSONObject allowNotificationJSON = new JSONObject();
            allowNotificationJSON.put("allowNotification", allowNotification);
            JSObject data = new JSObject();
            data.put("goefenceData", goefenceData);
            data.put("userData", userData);
            data.put("logsEndpoint", logsEndpoint);
            data.put("endPoint", endPoint);
            data.put("allowNotification", allowNotificationJSON);
            call.resolve(data);
        } catch (Exception e) {
            call.reject("Error in getStoredValue: " + e.getMessage());
        }
    }
    @PluginMethod
    public void getApiOptions(PluginCall call) {
        try {
            JSObject configData = new JSObject();
            configData.put("interval", interval);
            configData.put("distanceFilter", distanceFilter);
            configData.put("notificationTitle", notificationTitle);
            configData.put("notificationMessage", notificationText);
            call.resolve(configData);
        } catch (Exception e) {
            call.reject("Error in getApiOptions: " + e.getMessage());
        }
    }
    @PluginMethod
    public void setApiOptions(PluginCall call) {
        try {
            // Get the root object
            JSObject data = call.getData();

            // Extract nested objects
            JSObject endpointObj = data.getJSObject("endpoint");
            JSObject geofenceDataObj = data.getJSObject("geofenceData");
            JSObject userDataObj = data.getJSObject("userData");
            JSObject logsEndpointObj = data.getJSObject("logsEndpoint");
            JSObject allowNotificationObj = data.getJSObject("allowNotification");

            // Null checks
            if (endpointObj == null || geofenceDataObj == null || userDataObj == null || logsEndpointObj == null) {
                call.reject("If passing apiOptions, all fields (endpoint, geofenceData, userData, logsEndpoint) must be provided.");
                return;
            }

            // Extract strings
            String endpoint = endpointObj.getString("endPoint", null);
            String geofenceDataStr = geofenceDataObj.toString(); // store full JSON string
            String userDataStr = userDataObj.toString();         // store full JSON string
            String logsEndpoint = logsEndpointObj.getString("logsEndpoint", null);
            assert allowNotificationObj != null;
            boolean allowNotification = Boolean.TRUE.equals(allowNotificationObj.getBoolean("allowNotification", false));

            // Final null validation
            if (endpoint == null || logsEndpoint == null) {
                call.reject("endPoint and logsEndpoint must not be null inside their objects.");
                return;
            }
            // Save to SharedPreferences
            SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("endPoint", endpoint);
            editor.putString("geofenceData", geofenceDataStr);
            editor.putString("userData", userDataStr);
            editor.putString("logsEndpoint", logsEndpoint);
            editor.putBoolean("allowNotification", allowNotification);
            editor.apply();

            JSObject result = new JSObject();
            result.put("result", "saved successfully");
            call.resolve(result);

        } catch (Exception e) {
            call.reject("Error in setApiOptions: " + e.getMessage());
        }
    }

    @PluginMethod
    public void config(PluginCall call){
        try {
            int interval = call.getInt("interval", 5000); // default 5 seconds
            int distanceFilter = call.getInt("distanceFilter", 20); // default 20 meters
            String notificationTitle = call.getString("notificationTitle");
            if (notificationTitle == null) {
                notificationTitle = "Foreground Location Service";
            }
            String notificationText = call.getString("notificationMessage");
            if (notificationText == null) {
                notificationText = "Tracking location in background";
            }

            // Save to class fields for later use
            this.interval = interval;
            this.distanceFilter = distanceFilter;
            this.notificationTitle = notificationTitle;
            this.notificationText = notificationText;
            call.resolve();
        } catch (Exception e) {
            call.reject("Wrong configuration");
        }

    }

    private PluginCall savedCall;
    @PluginMethod
    public void requestPermission(PluginCall call) {
        savedCall = call;
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 14+ — request all three: location, foreground service, foreground service location
            bridge.getActivity().runOnUiThread(() -> {
                requestPermissionForAlias("greater13", savedCall, "permissionRequestResultAndroid13");
            });
        } else {
            // Android 13 and below — request only location
            bridge.getActivity().runOnUiThread(() -> {
                requestPermissionForAlias("android12", call, "permissionRequestResultAndroid12");
            });
        }
    }

    @PluginMethod
    public void startService(PluginCall call) {

        Context context = getContext();
        if (!isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, CapacitorForegroundLocationService.class);
            serviceIntent.putExtra("interval", interval);
            serviceIntent.putExtra("distanceFilter", distanceFilter);
            serviceIntent.putExtra("notificationTitle", notificationTitle);
            serviceIntent.putExtra("notificationText", notificationText);
            ContextCompat.startForegroundService(context, serviceIntent);
            Log.i(TAG, "Capacitor Foreground Location Service started.");
        } else {
            Log.i(TAG, "Capacitor Foreground Location Service already running.");
        }

        call.resolve();
    }

    @PluginMethod
    public void isLocationServiceRunning(PluginCall call) {
        JSObject result = new JSObject();
        result.put("running", isServiceRunning(getContext()));
        call.resolve(result);
    }
    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CapacitorForegroundLocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @PluginMethod
    public void stopService(PluginCall call) {
        Context context = getContext();
        Intent serviceIntent = new Intent(context, CapacitorForegroundLocationService.class);
        context.stopService(serviceIntent);
        clearClockHistory();
        call.resolve();
    }

    @PluginMethod
    public void setClockInHistory(PluginCall call) {
        if (!call.hasOption("clockNumber") || 
            !call.hasOption("clockingType") || 
            !call.hasOption("timestamp")) {
            call.reject("Missing required parameters");
            return;
        }

        int clockNumber = call.getInt("clockNumber");
        String type = call.getString("clockingType");
        long timestamp = call.getLong("timestamp");

        saveClockHistory(clockNumber, type, timestamp);

        JSObject result = new JSObject();
        result.put("status", "saved");
        call.resolve(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private boolean hasAllPermissions() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAllPermissionsbelowCake() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @PermissionCallback
    protected void permissionRequestResultAndroid12(PluginCall call) {
        JSObject result = new JSObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                result.put("granted", hasAllPermissions());
            } else {
                result.put("granted", hasAllPermissionsbelowCake());
            }
        }

        if (call != null) {
            call.resolve(result);
        }
    }

    @PermissionCallback
    protected void permissionRequestResultAndroid13(PluginCall call) {
        JSObject result = new JSObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                result.put("granted", hasAllPermissions());
            } else {
                result.put("granted", hasAllPermissionsbelowCake());
            }
        }

        if (call != null) {
            call.resolve(result);
        }
    }

    private static CapacitorForegroundLocationServicePlugin instance;
    public CapacitorForegroundLocationServicePlugin() {
        instance = this;
    }

    public static CapacitorForegroundLocationServicePlugin getInstance() {
        return instance;
    }

    public void broadcastLocation(Location location) {
        sendLocationUpdate(location);
    }
    @Override
    protected void handleOnPause() {
        super.handleOnPause();
        isAppInForeground = false;
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();
        isAppInForeground = true;
    }
    private void sendLocationUpdate(Location location) {
        JSObject data = new JSObject();
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("altitude", location.getAltitude());
        data.put("accuracy", location.getAccuracy());
        data.put("speed", location.getSpeed());
        data.put("bearing", location.getBearing());
        data.put("time", location.getTime());

        if (isAppInForeground) {
            notifyListeners("locationUpdate", data);
        } else {
            sendUpdatesToServer(data);
        }
    }
    private void sendUpdatesToServer(JSONObject data){
        SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String goefenceData = prefs.getString("geofenceData", null);
        String userData = prefs.getString("userData", null);
        String logsEndpoint = prefs.getString("logsEndpoint", null);
        String endPoint = prefs.getString("endPoint", null);
        if(goefenceData != null && userData != null && logsEndpoint != null && endPoint != null){
            try {

                JSONArray geofenceArray = new JSONObject(goefenceData).getJSONArray("geofenceData");
                JSONObject userJson = new JSONObject(userData);
                List<GeofenceInformation> geofenceList = new ArrayList<>();
                for (int i = 0; i < geofenceArray.length(); i++) {
                    JSONObject obj = geofenceArray.getJSONObject(i);
                    GeofenceInformation geo = new GeofenceInformation(
                            obj.getDouble("lat"),
                            obj.getDouble("lng"),
                            obj.getDouble("radius"),
                            obj.getString("clockDescription"),
                            obj.getInt("clockNumber"),
                            obj.getString("locationCode"),
                            obj.getString("locationDescription")
                    );
                    geofenceList.add(geo);
                }

                User user = new User(
                        userJson.getInt("userId"),
                        userJson.optString("username", null),
                        userJson.optString("_token", null)
                );
                calculateAndSend(geofenceList, user, logsEndpoint, data, endPoint);
            } catch (Exception e){
                Log.e("ERROR", "Something went Wrong:" + e.getMessage());
            }
        }
    }

    private void calculateAndSend(List<GeofenceInformation> geofenceList, User user, String logsEndpoint, JSONObject currentLocationData, String endPoint) throws Exception {
        double lat = currentLocationData.getDouble("lat");
        double lng = currentLocationData.getDouble("lng");

        GeofenceInformationDistance closest = null;
        for (GeofenceInformation geo : geofenceList) {
            double distance = calculateDistance(lat, lng, geo.getLat(), geo.getLng());
            GeofenceInformationDistance item = new GeofenceInformationDistance(distance, geo);

            if (distance < geo.getRadius()) {
                // Inside geofence
                sendLog(user.get_token(), logsEndpoint, new LogsPayload(
                        String.valueOf(user.getUserId()),
                        String.format("%.5f", lat),
                        String.format("%.5f", lng),
                        "Employee just entered the geofence " + geo.getClockDescription(),
                        System.currentTimeMillis()
                ));
                sendAutoClocking(user.get_token(), endPoint, new AutoClockingPayload(
                    String.valueOf(user.getUserId()),
                    String.format("%.5f", lat),
                     String.format("%.5f", lng),
                     true,
                     geo,
                     System.currentTimeMillis()
                ));
                return;
            }

            if (closest == null || distance < closest.getDistance()) {
                closest = item;
            }
        }

        if (closest != null) {
            double distanceToClock = closest.getDistance() - closest.getGeofence().getRadius();
            sendLog(user.get_token(), logsEndpoint, new LogsPayload(
                    String.valueOf(user.getUserId()),
                    String.format("%.5f", lat),
                    String.format("%.5f", lng),
                    "Employee is " + distanceToClock + " closer to " + closest.getGeofence().getClockDescription(),
                    System.currentTimeMillis()
            ));
            sendAutoClocking(user.get_token(), endPoint, new AutoClockingPayload(
                String.valueOf(user.getUserId()),
                String.format("%.5f", lat),
                String.format("%.5f", lng),
                false,
              closest.getGeofence(),
              System.currentTimeMillis()
            ));
        }
    }

    private void sendLog(String token, String url, LogsPayload payload) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject logJson = payload.toJson();
            RequestBody body = RequestBody.create(
                    logJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("sendLog", "Request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("sendLog", "Unexpected response: " + response.code());
                    } else {
                        Log.i("sendLog", "Log sent successfully");
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("sendLog", "JSON error: " + e.getMessage());
        }
    }

    private void sendAutoClocking(String token, String url, AutoClockingPayload payload) {

      retrySendingAutoClocking(token);
      Log.i(TAG,"Has clockin: " + hasClockedIn(payload.getGeofence().getClockNumber(), payload.getTimeStamp()));
      Log.i(TAG,"Has Clocout: " + hasClockedOut(payload.getGeofence().getClockNumber(), payload.getTimeStamp()));
      if (hasClockedIn(payload.getGeofence().getClockNumber(), payload.getTimeStamp()) && hasClockedOut(payload.getGeofence().getClockNumber(), payload.getTimeStamp()) ) {
          Log.i("ClockHistory", "Already clocked " + (payload.isClockIn() ? "in" : "out") + " today for " + payload.getGeofence().getClockDescription());
          return;
      }

        OkHttpClient client = new OkHttpClient.Builder()
          .connectTimeout(5, TimeUnit.SECONDS)  // Connection timeout
          .readTimeout(5, TimeUnit.SECONDS)     // Time to wait for server response
          .writeTimeout(5, TimeUnit.SECONDS)    // Time to send the request body
          .build();

        try {
            JSONObject logJson = payload.toJson();
            RequestBody body = RequestBody.create(
                    logJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
          Log.i(TAG, "Rquesting Clocking " + (payload.isClockIn() ? "IN" : "OUT"));
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                  Log.i(TAG, "Rquesting Clocking " + (payload.isClockIn() ? "IN" : "OUT") + " FAiled to proceed to offline");
                    logFailedPostRequest(url, payload, "Something went wrong: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean status = jsonResponse.optBoolean("status", false);
                            String message = jsonResponse.optString("message", "No message received");
                            if (status) {
                                saveClockHistory(payload.getGeofence().getClockNumber(), payload.clockType(), payload.getTimeStamp());
                                String autoClockNotification = message + " Time: " + payload.getDateTime();
                                showNotification("MyWorkplace", autoClockNotification);
                            }
                        } catch (JSONException e) {
                            Log.e("sendLog", "Response JSON parsing error: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("sendLog", "JSON error: " + e.getMessage());
        }
    }


  private void logFailedPostRequest(String endpoint, AutoClockingPayload payload, String errorMessage) {
    try {
      String clockType = payload.clockType();
      int clockNumber = payload.getGeofence().getClockNumber();
      long timeStamp = payload.getTimeStamp();
      boolean alreadyClockedIn = hasClockedIn(clockNumber, timeStamp);
      boolean alreadyClockedOut = hasClockedOut(clockNumber, timeStamp);

      if (alreadyClockedIn &&  alreadyClockedOut) {
        Log.i(TAG, "Already done clocking in and out today");
        return;
      }
      Log.e(TAG,"RESULT LOCKEC: " + clockType.equals("in"));
      Log.e(TAG,"RESULT: " + (alreadyClockedIn && payload.clockType().equals("out")));
      if (clockType.equals("in") && !alreadyClockedIn) {

        String autoClock = "Clocking IN initiated to Clock: " + payload.getGeofence().getClockDescription() +
          " Time: " + payload.getDateTime();
        showNotification("MyWorkplace is offline", autoClock);
        saveClockHistory(clockNumber, "in", timeStamp);
        SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        JSONObject logData = new JSONObject();
        logData.put("failedEndpoint", endpoint);
        logData.put("payload", payload.toJson());
        logData.put("error", errorMessage);

        String existingLogs = prefs.getString("failedLogs", "[]");
        JSONArray logsArray = new JSONArray(existingLogs);
        logsArray.put(logData);

        prefs.edit().putString("failedLogs", logsArray.toString()).apply();
      }
      else if(alreadyClockedIn && payload.clockType().equals("out")) {
        String autoClock = "Clocking OUT initiated to Clock: " + payload.getGeofence().getClockDescription() +
          " Time: " + payload.getDateTime();
        showNotification("MyWorkplace is offline", autoClock);
        saveClockHistory(clockNumber, "out", timeStamp);
        SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        JSONObject logData = new JSONObject();
        logData.put("failedEndpoint", endpoint);
        logData.put("payload", payload.toJson());
        logData.put("error", errorMessage);

        String existingLogs = prefs.getString("failedLogs", "[]");
        JSONArray logsArray = new JSONArray(existingLogs);
        logsArray.put(logData);

        prefs.edit().putString("failedLogs", logsArray.toString()).apply();
      }
      else {
        Log.i(TAG, "WALAy Clocking");
      }
    } catch (Exception e) {
      Log.e("LogError", "Exception while logging failed request: " + e.getMessage());
    }
  }

  private void retrySendingAutoClocking(String token) {
        OkHttpClient client = new OkHttpClient();
        SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String existingLogs = prefs.getString("failedLogs", "[]");
        Log.e(TAG,"EXISTING: " + existingLogs);
        try {
            JSONArray logsArray = new JSONArray(existingLogs);
            if (logsArray.length() == 0) return;

            boolean allSuccessful = true;

            for (int i = 0; i < logsArray.length(); i++) {
                JSONObject logJson = logsArray.getJSONObject(i);
                String url = logJson.getString("failedEndpoint");
                JSONObject payloadJson = logJson.getJSONObject("payload");
                AutoClockingPayload payload = new AutoClockingPayload(
                        payloadJson.getString("empId"),
                        payloadJson.getString("lat"),
                        payloadJson.getString("lng"),
                        payloadJson.getBoolean("isInside"),
                        new GeofenceInformation(
                                payloadJson.getJSONObject("geofence").getDouble("lat"),
                                payloadJson.getJSONObject("geofence").getDouble("lng"),
                                payloadJson.getJSONObject("geofence").getDouble("radius"),
                                payloadJson.getJSONObject("geofence").getString("clockDescription"),
                                payloadJson.getJSONObject("geofence").getInt("clockNumber"),
                                payloadJson.getJSONObject("geofence").getString("locationCode"),
                                payloadJson.getJSONObject("geofence").getString("locationDescription")
                        ),
                        payloadJson.getLong("timeStamp")
                );

                RequestBody body = RequestBody.create(
                        payload.toJson().toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] requestSuccess = {false};

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AutoClocking", "Retry failed: " + e.getMessage());
                        latch.countDown();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            requestSuccess[0] = true;
                            assert response.body() != null;
                            String responseBody = response.body().string();
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                boolean status = jsonResponse.optBoolean("status", false);
                                String message = jsonResponse.optString("message", "No message received");
                                JSONObject responseData = jsonResponse.optJSONObject("data");
                            } catch (JSONException e) {
                                Log.e("sendLog", "Response JSON parsing error: " + e.getMessage());
                            }
                        }
                        latch.countDown();
                    }
                });

                latch.await();

                if (!requestSuccess[0]) {
                    allSuccessful = false;
                }
            }

            if (allSuccessful) {
                prefs.edit().remove("failedLogs").apply();
                Log.i("AutoClocking", "All logs sent successfully. Clearing failedLogs.");
                showNotification("MyWorkplace Syncing", "Offline clocking has been synced successfully.");
            }

        } catch (InterruptedException | JSONException e) {
            Log.e("AutoClocking", "Error during retry: " + e.getMessage());
        }
    }


  // create notification
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "capacitor_foreground_location_service_local_notification";
    private void showNotification(String title, String message) {

      SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
      boolean allowNotification = Boolean.TRUE.equals(prefs.getBoolean("allowNotification", false));
      if(allowNotification){
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "MyWorkplace",
            NotificationManager.IMPORTANCE_DEFAULT
          );
          channel.setDescription("Channel for app notifications");
          notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getContext(), getActivity().getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
          getContext(),
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        int iconId = getContext().getResources().getIdentifier(
          "local_notification_icon", // icon name defined in the app
          "drawable",
          getContext().getPackageName()
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
          .setContentTitle(title)
          .setContentText(message)
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setContentIntent(pendingIntent)
          .setAutoCancel(true);

        builder.setSmallIcon(iconId != 0 ? iconId : android.R.drawable.ic_dialog_info);

        // Show or update the notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
      }

    }
    private String timeStampToLocalDate(long timeStamp) {
        Date date = new Date(timeStamp); // timeStamp must be in milliseconds
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault());
        return formatter.format(date);
    }

  private boolean hasClockedIn(int clockNumber, long timeStamp) {
    SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    String logsJson = prefs.getString("clockHistory", "{}");
    try {
      JSONObject history = new JSONObject(logsJson);
      String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timeStamp));
      JSONObject dayEntry = history.optJSONObject(dateKey);
      if (dayEntry == null) return false;

      String clockKey = String.valueOf(clockNumber);
      JSONObject geofenceEntry = dayEntry.optJSONObject(clockKey);
      if (geofenceEntry == null) return false;

      return geofenceEntry.optBoolean("in", false);

    } catch (JSONException e) {
      Log.e("ClockHistory", "Error reading clock history: " + e.getMessage());
      return false;
    }
  }
  private boolean hasClockedOut(int clockNumber, long timeStamp) {
    SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    String logsJson = prefs.getString("clockHistory", "{}");
    try {
      JSONObject history = new JSONObject(logsJson);
      String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timeStamp));
      JSONObject dayEntry = history.optJSONObject(dateKey);
      if (dayEntry == null) return false;

      String clockKey = String.valueOf(clockNumber);
      JSONObject geofenceEntry = dayEntry.optJSONObject(clockKey);
      if (geofenceEntry == null) return false;
      Log.i(TAG, logsJson);
      return geofenceEntry.optBoolean("out", false);

    } catch (JSONException e) {
      Log.e("ClockHistory", "Error reading clock history: " + e.getMessage());
      return false;
    }
  }

    public void saveClockHistory(int clockNumber, String type, long timeStamp) {
        SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String logsJson = prefs.getString("clockHistory", "{}");

        try {
            // Parse existing history
            JSONObject history = new JSONObject(logsJson);

            // Format today's date key
            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timeStamp));

            // Create or get today's entry
            JSONObject dayEntry = history.optJSONObject(dateKey);
            if (dayEntry == null) {
                dayEntry = new JSONObject();
            }

            // Create or update geofence entry
            String clockKey = String.valueOf(clockNumber);
            JSONObject geofenceEntry = dayEntry.optJSONObject(clockKey);
            if (geofenceEntry == null) {
                geofenceEntry = new JSONObject();
            }

            if (type.equals("in")) {
                geofenceEntry.put("in", true);
            }
            if (type.equals("out")) {
                geofenceEntry.put("out", true);
            }

            dayEntry.put(clockKey, geofenceEntry);

            // Create new history with only today's date
            JSONObject newHistory = new JSONObject();
            newHistory.put(dateKey, dayEntry);

            // Save the new history
            prefs.edit().putString("clockHistory", newHistory.toString()).apply();
            Log.i("ClockHistory", "Saved clock history: " + newHistory.toString());

        } catch (JSONException e) {
            Log.e("ClockHistory", "Error saving clock history: " + e.getMessage());
        }
    }
    // wipe the clock history
    private void clearClockHistory() {
        SharedPreferences prefs = getContext()
            .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .remove("clockHistory")
            .apply();
        Log.i("ClockHistory", "Cleared clock history");
    }

} // end of plugin

class GeofenceInformation {
    private double lat;
    private double lng;
    private double radius;
    private String clockDescription;
    private int clockNumber;
    private String locationCode;
    private String locationDescription;

    public GeofenceInformation(double lat, double lng, double radius, String clockDescription, int clockNumber, String locationCode, String locationDescription) {
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.clockDescription = clockDescription;
        this.clockNumber = clockNumber;
        this.locationCode = locationCode;
        this.locationDescription = locationDescription;
    }

    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getRadius() { return radius; }
    public String getClockDescription() { return clockDescription; }
    public int getClockNumber() { return clockNumber; }
    public String getLocationCode() { return locationCode; }

    public String getLocationDescription() {
        return locationDescription;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("lat", lat);
        obj.put("lng", lng);
        obj.put("radius", radius);
        obj.put("clockDescription", clockDescription);
        obj.put("clockNumber", clockNumber);
        obj.put("locationCode", locationCode);
        obj.put("locationDescription", locationDescription);
        return obj;
    }
}

class GeofenceInformationDistance {
    private double distance;
    private GeofenceInformation geofence;

    public GeofenceInformationDistance(double distance, GeofenceInformation geofence) {
        this.distance = distance;
        this.geofence = geofence;
    }

    public double getDistance() { return distance; }
    public GeofenceInformation getGeofence() { return geofence; }
}

class User {
    private int userId;
    private String username;
    private String _token;

    public User(int userId, String username, String _token) {
        this.userId = userId;
        this.username = username;
        this._token = _token;
    }

    public int getUserId() { return userId; }

    public String get_token() {
        return _token;
    }

    public String getUsername() {
        return username;
    }
}

class LogsPayload {
    private String empId;
    private String lat;
    private String lng;
    private String message;
    private long timeStamp;

    public LogsPayload(String empId, String lat, String lng, String message, long timeStamp) {
        this.empId = empId;
        this.lat = lat;
        this.lng = lng;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("empId", empId);
        obj.put("lat", lat);
        obj.put("lng", lng);
        obj.put("message", message);
        obj.put("timeStamp", timeStamp);
        return obj;
    }
}

class AutoClockingPayload {
    private String empId;
    private String lat;
    private String lng;
    private String clockType;
    private boolean isInside;
    private GeofenceInformation geofence;
    private Long timeStamp;
    public AutoClockingPayload(String empId, String lat, String lng, boolean isInside, GeofenceInformation geofence, Long timeStamp){
        this.empId = empId;
        this.lat = lat;
        this.lng = lng;
        this.isInside = isInside;
        this.geofence = geofence;
        this.timeStamp = timeStamp;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("empId", empId);
        obj.put("lat", lat);
        obj.put("lng", lng);
        obj.put("isInside", isInside);
        obj.put("geofence", geofence.toJson());
        obj.put("timeStamp", timeStamp);
        return obj;
    }
    public String getLat() {
        return lat;
    }
    public String getLng() {
        return lng;
    }
    public String getDateTime() {
        Date date = new Date(timeStamp); // timeStamp must be in milliseconds
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault());
        return formatter.format(date);
    }
    public GeofenceInformation getGeofence() {
        return geofence;
    }
    public boolean isClockIn() {
        return isInside;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public String clockType(){
      return this.isInside ? "in": "out";
    }
}
