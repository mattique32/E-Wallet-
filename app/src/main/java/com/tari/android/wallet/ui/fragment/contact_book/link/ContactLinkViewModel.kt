package com.tari.android.wallet.ui.fragment.contact_book.link

import android.text.SpannableString
import android.text.SpannedString
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_cancel
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.common_confirm
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_title
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_title
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState.EmptyStateItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.link.adapter.link_header.ContactLinkHeaderViewHolderItem
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactLinkViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val grantPermission = SingleLiveEvent<Unit>()

    val list = MediatorLiveData<List<CommonViewHolderItem>>()

    private val ffiContact = MutableLiveData<ContactDto>()

    private val contactListSource = MediatorLiveData<List<ContactItem>>()

    private val searchText = MutableLiveData("")

    private var searchModule: ContactLinkHeaderViewHolderItem? = null

    init {
        component.inject(this)

        list.addSource(contactListSource) { updateList() }
        list.addSource(searchText) { updateList() }

        collectFlow(contactsRepository.contactListFiltered) {
            contactListSource.value = it.map { contactDto -> ContactItem(contact = contactDto, isSimple = true) }
        }
    }

    fun initArgs(contact: ContactDto) {
        this.ffiContact.value = contact
    }

    fun onContactClick(item: CommonViewHolderItem) {
        (item as? ContactItem)?.let {
            showLinkDialog(it.contact)
        }
    }

    fun grantPermission() {
        permissionManager.runWithPermission(listOf(android.Manifest.permission.READ_CONTACTS), true) {
            viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.grantContactPermissionAndRefresh()
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        searchText.value = query
    }

    private fun updateList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        if (searchModule == null) {
            searchModule = ContactLinkHeaderViewHolderItem(::onSearchQueryChanged, ffiContact.value!!.contact.extractWalletAddress())
        }

        var list = source.filter { it.contact.contact is PhoneContactDto }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        list = list.sortedBy { it.contact.contact.getAlias().lowercase() }

        val endList: MutableList<CommonViewHolderItem> = list.toMutableList()

        if (list.isEmpty()) {
            endList.add(0, EmptyStateItem(getEmptyTitle(), getBody(source), getEmptyImage(), getButtonTitle()) { doAction() })
        } else {
            endList.add(0, searchModule!!)
        }

        this.list.postValue(endList)
    }

    private fun getEmptyTitle(): SpannedString =
        SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_title)))

    private fun getBody(sourceList: List<ContactItem>): SpannedString {
        val noContacts = sourceList.none { it.contact.contact is PhoneContactDto }
        val noMergedContacts = sourceList.none { it.contact.contact is MergedContactDto }
        val havePermission = contactsRepository.contactPermissionGranted

        val resource = when {
            !havePermission -> R.string.contact_book_contacts_book_link_empty_state_no_access
            noContacts && noMergedContacts -> R.string.contact_book_contacts_book_link_empty_state_empty_book
            noContacts -> R.string.contact_book_contacts_book_link_empty_state_no_contacts
            else -> throw IllegalStateException("Unknown state")
        }

        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = R.drawable.vector_contact_favorite_empty_state

    private fun getButtonTitle(): String {
        val havePermission = contactsRepository.contactPermissionGranted

        return when {
            !havePermission -> resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_go_to_permission_settings)
            else -> resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_add_contact)
        }
    }

    private fun doAction() {
        val havePermission = contactsRepository.contactPermissionGranted

        if (!havePermission) {
            grantPermission.postValue(Unit)
        } else {
            navigation.postValue(Navigation.ContactBookNavigation.ToAddPhoneContact)
        }
    }

    private fun showLinkDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.value!!.contact.extractWalletAddress()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_link_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_confirm), ButtonStyle.Normal) {
                viewModelScope.launch(Dispatchers.IO) {
                    contactsRepository.linkContacts(ffiContact.value!!, phoneContactDto)
                    viewModelScope.launch(Dispatchers.Main) {
                        dismissDialog.postValue(Unit)
                        showLinkSuccessDialog(phoneContactDto)
                    }
                }
            },
            ButtonModule(resourceManager.getString(common_cancel), ButtonStyle.Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showLinkSuccessDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.value!!.contact.extractWalletAddress()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_close), ButtonStyle.Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = Navigation.ContactBookNavigation.BackToContactBook
        }, modules))
    }
}