package com.tari.android.wallet.ui.fragment.send.addNote

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel

class AddNoteViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    fun emojiIdClicked(walletAddress: TariWalletAddress) {
        showAddressDetailsDialog(walletAddress)
    }
}