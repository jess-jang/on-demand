package com.jess.ondemand

import androidx.annotation.StringRes

enum class OnDemandModuleName(@StringRes val stringRes: Int) {
    JOINT_CERTIFICATE(R.string.feature_joint_certificate),
    EKYC(R.string.feature_ekyc_title),
    IDCARD(R.string.feature_id_card)
    ;

    companion object {
        fun getTitleRes(name: String): Int? = values().find {
            name.equals(it.name, true)
        }?.stringRes
    }
}

fun OnDemandModuleName.name() = this.name.lowercase()
