/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.tx.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.tari.android.wallet.R.dimen.add_amount_element_text_size
import com.tari.android.wallet.R.dimen.add_amount_gem_size
import com.tari.android.wallet.R.string.common_from
import com.tari.android.wallet.R.string.common_to
import com.tari.android.wallet.R.string.tx_detail_add_contact
import com.tari.android.wallet.R.string.tx_detail_completing_final_processing
import com.tari.android.wallet.R.string.tx_detail_edit
import com.tari.android.wallet.R.string.tx_detail_fee_tooltip_desc
import com.tari.android.wallet.R.string.tx_detail_fee_tooltip_transaction_fee
import com.tari.android.wallet.R.string.tx_detail_payment_cancelled
import com.tari.android.wallet.R.string.tx_detail_payment_received
import com.tari.android.wallet.R.string.tx_detail_payment_sent
import com.tari.android.wallet.R.string.tx_detail_pending_payment_received
import com.tari.android.wallet.R.string.tx_detail_waiting_for_recipient
import com.tari.android.wallet.R.string.tx_detail_waiting_for_sender_to_complete
import com.tari.android.wallet.R.string.tx_details_fee_value
import com.tari.android.wallet.R.string.tx_list_you_received_one_side_payment
import com.tari.android.wallet.databinding.FragmentTxDetailsBinding
import com.tari.android.wallet.extension.collectNonNullFlow
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.model.TxStatus.COINBASE
import com.tari.android.wallet.model.TxStatus.IMPORTED
import com.tari.android.wallet.model.TxStatus.MINED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.PENDING
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.tooltipDialog.TooltipDialogArgs
import com.tari.android.wallet.ui.extension.dimen
import com.tari.android.wallet.ui.extension.getFirstChild
import com.tari.android.wallet.ui.extension.getLastChild
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.setLayoutSize
import com.tari.android.wallet.ui.extension.setTextSizePx
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.tx.details.gif.GifView
import com.tari.android.wallet.ui.fragment.tx.details.gif.GifViewModel
import com.tari.android.wallet.ui.fragment.tx.details.gif.TxState
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import java.util.Date

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
class TxDetailsFragment : CommonFragment<FragmentTxDetailsBinding, TxDetailsViewModel>() {

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentTxDetailsBinding.inflate(layoutInflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().hideKeyboard()

        val viewModel: TxDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeVM()
    }

    private fun observeVM() = with(viewModel) {
        collectNonNullFlow(tx) { tx ->
            fetchGIFIfAttached(tx)
            bindTxData(tx)
        }

        collectNonNullFlow(contact) { updateContactInfo(it) }

        observe(cancellationReason) { setCancellationReason(it) }

        observe(explorerLink) { link ->
            if (networkRepository.currentNetwork.isBlockExplorerAvailable) {
                showExplorerLink(link)
            }
        }
    }

    private fun updateContactInfo(contact: ContactDto) {
        val alias = contact.contactInfo.getAlias()
        val addEditText = if (alias.isEmpty()) tx_detail_add_contact else tx_detail_edit
        ui.editContactLabelTextView.text = getString(addEditText)
        ui.contactNameTextView.setText(contact.contactInfo.getAlias())
    }

    private fun setCancellationReason(text: String) {
        ui.cancellationReasonView.text = text
        ui.cancellationReasonView.setVisible(text.isNotBlank())
    }

    private fun fetchGIFIfAttached(tx: Tx) {
        val gifId = TxNote.fromNote(tx.message).gifId ?: return
        val gifViewModel: GifViewModel by viewModels()
        gifViewModel.onGIFFetchRequested(gifId)
        GifView(ui.gifContainer, Glide.with(this), gifViewModel, this).displayGif()
    }

