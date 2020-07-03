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
package com.tari.android.wallet.ui.fragment.settings.backup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R.color.change_password_cta_disabled
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.databinding.FragmentEnterBackupPasswordBinding
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import javax.inject.Inject

class EnterCurrentPasswordFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    private lateinit var ui: FragmentEnterBackupPasswordBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        backupAndRestoreComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentEnterBackupPasswordBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ui.passwordEditText.requestFocus()
        ui.root.postDelayed(LOCAL_AUTH_DELAY_TIME) { UiUtil.showKeyboard(requireActivity()) }
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.changePasswordCtaTextView.setOnClickListener {
            val input = (ui.passwordEditText.text?.toString() ?: "").toCharArray()
            val backupPassword = sharedPrefs.backupPassword ?: charArrayOf()
            if (input.contentEquals(backupPassword)) {
                (requireActivity() as SettingsRouter).toChangePassword()
            } else {
                ui.changePasswordCtaTextView.isEnabled = false
                ui.changePasswordCtaTextView.setTextColor(color(change_password_cta_disabled))
                ui.root.postDelayed(DISABLE_BUTTON_TIME) {
                    ui.changePasswordCtaTextView.isEnabled = true
                    ui.changePasswordCtaTextView.setTextColor(color(white))
                }
                ui.passwordsNotMatchLabelView.visible()
            }
        }
        ui.passwordEditText.addTextChangedListener(
            afterTextChanged = { ui.passwordsNotMatchLabelView.gone() }
        )
        ui.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) ui.passwordsNotMatchLabelView.gone()
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = EnterCurrentPasswordFragment()

        private const val DISABLE_BUTTON_TIME = 1000L
        private const val LOCAL_AUTH_DELAY_TIME = 500L
    }

}
