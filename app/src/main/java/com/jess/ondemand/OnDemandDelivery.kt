package com.jess.ondemand

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

interface OnDemandDelivery : LifecycleEventObserver {

    // manager
    val splitInstallManager: SplitInstallManager

    fun setOnSplitInstallListener(
        onInstallEvent: (OnDemandInstallEvent) -> Unit,
        onInstallError: (OnDemandInstallError) -> Unit
    )

    // 모듈 설치, 실행
    fun loadAndLaunchModule(
        vararg modules: OnDemandModuleName
    )

}

class OnDemandDeliveryImpl() : OnDemandDelivery {

    override val splitInstallManager = SplitInstallManagerFactory.create(App.getApp())

    private var onInstallEvent: ((OnDemandInstallEvent) -> Unit)? = null
    private var onInstallError: ((OnDemandInstallError) -> Unit)? = null

    private val sessionIds = mutableSetOf<Int>()

    private val splitInstallLister = SplitInstallStateUpdatedListener { state ->

        if (sessionIds.contains(state.sessionId()).not()) {
            return@SplitInstallStateUpdatedListener
        }

        onInstallEvent?.invoke(
            OnDemandInstallEvent(
                state,
                state.status()
            )
        )
    }

    override fun setOnSplitInstallListener(
        onInstallEvent: (OnDemandInstallEvent) -> Unit,
        onInstallError: (OnDemandInstallError) -> Unit
    ) {
        this.onInstallEvent = onInstallEvent
        this.onInstallError = onInstallError
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> splitInstallManager.registerListener(splitInstallLister)
            Lifecycle.Event.ON_PAUSE -> splitInstallManager.unregisterListener(splitInstallLister)
            else -> Unit
        }
    }

    /**
     * 특정 module 설치 여부를 확인합니다.
     */
    private fun isInstalledModule(module: OnDemandModuleName) =
        splitInstallManager.installedModules.contains(module.name())

    override fun loadAndLaunchModule(vararg modules: OnDemandModuleName) {

        // 미설치된 모듈 찾기
        val notInstallModules = modules.filter {
            isInstalledModule(it).not()
        }

        // 이미 설치된 모듈
        if (notInstallModules.isEmpty()) {
            onInstallEvent?.invoke(
                OnDemandInstallEvent(
                    status = SplitInstallSessionStatus.INSTALLED
                )
            )
            return
        }

        // 모듈 설치
        val request = SplitInstallRequest.newBuilder().apply {
            notInstallModules.forEach { module ->
                addModule(module.name())
            }
        }

        splitInstallManager
            .startInstall(request.build())
            .addOnSuccessListener { sessionId ->
                sessionIds.add(sessionId)
            }
            .addOnFailureListener { exception ->
                downloadFailed(modules, exception as? SplitInstallException)
            }
    }

    private fun downloadFailed(
        modules: Array<out OnDemandModuleName>,
        exception: SplitInstallException?
    ) {
        val errorMessage = when (exception?.errorCode) {
            // 에러
            SplitInstallErrorCode.MODULE_UNAVAILABLE -> "MODULE_UNAVAILABLE"
            SplitInstallErrorCode.INVALID_REQUEST -> "INVALID_REQUEST"
            SplitInstallErrorCode.SESSION_NOT_FOUND -> "SESSION_NOT_FOUND"
            SplitInstallErrorCode.API_NOT_AVAILABLE -> "API_NOT_AVAILABLE"
            SplitInstallErrorCode.NETWORK_ERROR -> "NETWORK_ERROR"
            SplitInstallErrorCode.ACCESS_DENIED -> "ACCESS_DENIED"

            SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION -> "INCOMPATIBLE_WITH_EXISTING_SESSION"
            SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED -> "ACTIVE_SESSIONS_LIMIT_EXCEEDED"

            else -> "NO_ERROR"
        }

        val moduleName = modules.joinToString(separator = ", ") { it.name() }
        onInstallError?.invoke(
            OnDemandInstallError(
                moduleName,
                exception?.errorCode,
                errorMessage
            )
        )
    }
}