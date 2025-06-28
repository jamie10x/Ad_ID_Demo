package com.jamie.adiddemo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.identifier.AdvertisingIdClient // IMPORTANT: Using GMS client
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AdvertisingIdViewModel(application: Application) : AndroidViewModel(application) {

    private val _advertisingId = MutableStateFlow<String?>("Loading...")
    val advertisingId: StateFlow<String?> = _advertisingId

    private val _isLimitAdTrackingEnabled = MutableStateFlow<Boolean?>(null)
    val isLimitAdTrackingEnabled: StateFlow<Boolean?> = _isLimitAdTrackingEnabled

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchAdvertisingId()
    }

    private fun fetchAdvertisingId() {
        viewModelScope.launch {
            _advertisingId.value = "Loading..."
            _isLimitAdTrackingEnabled.value = null
            _errorMessage.value = null

            try {
                // IMPORTANT: AdvertisingIdClient.getAdvertisingIdInfo() is a blocking call.
                // It MUST be executed on a background thread (Dispatchers.IO).
                val adInfo: AdvertisingIdClient.Info? = withContext(Dispatchers.IO) {
                    try {
                        AdvertisingIdClient.getAdvertisingIdInfo(getApplication())
                    } catch (e: Exception) {
                        // Catching general exceptions here if getAdvertisingIdInfo itself fails for some reason
                        // This allows the outer try-catch to handle specific GMS exceptions.
                        Log.e("AdsIdViewModel", "Error inside background thread for getAdvertisingIdInfo: ${e.message}", e)
                        throw e // Re-throw to be caught by the outer specific catches
                    }
                }

                if (adInfo != null) {
                    _advertisingId.value = adInfo.id
                    _isLimitAdTrackingEnabled.value = adInfo.isLimitAdTrackingEnabled
                    Log.d("AdsIdViewModel", "Advertising ID: ${adInfo.id}")
                    Log.d("AdsIdViewModel", "Limit Ad Tracking: ${adInfo.isLimitAdTrackingEnabled}")
                } else {
                    _advertisingId.value = "Not available (null info)"
                    _isLimitAdTrackingEnabled.value = null
                    Log.w("AdsIdViewModel", "AdvertisingIdClient.Info was null after retrieval.")
                }
            } catch (e: GooglePlayServicesNotAvailableException) {
                _errorMessage.value = "Google Play Services is not available on this device."
                Log.e("AdsIdViewModel", "Play Services not available", e)
                _advertisingId.value = "N/A"
                _isLimitAdTrackingEnabled.value = null
            } catch (e: GooglePlayServicesRepairableException) {
                _errorMessage.value = "Google Play Services needs to be updated or enabled."
                Log.e("AdsIdViewModel", "Play Services needs repair", e)
                _advertisingId.value = "N/A"
                _isLimitAdTrackingEnabled.value = null
            } catch (e: IOException) {
                _errorMessage.value = "IO Error connecting to Play Services: ${e.localizedMessage}"
                Log.e("AdsIdViewModel", "IO Error", e)
                _advertisingId.value = "N/A"
                _isLimitAdTrackingEnabled.value = null
            } catch (e: Exception) {
                // Catch any other unexpected exceptions
                _advertisingId.value = "Error"
                _isLimitAdTrackingEnabled.value = null
                _errorMessage.value = "An unexpected error occurred: ${e.localizedMessage ?: e.message}"
                Log.e("AdsIdViewModel", "Final error state: ${e.message}", e)
            }
        }
    }

    fun refreshId() {
        fetchAdvertisingId()
    }
}