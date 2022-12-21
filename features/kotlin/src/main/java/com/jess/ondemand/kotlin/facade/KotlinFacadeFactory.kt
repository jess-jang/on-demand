package com.jess.ondemand.kotlin.facade

import android.content.Context
import android.content.Intent
import com.jess.ondemand.facade.kotlin.KotlinFacade
import com.jess.ondemand.kotlin.KotlinSampleActivity
import com.linecorp.lich.component.DelegatedComponentFactory

class KotlinFacadeFactory : DelegatedComponentFactory<KotlinFacade>() {

    override fun createComponent(context: Context): KotlinFacade = KotlinFacadeImpl()
}

internal class KotlinFacadeImpl : KotlinFacade {

    override val isModuleLoaded: Boolean = true

    override fun getIntent(
        context: Context
    ): Intent = KotlinSampleActivity.newIntent(context)


}