package com.tanyakron.cordovadetectmocklocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;

public class CordovaDetectMockLocationPlugin extends CordovaPlugin {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private CallbackContext callbackContext; // Member variable to store the callback context

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext; // Store the callback context
        if ("detectMockLocation".equals(action)) {
            if (checkLocationPermission()) {
                detectMockLocation(callbackContext);
            } else {
                requestLocationPermission();
            }
            return true;
        }

        if ("detectAllowMockLocation".equals(action)) {
            detectAllowMockLocation(callbackContext);
            return true;
        }

        return false;
    }

    @SuppressLint("MissingPermission")
    private void detectMockLocation(CallbackContext callbackContext) {
        JSONObject resultJSON = new JSONObject();

        LocationManager locationManager = (LocationManager) cordova.getActivity()
                .getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            handleError(resultJSON, "LOCATION_MANAGER_OBJ_NOT_FOUND", "\"locationManager\" not found.",
                    callbackContext);
        } else {
            Location location = null;
            String usedProvider = "";
            List<String> providers = locationManager.getProviders(true);

            for (String provider : providers) {
                Location locationFromProvider = locationManager.getLastKnownLocation(provider);

                if (locationFromProvider != null
                        && (location == null || locationFromProvider.getAccuracy() < location.getAccuracy())) {
                    location = locationFromProvider;
                    usedProvider = provider;
                }
            }

            if (location != null) {
                boolean isMockLocation = (Build.VERSION.SDK_INT < 31) ? location.isFromMockProvider()
                        : location.isMock();

                try {
                    resultJSON.put("isMockLocation", isMockLocation);
                    Log.d("CordovaDetectMockLocation", "Used location provider is: " + usedProvider);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                handleError(resultJSON, "LOCATION_OBJ_NOT_FOUND",
                        "\"location\" object not found. (lastKnownLocation may be null)", callbackContext);
            }
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, resultJSON);
        callbackContext.sendPluginResult(result);
    }

    private void detectAllowMockLocation(CallbackContext callbackContext) {
        boolean isAllowMockLocation;
        JSONObject resultJSON = new JSONObject();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            isAllowMockLocation = !Settings.Secure.getString(
                    cordova.getActivity().getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        } else {
            isAllowMockLocation = false;
        }

        try {
            resultJSON.put("isAllowMockLocation", isAllowMockLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, resultJSON);
        callbackContext.sendPluginResult(result);
    }

    private boolean checkLocationPermission() {
        if (cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return true;
        }
        return false;
    }

    private void requestLocationPermission() {
        cordova.requestPermission(this, REQUEST_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                detectMockLocation(callbackContext); // Use the stored callback context
            } else {
                handleError(new JSONObject(), "PERMISSION_DENIED", "Location permission denied.", callbackContext);
            }
        }
    }

    private void handleError(JSONObject resultJSON, String code, String message, CallbackContext callbackContext) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);
            resultJSON.put("isMockLocation", "");
            resultJSON.put("error", error);
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, resultJSON);
            callbackContext.sendPluginResult(result);
        } catch (JSONException e) {
            Log.e("CordovaDetectMockLocation", "Error creating JSON error object", e);
        }
    }
}
