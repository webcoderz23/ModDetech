package com.moddetech

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AppInstalledService : BroadcastReceiver() {
    private val TAG = "AppInstalledService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "mod_detech_channel"
    private val PREFS_NAME = "ModDetechPrefs"
    private val NEW_APPS_KEY = "NewlyInstalledApps"
    
    // Google Play Store package
    private val PLAY_STORE_PACKAGE = "com.android.vending"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received: ${intent.action}")
        
        val packageName = intent.data?.schemeSpecificPart
        Log.d(TAG, "Package from intent: $packageName")

        // Log all extras in the intent
        intent.extras?.keySet()?.forEach { key ->
            Log.d(TAG, "Intent extra - $key: ${intent.extras?.get(key)}")
        }

        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            Log.d(TAG, "New app installation detected: $packageName")
            
            if (packageName != null) {
                // Check if the app is from Play Store or sideloaded
                if (isAppSideloaded(context, packageName)) {
                    Log.d(TAG, "App is sideloaded (not from Play Store): $packageName")
                    
                    // Store the newly installed app
                    storeNewlyInstalledApp(context, packageName)
                    
                    // Send a notification
                    showNotification(context, packageName)
                } else {
                    Log.d(TAG, "App is from Play Store, ignoring: $packageName")
                }
            }
        } else {
            Log.d(TAG, "Not a package added action. Action was: ${intent.action}")
        }
    }
    
    /**
     * Determines if an app was installed from a source other than the Google Play Store
     */
    private fun isAppSideloaded(context: Context, packageName: String): Boolean {
        try {
            val pm = context.packageManager
            val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            
            Log.d(TAG, "Installer package for $packageName is: $installerPackageName")
            
            // If installer is null or not Play Store, it's sideloaded
            return installerPackageName == null || installerPackageName != PLAY_STORE_PACKAGE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking installer package", e)
            // If we can't determine the installer, assume it's sideloaded
            return true
        }
    }
    
    private fun storeNewlyInstalledApp(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newAppsSet = prefs.getStringSet(NEW_APPS_KEY, HashSet<String>())?.toMutableSet() ?: mutableSetOf()
        
        // Add the new package
        newAppsSet.add(packageName)
        
        // Save back to preferences
        val editor = prefs.edit()
        editor.putStringSet(NEW_APPS_KEY, newAppsSet)
        val success = editor.commit()  // Using commit instead of apply to get immediate result
        
        Log.d(TAG, "Stored new app: $packageName, total new apps: ${newAppsSet.size}, save success: $success")
        
        // Log all stored apps for debugging
        Log.d(TAG, "Currently stored apps: ${newAppsSet.joinToString(", ")}")
    }
    
    private fun showNotification(context: Context, packageName: String) {
        createNotificationChannel(context)
        
        // Create intent to open warning activity when notification is tapped
        val intent = Intent(context, AppWarningActivity::class.java).apply {
            putExtra("package_name", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get app name if possible
        val appName = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app name for $packageName", e)
            packageName
        }
        
        Log.d(TAG, "Showing notification for app: $appName (package: $packageName)")
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mod app installed detech")
            .setContentText("$appName has been installed")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d(TAG, "Notification sent")
    }
    
    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ModDetech Notifications"
            val descriptionText = "Notifications for newly installed applications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
} 