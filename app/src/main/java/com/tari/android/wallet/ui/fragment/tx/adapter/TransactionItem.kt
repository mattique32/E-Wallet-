package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

data class TransactionItem(
    val tx: Tx,
    val contact: ContactDto?,
    val position: Int,
    val viewModel: GIFViewModel,
    val requiredConfirmationCount: Long
) :
    CommonViewHolderItem() {
    override val viewHolderUUID: String = "TransactionItem" + tx.id

    fun isContains(text: String): Boolean = tx.tariContact.walletAddress.emojiId.contains(text)
            || tx.tariContact.walletAddress.hexString.contains(text)
            || tx.message.contains(text)
            || contact?.contact?.getAlias()?.contains(text) ?: false
}