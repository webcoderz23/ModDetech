package com.moddetech

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {
  private val TAG = "MainActivity"
  private val appInstalledReceiver = AppInstalledService()

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "ModDetech"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
      
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Register broadcast receiver for package added
    val filter = IntentFilter().apply {
      addAction("android.intent.action.PACKAGE_ADDED")
      addDataScheme("package")
    }
    
    try {
      Log.d(TAG, "Registering AppInstalledService broadcast receiver")
      registerReceiver(appInstalledReceiver, filter)
      Log.d(TAG, "AppInstalledService registered successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error registering AppInstalledService receiver: ${e.message}", e)
    }
  }
  
  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(appInstalledReceiver)
      Log.d(TAG, "AppInstalledService unregistered")
    } catch (e: Exception) {
      Log.e(TAG, "Error unregistering AppInstalledService: ${e.message}", e)
    }
  }
}
