package com.fusion5.capacitorforegroundlocationservice;

import static com.fusion5.capacitorforegroundlocationservice.GeoUtils.calculateDistance;

import android.Manifest;
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

    private boolean isAppInForeground = true;
    int interval;
    int distanceFilter;
    String notificationTitle;
    String notificationText;

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
                notificationText = "Tracking location in foreground...";
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
        Intent serviceIntent = new Intent(context, CapacitorForegroundLocationService.class);
        serviceIntent.putExtra("interval", interval);
        serviceIntent.putExtra("distanceFilter", distanceFilter);
        serviceIntent.putExtra("notificationTitle", notificationTitle);
        serviceIntent.putExtra("notificationText", notificationText);
        ContextCompat.startForegroundService(context, serviceIntent);
        call.resolve();
    }

    @PluginMethod
    public void stopService(PluginCall call) {
        Context context = getContext();
        Intent serviceIntent = new Intent(context, CapacitorForegroundLocationService.class);
        context.stopService(serviceIntent);
        call.resolve();
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
                        "Employee just entered the geofence " + geo.getClockDescription()
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
                    "Employee is " + distanceToClock + " closer to " + closest.getGeofence().getClockDescription()
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

    private void sendAutoClocking(String token, String url, AutoClockingPayload payload){

        retrySendingAutoClocking(token);

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
                  logFailedPostRequest(url, payload, "JSON error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                      logFailedPostRequest(url, payload, "Error" + response.code());
                    } else {
                        Log.i("sendLog", "Log sent successfully");
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("sendLog", "JSON error: " + e.getMessage());
        }
    }

  private void logFailedPostRequest(String endpoint, AutoClockingPayload payload, String errorMessage) {
    try {
      Log.e("LogError", "Failed to log failed request: " + errorMessage);
      SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);

      JSONObject logData = new JSONObject();
      logData.put("failedEndpoint", endpoint);
      logData.put("payload", payload.toJson());
      logData.put("error", errorMessage);



      String existingLogs = prefs.getString("failedLogs", "[]");
      JSONArray logsArray = new JSONArray(existingLogs);
      logsArray.put(logData);

      SharedPreferences.Editor editor = prefs.edit();
      editor.putString("failedLogs", logsArray.toString());
      editor.apply();

    } catch (Exception e) {
      Log.e("LogError", "Failed to log failed request: " + e.getMessage());
    }
  }

  private void retrySendingAutoClocking(String token){
    OkHttpClient client = new OkHttpClient();
    SharedPreferences prefs = getContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    String existingLogs = prefs.getString("failedLogs", "[]");
    if(existingLogs.isEmpty()){
      return;
    }
    try {

      JSONArray logsArray = new JSONArray(existingLogs);
      Log.e("LOGS", "retrySendingAutoClocking:" + logsArray.toString());
      for (int i = 0; i < logsArray.length(); i++) {
        JSONObject logJson = logsArray.getJSONObject(i);
        String url = logJson.getString("failedEndpoint");

        RequestBody body = RequestBody.create(
          logJson.getJSONObject("payload").toString(),
          MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
          .url(url)
          .addHeader("Authorization", "Bearer " + token)
          .addHeader("Content-Type", "application/json")
          .post(body)
          .build();

        CountDownLatch latch = new CountDownLatch(1);

        client.newCall(request).enqueue(new Callback() {
          boolean success = false;

          @Override
          public void onFailure(@NonNull Call call, @NonNull IOException e) {
            latch.countDown();
          }

          @Override
          public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.isSuccessful()) {
              SharedPreferences.Editor editor = prefs.edit();
              editor.remove("failedLogs");
              editor.apply();
            }
            latch.countDown();
          }
        });

        latch.await(); // wait for response before next retry
      }

    } catch (InterruptedException e) {
      Log.e("sendLog", "JSON error: " + e.getMessage());
    } catch (JSONException e) {
      Log.e("sendLog", "JSON error: " + e.getMessage());
    }
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

    public LogsPayload(String empId, String lat, String lng, String message) {
        this.empId = empId;
        this.lat = lat;
        this.lng = lng;
        this.message = message;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("empId", empId);
        obj.put("lat", lat);
        obj.put("lng", lng);
        obj.put("message", message);
        return obj;
    }
}

class AutoClockingPayload {
    private String empId;
    private String lat;
    private String lng;
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
}
