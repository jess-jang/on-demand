package com.jess.ondemand.facade.kotlin

import android.content.Context
import android.content.Intent

class DummyKotlinFacade : KotlinFacade {

    override val isModuleLoaded: Boolean = false

    override fun getIntent(context: Context) = Intent()

}