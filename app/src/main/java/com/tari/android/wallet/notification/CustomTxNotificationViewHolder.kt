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
package com.tari.android.wallet.notification

import android.app.KeyguardManager
import android.content.Context
import android.icu.text.BreakIterator
import android.view.View
import android.widget.RemoteViews
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.util.WalletUtil

/**
 * Displays custom transaction notification.
 *
 * @author The Tari Development Team
 */
class CustomTxNotificationViewHolder(val context: Context, tx: Tx) : RemoteViews(context.packageName, R.layout.notification_remote_tx) {

    init {
        val user = tx.tariContact
        if (user.alias.isNotEmpty()) {
            displayTxContactAlias(user)
        } else {
            displayTxUserEmojiId(user)
        }
        displayTxMessage(tx.message)

        // amount - display just "NEW" if the screen is locked
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        val deviceIsLocked = keyguardManager?.isDeviceLocked ?: true
        when (tx.direction) {
            Tx.Direction.INBOUND -> displayIncomingTxValue(tx.amount, deviceIsLocked)
            Tx.Direction.OUTBOUND -> displayOutgoingTxValue(tx.amount, deviceIsLocked)
        }

    }

    private fun displayTxContactAlias(tariContact: TariContact) {
        setTextViewText(R.id.notification_tx_received_txt_contact_alias, tariContact.alias)
        setViewVisibility(R.id.notification_tx_received_vw_emoji_summary, View.INVISIBLE)
    }

    private fun displayTxUserEmojiId(tariContact: TariContact) {
        setTextViewText(R.id.notification_tx_received_txt_contact_alias, "")
        setViewVisibility(R.id.notification_tx_received_txt_contact_alias, View.INVISIBLE)
        val emojis = ArrayList<String>()
        val it: BreakIterator = BreakIterator.getCharacterInstance()
        it.setText(tariContact.walletAddress.fullEmojiId)
        // TODO move it to EmojiUtil
        var previous = 0
        while (it.next() != BreakIterator.DONE) {
            val builder = StringBuilder()
            for (i in previous until it.current()) {
                builder.append(tariContact.walletAddress.fullEmojiId[i])
            }
            emojis.add(builder.toString())
            previous = it.current()
        }
        setTextViewText(R.id.emoji_id_summary_emoji_1_text_view, emojis[0])
        setTextViewText(R.id.emoji_id_summary_emoji_2_text_view, emojis[1])
        setTextViewText(R.id.emoji_id_summary_emoji_3_text_view, emojis[2])
        setTextViewText(R.id.emoji_id_summary_emoji_4_text_view, emojis.takeLast(3)[0])
        setTextViewText(R.id.emoji_id_summary_emoji_5_text_view, emojis.takeLast(2)[0])
        setTextViewText(R.id.emoji_id_summary_emoji_6_text_view, emojis.takeLast(1)[0])
    }

    private fun displayTxMessage(message: String) {
        setTextViewText(R.id.notification_tx_received_txt_message, message)
    }

    private fun displayIncomingTxValue(amount: MicroTari, deviceIsLocked: Boolean) {
        if (deviceIsLocked) {
            setTextViewText(R.id.notification_tx_received_txt_positive_amount, context.getString(R.string.common_new_uppercase))
        } else {
            val formattedValue = "+" + WalletUtil.amountFormatter.format(amount.tariValue)
            setTextViewText(R.id.notification_tx_received_txt_positive_amount, formattedValue)
        }
        setViewVisibility(R.id.notification_tx_received_txt_positive_amount, View.VISIBLE)
        setViewVisibility(R.id.notification_tx_received_txt_negative_amount, View.GONE)
    }

    private fun displayOutgoingTxValue(amount: MicroTari, deviceIsLocked: Boolean) {
        if (deviceIsLocked) {
            setTextViewText(R.id.notification_tx_received_txt_negative_amount, context.getString(R.string.common_new_uppercase))
        } else {
            val formattedValue = "-" + WalletUtil.amountFormatter.format(amount.tariValue)
            setTextViewText(R.id.notification_tx_received_txt_negative_amount, formattedValue)
        }
        setViewVisibility(R.id.notification_tx_received_txt_negative_amount, View.VISIBLE)
        setViewVisibility(R.id.notification_tx_received_txt_positive_amount, View.GONE)
    }
}
