package com.moddetech;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewAppsDetectionModule extends ReactContextBaseJavaModule {
    private static final String TAG = "NewAppsDetectionModule";
    private static final String PREFS_NAME = "ModDetechPrefs";
    private static final String NEW_APPS_KEY = "NewlyInstalledApps";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    private final ReactApplicationContext reactContext;

    public NewAppsDetectionModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Log.d(TAG, "NewAppsDetectionModule initialized");
    }

    @Override
    public String getName() {
        return "NewAppsDetection";
    }

    /**
     * Determines if an app was installed from a source other than the Google Play Store
     */
    private boolean isAppSideloaded(String packageName) {
        try {
            PackageManager pm = reactContext.getPackageManager();
            String installerPackageName;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                installerPackageName = pm.getInstallSourceInfo(packageName).getInstallingPackageName();
            } else {
                installerPackageName = pm.getInstallerPackageName(packageName);
            }
            
            Log.d(TAG, "Installer package for " + packageName + " is: " + installerPackageName);
            
            // If installer is null or not Play Store, it's sideloaded
            return installerPackageName == null || !PLAY_STORE_PACKAGE.equals(installerPackageName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking installer package", e);
            // If we can't determine the installer, assume it's sideloaded
            return true;
        }
    }

    @ReactMethod
    public void getNewlyInstalledApps(Promise promise) {
        try {
            Log.d(TAG, "getNewlyInstalledApps called from JS");
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> newPackages = prefs.getStringSet(NEW_APPS_KEY, new HashSet<>());
            
            Log.d(TAG, "Retrieved " + newPackages.size() + " newly installed packages");
            for (String pkg : newPackages) {
                Log.d(TAG, "Retrieved package: " + pkg);
            }
            
            WritableArray newAppsArray = Arguments.createArray();
            PackageManager pm = reactContext.getPackageManager();
            
            for (String packageName : newPackages) {
                try {
                    // We only add sideloaded apps to the array, Play Store apps are filtered
                    if (isAppSideloaded(packageName)) {
                        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                        String appName = pm.getApplicationLabel(appInfo).toString();
                        
                        WritableMap appData = Arguments.createMap();
                        appData.putString("packageName", packageName);
                        appData.putString("appName", appName);
                        
                        newAppsArray.pushMap(appData);
                        Log.d(TAG, "Added to result: " + appName + " (" + packageName + ")");
                    } else {
                        Log.d(TAG, "Skipping Play Store app: " + packageName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting info for package: " + packageName, e);
                    // Still include the package even if we can't get the app name
                    WritableMap appData = Arguments.createMap();
                    appData.putString("packageName", packageName);
                    appData.putString("appName", packageName); // Fallback to using package name as app name
                    
                    newAppsArray.pushMap(appData);
                    Log.d(TAG, "Added to result with fallback: " + packageName);
                }
            }
            
            Log.d(TAG, "Returning " + newAppsArray.size() + " results to JS");
            promise.resolve(newAppsArray);
        } catch (Exception e) {
            Log.e(TAG, "Error getting newly installed apps", e);
            promise.reject("ERROR", "Failed to get newly installed apps: " + e.getMessage());
        }
    }
    
    @ReactMethod
    public void clearNewlyInstalledApps(Promise promise) {
        try {
            Log.d(TAG, "clearNewlyInstalledApps called from JS");
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean success = prefs.edit().remove(NEW_APPS_KEY).commit();
            Log.d(TAG, "Cleared newly installed apps, success: " + success);
            promise.resolve(success);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing newly installed apps", e);
            promise.reject("ERROR", "Failed to clear newly installed apps: " + e.getMessage());
        }
    }
    
    @ReactMethod
    public void getAllInstalledApps(Promise promise) {
        try {
            Log.d(TAG, "getAllInstalledApps called from JS");
            PackageManager pm = reactContext.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            
            WritableArray appsArray = Arguments.createArray();
            int count = 0;
            
            for (PackageInfo packageInfo : packages) {
                // Filter out system apps if needed
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    try {
                        String packageName = packageInfo.packageName;
                        
                        // Only include sideloaded apps
                        if (isAppSideloaded(packageName)) {
                            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                            String appName = pm.getApplicationLabel(appInfo).toString();
                            
                            WritableMap appData = Arguments.createMap();
                            appData.putString("packageName", packageName);
                            appData.putString("appName", appName);
                            
                            appsArray.pushMap(appData);
                            count++;
                            if (count % 10 == 0) {
                                Log.d(TAG, "Processed " + count + " apps");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting info for package: " + packageInfo.packageName, e);
                    }
                }
            }
            
            Log.d(TAG, "Returning " + appsArray.size() + " sideloaded apps to JS");
            promise.resolve(appsArray);
        } catch (Exception e) {
            Log.e(TAG, "Error getting all installed apps", e);
            promise.reject("ERROR", "Failed to get all installed apps: " + e.getMessage());
        }
    }
    
    @ReactMethod
    public void manuallyAddPackage(String packageName, Promise promise) {
        try {
            Log.d(TAG, "manuallyAddPackage called from JS: " + packageName);
            
            // Store in SharedPreferences
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> newPackages = prefs.getStringSet(NEW_APPS_KEY, new HashSet<>());
            Set<String> updatedPackages = new HashSet<>(newPackages);
            updatedPackages.add(packageName);
            
            boolean success = prefs.edit().putStringSet(NEW_APPS_KEY, updatedPackages).commit();
            
            Log.d(TAG, "Added package manually: " + packageName + ", success: " + success);
            Log.d(TAG, "Updated packages count: " + updatedPackages.size());
            
            promise.resolve(success);
        } catch (Exception e) {
            Log.e(TAG, "Error manually adding package", e);
            promise.reject("ERROR", "Failed to add package: " + e.getMessage());
        }
    }
} 