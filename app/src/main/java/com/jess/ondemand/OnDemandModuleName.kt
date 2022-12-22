package com.jess.ondemand

enum class OnDemandModuleName {
    ASSETS, EKYC, IDCARD
}

fun OnDemandModuleName.name() = this.name.lowercase()