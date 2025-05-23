package com.fusion5.capacitorforegroundlocationservice;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

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
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }
                )
        }
)
public class CapacitorForegroundLocationServicePlugin extends Plugin {

    private PluginCall savedCall;
    @PluginMethod
    public void requestPermission(PluginCall call) {
        Log.e("PERMISSION","REquest Permission First");
        savedCall = call;

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // greater than android 10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // should greater than 34
                if (hasAllPermissions()) {
                    JSObject result = new JSObject();
                    result.put("granted", true);
                    Log.e("PERMISSION","granted should be true");
                    call.resolve(result);
                } else {
                    Log.e("PERMISSION","grequest Again?");
                    requestAllPermissions();
                }
            } else {
                // this is for android 12 which is working fine
                if(hasAllPermissionsbelowCake()){
                    JSObject result = new JSObject();
                    result.put("granted", true);
                    Log.e("PERMISSION","granted should be true");
                    call.resolve(result);
                } else {
                    requestAllPermissions();
                }
            }
        }*/

        if (Build.VERSION.SDK_INT >= 34) {
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

    private void sendLocationUpdate(Location location) {
        JSObject data = new JSObject();
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        notifyListeners("locationUpdate", data);
    }
    
}
