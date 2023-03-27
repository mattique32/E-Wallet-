package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

class ContactItem(
    val contact: ContactDto,
    val isSimple: Boolean = false,
    val contactAction: (ContactDto, ContactAction) -> Unit = { _, _ -> },
    var toggleBadges: (isOpen: Boolean) -> Unit = {},
    var notifyToggling: (current: ContactItem) -> Unit = {}
) : CommonViewHolderItem() {
    fun filtered(text: String): Boolean = contact.filtered(text)
}

