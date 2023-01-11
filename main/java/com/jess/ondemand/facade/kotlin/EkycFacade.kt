package com.jess.ondemand.facade.kotlin

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.jess.ondemand.facade.KeepModule
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent

@KeepModule
interface EkycFacade {

    val isModuleLoaded: Boolean

    fun getIntent(context: Context): Intent

    companion object : ComponentFactory<EkycFacade>() {

        override fun createComponent(
            context: Context
        ): EkycFacade = EkycFacade.runCatching {
            EkycFacade.delegateCreation(
                context,
                "com.jess.ondemand.kotlin.facade.EkycFacadeFactory"
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

fun Context.ekycFacade(): EkycFacade {
    return getComponent(EkycFacade)
}