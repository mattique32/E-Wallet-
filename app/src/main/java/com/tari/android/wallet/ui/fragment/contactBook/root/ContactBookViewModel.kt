package com.tari.android.wallet.ui.fragment.contactBook.root

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkFormatter
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.repository.TariAddressRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    @Inject
    lateinit var deeplinkFormatter: DeeplinkFormatter

    @Inject
    lateinit var contactUtil: ContactUtil

    @Inject
    lateinit var tariAddressRepository: TariAddressRepository

    val shareList = MutableLiveData<List<ShareOptionArgs>>()

    val walletAddressViewModel = WalletAddressViewModel()

    val query = MutableLiveData<String>()

    init {
        component.inject(this)

        val list = mutableListOf<ShareOptionArgs>()
        list.add(
            ShareOptionArgs(
                type = ShareType.QR_CODE,
                title = resourceManager.getString(R.string.share_contact_via_qr_code),
                icon = R.drawable.vector_share_qr_code,
                isSelected = true,
            ) { shareViaQrCode() }
        )
        list.add(
            ShareOptionArgs(
                type = ShareType.LINK,
                title = resourceManager.getString(R.string.share_contact_via_qr_link),
                icon = R.drawable.vector_share_link,
            ) { shareViaLink() }
        )
        list.add(
            ShareOptionArgs(
                type = ShareType.BLE,
                title = resourceManager.getString(R.string.share_contact_via_qr_ble),
                icon = R.drawable.vector_share_ble,
            ) { shareViaBLE() }
        )

        shareList.postValue(list)
    }

    fun handleDeeplink(deeplinkString: String) {
        launchOnIo {
            when (val deeplink = deeplinkFormatter.parse(deeplinkString)) {
                is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.base58
                is DeepLink.Send -> deeplink.walletAddressBase58
                is DeepLink.UserProfile -> deeplink.tariAddressBase58
                else -> null
            }?.let { tariAddressRepository.walletAddressFromHex(it).getOrNull() }
                ?.let { walletAddress ->
                    launchOnMain {
                        query.postValue(walletAddress.fullEmojiId)
                    }
                }
        }
    }

    fun doSearch(query: String) {
        walletAddressViewModel.checkQueryForValidEmojiId(query)
    }

    fun send() {
        val walletAddress = walletAddressViewModel.discoveredWalletAddressFromQuery.value!!
        val contact = contactsRepository.getContactByAddress(walletAddress)
        navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(contact))
    }

    fun grantPermission() {
        permissionManager.runWithPermission(listOf(android.Manifest.permission.READ_CONTACTS), silently = true) {
            viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.grantContactPermissionAndRefresh()
            }
        }
    }

    fun shareSelectedContacts() {
        val args = shareList.value!!.first { it.isSelected }
        val selectedContacts = contactSelectionRepository.selectedContacts.map { it.contact }
        contactSelectionRepository.clear()
        val deeplink = getDeeplink(selectedContacts)
        ShareViewModel.currentInstant?.share(args.type, deeplink)
    }

    private fun shareViaQrCode() = setSelectedToShareType(ShareType.QR_CODE)

    private fun shareViaLink() = setSelectedToShareType(ShareType.LINK)

    private fun shareViaBLE() = setSelectedToShareType(ShareType.BLE)

    private fun getDeeplink(selectedContacts: List<ContactDto>): String {
        val contacts = selectedContacts.map {
            DeepLink.Contacts.DeeplinkContact(
                alias = contactUtil.normalizeAlias(it.contactInfo.getAlias(), it.contactInfo.extractWalletAddress()),
                base58 = it.contactInfo.extractWalletAddress().fullBase58,
            )
        }
        return deeplinkHandler.getDeeplink(DeepLink.Contacts(contacts))
    }

    private fun setSelectedToShareType(shareType: ShareType) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values.firstOrNull { it.type == shareType }?.isSelected = true
        shareList.postValue(values)
    }
}
