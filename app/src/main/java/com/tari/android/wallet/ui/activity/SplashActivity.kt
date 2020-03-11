/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.romellfudi.permission.PermisionInterface
import com.romellfudi.permission.PermisionServiceInterface
import com.romellfudi.permission.PermissionService
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.util.Constants.UI.Splash
import com.tari.android.wallet.util.SharedPrefsWrapper
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/**
 * Splash screen activity.
 *
 * @author The Tari Development Team
 */
internal class SplashActivity : BaseActivity() {

    override val contentViewId = R.layout.activity_splash

    private val uiHandler = Handler()

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String
    @Inject
    internal lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        PermissionService.handler(callback,grantResults,permissions)
        init()
    }

    private val callback = object : PermissionService.Callback() {
        override fun onResponse(refusePermissions: java.util.ArrayList<String>?) {
            if (refusePermissions!=null) {
                Toast.makeText(baseContext,
                    "Have to allow all permissions",
                    Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ finish() }, 2000)
            } else
            {
                init()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionService(this).request(callback)
    }

    private fun init() {
        // check whether there's an existing wallet
        val walletExists = File(walletFilesDirPath).list()!!.isNotEmpty()
        if (walletExists && sharedPrefsWrapper.onboardingAuthSetupCompleted) {
            uiHandler.postDelayed({
                startAuthActivity()
            }, Splash.createWalletStartUpDelayMs)
        } else {
            uiHandler.postDelayed({
                startOnboardingActivity()
            }, Splash.createWalletStartUpDelayMs)
        }
    }

    override fun onBackPressed() {
        // no-op
    }

    private fun startOnboardingActivity() {
        val intent = Intent(this, OnboardingFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        // finish this activity
        finish()
    }

    private fun startAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        // finish this activity
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
    }
}