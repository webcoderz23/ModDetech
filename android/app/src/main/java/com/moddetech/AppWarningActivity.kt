package com.moddetech

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AppWarningActivity : AppCompatActivity() {
    private val TAG = "AppWarningActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_warning)
        
        val packageName = intent.getStringExtra("package_name") ?: ""
        Log.d(TAG, "Opened warning for package: $packageName")
        
        if (packageName.isEmpty()) {
            Log.e(TAG, "No package name provided")
            finish()
            return
        }
        
        // Get app info
        val appNameTextView = findViewById<TextView>(R.id.app_name)
        val appIconView = findViewById<ImageView>(R.id.app_icon)
        val packageNameTextView = findViewById<TextView>(R.id.package_name)
        
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val appIcon: Drawable = pm.getApplicationIcon(appInfo)
            
            appNameTextView.text = appName
            appIconView.setImageDrawable(appIcon)
            packageNameTextView.text = packageName
            
            Log.d(TAG, "Loaded app info: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading app info", e)
            appNameTextView.text = "Unknown App"
            packageNameTextView.text = packageName
            appIconView.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert))
        }
        
        // Set up buttons
        val openAppButton = findViewById<Button>(R.id.open_app_button)
        openAppButton.setOnClickListener {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Log.d(TAG, "No launch intent for package $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching app", e)
            }
            finish()
        }
        
        val uninstallButton = findViewById<Button>(R.id.uninstall_button)
        uninstallButton.setOnClickListener {
            try {
                // Try two different approaches to ensure compatibility across Android versions
                try {
                    // First attempt with ACTION_DELETE (more widely supported)
                    val deleteIntent = Intent(Intent.ACTION_DELETE)
                    deleteIntent.data = android.net.Uri.parse("package:$packageName")
                    deleteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(deleteIntent)
                    Log.d(TAG, "Uninstall intent sent using ACTION_DELETE for package: $packageName")
                } catch (e: Exception) {
                    // If that fails, try with ACTION_UNINSTALL_PACKAGE
                    Log.e(TAG, "Failed with ACTION_DELETE, trying ACTION_UNINSTALL_PACKAGE", e)
                    val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                    uninstallIntent.data = android.net.Uri.parse("package:$packageName")
                    uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(uninstallIntent)
                    Log.d(TAG, "Uninstall intent sent using ACTION_UNINSTALL_PACKAGE for package: $packageName")
                }
                
                // Don't finish the activity yet, let user come back after uninstall dialog
            } catch (e: Exception) {
                Log.e(TAG, "Error uninstalling app (both methods failed)", e)
                // Show a toast that uninstall failed
                android.widget.Toast.makeText(
                    this,
                    "Error uninstalling app: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
        
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            finish()
        }
    }
} 