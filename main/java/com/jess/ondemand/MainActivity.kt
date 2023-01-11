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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.jess.ondemand.facade.kotlinFacade

private const val PACKAGE_NAME = "com.jess.ondemand"

private const val KOTLIN_SAMPLE_CLASSNAME = "$PACKAGE_NAME.kotlin.KotlinSampleActivity"

private const val JAVA_SAMPLE_CLASSNAME = "$PACKAGE_NAME.java.JavaSampleActivity"

private const val INSTANT_PACKAGE_NAME = "$PACKAGE_NAME.instant"

private const val INSTANT_SAMPLE_CLASSNAME =
    "$INSTANT_PACKAGE_NAME.spilt.SplitInstallInstantActivity"

private const val CONFIRMATION_REQUEST_CODE = 1

private const val TAG = "DynamicFeatures"

class MainActivity : BaseSplitActivity() {
    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        val langsInstall = state.languages().isNotEmpty()

        val names = if (langsInstall) {
            // We always request the installation of a single language in this sample
            state.languages().first()
        } else state.moduleNames().joinToString(" - ")

        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                statusText.text = "DOWNLOADING $names"
                //  In order to see this, the application has to be uploaded to the Play Store.
                displayLoadingState(state, getString(R.string.downloading, names))
            }
            SplitInstallSessionStatus.DOWNLOADED -> {
                statusText.text = "DOWNLOADED"
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                statusText.text = "REQUIRES_USER_CONFIRMATION"
                /*
                  This may occur when attempting to download a sufficiently large module.

                  In order to see this, the application has to be uploaded to the Play Store.
                  Then features can be requested until the confirmation path is triggered.
                 */
                manager.startConfirmationDialogForResult(state, this, CONFIRMATION_REQUEST_CODE)
            }
            SplitInstallSessionStatus.INSTALLED -> {
                statusText.text = "INSTALLED"
                if (langsInstall) {
                    onSuccessfulLanguageLoad(names)
                } else {
                    onSuccessfulLoad(names, launch = !multiInstall)
                }
            }

            SplitInstallSessionStatus.INSTALLING -> {
                statusText.text = "INSTALLING"
                displayLoadingState(
                    state,
                    getString(R.string.installing, names)
                )
            }
            SplitInstallSessionStatus.FAILED -> {
                statusText.text = "FAILED errorCode : ${state.errorCode()} / ${state.moduleNames()}"
                toastAndLog(
                    getString(
                        R.string.error_for_module, state.errorCode(),
                        state.moduleNames()
                    )
                )
            }
        }
    }

    /** This is needed to handle the result of the manager.startConfirmationDialogForResult
    request that can be made from SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION
    in the listener above. */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CONFIRMATION_REQUEST_CODE) {
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            if (resultCode == Activity.RESULT_CANCELED) {
                toastAndLog(getString(R.string.user_cancelled))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val moduleKotlin by lazy { getString(R.string.module_feature_kotlin) }
    private val moduleJava by lazy { getString(R.string.module_feature_java) }
    private val moduleAssets by lazy { getString(R.string.module_assets) }

    private val clickListener by lazy {
        View.OnClickListener {
            when (it.id) {
                R.id.btn_load_kotlin -> loadAndLaunchModule(moduleKotlin)
                R.id.btn_load_java -> loadAndLaunchModule(moduleJava)
                R.id.btn_load_assets -> loadAndLaunchModule(moduleAssets)
                R.id.btn_install_all_now -> installAllFeaturesNow()
                R.id.btn_install_all_deferred -> installAllFeaturesDeferred()
                R.id.btn_request_uninstall -> requestUninstall()
                R.id.btn_request_uninstall_java -> requestUninstallJava()
            }
        }
    }

    private lateinit var manager: SplitInstallManager

    private lateinit var statusText: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressByte: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.app_name)
        setContentView(R.layout.activity_main)
        manager = SplitInstallManagerFactory.create(this)
        initializeViews()
    }

    override fun onResume() {
        // Listener can be registered even without directly triggering a download.
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        // Make sure to dispose of the listener once it's no longer needed.
        manager.unregisterListener(listener)
        super.onPause()
    }

    /**
     * Load a feature by module name.
     * @param name The name of the feature module to load.
     */
    private fun loadAndLaunchModule(name: String) {
        updateProgressMessage(getString(R.string.loading_module, name))
        // Skip loading if the module already is installed. Perform success action directly.
        if (manager.installedModules.contains(name)) {
            updateProgressMessage(getString(R.string.already_installed))
            onSuccessfulLoad(name, launch = true)
            return
        }

        // Create request to install a feature module by name.
        val request = SplitInstallRequest.newBuilder()
            .addModule(name)
            .build()

        // Load and install the requested feature module.
        manager.startInstall(request)

        updateProgressMessage(getString(R.string.starting_install_for, name))
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage(BuildConfig.APPLICATION_ID)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        startActivity(intent)
    }

    /** Display assets loaded from the assets feature module. */
    private fun displayAssets() {
        // Get the asset manager with a refreshed context, to access content of newly installed apk.
        val assetManager = createPackageContext(packageName, 0).also {
            SplitCompat.install(it)
        }.assets
        // Now treat it like any other asset file.
        val assetsStream = assetManager.open("assets.txt")
        val assetContent = assetsStream.bufferedReader()
            .use {
                it.readText()
            }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.asset_content))
            .setMessage(assetContent)
            .show()
    }

    /** Install all features but do not launch any of them. */
    private fun installAllFeaturesNow() {
        // Request all known modules to be downloaded in a single session.
        val moduleNames = listOf(moduleKotlin, moduleJava, moduleAssets)
        val requestBuilder = SplitInstallRequest.newBuilder()

        moduleNames.forEach { name ->
            if (!manager.installedModules.contains(name)) {
                requestBuilder.addModule(name)
            }
        }

        val request = requestBuilder.build()

        manager.startInstall(request).addOnSuccessListener {
            toastAndLog("Loading ${request.moduleNames}")
        }.addOnFailureListener {
            toastAndLog("Failed loading ${request.moduleNames}")
        }
    }

    /** Install all features deferred. */
    private fun installAllFeaturesDeferred() {
        val modules = listOf(moduleKotlin, moduleJava, moduleAssets)

        manager.deferredInstall(modules)
            .addOnSuccessListener {
                toastAndLog("Deferred installation of $modules")
            }
    }

    /** Request uninstall of all features. */
    private fun requestUninstall() {

//        toastAndLog(
//            "Requesting uninstall of all modules." +
//                    "This will happen at some point in the future."
//        )

        val installedModules = manager.installedModules.toList()
        manager.deferredUninstall(installedModules)
            .addOnSuccessListener {
                toastAndLog("Uninstalling $installedModules")
            }.addOnFailureListener {
                toastAndLog("Failed installation of $installedModules")
            }
    }

    private fun requestUninstallKotlin() {

//        toastAndLog(
//            "Requesting uninstall of all modules." +
//                    "This will happen at some point in the future."
//        )

        val installedModules = manager.installedModules.toList()
        manager.deferredUninstall(listOf("kotlin"))
            .addOnSuccessListener {
                toastAndLog("Uninstalling $installedModules")
            }.addOnFailureListener {
                toastAndLog("Failed installation of $installedModules")
            }
    }

    private fun requestUninstallJava() {
        if (manager.installedModules.contains("java")) {
            manager.deferredUninstall(listOf("java"))
                .addOnSuccessListener {
                    toastAndLog("Uninstalling 15mb feature")
                }.addOnFailureListener {
                    toastAndLog("Failed installation of 15mb feature")
                }
        }
    }

    private fun requestUninstallAssets() {

//        toastAndLog(
//            "Requesting uninstall of all modules." +
//                    "This will happen at some point in the future."
//        )

        val installedModules = manager.installedModules.toList()
        manager.deferredUninstall(listOf("assets"))
            .addOnSuccessListener {
                toastAndLog("Uninstalling $installedModules")
            }.addOnFailureListener {
                toastAndLog("Failed installation of $installedModules")
            }
    }

    /**
     * Define what to do once a feature module is loaded successfully.
     * @param moduleName The name of the successfully loaded module.
     * @param launch `true` if the feature module should be launched, else `false`.
     */
    private fun onSuccessfulLoad(moduleName: String, launch: Boolean) {
        if (launch) {
            when (moduleName) {
                moduleKotlin -> launchActivity(KOTLIN_SAMPLE_CLASSNAME)
                moduleJava -> launchActivity(JAVA_SAMPLE_CLASSNAME)
                moduleAssets -> displayAssets()
            }
        }
    }

    private fun onSuccessfulLanguageLoad(lang: String) {
        LanguageHelper.language = lang
        recreate()
    }

    /** Launch an activity by its class name. */
    private fun launchActivity(className: String) {
        when (className) {
            KOTLIN_SAMPLE_CLASSNAME -> {
//                startActivity(kotlinFacade.kotlinFacade().getIntent(this))
            }
            else -> {
//                Intent().setClassName(BuildConfig.APPLICATION_ID, className)
//                    .also {
//                        startActivity(it)
//                    }
            }
        }

    }

    /** Display a loading state to the user. */
    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        progressBar.max = state.totalBytesToDownload().toInt()
        progressBar.progress = state.bytesDownloaded().toInt()
//        progressByte.text = "${state.bytesDownloaded().toInt()} / ${state.totalBytesToDownload()}"

        updateProgressMessage(message)
    }

    /** Set up all view variables. */
    private fun initializeViews() {
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
        progressByte = findViewById(R.id.progress_byte)
        setupClickListener()
    }

    /** Set all click listeners required for the buttons on the UI. */
    private fun setupClickListener() {
        setClickListener(R.id.btn_load_kotlin, clickListener)
        setClickListener(R.id.btn_load_java, clickListener)
        setClickListener(R.id.btn_load_assets, clickListener)
        setClickListener(R.id.btn_install_all_now, clickListener)
        setClickListener(R.id.btn_install_all_deferred, clickListener)
        setClickListener(R.id.btn_request_uninstall, clickListener)
        setClickListener(R.id.btn_request_uninstall_java, clickListener)
    }

    private fun setClickListener(id: Int, listener: View.OnClickListener) {
        findViewById<View>(id).setOnClickListener(listener)
    }

    private fun updateProgressMessage(message: String) {
        progressText.text = message
    }

}

fun MainActivity.toastAndLog(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    Log.d(TAG, text)
}
