package com.jess.ondemand

import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

data class OnDemandInstallEvent(
    val state: SplitInstallSessionState? = null,
    @SplitInstallSessionStatus val status: Int
)

data class OnDemandInstallError(
    val moduleName: String,
    @SplitInstallErrorCode val errorCode: Int?,
    val errorMessage: String
)