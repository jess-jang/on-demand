package com.jess.ondemand.facade

import com.jess.ondemand.MyApplication

val kotlinFacade by lazy {
    MyApplication.getApp()
}