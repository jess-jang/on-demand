package com.jess.ondemand

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

//interface OnDemandStatus() : LifecycleEventObserver {
//
//}

class OnDemandStatus : LifecycleEventObserver {

    private val onStatusEvent: ((Int) -> Unit)? = null

    private val splitInstallManager = SplitInstallManagerFactory.create(App.getApp())

    private val splitInstallLister = SplitInstallStateUpdatedListener { state ->

        when (state.status()) {
            SplitInstallSessionStatus.INSTALLED -> {

//                val title = state.moduleNames().mapNotNull {
//                    OnDemandModuleName.getTitleRes(it)
//                }.joinToString(separator = ", ") {
//                    getString(it)
//                }

                val titles = state.moduleNames().mapNotNull {
                    OnDemandModuleName.getTitleRes(it)
                }
            }

            else -> {
                onStatusEvent?.invoke(state.status())
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> splitInstallManager.registerListener(splitInstallLister)
            Lifecycle.Event.ON_PAUSE -> splitInstallManager.unregisterListener(splitInstallLister)
            else -> Unit
        }
    }

    fun setOnSplitStatusListener(
        onStatusEvent: (Int) -> Unit
    ) {

    }

}