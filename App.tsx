/**
 * App Detection Module
 * Detects and shows notifications for newly installed apps
 */

import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Switch,
  Alert,
  PermissionsAndroid,
  Platform,
  TouchableOpacity,
  Linking,
  NativeModules,
  FlatList,
  TextInput,
  ScrollView,
} from 'react-native';
import DeviceInfo from 'react-native-device-info';

// Interface for the native module
interface NewAppsDetectionInterface {
  getNewlyInstalledApps(): Promise<AppInfo[]>;
  clearNewlyInstalledApps(): Promise<boolean>;
  getAllInstalledApps(): Promise<AppInfo[]>;
  manuallyAddPackage(packageName: string): Promise<boolean>;
}

// Interface for app info
interface AppInfo {
  packageName: string;
  appName: string;
}

// Get the native module
const { NewAppsDetection } = NativeModules as {
  NewAppsDetection: NewAppsDetectionInterface;
};

function App(): React.JSX.Element {
  const [isEnabled, setIsEnabled] = useState(true);
  const [status, setStatus] = useState('Monitoring new app installations...');
  const [newlyInstalledApps, setNewlyInstalledApps] = useState<AppInfo[]>([]);
  const [initialInstalledApps, setInitialInstalledApps] = useState<string[]>([]);
  const [debugPackage, setDebugPackage] = useState('com.example.app');
  const [error, setError] = useState<string | null>(null);

  // Function to open usage stats settings
  const openUsageAccessSettings = () => {
    if (Platform.OS === 'android') {
      try {
        Linking.openSettings();
      } catch (error) {
        console.error('Error opening settings:', error);
        setError('Error opening settings: ' + (error as Error).message);
      }
    }
  };

  // Function to add package manually for testing
  const addPackageManually = async () => {
    try {
      setStatus('Adding package manually: ' + debugPackage);
      const success = await NewAppsDetection.manuallyAddPackage(debugPackage);
      if (success) {
        setStatus('Package added successfully, refreshing...');
        checkForNewApps();
      } else {
        setStatus('Failed to add package manually');
      }
    } catch (error) {
      console.error('Error adding package manually:', error);
      setError('Error adding package: ' + (error as Error).message);
    }
  };

  // Get all installed apps for debugging
  const listAllInstalledApps = async () => {
    try {
      setStatus('Fetching all installed apps...');
      const apps = await NewAppsDetection.getAllInstalledApps();
      if (apps.length > 0) {
        setStatus(`Found ${apps.length} installed apps`);
        if (apps.length > 0) {
          // Use first app for debugging
          setDebugPackage(apps[0].packageName);
        }
        Alert.alert(
          'Installed Apps',
          `Found ${apps.length} installed apps.\nFirst few: ${apps.slice(0, 5).map(app => app.appName).join(', ')}...`
        );
      } else {
        setStatus('No installed apps found');
      }
    } catch (error) {
      console.error('Error listing installed apps:', error);
      setError('Error listing apps: ' + (error as Error).message);
    }
  };

  // Function to request necessary permissions
  const requestPermissions = async () => {
    try {
      // Request notification permission
      if (Platform.OS === 'android' && Platform.Version >= 33) {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
        );
      }

      // Request package usage stats permission (for app detection)
      if (Platform.OS === 'android') {
        try {
          await PermissionsAndroid.request(
            'android.permission.PACKAGE_USAGE_STATS' as any,
            {
              title: 'Package Usage Stats Permission',
              message: 'This app needs access to your app usage stats to detect newly installed apps.',
              buttonPositive: 'OK',
            }
          );
          
          Alert.alert(
            'Additional Permission Required',
            'Please enable Usage Access for this app in Settings',
            [
              {
                text: 'Open Settings',
                onPress: openUsageAccessSettings,
              },
              {
                text: 'Later',
                onPress: () => console.log('Later Pressed'),
                style: 'cancel',
              },
            ]
          );
        } catch (error) {
          console.error('Error requesting package usage stats permission:', error);
          setError('Error requesting permissions: ' + (error as Error).message);
        }
      }
    } catch (error) {
      console.error('Error requesting permissions:', error);
      setError('Error requesting permissions: ' + (error as Error).message);
    }
  };

  // Check for newly installed apps
  const checkForNewApps = async () => {
    try {
      if (!isEnabled) return;

      setStatus('Checking for newly installed apps...');
      const newApps = await NewAppsDetection.getNewlyInstalledApps();
      if (newApps.length > 0) {
        setNewlyInstalledApps(newApps);
        
        // Log newly detected apps (but don't try to send notifications - native side handles this)
        newApps.forEach(app => {
          console.log('New app detected:', app.appName);
        });
        
        setStatus(`${newApps.length} new app(s) detected`);
      } else {
        setStatus('No new apps detected');
      }
    } catch (error) {
      console.error('Error checking for new apps:', error);
      setError('Error checking for new apps: ' + (error as Error).message);
    }
  };

  // Reset newly installed apps
  const resetNewlyInstalledApps = async () => {
    try {
      const success = await NewAppsDetection.clearNewlyInstalledApps();
      if (success) {
        setNewlyInstalledApps([]);
        setStatus('App monitoring reset.');
      } else {
        setStatus('Failed to reset monitoring');
      }
    } catch (error) {
      console.error('Error resetting newly installed apps:', error);
      setError('Error resetting: ' + (error as Error).message);
    }
  };

  // Toggle app monitoring
  const toggleSwitch = () => {
    setIsEnabled(previousState => !previousState);
    setStatus(isEnabled ? 'App monitoring disabled' : 'Monitoring new app installations...');
  };

  // Initialize on mount
  useEffect(() => {
    const initializeApp = async () => {
      try {
        await requestPermissions();
        
        // Initial app check
        await checkForNewApps();
      } catch (error) {
        console.error('Error during initialization:', error);
        setError('Error during initialization: ' + (error as Error).message);
      }
    };
    
    initializeApp();

    // Set interval to check for new apps (every 10 seconds)
    const interval = setInterval(checkForNewApps, 10000);

    // Clean up on unmount
    return () => clearInterval(interval);
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView>
        <View style={styles.header}>
          <Text style={styles.title}>App Installation Detector</Text>
        </View>
        
        <View style={styles.settingContainer}>
          <Text style={styles.settingText}>Monitor app installations</Text>
          <Switch
            trackColor={{ false: '#767577', true: '#81b0ff' }}
            thumbColor={isEnabled ? '#f5dd4b' : '#f4f3f4'}
            ios_backgroundColor="#3e3e3e"
            onValueChange={toggleSwitch}
            value={isEnabled}
          />
        </View>
        
        <View style={styles.statusContainer}>
          <Text style={styles.statusText}>{status}</Text>
          
          {error && (
            <View style={styles.errorContainer}>
              <Text style={styles.errorText}>{error}</Text>
              <TouchableOpacity 
                style={styles.smallButton}
                onPress={() => setError(null)}
              >
                <Text style={styles.buttonText}>Clear Error</Text>
              </TouchableOpacity>
            </View>
          )}
          
          {newlyInstalledApps.length > 0 ? (
            <>
              <Text style={styles.listHeader}>Newly Installed Apps:</Text>
              <FlatList
                data={newlyInstalledApps}
                keyExtractor={(item) => item.packageName}
                renderItem={({item}) => (
                  <View style={styles.appItem}>
                    <Text style={styles.appName}>{item.appName}</Text>
                    <Text style={styles.packageName}>{item.packageName}</Text>
                  </View>
                )}
                style={styles.flatList}
                nestedScrollEnabled
              />
              <TouchableOpacity 
                style={styles.resetButton}
                onPress={resetNewlyInstalledApps}
              >
                <Text style={styles.buttonText}>Reset List</Text>
              </TouchableOpacity>
            </>
          ) : (
            <Text style={styles.emptyText}>No newly installed apps detected yet</Text>
          )}
        </View>
        
        <View style={styles.debugContainer}>
          <Text style={styles.debugHeader}>Debug Tools</Text>
          <View style={styles.debugRow}>
            <TextInput
              style={styles.debugInput}
              value={debugPackage}
              onChangeText={setDebugPackage}
              placeholder="Enter package name"
            />
            <TouchableOpacity 
              style={styles.smallButton}
              onPress={addPackageManually}
            >
              <Text style={styles.buttonText}>Add</Text>
            </TouchableOpacity>
          </View>
          
          <View style={styles.buttonRow}>
            <TouchableOpacity 
              style={styles.smallButton}
              onPress={checkForNewApps}
            >
              <Text style={styles.buttonText}>Force Check</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={styles.smallButton}
              onPress={listAllInstalledApps}
            >
              <Text style={styles.buttonText}>List All Apps</Text>
            </TouchableOpacity>
          </View>
        </View>
        
        <TouchableOpacity 
          style={styles.permissionButton}
          onPress={openUsageAccessSettings}
        >
          <Text style={styles.buttonText}>Open Permission Settings</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  header: {
    backgroundColor: '#4A6572',
    padding: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 22,
    color: 'white',
    fontWeight: 'bold',
  },
  settingContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: 'white',
    marginTop: 20,
    marginHorizontal: 15,
    borderRadius: 10,
    elevation: 2,
  },
  settingText: {
    fontSize: 18,
    fontWeight: '500',
  },
  statusContainer: {
    padding: 20,
    backgroundColor: 'white',
    marginTop: 20,
    marginHorizontal: 15,
    borderRadius: 10,
    elevation: 2,
    minHeight: 100,
  },
  statusText: {
    fontSize: 16,
    color: '#555',
    textAlign: 'center',
    marginBottom: 10,
  },
  errorContainer: {
    backgroundColor: '#ffebee',
    padding: 10,
    borderRadius: 5,
    marginVertical: 10,
  },
  errorText: {
    color: '#d32f2f',
    fontSize: 14,
    marginBottom: 5,
  },
  listHeader: {
    fontSize: 16,
    fontWeight: 'bold',
    marginTop: 10,
    marginBottom: 5,
  },
  appItem: {
    backgroundColor: '#f8f8f8',
    padding: 10,
    marginVertical: 5,
    borderRadius: 5,
  },
  appName: {
    fontSize: 16,
    fontWeight: '500',
  },
  packageName: {
    fontSize: 12,
    color: '#777',
  },
  emptyText: {
    textAlign: 'center',
    color: '#777',
    fontStyle: 'italic',
    marginTop: 20,
  },
  permissionButton: {
    backgroundColor: '#4A6572',
    margin: 15,
    borderRadius: 10,
    padding: 15,
    alignItems: 'center',
  },
  resetButton: {
    backgroundColor: '#FF6B6B',
    marginTop: 10,
    borderRadius: 5,
    padding: 10,
    alignItems: 'center',
  },
  debugContainer: {
    padding: 20,
    backgroundColor: '#e8f5e9',
    marginHorizontal: 15,
    marginTop: 20,
    borderRadius: 10,
  },
  debugHeader: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#2e7d32',
  },
  debugRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  debugInput: {
    flex: 1,
    backgroundColor: 'white',
    borderRadius: 5,
    padding: 8,
    marginRight: 10,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  smallButton: {
    backgroundColor: '#388e3c',
    borderRadius: 5,
    padding: 8,
    minWidth: 80,
    alignItems: 'center',
  },
  buttonText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '500',
  },
  flatList: {
    maxHeight: 200,
  }
});

export default App;
