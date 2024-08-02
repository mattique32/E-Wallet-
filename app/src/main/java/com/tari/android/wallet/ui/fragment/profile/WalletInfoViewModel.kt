package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.ffi.Base58
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.splitAlias
import com.tari.android.wallet.ui.fragment.contactBook.root.ShareViewModel
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.util.ContactUtil
import javax.inject.Inject

class WalletInfoViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var yatSharedPrefsRepository: YatPrefRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactUtil: ContactUtil

    private val _emojiId: MutableLiveData<String> = MutableLiveData()
    val emojiId: LiveData<String> = _emojiId

    private val _base58: MutableLiveData<Base58> = MutableLiveData()
    val base58: LiveData<Base58> = _base58

    private val _yat: MutableLiveData<String> = MutableLiveData()
    val yat: LiveData<String> = _yat

    val alias: MutableLiveData<String> = MutableLiveData("")

    private val _yatDisconnected: MutableLiveData<Boolean> = MutableLiveData(false)
    val yatDisconnected: LiveData<Boolean> = _yatDisconnected

    private val _reconnectVisibility: MediatorLiveData<Boolean> = MediatorLiveData()
    val reconnectVisibility: LiveData<Boolean> = _reconnectVisibility

    init {
        component.inject(this)

        _reconnectVisibility.addSource(_yatDisconnected) { updateReconnectVisibility() }

        refreshData()
    }

    fun refreshData() {
        _emojiId.postValue(corePrefRepository.emojiId)
        _base58.postValue(corePrefRepository.walletAddressBase58)
        _yat.postValue(yatSharedPrefsRepository.connectedYat.orEmpty())
        _yatDisconnected.postValue(yatSharedPrefsRepository.yatWasDisconnected)
        alias.postValue(corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty())

        checkEmojiIdConnection()
    }

    fun openYatOnboarding(context: Context) {
        yatAdapter.openOnboarding(context)
    }

    private fun checkEmojiIdConnection() {
        val connectedYat = yatSharedPrefsRepository.connectedYat.orEmpty()
        if (connectedYat.isNotEmpty()) {
            launchOnIo {
                yatAdapter.searchTariYats(connectedYat).let {
                    if (it?.status == true) {
                        it.result?.entries?.firstOrNull()?.let { response ->
                            val wasDisconnected = response.value.address.lowercase() != corePrefRepository.walletAddressBase58.orEmpty().lowercase()
                            yatSharedPrefsRepository.yatWasDisconnected = wasDisconnected
                            _yatDisconnected.postValue(wasDisconnected)
                        }
                    } else {
                        yatSharedPrefsRepository.yatWasDisconnected = true
                        _yatDisconnected.postValue(true)
                    }
                }
            }
        }
    }

    private fun updateReconnectVisibility() {
        _reconnectVisibility.postValue(_yatDisconnected.value!!)
    }

    fun shareData(type: ShareType) {
        val walletAddress = corePrefRepository.walletAddress

        val deeplink = deeplinkHandler.getDeeplink(
            DeepLink.UserProfile(
                tariAddress = corePrefRepository.walletAddressBase58.orEmpty(),
                alias = contactUtil.normalizeAlias(alias.value.orEmpty(), walletAddress),
            )
        )

        ShareViewModel.currentInstant?.share(type, deeplink)
    }

    fun showEditAliasDialog() {
        val name = (corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty()).trim()

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(
            value = name,
            hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
            isFirst = true,
            isEnd = false,
            onDoneAction = { saveAction() },
        )

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.wallet_info_alias_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button),
            rightButtonAction = { saveAction() },
        )

        val moduleList = mutableListOf(headModule, nameModule)
        saveAction = {
            saveDetails(nameModule.value)
            true
        }

        showInputModalDialog(ModularDialogArgs(DialogArgs(), moduleList))
    }

    fun openRequestTari() {
        navigation.postValue(Navigation.AllSettingsNavigation.ToRequestTari)
    }

    private fun saveDetails(name: String) {
        corePrefRepository.firstName = splitAlias(name).firstName
        corePrefRepository.lastName = splitAlias(name).lastName
        alias.postValue(name)
        hideDialog()
    }

    fun onAddressDetailsClicked() {
        val walletAddress = corePrefRepository.walletAddress
        showModularDialog(
            HeadModule(
                title = resourceManager.getString(R.string.wallet_info_address_details_title),
                rightButtonIcon = R.drawable.vector_common_close,
                rightButtonAction = { hideDialog() },
            ),
            AddressDetailsModule(
                tariWalletAddress = walletAddress,
                copyBase58 = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullBase58,
                    )
                },
                copyEmojis = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullEmojiId,
                    )
                },
            )
        )
    }
}