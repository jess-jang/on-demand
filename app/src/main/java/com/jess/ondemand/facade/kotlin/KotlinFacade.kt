package com.jess.ondemand.facade.kotlin

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.jess.ondemand.facade.KeepModule
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent

@KeepModule
interface KotlinFacade {

    val isModuleLoaded: Boolean

    fun getIntent(context: Context): Intent

    companion object : ComponentFactory<KotlinFacade>() {

        override fun createComponent(
            context: Context
        ): KotlinFacade = KotlinFacade.runCatching {
            KotlinFacade.delegateCreation(
                context,
                "com.jess.ondemand.kotlin.facade.KotlinFacadeFactory"
            )
        }.onFailure {
            Log.e("jess", it.message.toString())
            Toast.makeText(
                context,
                it.message,
                Toast.LENGTH_LONG
            ).show()
        }.getOrThrow()

    }
}

fun Context.kotlinFacade(): KotlinFacade {
    return getComponent(KotlinFacade)
}