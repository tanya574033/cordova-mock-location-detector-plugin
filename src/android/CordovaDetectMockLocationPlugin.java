package com.tanyakron.cordovadetectmocklocation;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.annotation.SuppressLint;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class CordovaDetectMockLocationPlugin extends CordovaPlugin {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient mFusedLocationClient;
    private CallbackContext permissionCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("detectMockLocation")) {
            this.permissionCallbackContext = callbackContext;
            this.detectMockLocation(callbackContext);
            return true;
        }
        return false;
    }

    private void detectMockLocation(CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showPermissionAlert(activity);
        } else {
            boolean result = isLocationFromMockProvider(activity);
            callbackContext.success("Mock location detection result: " + result);
        }
    }

    private void showPermissionAlert(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Location Permission Needed")
                .setMessage(
                        "This app needs the Location permission to detect mock locations. Please grant the permission to proceed.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity,
                                new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION },
                                LOCATION_PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        permissionCallbackContext.error("Location permissions are not granted.");
                    }
                })
                .create()
                .show();
    }

    @SuppressLint("ObsoleteSdkInt")
    public boolean isLocationFromMockProvider(Activity activity) {
        boolean isFromMockProvider = false;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    if (Build.VERSION.SDK_INT <= 30) {
                        isFromMockProvider = location.isFromMockProvider();
                    } else if (Build.VERSION.SDK_INT >= 31) {
                        isFromMockProvider = location.isMock();
                    }
                }
            }
        };

        return isFromMockProvider;
    }
}
