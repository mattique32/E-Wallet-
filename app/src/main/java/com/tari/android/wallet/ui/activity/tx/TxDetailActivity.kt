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
package com.tari.android.wallet.ui.activity.tx

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.databinding.ActivityTxDetailBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.TxStatus.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.animation.collapseAndHideAnimation
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*
import javax.inject.Inject

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
internal class TxDetailActivity : AppCompatActivity(), ServiceConnection {

    companion object {
        const val TX_DETAIL_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"

        fun createIntent(context: Context, transaction: Tx): Intent {
            return Intent(context, TxDetailActivity::class.java)
                .apply {
                    putExtra(TX_DETAIL_EXTRA_KEY, transaction)
                }
        }
    }

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private var walletService: TariWalletService? = null

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f

    private lateinit var tx: Tx
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    /**
     * Animates the emoji id "copied" text.
     */
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    private lateinit var ui: ActivityTxDetailBinding
    private lateinit var dimmerViews: MutableList<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityTxDetailBinding.inflate(layoutInflater).apply { setContentView(root) }
        TxDetailActivityVisitor.visit(this)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        tx =
            if (savedInstanceState == null) intent.getParcelableExtra(TX_DETAIL_EXTRA_KEY) as Tx
            else savedInstanceState.getParcelable(TX_DETAIL_EXTRA_KEY)!!
        setupUi()
        tracker.screen(path = "/home/tx_details", title = "Transaction Details")
    }

    override fun onStart() {
        super.onStart()
        // start service if not started yet
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            Log.i("Debug", "Issuing bindService (${System.currentTimeMillis()})")
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        unbindService(this)
        EventBus.unsubscribe(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(TX_DETAIL_EXTRA_KEY, this.tx)
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service.")
        Log.i(
            "Debug",
            "Service connected (${System.currentTimeMillis()})"
        )
        enableCTAs()
        walletService = TariWalletService.Stub.asInterface(service)
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        disableCTAs()
        walletService = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun setupUi() {
        bindViews()
        setUiCommands()
        bindTxData()
        observeTxUpdates()
    }

    private fun bindViews() {
        if (this.walletService == null) {
            disableCTAs()
        }
        dimmerViews = mutableListOf(ui.topDimmerView, ui.bottomDimmerView)
        currentTextSize = dimen(add_amount_element_text_size)
        currentAmountGemSize = dimen(add_amount_gem_size)
        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
    }

    private fun disableCTAs() {
        arrayOf<TextView>(
            ui.addContactButton,
            ui.cancelTxView,
            ui.editContactLabelTextView
        ).forEach {
            it.isEnabled = false
            it.setTextColor(color(disabled_cta))
        }
    }

    private fun enableCTAs() {
        arrayOf<TextView>(ui.addContactButton, ui.cancelTxView, ui.editContactLabelTextView)
            .forEach { it.isEnabled = true }
        ui.addContactButton.setTextColor(color(purple))
        ui.editContactLabelTextView.setTextColor(color(purple))
        ui.cancelTxView.setTextColor(color(tx_detail_cancel_tx_cta))
    }

    private fun setUiCommands() {
        ui.backView.setOnClickListener { onBackPressed() }
        ui.emojiIdSummaryContainerView.setOnClickListener { onEmojiSummaryClicked(it) }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked(it) }
        ui.copyEmojiIdButton.setOnLongClickListener { view ->
            onCopyEmojiIdButtonLongClicked(view)
            true
        }
        dimmerViews.forEach { it.setOnClickListener { hideFullEmojiId() } }
        ui.feeLabelTextView.setOnClickListener { showTxFeeToolTip() }
        ui.addContactButton.setOnClickListener { onAddContactClick() }
        ui.editContactLabelTextView.setOnClickListener { onEditContactClick() }
        ui.createContactEditText.setOnEditorActionListener { _, actionId, _ ->
            onContactEditTextEditAction(actionId)
        }
        ui.cancelTxView.setOnClickListener { onTransactionCancel() }
    }

    private fun bindTxData() {
        Log.i("Debug", "Current TX: $tx")
        val tx = this.tx
        setTxStatusData(tx)
        setTxMetaData(tx)
        setTxAddresseeData(tx)
        setTxPaymentData(tx)
    }

    private fun setTxPaymentData(tx: Tx) {
        ui.amountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        ui.paymentStateTextView.text = when (tx) {
            is CompletedTx ->
                if (tx.direction == Tx.Direction.INBOUND) string(tx_detail_payment_received)
                else string(tx_detail_payment_sent)
            is PendingInboundTx -> string(tx_detail_pending_payment_received)
            is PendingOutboundTx -> string(tx_detail_pending_payment_sent)
            else ->
                throw IllegalArgumentException("Unexpected transaction type for transaction: ${tx.id}")
        }
        when {
            tx is CompletedTx && tx.direction == Tx.Direction.OUTBOUND -> setFeeData(tx.fee)
            tx is PendingOutboundTx -> setFeeData(tx.fee)
            else -> ui.txFeeGroup.gone()
        }
        ui.amountContainerView.post { scaleDownAmountTextViewIfRequired() }
    }

    private fun setFeeData(fee: MicroTari) {
        ui.txFeeGroup.visible()
        ui.txFeeTextView.text = string(
            tx_details_fee_value,
            WalletUtil.amountFormatter.format(fee.tariValue)
        )
    }

    private fun setTxMetaData(tx: Tx) {
        ui.dateTextView.text = Date(tx.timestamp.toLong() * 1000).txFormattedDate()
        ui.txIdTextView.text = string(tx_detail_transaction_id, tx.id)
        ui.txNoteTextView.text = tx.message
        if (tx.message.isBlank()) ui.noteLabelTextView.invisible()
    }

    private fun setTxAddresseeData(tx: Tx) {
        val user = tx.user
        if (user is Contact) {
            ui.contactContainerView.visible()
            setUIAlias(user.alias)
        } else {
            ui.addContactButton.visible()
            ui.contactContainerView.gone()
        }
        ui.fromTextView.text = when (tx) {
            is CompletedTx ->
                if (tx.direction == Tx.Direction.INBOUND) string(common_from)
                else string(common_to)
            is PendingInboundTx -> string(common_from)
            is PendingOutboundTx -> string(common_to)
            else ->
                throw IllegalArgumentException("Unexpected transaction type for transaction: ${tx.id}")
        }
        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            tx.user.publicKey.emojiId,
            string(emoji_id_chunk_separator), color(black), color(light_gray)
        )
        emojiIdSummaryController.display(tx.user.publicKey.emojiId)
    }

    private fun setTxStatusData(tx: Tx) {
        val statusText = when {
            tx is PendingOutboundTx && tx.status == PENDING ->
                string(tx_detail_waiting_for_recipient)
            tx is PendingOutboundTx && (tx.status == COMPLETED ||
                    tx.status == BROADCAST) -> string(tx_detail_broadcasting)
            tx is PendingInboundTx && (tx.status == PENDING ||
                    tx.status == COMPLETED) -> string(tx_detail_waiting_for_sender_to_complete)
            tx is PendingInboundTx -> string(tx_detail_broadcasting)
            else -> ""
        }
        ui.statusTextView.text = statusText
        if (statusText.isEmpty()) {
            ui.statusContainerView.gone()
        } else {
            ui.statusContainerView.visible()
        }
        if (tx is PendingOutboundTx && tx.status == PENDING) {
            ui.cancelTxContainerView.setOnClickListener { onTransactionCancel() }
            ui.cancelTxContainerView.visible()
        } else if (ui.cancelTxContainerView.visibility == View.VISIBLE) {
            ui.cancelTxContainerView.setOnClickListener(null)
            collapseAndHideAnimation(ui.cancelTxContainerView).start()
        }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert =
            ui.amountContainerView.getLastChild()!!.right - ui.amountContainerView.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + ui.amountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > ui.amountContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor

        // adjust gem size
        UiUtil.setWidthAndHeight(
            ui.amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        ui.amountTextView.setTextSizePx(currentTextSize)
    }

    private fun observeTxUpdates() {
        EventBus.subscribe<Event.Wallet.TxBroadcast>(this) { updateTxData(it.completedTx) }
        EventBus.subscribe<Event.Wallet.TxFinalized>(this) { updateTxData(it.completedTx) }
        EventBus.subscribe<Event.Wallet.TxMined>(this) { updateTxData(it.completedTx) }
        EventBus.subscribe<Event.Wallet.TxReplyReceived>(this) { updateTxData(it.completedTx) }
        EventBus.subscribe<Event.Wallet.TxCancellation>(this) {
            //  Main thread invocation is necessary due to happens-before relationship guarantee
            Handler(Looper.getMainLooper()).post {
                if (it.completedTx.id == this.tx.id) {
                    // TODO handle cancellation
                }
            }
        }
    }

    private fun updateTxData(tx: Tx) {
        //  Main thread invocation is necessary due to happens-before relationship guarantee
        Handler(Looper.getMainLooper()).post {
            if (tx.id == this.tx.id) {
                this.tx = tx
                bindTxData()
            }
        }
    }

    private fun onEmojiSummaryClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        // make dimmers non-clickable until the anim is over
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        // prepare views
        ui.emojiIdSummaryContainerView.invisible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = ui.emojiIdContainerView.width - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            ui.fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        ui.fullEmojiIdContainerView.alpha = 0f
        ui.fullEmojiIdContainerView.visible()
        // scroll to end
        ui.fullEmojiIdScrollView.post {
            ui.fullEmojiIdScrollView.scrollTo(
                ui.fullEmojiIdTextView.width - ui.fullEmojiIdScrollView.width,
                0
            )
        }
        ui.copyEmojiIdContainerView.alpha = 0f
        ui.copyEmojiIdContainerView.visible()
        UiUtil.setBottomMargin(
            ui.copyEmojiIdContainerView,
            0
        )
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // display overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = value * 0.6f
            }
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = value
            ui.fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            ui.fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdContainerView,
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim, copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView -> dimmerView.isClickable = true }
            }
        })
        // scroll animation
        ui.fullEmojiIdScrollView.postDelayed({
            ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true) {
        dimmerViews.forEach { UiUtil.temporarilyDisableClick(it) }
        ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        ui.emojiIdSummaryContainerView.visible()
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdContainerView,
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        // emoji id anim
        val fullEmojiIdInitialWidth = ui.emojiIdContainerView.width
        val fullEmojiIdDeltaWidth =
            ui.emojiIdSummaryContainerView.width - ui.emojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // hide overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = (1 - value) * 0.6f
            }
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView ->
                    dimmerView.gone()
                }
                ui.fullEmojiIdContainerView.gone()
                ui.copyEmojiIdContainerView.gone()
            }
        })
    }

    private fun completeCopyEmojiId(clipboardString: String) {
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Identity",
            clipboardString
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false)
        }
        val copyEmojiIdButtonAnim = ui.copyEmojiIdContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(tx.user.publicKey.emojiId)
    }

    private fun onCopyEmojiIdButtonLongClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(tx.user.publicKey.hexString)
    }

    private fun onContactEditTextEditAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val alias = ui.createContactEditText.text?.toString()
            if (!alias.isNullOrEmpty()) {
                updateContactAlias(alias)
                setUIAlias(alias)
            }
            ui.contactLabelTextView.setTextColor(color(tx_detail_contact_name_label_text))
            return false
        }
        return true
    }

    private fun onTransactionCancel() {
        val service = this.walletService ?: return
        val tx = this.tx
        if (tx is PendingOutboundTx && tx.direction == Tx.Direction.OUTBOUND && tx.status == PENDING) {
            showTxCancelDialog(service)
        } else {
            Logger.e(
                "cancelTransaction was issued, but current transaction is not pending " +
                        "outbound, but rather $tx"
            )
        }
    }

    private fun showTxCancelDialog(service: TariWalletService) {
        BottomSlideDialog(
            this,
            R.layout.dialog_cancel_tx,
            dismissViewId = R.id.cancel_tx_dialog_not_cancel_button
        ).apply {
            findViewById<Button>(R.id.cancel_tx_dialog_cancel_button)
                .setOnClickListener {
                    cancelTransaction(service)
                    dismiss()
                }
        }.show()
    }

    private fun cancelTransaction(service: TariWalletService) {
        val error = WalletError()
        val isCancelled = service.cancelPendingTx(TxId(this.tx.id), error)
        if (isCancelled || error.code == WalletErrorCode.NO_ERROR) {
            this.ui.cancelTxView.setOnClickListener(null)
            finish()
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
        } else {
            ErrorDialog(
                this, string(tx_detail_cancellation_error_title),
                string(tx_detail_cancellation_error_description)
            ).show()
            Logger.e(
                "Error occurred during TX cancellation.\nCancelled? $isCancelled" +
                        "\nError: $error"
            )
        }
    }

    private fun updateContactAlias(newAlias: String) {
        if (walletService == null) {
            return
        }
        val error = WalletError()
        walletService?.updateContactAlias(
            tx.user.publicKey,
            newAlias,
            error
        )
        if (error.code == WalletErrorCode.NO_ERROR) {
            (tx.user as? Contact)?.alias = newAlias
            EventBus.post(
                Event.Contact.ContactAddedOrUpdated(
                    tx.user.publicKey,
                    newAlias
                )
            )
        } else {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun setUIAlias(alias: String) {
        ui.contactNameTextView.visible()
        ui.createContactEditText.invisible()
        ui.contactNameTextView.text = alias
        ui.editContactLabelTextView.visible()
        ui.addContactButton.invisible()
    }

    /**
     * Add contact alias.
     */
    private fun onAddContactClick() {
        ui.contactContainerView.visible()
        ui.addContactButton.invisible()
        ui.createContactEditText.visible()
        ui.contactLabelTextView.visible()
        ui.editContactLabelTextView.invisible()

        focusContactEditText()
    }

    private fun focusContactEditText() {
        ui.createContactEditText.post {
            ui.createContactEditText.requestFocus()
            ui.createContactEditText.setSelection(ui.createContactEditText.text?.length ?: 0)
            UiUtil.showKeyboard(this)
        }
        ui.contactLabelTextView.setTextColor(color(black))
    }

    /**
     * Edit contact alias.
     */
    private fun onEditContactClick() {
        ui.editContactLabelTextView.invisible()
        ui.createContactEditText.visible()
        focusContactEditText()
        val user = tx.user
        if (user is Contact) {
            ui.createContactEditText.setText(user.alias)
        }
        ui.contactNameTextView.invisible()
    }

    private fun showTxFeeToolTip() {
        BottomSlideDialog(
            context = this,
            layoutId = R.layout.tx_fee_tooltip_dialog,
            dismissViewId = R.id.tx_fee_tooltip_dialog_txt_close
        ).show()
    }

    private object TxDetailActivityVisitor {
        internal fun visit(activity: TxDetailActivity) {
            (activity.application as TariWalletApplication).appComponent.inject(activity)
        }
    }

}
