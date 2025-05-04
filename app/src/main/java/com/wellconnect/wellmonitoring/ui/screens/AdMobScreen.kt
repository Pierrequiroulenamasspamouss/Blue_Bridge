package com.wellconnect.wellmonitoring.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AdMobScreen"
// Test Ad IDs from Google (for development purposes only)
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdMobScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }
    var showAdLoadButton by remember { mutableStateOf(true) }
    var adLoadStatus by remember { mutableStateOf("Support WellConnect by viewing ads") }
    
    // Initialize MobileAds when the screen is launched
    LaunchedEffect(Unit) {
        try {
            MobileAds.initialize(context) {
                Log.d(TAG, "MobileAds initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds: ${e.message}", e)
        }
    }
    
    // Load interstitial ad
    fun loadInterstitialAd() {
        try {
            adLoadStatus = "Loading full-screen ad..."
            showAdLoadButton = false
            
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded successfully")
                        interstitialAd = ad
                        isAdLoaded = true
                        adLoadStatus = "Ad loaded successfully! Click to view."
                        showAdLoadButton = true
                        
                        // Set up full screen content callback
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Ad was dismissed")
                                interstitialAd = null
                                isAdLoaded = false
                                adLoadStatus = "Thanks for supporting WellConnect!"
                                coroutineScope.launch {
                                    delay(2000)
                                    showAdLoadButton = true
                                    adLoadStatus = "Support WellConnect by viewing more ads"
                                }
                            }
                            
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.e(TAG, "Ad failed to show: ${adError.message}")
                                interstitialAd = null
                                isAdLoaded = false
                                adLoadStatus = "Failed to show ad. Try again."
                                showAdLoadButton = true
                            }
                            
                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Ad showed fullscreen content")
                                adLoadStatus = "Ad showing..."
                            }
                        }
                    }
                    
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Failed to load interstitial ad: ${loadAdError.message}")
                        interstitialAd = null
                        isAdLoaded = false
                        adLoadStatus = "Failed to load ad: ${loadAdError.message}"
                        showAdLoadButton = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading interstitial ad: ${e.message}", e)
            adLoadStatus = "Error: ${e.message}"
            showAdLoadButton = true
        }
    }
    
    // Show interstitial ad if loaded
    fun showInterstitialAd() {
        if (isAdLoaded && interstitialAd != null) {
            interstitialAd?.show(context as androidx.activity.ComponentActivity)
        } else {
            adLoadStatus = "Ad not ready yet. Loading..."
            loadInterstitialAd()
        }
    }
    
    // Clean up resources when the screen is closed
    DisposableEffect(Unit) {
        onDispose {
            interstitialAd = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support WellConnect") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Banner ad at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    // Banner ad implementation
                    AndroidView(
                        factory = { context ->
                            AdView(context).apply {
                                setAdSize(AdSize.BANNER)
                                adUnitId = BANNER_AD_UNIT_ID
                                
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        Log.d(TAG, "Banner ad loaded successfully")
                                    }
                                    
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        Log.e(TAG, "Failed to load banner ad: ${error.message}")
                                    }
                                }
                                
                                try {
                                    loadAd(AdRequest.Builder().build())
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading banner ad: ${e.message}", e)
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Information about supporting the app
            Text(
                text = "Support WellConnect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "WellConnect is a free app helping communities share water resources. By viewing ads, you help support our mission to improve water access for everyone.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Ad status message
            Text(
                text = adLoadStatus,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Interstitial ad button
            if (showAdLoadButton) {
                Button(
                    onClick = {
                        if (isAdLoaded) {
                            showInterstitialAd()
                        } else {
                            loadInterstitialAd()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = if (isAdLoaded) "Watch Ad" else "Load Full-screen Ad",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Additional banner ad at the bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    // Banner ad implementation
                    AndroidView(
                        factory = { context ->
                            AdView(context).apply {
                                setAdSize(AdSize.MEDIUM_RECTANGLE)
                                adUnitId = BANNER_AD_UNIT_ID
                                
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        Log.d(TAG, "Medium rectangle ad loaded successfully")
                                    }
                                    
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        Log.e(TAG, "Failed to load medium rectangle ad: ${error.message}")
                                    }
                                }
                                
                                try {
                                    loadAd(AdRequest.Builder().build())
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading medium rectangle ad: ${e.message}", e)
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Thank you message
            Text(
                text = "Thank you for supporting WellConnect!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
} 