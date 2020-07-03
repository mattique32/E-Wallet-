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

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.DriveScopes
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.back_up_settings_permission_processing
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentWalletBackupSettingsBinding
import com.tari.android.wallet.infrastructure.backup.WalletBackup
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorage
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorageFactory
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.launch
import org.joda.time.format.DateTimeFormat
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class WalletBackupSettingsFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var factory: BackupStorageFactory

    @Inject
    lateinit var backup: WalletBackup

    @Inject
    lateinit var service: BiometricAuthenticationService

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    private lateinit var googleClient: GoogleSignInClient
    private lateinit var ui: FragmentWalletBackupSettingsBinding
    private var viewModel: StorageBackupViewModel? = null
    private var optionsAnimation: Animator? = null
    private var blockingBackPressedDispatcher = object : OnBackPressedCallback(false) {
        // No-op by design
        override fun handleOnBackPressed() = Unit
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        backupAndRestoreComponent.inject(this)
        googleClient = GoogleSignIn.getClient(
            requireActivity(), GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentWalletBackupSettingsBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAndObserveViewModel()
        setupViews()
        setupCTAs()
    }

    private fun setupViews() {
        ui.backupPermissionProgressBar.setColor(color(back_up_settings_permission_processing))
        ui.cloudBackupStatusProgressView
            .setColor(color(R.color.all_settings_back_up_status_processing))
        val isSignedIn = GoogleSignIn.getLastSignedInAccount(requireContext()) != null
        ui.backupPermissionSwitch.isChecked = isSignedIn
        if (isSignedIn) {
            updatePasswordChangeLabel()
            showLastSuccessfulBackupDateTime()
        } else {
            hideAllBackupOptions()
            ui.lastBackupTimeTextView.gone()
        }
    }

    private fun updatePasswordChangeLabel() {
        ui.updatePasswordLabelTextView.text =
            if (sharedPrefs.backupPassword == null) string(back_up_wallet_set_backup_password_cta)
            else string(back_up_wallet_change_backup_password_cta)
    }

    private fun showLastSuccessfulBackupDateTime() {
        ui.lastBackupTimeTextView.visible()
        val time = sharedPrefs.lastSuccessfulBackupDateTime?.toLocalDateTime()
        if (time == null) {
            ui.lastBackupTimeTextView.text = ""
        } else {
            ui.lastBackupTimeTextView.text = string(
                back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(time),
                BACKUP_TIME_FORMATTER.print(time)
            )
        }
    }

    private fun setupAndObserveViewModel() {
        val lastSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (lastSignInAccount != null) {
            viewModel = setupAndObserverViewModel(factory.google(requireContext(), lastSignInAccount))
        }
    }

    private fun setupCTAs() {
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.backupWithRecoveryPhraseCtaView.setOnClickListener(ThrottleClick {
            requireAuthorization {
                (requireActivity() as SettingsRouter).toWalletBackupWithRecoveryPhrase()
            }
        })
        ui.backupWalletToCloudCtaView.setOnClickListener(ThrottleClick {
            requireAuthorization { performBackup() }
        })
        ui.updatePasswordCtaView.setOnClickListener(ThrottleClick {
            requireAuthorization {
                val router = requireActivity() as SettingsRouter
                if (sharedPrefs.backupPassword == null) {
                    router.toChangePassword()
                } else {
                    router.toConfirmPassword()
                }
            }
        })
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressedDispatcher)
        setPermissionSwitchListener()
    }

    private fun setPermissionSwitchListener() {
        ui.backupPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hideSwitchAndShowProgressBar(isChecked = false)
                requestGDriveAuth()
            } else {
                hideSwitchAndShowProgressBar(isChecked = true)
                signOutFromGDrive(hideOptionsWithAnimation = true)
            }
        }
    }

    private fun hideSwitchAndShowProgressBar(isChecked: Boolean) {
        ui.backupPermissionSwitch.invisible()
        ui.backupPermissionSwitch.isEnabled = false
        setSwitchCheck(isChecked)
        ui.backupPermissionProgressBar.visible()
    }

    private fun requestGDriveAuth() {
        startActivityForResult(googleClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun signOutFromGDrive(hideOptionsWithAnimation: Boolean) {
        lifecycleScope.launch {
            try {
                suspendCoroutine<Unit> { continuation ->
                    googleClient.signOut()
                        .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                        .addOnSuccessListener { continuation.resumeWith(Result.success(Unit)) }
                }
                sharedPrefs.backupPassword = null
                enableSwitchAndHideProgressBar(isChecked = false)
                if (hideOptionsWithAnimation) hideAllBackupOptionsWithAnimation()
                else hideAllBackupOptions()
                viewModel!!.state.removeObservers(viewLifecycleOwner)
                viewModel = null
                requireActivity().viewModelStore.clear()
            } catch (e: Exception) {
                showSignOutFailedDialog()
                Logger.e(e, "Error occurred during signing out")
            }
        }

    }

    private fun handleBackupState(state: StorageBackupState) {
        resetStatusIcons()
        handleBackupCheckState(state)
        handleBackupProcessState(state)
        updateBackupNowButtonState(state)
    }

    private fun handleBackupCheckState(state: StorageBackupState) = when (state.backupStatus) {
        StorageBackupStatus.CHECKING_STATUS -> ui.backupWalletToCloudCtaView.isEnabled = false
        StorageBackupStatus.STATUS_CHECK_FAILURE -> handleStatusCheckFailure(state)
        StorageBackupStatus.BACKED_UP -> showLastSuccessfulBackupDateTime()
        StorageBackupStatus.NOT_BACKED_UP, StorageBackupStatus.UNKNOWN -> {
            // No-op
        }
    }

    private fun handleStatusCheckFailure(state: StorageBackupState) {
        val exception = state.statusCheckException!!
        if (exception is UserRecoverableAuthIOException) {
            startActivityForResult(exception.intent, REQUEST_CODE_REPEAT_AUTH)
        } else {
            showStatusCheckFailureDialog(
                exception.message ?: string(back_up_wallet_status_check_unknown_error)
            )
            viewModel!!.clearStatusCheckFailure()
        }
    }

    private fun handleBackupProcessState(state: StorageBackupState) {
        blockingBackPressedDispatcher.isEnabled =
            state.processStatus == BackupProcessStatus.BACKING_UP
        when (state.processStatus) {
            BackupProcessStatus.BACKING_UP -> ui.backupWalletToCloudCtaView.isEnabled = false
            BackupProcessStatus.FAILURE -> handleBackingUpFailure(state.processException!!)
            BackupProcessStatus.SUCCESS -> {
                showLastSuccessfulBackupDateTime()
                viewModel!!.resetProcessStatus()
            }
            BackupProcessStatus.IDLE -> {
                // No-op
            }
        }
    }

    private fun updateBackupNowButtonState(state: StorageBackupState) = when {
        state.processStatus == BackupProcessStatus.BACKING_UP -> activateBackupStatusView(
            ui.cloudBackupStatusProgressView,
            back_up_wallet_backup_status_in_progress,
            R.color.all_settings_back_up_status_processing
        )
        state.backupStatus == StorageBackupStatus.CHECKING_STATUS -> activateBackupStatusView(
            ui.cloudBackupStatusProgressView,
            back_up_wallet_backup_status_checking_backup,
            R.color.all_settings_back_up_status_processing
        )
        state.backupStatus == StorageBackupStatus.BACKED_UP -> activateBackupStatusView(
            ui.cloudBackupStatusSuccessView,
            back_up_wallet_backup_status_actual,
            R.color.all_settings_back_up_status_up_to_date
        )
        state.backupStatus == StorageBackupStatus.NOT_BACKED_UP -> activateBackupStatusView(
            ui.cloudBackupStatusWarningView,
            back_up_wallet_backup_status_outdated,
            R.color.all_settings_back_up_status_error
        )
        else -> activateBackupStatusView(ui.cloudBackupStatusWarningView)
    }

    private fun activateBackupStatusView(icon: View?, textId: Int = -1, textColor: Int = -1) {
        fun View.adjustVisibility() {
            visibility = if (this == icon) View.VISIBLE else View.INVISIBLE
        }
        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        val hideText = textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (textColor != -1) ui.backupStatusTextView.setTextColor(color(textColor))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_REPEAT_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel!!.clearStatusAndProcessErrors()
                viewModel!!.checkBackupStatus()
            } else {
                showStatusCheckFailureDialog(
                    string(back_up_wallet_status_check_authentication_cancellation)
                )
                viewModel!!.clearStatusCheckFailure()
            }
        } else if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                setupStorageAndBackupState(data)
            } else {
                Logger.e("Authentication failed, requestCode = REQUEST_CODE_SIGN_IN, resultCode = $resultCode")
                showAuthFailedDialog()
            }
        }
    }

    private fun setupStorageAndBackupState(data: Intent?) {
        lifecycleScope.launch {
            val storage = createStorage(data) ?: return@launch
            try {
                val vm = setupViewModel(storage)
                this@WalletBackupSettingsFragment.viewModel = vm
                awaitForBackup(vm)
                vm.state.observe(viewLifecycleOwner, Observer(::handleBackupState))
                updatePasswordChangeLabel()
                enableSwitchAndHideProgressBar(isChecked = true)
                showAllBackupOptionsWithAnimation()
            } catch (e: Exception) {
                showBackingUpFailureDialog(e)
                hideSwitchAndShowProgressBar(isChecked = true)
                signOutFromGDrive(hideOptionsWithAnimation = false)
            }
        }
    }

    private suspend fun createStorage(data: Intent?): BackupStorage? = try {
        factory.google(requireActivity(), data)
    } catch (e: Exception) {
        Logger.e(e, "Error occurred during storage obtaining")
        showAuthFailedDialog()
        null
    }

    private suspend fun awaitForBackup(vm: StorageBackupViewModel) = suspendCoroutine<Unit> { c ->
        vm.state.observe(viewLifecycleOwner, Observer {
            when (it.processStatus) {
                BackupProcessStatus.SUCCESS -> {
                    vm.state.removeObservers(viewLifecycleOwner)
                    c.resumeWith(Result.success(Unit))
                }
                BackupProcessStatus.FAILURE -> {
                    vm.state.removeObservers(viewLifecycleOwner)
                    c.resumeWith(Result.failure(it.processException!!))
                }
                else -> {
                }
            }
        })
        vm.backup(charArrayOf())
    }

    private fun enableSwitchAndHideProgressBar(isChecked: Boolean) {
        ui.backupPermissionProgressBar.gone()
        ui.backupPermissionSwitch.isEnabled = true
        setSwitchCheck(isChecked)
        ui.backupPermissionSwitch.visible()
    }

    private fun setSwitchCheck(isChecked: Boolean) {
        ui.backupPermissionSwitch.setOnCheckedChangeListener(null)
        ui.backupPermissionSwitch.isChecked = isChecked
        setPermissionSwitchListener()
    }

    private fun showAllBackupOptionsWithAnimation() {
        val views = arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.deleteAllBackupsCtaView,
            ui.lastBackupTimeTextView,
            ui.backupWalletToCloudCtaContainerView
        )
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_INVISIBLE, ALPHA_VISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.visible()
                        v.alpha = ALPHA_INVISIBLE
                    }
                },
                onCancel = { views.forEach { v -> v.alpha = ALPHA_VISIBLE } }
            )
            start()
        }
    }

    private fun hideAllBackupOptionsWithAnimation() {
        val views = arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.backupWalletToCloudCtaContainerView,
            ui.deleteAllBackupsCtaView,
            ui.lastBackupTimeTextView
        )
        val wasClickable = views.map { it.isClickable }
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_VISIBLE, ALPHA_INVISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            val finalizeAnimation: (Animator?) -> Unit = {
                views.zip(wasClickable).forEach { (view, wasClickable) ->
                    view.isClickable = wasClickable
                    view.gone()
                    view.alpha = ALPHA_VISIBLE
                }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.isClickable = false
                        v.alpha = ALPHA_VISIBLE
                    }
                },
                onEnd = finalizeAnimation,
                onCancel = finalizeAnimation
            )
            start()
        }
    }

    private fun hideAllBackupOptions() {
        arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.backupWalletToCloudCtaContainerView,
            ui.deleteAllBackupsCtaView,
            ui.lastBackupTimeTextView
        ).forEach(View::gone)
    }

    private fun setupAndObserverViewModel(storage: BackupStorage) =
        setupViewModel(storage)
            .apply { state.observe(viewLifecycleOwner, Observer(::handleBackupState)) }

    private fun setupViewModel(storage: BackupStorage) =
        ViewModelProvider(requireActivity(), viewModelFactory(storage))
            .get(StorageBackupViewModel::class.java)

    private fun viewModelFactory(storage: BackupStorage) =
        StorageBackupViewModelFactory(storage, backup, sharedPrefs)

    private fun showAuthFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_authentication_error_title),
            description = string(back_up_wallet_authentication_error_desc),
            onClose = ::resetSwitchState
        ).show()
    }

    private fun showSignOutFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_sign_out_error_title),
            description = string(back_up_wallet_sign_out_error_desc),
            onClose = ::resetSwitchState
        ).show()
    }

    private fun resetSwitchState() {
        ui.backupPermissionSwitch.isEnabled = true
        ui.backupPermissionProgressBar.gone()
        ui.backupPermissionSwitch.visible()
    }

    private fun showStatusCheckFailureDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_back_up_check_error_title),
            description = string(
                back_up_wallet_back_up_check_error_desc,
                message
            )
        ).show()
    }

    private fun handleBackingUpFailure(exception: Exception) {
        if (exception is UserRecoverableAuthIOException) {
            startActivityForResult(exception.intent, REQUEST_CODE_REPEAT_AUTH)
        } else {
            showBackingUpFailureDialog(exception)
            viewModel!!.resetProcessStatus()
        }
    }

    private fun showBackingUpFailureDialog(e: Exception?) {
        val errorMessage = when {
            e is UnknownHostException -> string(error_no_connection_title)
            e?.message == null -> string(back_up_wallet_backing_up_unknown_error)
            else -> string(back_up_wallet_backing_up_error_desc, e.message!!)
        }
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_backing_up_error_title),
            description = errorMessage
        ).show()
    }

    private fun resetStatusIcons() {
        ui.backupWalletToCloudCtaView.isEnabled = true
        ui.cloudBackupStatusProgressView.invisible()
        ui.cloudBackupStatusSuccessView.invisible()
        ui.cloudBackupStatusWarningView.invisible()
        ui.backupWithRecoveryPhraseWarningView.gone()
    }

    private fun performBackup() {
        try {
            viewModel!!.backup(sharedPrefs.backupPassword ?: charArrayOf())
        } catch (e: IllegalStateException) {
            ErrorDialog(
                requireContext(),
                title = string(back_up_wallet_back_up_is_in_progress_error),
                description = e.message ?: ""
            ).show()
        }
    }

    private fun requireAuthorization(onAuthorized: () -> Unit) {
        if (service.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    // prompt system authentication dialog
                    service.authenticate(
                        this@WalletBackupSettingsFragment,
                        title = string(auth_title),
                        subtitle =
                        if (service.isBiometricAuthAvailable) string(auth_biometric_prompt)
                        else string(auth_device_lock_code_prompt)
                    )
                    onAuthorized()
                } catch (e: BiometricAuthenticationService.BiometricAuthenticationException) {
                    if (e.code != ERROR_USER_CANCELED && e.code != ERROR_CANCELED)
                        Logger.e("Other biometric error. Code: ${e.code}")
                    showAuthenticationCancellationError()
                }
            }
        } else {
            onAuthorized()
        }
    }

    private fun showAuthenticationCancellationError() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(getString(auth_failed_desc))
            .setNegativeButton(string(exit)) { dialog, _ -> dialog.cancel() }
            .create()
            .apply { setTitle(string(auth_failed_title)) }
            .show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        val vm = viewModel
        if (vm != null) {
            if (hidden) {
                vm.state.removeObservers(viewLifecycleOwner)
            } else {
                showLastSuccessfulBackupDateTime()
                updatePasswordChangeLabel()
                vm.state.observe(viewLifecycleOwner, Observer(::handleBackupState))
            }
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = WalletBackupSettingsFragment()
            .apply { arguments = Bundle() }

        private const val REQUEST_CODE_REPEAT_AUTH = 1355
        private const val REQUEST_CODE_SIGN_IN = 1356
        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy")
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
        private const val OPTIONS_ANIMATION_DURATION = 500L
        private const val ALPHA_INVISIBLE = 0F
        private const val ALPHA_VISIBLE = 1F
    }

}
