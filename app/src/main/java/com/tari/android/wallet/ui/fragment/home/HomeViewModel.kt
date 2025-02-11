package com.tari.android.wallet.ui.fragment.home

import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.root.ShareViewModel
import javax.inject.Inject

class HomeViewModel : CommonViewModel() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val shareViewModel = ShareViewModel()

    init {
        component.inject(this)

        shareViewModel.tariBluetoothServer.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, false, action)
        }

        shareViewModel.tariBluetoothClient.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, false, action)
        }
    }
}