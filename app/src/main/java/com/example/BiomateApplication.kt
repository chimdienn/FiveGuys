package com.example

import android.app.Application
import android.util.Log
import com.example.core.AppContainer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Owns the dependency container and an application-scoped coroutine scope for work that
 * must outlive any one screen — first-run seeding, for instance, should not be cancelled
 * because the user rotated the device.
 */
class BiomateApplication : Application() {

    /**
     * SupervisorJob so one failing background task cannot take down the others, plus an
     * explicit handler: an uncaught exception in this scope would otherwise crash the
     * process with no indication of which task threw.
     */
    val applicationScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Unhandled error in application scope", throwable)
        }
    )

    val container: AppContainer by lazy { AppContainer(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { container.seedIfNeeded() }
    }

    private companion object {
        const val TAG = "BiomateApplication"
    }
}
