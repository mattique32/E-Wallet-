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
package com.tari.android.wallet.ui.fragment.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.infrastructure.backup.WalletBackup
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorageFactory
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.backupAndRestoreComponent
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.settings.backup.StorageBackupViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.StorageBackupViewModelFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

class AllSettingsFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var factory: BackupStorageFactory

    @Inject
    lateinit var backup: WalletBackup

    private lateinit var ui: FragmentAllSettingsBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        backupAndRestoreComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAllSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        bindCTAs()
    }

    private fun bindCTAs() {
        ui.doneCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.visitSiteCtaView.setOnClickListener { openLink(string(tari_url)) }
        ui.contributeCtaView.setOnClickListener { openLink(string(github_repo_url)) }
        ui.userAgreementCtaView.setOnClickListener { openLink(string(user_agreement_url)) }
        ui.privacyPolicyCtaView.setOnClickListener { openLink(string(privacy_policy_url)) }
        ui.disclaimerCtaView.setOnClickListener { openLink(string(disclaimer_url)) }
        ui.backUpWalletCtaView.setOnClickListener { processBackupWalletCta() }
    }

    private fun openLink(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    private fun processBackupWalletCta() {
        try {
            ViewModelProvider(requireActivity()).get(StorageBackupViewModel::class.java)
            navigateToBackUpSettings()
        } catch (e: Exception) {
            // Means that we aren't authenticated yet
            requestAuthenticationForGDrive()
        }
    }

    private fun requestAuthenticationForGDrive() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        val client = GoogleSignIn.getClient(requireActivity(), signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                setupBackupProcessAndNavigateToBackupSettings(data)
            } else {
                showAuthFailedDialog()
            }
        }
    }

    private fun setupBackupProcessAndNavigateToBackupSettings(data: Intent?) {
        lifecycleScope.launch {
            try {
                val storage = factory.google(requireActivity(), data)
                // Creating a shared viewmodel for further use
                ViewModelProvider(requireActivity(), StorageBackupViewModelFactory(storage, backup))
                    .get(StorageBackupViewModel::class.java)
                navigateToBackUpSettings()
            } catch (e: Exception) {
                Logger.e(e, "Error occurred during storage obtaining")
                showAuthFailedDialog()
            }
        }
    }

    private fun showAuthFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_authentication_error_title),
            description = string(back_up_wallet_authentication_error_desc)
        ).show()
    }

    private fun navigateToBackUpSettings() {
        (requireActivity() as SettingsRouter).toWalletBackupSettings()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = AllSettingsFragment()

        private const val REQUEST_CODE_SIGN_IN = 1123
    }

}
