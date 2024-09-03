package com.tari.android.wallet.ui.fragment.contactBook.contactSelection

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto
import com.tari.android.wallet.util.EmojiId

object ContactSelectionModel {

    data class YatState(
        val yatUser: YatUser? = null,
        val eyeOpened: Boolean = false,
    ) {
        val showYatIcons: Boolean
            get() = yatUser != null

        fun toggleEye() = this.copy(eyeOpened = !eyeOpened)

        data class YatUser( // TODO use YatDto directly
            val yat: EmojiId,
            val walletAddress: TariWalletAddress,
            val connectedWallets: List<YatDto.ConnectedWallet>,
        )
    }

    sealed interface Effect {
        data object ShowNotValidEmojiId : Effect
        data object ShowNextButton : Effect
        data object GoToNext : Effect
    }
}