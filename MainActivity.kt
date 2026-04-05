package dev.krinry.jarvis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.krinry.jarvis.ui.theme.JarvisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val REQUIRED_PERMISSIONS_MIN_SDK_30 = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        private const val REQUIRED_PERMISSIONS_ABOVE_SDK_30 = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    
    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkAvailable = mutableStateOf(true)
    private var isDarkThemeEnabled = mutableStateOf(false)
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
        } else {
            Log.w(TAG, "Some permissions were denied")
            showPermissionDeniedMessage()
        }
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            isNetworkAvailable.value = true
            Log.d(TAG, "Network available")
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            isNetworkAvailable.value = false
            Log.w(TAG, "Network lost")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Log.d(TAG, "Network capabilities changed - Has Internet: $hasInternet")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        initializeNetworkMonitoring()
        logLifecycleEvent("onCreate")
        requestRequiredPermissions()
        handleIntentData(intent)
        
        setContent {
            isDarkThemeEnabled.value = isSystemInDarkMode()
            JarvisTheme(darkTheme = isDarkThemeEnabled.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        isNetworkAvailable = isNetworkAvailable.value,
                        isDarkTheme = isDarkThemeEnabled.value,
                        onThemeToggle = { isDarkThemeEnabled.value = !isDarkThemeEnabled.value }
                    )
                }
            }
        }
        
        setupLifecycleObserver()
    }
    
    private fun initializeNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }
    
    private fun requestRequiredPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            REQUIRED_PERMISSIONS_ABOVE_SDK_30
        } else {
            REQUIRED_PERMISSIONS_MIN_SDK_30
        }
        
        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsNeeded.joinToString()}")
            permissionsLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
    
    private fun handleIntentData(intent: Intent?) {
        try {
            val action = intent?.action
            val data = intent?.data
            val extras = intent?.extras
            
            Log.d(TAG, "Intent Action: $action, Data: $data")
            
            when {
                action == Intent.ACTION_VIEW && data != null -> {
                    Log.d(TAG, "Deep link received: ${data.toString()}")
                }
                extras != null -> {
                    Log.d(TAG, "Intent extras received: ${extras.keySet()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent data", e)
        }
    }
    
    private fun setupLifecycleObserver() {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> logLifecycleEvent("onStart")
                Lifecycle.Event.ON_RESUME -> logLifecycleEvent("onResume")
                Lifecycle.Event.ON_PAUSE -> logLifecycleEvent("onPause")
                Lifecycle.Event.ON_STOP -> logLifecycleEvent("onStop")
                Lifecycle.Event.ON_DESTROY -> logLifecycleEvent("onDestroy")
                else -> {}
            }
        })
    }
    
    private fun logLifecycleEvent(eventName: String) {
        Log.d(TAG, "Lifecycle Event: $eventName at ${System.currentTimeMillis()}")
        lifecycleScope.launch {
            // Can be extended for analytics tracking
        }
    }
    
    private fun showPermissionDeniedMessage() {
        Log.w(TAG, "Required permissions were denied by user")
        // Can be extended to show user-facing message
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
    }
}

@Composable
fun MainNavigation(
    isNetworkAvailable: Boolean,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "agent_settings"
    ) {
        composable("agent_settings") {
            AgentSettingsScreen(
                onBack = { /* Handle back navigation */ },
                isNetworkAvailable = isNetworkAvailable,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
        }
    }
}