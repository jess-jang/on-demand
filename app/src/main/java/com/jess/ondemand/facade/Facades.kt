package com.jess.ondemand.facade

import com.jess.ondemand.App

val kotlinFacade by lazy {
    App.getApp()
}