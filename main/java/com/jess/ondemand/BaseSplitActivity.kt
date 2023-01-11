/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jess.ondemand

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

/**
 * This base activity unifies calls to attachBaseContext as described in:
 * https://developer.android.com/guide/app-bundle/playcore#invoke_splitcompat_at_runtime
 */
abstract class BaseSplitActivity : AppCompatActivity() {

    private val onDemandStatus by lazy {
        OnDemandStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        lifecycle.addObserver(onDemandStatus)

        onDemandStatus.setOnSplitStatusListener { status ->
            when (status) {
                SplitInstallSessionStatus.INSTALLED -> {

                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val ctx = newBase?.let { LanguageHelper.getLanguageConfigurationContext(it) }
        super.attachBaseContext(ctx)
        SplitCompat.install(this)
    }
}
