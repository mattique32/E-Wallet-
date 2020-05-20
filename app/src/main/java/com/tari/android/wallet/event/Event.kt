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
package com.tari.android.wallet.event

import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.fragment.send.FinalizeSendTxFragment

/**
 * App-wide events.
 */
object Event {

    object App {
        class AppBackgrounded
        class AppForegrounded
    }

    /**
     * Wallet events.
     */
    object Wallet {
        data class TxBroadcast(val completedTx: CompletedTx)
        data class TxMined(val completedTx: CompletedTx)
        data class TxReceived(val pendingInboundTx: PendingInboundTx)
        data class TxReplyReceived(val completedTx: CompletedTx)
        data class TxFinalized(val completedTx: CompletedTx)
        data class DirectSendResult(val txId: TxId, val success: Boolean)
        data class StoreAndForwardSendResult(val txId: TxId, val success: Boolean)
        data class TxCancellation(val completedTx: CompletedTx)
        data class BaseNodeSyncComplete(val rxId: RxId, val success: Boolean)
    }

    /**
     * Contact events.
     */
    object Contact {
        data class ContactAddedOrUpdated(val contactPublicKey: PublicKey, val contactAlias: String)
    }

    /**
     * Transaction events.
     */
    object Tx {
        class TxSendSuccessful
        data class TxSendFailed(val failureReason: FinalizeSendTxFragment.FailureReason)
    }

    object Testnet {
        class TestnetTariRequestSuccessful
        data class TestnetTariRequestError(val errorMessage: String)
    }

}