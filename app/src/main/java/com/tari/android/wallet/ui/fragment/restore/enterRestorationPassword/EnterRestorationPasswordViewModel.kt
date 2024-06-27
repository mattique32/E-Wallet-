package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.WalletStartFailedException
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException
import javax.inject.Inject

class EnterRestorationPasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component.inject(this)

        viewModelScope.launch {
            walletStateHandler.doOnWalletRunning {
                if (WalletUtil.walletExists(walletConfig)) {
                    val dto = backupSettingsRepository.getOptionDto(backupManager.currentOption!!)!!.copy(isEnable = true)
                    backupSettingsRepository.updateOption(dto)
                    backupManager.backupNow()

                    navigation.postValue(Navigation.EnterRestorationPasswordNavigation.OnRestore)
                }
            }
        }

        viewModelScope.launch {
            walletStateHandler.doOnWalletFailed {
                handleRestorationFailure(WalletStartFailedException(it))
            }
        }
    }

    private val _state = SingleLiveEvent<EnterRestorationPasswordState>()
    val state: LiveData<EnterRestorationPasswordState> = _state

    fun onBack() {
        backPressed.postValue(Unit)
        viewModelScope.launch(Dispatchers.IO) {
            backupManager.signOut()
        }
    }

    fun onRestore(password: String) {
        _state.postValue(EnterRestorationPasswordState.RestoringInProgressState)
        performRestoration(password)
    }

    private fun performRestoration(password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.restoreLatestBackup(password)
                backupSettingsRepository.backupPassword = password
                viewModelScope.launch(Dispatchers.Main) {
                    walletServiceLauncher.start()
                }
            } catch (exception: Throwable) {
                handleRestorationFailure(exception)
            }
        }
    }

    private fun handleRestorationFailure(exception: Throwable) {
        exception.cause?.let {
            if (it is GeneralSecurityException) {
                _state.postValue(EnterRestorationPasswordState.WrongPasswordErrorState)
                return
            }
        }
        when (exception) {
            is GeneralSecurityException -> _state.postValue(EnterRestorationPasswordState.WrongPasswordErrorState)
            else -> showUnrecoverableExceptionDialog(exception.message ?: resourceManager.getString(R.string.common_unknown_error))
        }
    }

    private fun showUnrecoverableExceptionDialog(message: String) {
        val args = SimpleDialogArgs(title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = { backPressed.call() })
        showModularDialog(args.getModular(resourceManager))
    }
}