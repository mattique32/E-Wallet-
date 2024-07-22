package com.tari.android.wallet.ui.component.clipboardController

import android.content.ClipboardManager
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.repository.TariAddressRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import javax.inject.Inject

class WalletAddressViewModel : CommonViewModel() {

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var tariAddressRepository: TariAddressRepository

    val discoveredWalletAddressFromClipboard = SingleLiveEvent<TariWalletAddress>()

    val discoveredWalletAddressFromQuery = SingleLiveEvent<TariWalletAddress>()

    init {
        component.inject(this)
    }

    fun tryToCheckClipboard() {
        checkClipboardForValidEmojiId()
    }

    fun checkClipboardForValidEmojiId() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return

        launchOnIo {
            findValidEmojiId(clipboardString)?.let {
                launchOnMain {
                    discoveredWalletAddressFromClipboard.postValue(it)
                }
            }
        }
    }

    fun checkQueryForValidEmojiId(query: String) {
        launchOnIo {
            findValidEmojiId(query)?.let {
                launchOnMain {
                    discoveredWalletAddressFromQuery.postValue(it)
                }
            }
        }
    }

    private fun findValidEmojiId(query: String): TariWalletAddress? {
        return when (val deepLink = deeplinkHandler.handle(query)) {
            is DeepLink.Send -> deepLink.walletAddressBase58
            is DeepLink.UserProfile -> deepLink.tariAddressBase58
            is DeepLink.Contacts -> deepLink.contacts.firstOrNull()?.base58
            else -> null
        }?.let { deeplinkBase58 -> TariWalletAddress.fromBase58OrNull(deeplinkBase58) }
            ?: tariAddressRepository.parseValidWalletAddress(query)
    }
}