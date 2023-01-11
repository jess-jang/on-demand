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
    fun loadAndLaunchModule()

    fun deferredInstall()

    fun checkForActiveDownload(
        onActiveDownload: (Boolean) -> Unit
    )

}

/**
 * - on demand 는 한개의 session 만 다운로드가 가능함
 * ㄴ A module 을 받고 있으면 A 가 끝날때 까지 B module 다운로드 불가능

 * - SplitInstallStateUpdatedListener 는 특정 module 과 상관없는 글로벌한 listener 이기 때문에 특정 session 을 걸러낼 필요가 있음
 * ㄴ 특정 모듈에서 발생한 session 을 전역으로 관리하지 않는 이상 session 을 관리하기가 어려움
 *
 * - module 을 다운로드하고 화면을 이탈하고 다시 왔을때도 모듈 체크가 가능해야함
 * ㄴ 화면을 이탈했을때는 화면(Activity, Fragment)의 lifecycle 을 체크해서 잘 관리해야 할듯함
 * ㄴㄴ 확인 결과 onPause 에서 unregisterListener 를 하기 때문에 리스너를 받지 못함
 * ㄴㄴ 프로세스에 걸린다면 뒤로가기를 막던가, 굳이 화면을 이탈(앱 종료)후 재접근 했다면 모듈 다운로드 중이라고 Dialog 를 보여줘야함
 *
 */
class OnDemandDeliveryImpl(
    private vararg val modules: OnDemandModuleName
) : OnDemandDelivery {

    override val splitInstallManager = SplitInstallManagerFactory.create(App.getApp())

    private var onInstallEvent: ((OnDemandInstallEvent) -> Unit)? = null
    private var onInstallError: ((OnDemandInstallError) -> Unit)? = null

    private val splitInstallLister = SplitInstallStateUpdatedListener { state ->

        // state.moduleNames 과 constructor modules 을 비교합니다.
        val moduleNames = modules.map { module -> module.name() }
        if (state.moduleNames().containsAll(moduleNames).not()) {
            return@SplitInstallStateUpdatedListener
        }

        onInstallEvent?.invoke(
            OnDemandInstallEvent(
                state,
                state.status()
            )
        )
    }

    override fun deferredInstall() {
        val moduleNames = modules.map { module -> module.name() }
        splitInstallManager.deferredInstall(moduleNames)
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

    override fun loadAndLaunchModule() {

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

            }
            .addOnFailureListener { exception ->
                downloadFailed(modules, exception as? SplitInstallException)
            }
    }

    /**
     * PENDING -> INSTALLING 중인 상태 체크
     */
    private fun isBetweenPendingInstalling(@SplitInstallSessionStatus status: Int?) =
        when (status) {
            SplitInstallSessionStatus.PENDING,
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION,
            SplitInstallSessionStatus.DOWNLOADING,
            SplitInstallSessionStatus.DOWNLOADED,
            SplitInstallSessionStatus.INSTALLING -> true
            else -> false
        }

    /**
     * 다운로드 중인 상태인가?
     */
    override fun checkForActiveDownload(
        onActiveDownload: (Boolean) -> Unit,
    ) {
        val task = splitInstallManager.sessionStates
        task.addOnCompleteListener { taskList ->
            if (taskList.isSuccessful) {
                val state = task.result.find { task ->
                    val moduleNames = modules.map { module -> module.name() }
                    task.moduleNames().containsAll(moduleNames)
                }
                onActiveDownload.invoke(
                    isBetweenPendingInstalling(state?.status())
                )
            } else {
                onActiveDownload.invoke(false)
            }
        }.addOnFailureListener {
            onActiveDownload.invoke(false)
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

            // 이미 설치된 모듈
            SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION -> "INCOMPATIBLE_WITH_EXISTING_SESSION"

            // 다른 모듈이 설치 중임
            SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED -> "ACTIVE_SESSIONS_LIMIT_EXCEEDED"

            else -> "UNKNOWN_ERROR"
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