    private fun setupUI() {
        bindViews()
        setUICommands()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindViews() {
        currentTextSize = dimen(add_amount_element_text_size)
        currentAmountGemSize = dimen(add_amount_gem_size)

        ui.gifContainer.root.invisible()
    }

    private fun setUICommands() {
        ui.emojiIdSummaryContainerView.setOnClickListener { viewModel.onAddressDetailsClicked() }
        ui.feeLabelTextView.setOnClickListener { showTxFeeToolTip() }
        ui.editContactLabelTextView.setOnClickListener { viewModel.addOrEditContact() }
        ui.cancelTxView.setOnClickListener { viewModel.onTransactionCancel() }
    }

    private fun bindTxData(tx: Tx) {
        ui.userContainer.setVisible(!tx.isOneSided && !tx.isCoinbase)
        ui.contactContainerView.setVisible(!tx.isOneSided && !tx.isCoinbase)

        setTxStatusData(tx)
        setTxMetaData(tx)
        setTxAddressData(tx)
        setTxPaymentData(tx)
    }

    private fun setTxPaymentData(tx: Tx) {
        val state = TxState.from(tx)
        ui.amountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        ui.paymentStateTextView.text = when {
            tx is CancelledTx -> string(tx_detail_payment_cancelled)
            state.status == MINED_CONFIRMED || state.status == IMPORTED ->
                if (state.direction == INBOUND) string(tx_detail_payment_received)
                else string(tx_detail_payment_sent)

            else -> string(tx_detail_pending_payment_received)
        }
        when {
            tx is CompletedTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is CancelledTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is PendingOutboundTx -> setFeeData(tx.fee)
            else -> {
                ui.txFeeTextView.gone()
                ui.feeLabelTextView.gone()
            }
        }
        scaleDownAmountTextViewIfRequired()
    }

    private fun setFeeData(fee: MicroTari) {
        ui.txFeeTextView.visible()
        ui.feeLabelTextView.visible()
        ui.txFeeTextView.text = string(tx_details_fee_value, WalletUtil.amountFormatter.format(fee.tariValue))
    }

    private fun setTxMetaData(tx: Tx) {
        ui.dateTextView.text = Date(tx.timestamp.toLong() * 1000).txFormattedDate()
        val note = TxNote.fromNote(tx.message)
        if (note.message == null) {
            ui.txNoteTextView.gone()
        } else {
            ui.txNoteTextView.text = if (tx.isOneSided) string(tx_list_you_received_one_side_payment) else note.message
        }
        ui.gifContainer.root.visible()
    }

    private fun setTxAddressData(tx: Tx) {
        val state = TxState.from(tx)
        ui.fromTextView.text = if (state.direction == INBOUND) string(common_from) else string(common_to)
        if (tx.tariContact.walletAddress.isUnknownUser()) {
            ui.emojiIdViewContainer.root.gone()
            ui.unknownSource.visible()
        } else {
            ui.unknownSource.gone()
            ui.emojiIdViewContainer.root.visible()
            ui.emojiIdViewContainer.textViewEmojiPrefix.text = tx.tariContact.walletAddress.addressPrefixEmojis()
            ui.emojiIdViewContainer.textViewEmojiFirstPart.text = tx.tariContact.walletAddress.addressFirstEmojis()
            ui.emojiIdViewContainer.textViewEmojiLastPart.text = tx.tariContact.walletAddress.addressLastEmojis()
        }
    }

    private fun setTxStatusData(tx: Tx) {
        val state = TxState.from(tx)

        val statusText = when {
            tx is CancelledTx -> ""
            state == TxState(INBOUND, PENDING) -> string(tx_detail_waiting_for_sender_to_complete)
            state == TxState(OUTBOUND, PENDING) -> string(tx_detail_waiting_for_recipient)
            state == TxState(INBOUND, ONE_SIDED_UNCONFIRMED) || state == TxState(INBOUND, ONE_SIDED_CONFIRMED) -> ""
            state.status != MINED_CONFIRMED && state.status != COINBASE -> string(
                tx_detail_completing_final_processing,
                if (tx is CompletedTx) tx.confirmationCount.toInt() + 1 else 1,
                viewModel.requiredConfirmationCount + 1
            )

            else -> ""
        }
        ui.statusTextView.text = statusText
        ui.statusContainerView.visibility = if (statusText.isEmpty()) View.GONE else View.VISIBLE
        if (tx !is CancelledTx && state.direction == OUTBOUND && state.status == PENDING) {
            ui.cancelTxView.setOnClickListener { viewModel.onTransactionCancel() }
            ui.cancelTxView.visible()
        } else if (ui.cancelTxView.visibility == View.VISIBLE) {
            ui.cancelTxView.setOnClickListener(null)
            ui.cancelTxView.gone()
        }
    }

    private fun showExplorerLink(explorerLink: String) {
        ui.explorerContainerView.setVisible(explorerLink.isNotEmpty())
        ui.explorerContainerView.setOnClickListener { viewModel.openInBlockExplorer() }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert = ui.amountContainerView.getLastChild()!!.right - ui.amountContainerView.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + ui.amountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > ui.amountContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor

        // adjust gem size
        ui.amountGemImageView.setLayoutSize(currentAmountGemSize.toInt(), currentAmountGemSize.toInt())
        ui.amountTextView.setTextSizePx(currentTextSize)
    }

    private fun showTxFeeToolTip() {
        val args = TooltipDialogArgs(string(tx_detail_fee_tooltip_transaction_fee), string(tx_detail_fee_tooltip_desc))
            .getModular(viewModel.resourceManager)
        ModularDialog(requireContext(), args).show()
    }

    companion object {
        const val TX_EXTRA_KEY = "TX_EXTRA_KEY"
        const val TX_ID_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"
    }
}

