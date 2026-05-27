package net.pfiers.osmfocus.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A wrapper around LocationManager that:
 *  - Handles permissions
 *  - Allows getting requesting the current or cached location and getting updates
 *  - Is compatible with API level 30 and under
 *  - Handles the looper/background thread
 */
class LocationHelper(private val context: Context) {
    val events = createEventChannel()

    private val handlerThread by lazy {
        HandlerThread("LocationHelper").also { thread ->
            thread.start()
        }
    }
    private val locationManager = ContextCompat.getSystemService(
        context,
        LocationManager::class.java
    )!!
    private val lastLocationFutures = ArrayList<CompletableDeferred<Result<Location?, Exception>>>()
    private val getLastLocationScope = CoroutineScope(Job() + Dispatchers.Main)
    private val currentLocationExecutor by lazy {
        val workQueue = ArrayBlockingQueue<Runnable>(5)
        ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, workQueue)
    }

    fun activityResultCallback(isGranted: Boolean) {
        if (lastLocationFutures.isNotEmpty()) {
            getLastLocationScope.launch {
                val result = if (isGranted) {
                    getLastKnownLocation(launchRequestIfDenied = false)
                } else {
                    Result.error(LocationPermissionDeniedException())
                }
                lastLocationFutures.forEach { future ->
                    future.complete(result)
                }
                lastLocationFutures.clear()
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun getLastKnownLocation(launchRequestIfDenied: Boolean): Result<Location?, Exception> {
        val locationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (locationPermission != PermissionChecker.PERMISSION_GRANTED) {
            return if (launchRequestIfDenied) {
                val deferred = CompletableDeferred<Result<Location?, Exception>>()
                lastLocationFutures.add(deferred)
                events.trySend(RequestPermissionEvent(Manifest.permission.ACCESS_FINE_LOCATION))
                deferred.await()
            } else {
                Result.error(LocationPermissionDeniedException())
            }
        }

        val bestAvailableProvider =
            getBestProvider() ?: return Result.error(LocationUnavailableException())
        val lastKnownLocation = locationManager.getLastKnownLocation(bestAvailableProvider)
        if (lastKnownLocation != null) {
            Timber.d(
                "Got location from passive request: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}"
            )
            events.trySend(LocationEvent(lastKnownLocation)).discard()
        }
        return Result.success(lastKnownLocation)
    }

    @SuppressLint("MissingPermission")
    suspend fun getLocation(launchRequestIfDenied: Boolean): Result<Location, Exception> {
        val lastKnownLocation = getLastKnownLocation(launchRequestIfDenied).getOrElse {
            return Result.error(it)
        }
        if (lastKnownLocation != null) return Result.success(lastKnownLocation)

        val bestAvailableProvider =
            getBestProvider() ?: return Result.error(LocationUnavailableException())
        val locationFuture = CompletableDeferred<Location?>()
        platformGetCurrentLocation(bestAvailableProvider) { location ->
            if (location != null) {
                events.trySend(LocationEvent(location))
            }
            // Some devices/API levels can call back with null; resolve to an explicit error.
            locationFuture.complete(location)
        }

        val location = locationFuture.await() ?: return Result.error(LocationUnavailableException())
        return Result.success(location)
    }

    @SuppressLint("MissingPermission")
    private fun platformGetCurrentLocation(
        provider: String,
        callback: (location: Location?) -> Unit
    ) {
        Handler(handlerThread.looper).post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    currentLocationExecutor,
                    callback
                )
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        callback(location)
                    }

                    override fun onProviderDisabled(provider: String) {
                        locationManager.removeUpdates(this)
                        callback(null)
                    }
                }
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0.0F,
                    listener,
                    handlerThread.looper
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun startLocationUpdates(launchRequestIfDenied: Boolean): Result<Location, Exception> {
        return getLocation(launchRequestIfDenied).map { location ->
            val bestAvailableProvider =
                getBestProvider() ?: return Result.error(LocationUnavailableException())
            Handler(handlerThread.looper).post {
                // TODO: Already requested?
                locationManager.requestLocationUpdates(
                    bestAvailableProvider,
                    1000,
                    5.0F,
                    locationListener
                )
            }
            location
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    private fun getBestProvider(): String? {
        val enabledProviders = locationManager.getProviders(true)
        return listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).firstOrNull(enabledProviders::contains) ?: enabledProviders.firstOrNull()
    }

    private val locationListener by lazy {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                events.trySend(LocationEvent(location))
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {
                events.trySend(LocationProviderDisableEvent())
            }
        }
    }

    class RequestPermissionEvent(val permission: String) : Event()
    class LocationEvent(val location: Location) : Event()
    class LocationProviderDisableEvent : Event()

    class LocationUnavailableException : Exception()
    class LocationPermissionDeniedException : Exception()
}
