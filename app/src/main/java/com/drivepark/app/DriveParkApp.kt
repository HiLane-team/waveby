package com.drivepark.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 주행 감시(DriveSession.start)는 DriveWatchService가 맡는다.
 * 서비스를 시작할 수 없으면 MainActivity가 [startSessionInProcess]로 대체한다.
 */
class DriveParkApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)
        applicationScope.launch {
            container.driveSession.arrivals.collect {
                if (container.settings.current().arrivalNotification) container.notifier.notify(it)
            }
        }
    }

    fun startSessionInProcess() = container.driveSession.start(applicationScope)
}
