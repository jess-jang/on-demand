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

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jess.ondemand.feature.EkycActivity
import com.jess.ondemand.feature.IdCardActivity
import com.jess.ondemand.feature.JointCertificateActivity

class LauncherActivity : AppCompatActivity() {

    private val onDemandIdCard: OnDemandDelivery by lazy {
        OnDemandDeliveryImpl(OnDemandModuleName.IDCARD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        initView()

    }

    private fun initView() {

        findViewById<Button>(R.id.btn_joint_certificate).setOnClickListener {
            startActivity(Intent(this, JointCertificateActivity::class.java))
        }

        findViewById<Button>(R.id.btn_ekyc).run {
            setOnClickListener {
                startActivity(Intent(this@LauncherActivity, EkycActivity::class.java))
            }

        }

        findViewById<Button>(R.id.btn_idcard).run {
            setOnClickListener {
                startActivity(Intent(this@LauncherActivity, IdCardActivity::class.java))
            }

            setOnLongClickListener {
                onDemandIdCard.deferredInstall()
                Toast.makeText(
                    this@LauncherActivity,
                    "IdCard deferredInstall",
                    Toast.LENGTH_SHORT
                ).show()
                true

            }
        }
    }
}
