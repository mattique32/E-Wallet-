package com.tari.android.wallet.ui.fragment.contactBook.details

import android.text.SpannableString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_cancel
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.common_confirm
import com.tari.android.wallet.R.string.contact_book_add_contact_done_button
import com.tari.android.wallet.R.string.contact_book_add_contact_first_name_hint
import com.tari.android.wallet.R.string.contact_book_add_contact_yat_hint
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_title
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_title
import com.tari.android.wallet.R.string.contact_book_details_connected_wallets
import com.tari.android.wallet.R.string.contact_book_details_delete_button_title
import com.tari.android.wallet.R.string.contact_book_details_delete_contact
import com.tari.android.wallet.R.string.contact_book_details_delete_message
import com.tari.android.wallet.R.string.contact_book_details_edit_title
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Close
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Normal
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Warning
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.dialog.modular.modules.yatInput.YatInputModule
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactAction
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.splitAlias
import com.tari.android.wallet.ui.fragment.contactBook.details.adapter.contactType.ContactTypeViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.details.adapter.profile.ContactProfileViewHolderItem
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.fragment.settings.allSettings.row.SettingsRowStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.row.SettingsRowViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    // TODO don't understand the purpose of this. Need a refactor once Yat is implemented
    private var searchingJob: Deferred<YatDto?>? = null
    private var updatingJob: Job? = null

    private val _uiState = MutableStateFlow(ContactDetailsModel.UiState(contact = savedState.get<ContactDto>(PARAMETER_CONTACT)!!))
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        _uiState.update { it.copy(list = updateList(it.contact)) }
    }


    private fun updateList(contact: ContactDto): List<CommonViewHolderItem> {
        updateYatInfo(contact)

        val availableActions = contact.getContactActions()
        val connectedYatWallets = contact.yatDto?.connectedWallets?.filter { it.name != null }

        return listOfNotNull(
            ContactProfileViewHolderItem(
                contactDto = contact,
                onAddressClick = { showAddressDetailsDialog(it) },
            ),

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.Send.title),
                action = { tariNavigator.navigate(Navigation.ContactBookNavigation.ToSendTari(contact)) }
            ).takeIf { availableActions.contains(ContactAction.Send) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Send) },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.Link.title),
                action = { tariNavigator.navigate(Navigation.ContactBookNavigation.ToLinkContact(contact)) }
            ).takeIf { availableActions.contains(ContactAction.Link) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Link) },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(R.string.contact_details_transaction_history),
                action = { tariNavigator.navigate(Navigation.ContactBookNavigation.ToContactTransactionHistory(contact)) }
            ).takeIf { contact.getFFIContactInfo() != null },
            DividerViewHolderItem().takeIf { contact.getFFIContactInfo() != null },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.ToFavorite.title),
                action = { toggleFavorite(contact) }
            ).takeIf { availableActions.contains(ContactAction.ToFavorite) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.ToFavorite) },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.ToUnFavorite.title),
                action = { toggleFavorite(contact) }
            ).takeIf { availableActions.contains(ContactAction.ToUnFavorite) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.ToUnFavorite) },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.Unlink.title),
                action = { showUnlinkDialog(contact) }
            ).takeIf { availableActions.contains(ContactAction.Unlink) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Unlink) },

            SettingsRowViewHolderItem(
                title = resourceManager.getString(ContactAction.Delete.title),
                style = SettingsRowStyle.Warning,
                iconId = R.drawable.tari_empty_drawable,
                action = { showDeleteContactDialog(contact) },
            ).takeIf { availableActions.contains(ContactAction.Delete) },
            DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Delete) },

            SettingsTitleViewHolderItem(
                title = resourceManager.getString(contact_book_details_connected_wallets),
            ).takeIf { connectedYatWallets?.isNotEmpty() == true },
            *connectedYatWallets?.map { connectedWallet ->
                listOf(
                    SettingsRowViewHolderItem(
                        title = resourceManager.getString(connectedWallet.name!!),
                        action = { tariNavigator.navigate(Navigation.ContactBookNavigation.ToExternalWallet(connectedWallet)) },
                    ),
                    DividerViewHolderItem(),
                )
            }.orEmpty().flatten().toTypedArray(),

            ContactTypeViewHolderItem(
                type = resourceManager.getString(contact.getTypeName()),
                icon = contact.getTypeIcon(),
            ),

            SpaceVerticalViewHolderItem(20),
        )
    }

    private fun toggleFavorite(contactDto: ContactDto) {
        launchOnIo {
            val newContact = contactsRepository.toggleFavorite(contactDto)

            launchOnMain {
                _uiState.update {
                    it.copy(
                        contact = contactsRepository.toggleFavorite(contactDto),
                        list = updateList(newContact),
                    )
                }
            }
        }
    }

    private fun updateYatInfo(contact: ContactDto) {
        if (updatingJob != null) return

        contact.yatDto?.takeUnless { it.yat.isEmpty() }?.let { yatDto ->
            updatingJob = launchOnIo {
                try {
                    val entries = yatAdapter.searchAnyYats(yatDto.yat)?.result?.entries
                    entries ?: return@launchOnIo
                    val map = entries.associate { entry -> entry.key to entry.value }
                    contactsRepository.updateYatInfo(contactDto = contact, connectedWallets = map)
                    val newContact = contactsRepository.getByUuid(contact.uuid)
                    launchOnMain {
                        _uiState.update { it.copy(contact = newContact, list = updateList(newContact)) }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onEditClick() {
        val contact = uiState.value.contact

        val name = (contact.contactInfo.firstName + " " + contact.contactInfo.lastName).trim()
        val phoneDto = contact.getPhoneContactInfo()
        val yatDto = contact.yatDto

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(
            value = name,
            hint = resourceManager.getString(contact_book_add_contact_first_name_hint),
            isFirst = true,
            isEnd = true,
            onDoneAction = { saveAction.invoke() },
        )

        val yatModule = YatInputModule(
            search = this::yatSearchAction,
            value = yatDto?.yat.orEmpty(),
            hint = resourceManager.getString(contact_book_add_contact_yat_hint),
            isFirst = false,
            isEnd = true,
            onDoneAction = { saveAction.invoke() },
        ).takeIf { DebugConfig.isYatEnabled && phoneDto != null }

        val headModule = HeadModule(
            title = resourceManager.getString(contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(contact_book_add_contact_done_button),
            rightButtonAction = { saveAction.invoke() },
        )

        saveAction = {
            saveDetails(contact, nameModule.value, yatModule?.value ?: "")
            true
        }

        showInputModalDialog(
            ModularDialogArgs(
                DialogArgs(),
                listOfNotNull(
                    headModule,
                    nameModule,
                    yatModule.takeIf { it != null },
                ),
            )
        )
    }

    private suspend fun yatSearchAction(yat: String): Boolean {
        searchingJob?.cancel()

        if (yat.isEmpty()) return false

        searchingJob = viewModelScope.async(Dispatchers.IO) {
            val entries = yatAdapter.searchTariYats(yat)?.result?.entries?.firstOrNull()
            entries ?: return@async null
            // TODO: Weird code. Returns nothing, don't understand the purpose of this. Also check if returns base58 or not
            val pubkey = entries.value.address
            val address = TariWalletAddress.fromBase58OrNull(pubkey) ?: return@async null
            YatDto(yat)
        }
        return searchingJob?.await() != null
    }

    private fun saveDetails(contact: ContactDto, newName: String, yat: String = "") {
        if (newName.isBlank()) {
            showSimpleDialog(
                title = R.string.contact_details_empty_name_title,
                description = R.string.contact_details_empty_name_description,
            )
        } else {
            updatingJob?.cancel()
            launchOnIo {
                val firstName = splitAlias(newName).firstName
                val lastName = splitAlias(newName).lastName

                val newContact = contactsRepository.updateContactInfo(contact, firstName, lastName, yat)

                launchOnMain {
                    _uiState.update { it.copy(contact = newContact, list = updateList(newContact)) }
                    hideDialog()
                }
            }
        }
    }

    private fun showUnlinkDialog(contact: ContactDto) {
        val mergedDto = uiState.value.contact.contactInfo as MergedContactInfo
        val walletAddress = mergedDto.ffiContactInfo.walletAddress
        val name = mergedDto.phoneContactInfo.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_secondLine, name))

        showModularDialog(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_confirm), Normal) {
                launchOnIo {
                    contactsRepository.unlinkContact(contact)
                    launchOnMain {
                        hideDialog()
                        showUnlinkSuccessDialog(contact)
                    }
                }
            },
            ButtonModule(resourceManager.getString(common_cancel), Close)
        )
    }

    private fun showUnlinkSuccessDialog(contact: ContactDto) {
        val mergedDto = contact.contactInfo as MergedContactInfo
        val walletAddress = mergedDto.ffiContactInfo.walletAddress
        val name = mergedDto.phoneContactInfo.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_firstLine))
        val secondLineHtml =
            HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_close), Close)
        )
        showModularDialog(
            ModularDialogArgs(
                dialogArgs = DialogArgs(
                    onDismiss = { tariNavigator.navigate(Navigation.ContactBookNavigation.BackToContactBook) }
                ),
                modules = modules,
            )
        )
    }

    private fun showDeleteContactDialog(contact: ContactDto) {
        showModularDialog(
            HeadModule(resourceManager.getString(contact_book_details_delete_contact)),
            BodyModule(resourceManager.getString(contact_book_details_delete_message)),
            ButtonModule(resourceManager.getString(contact_book_details_delete_button_title), Warning) {
                launchOnIo {
                    contactsRepository.deleteContact(contact)
                    launchOnMain {
                        hideDialog()
                        tariNavigator.navigate(Navigation.ContactBookNavigation.BackToContactBook)
                    }
                }
            },
            ButtonModule(resourceManager.getString(common_close), Close)
        )
    }
}