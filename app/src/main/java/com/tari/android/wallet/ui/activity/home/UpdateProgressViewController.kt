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
package com.tari.android.wallet.ui.activity.home

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.addListener
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.WalletErrorCode
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Controls the transaction list update (pull to refresh) views.
 *
 * @author The Tari Development Team
 */
internal class UpdateProgressViewController(
    private val view: View,
    listener: Listener
) : CoroutineScope {

    private val mJob = Job()
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    enum class FailureReason {
        NETWORK_CONNECTION_ERROR,
        BASE_NODE_CONNECTION_ERROR
    }

    enum class State {
        IDLE,
        RUNNING,
        RECEIVING
    }

    private val hourglassIconTextView: TextView =
        view.findViewById(R.id.home_txt_update_hourglass_icon)
    private val handshakeIconTextView: TextView =
        view.findViewById(R.id.home_txt_update_handshake_icon)
    private val checkingForUpdatesTextView: TextView =
        view.findViewById(R.id.home_txt_checking_for_updates)
    private val receivingTxsTextView: TextView = view.findViewById(R.id.home_txt_receiving_txs)
    private val completingTxsTextView: TextView = view.findViewById(R.id.home_txt_completing_txs)
    private val updatingTxsTextView: TextView = view.findViewById(R.id.home_txt_updating_txs)
    private val upToDateTextView: TextView = view.findViewById(R.id.home_txt_up_to_date)
    private val progressBar: ProgressBar = view.findViewById(R.id.home_prog_bar_update)

    var state = State.IDLE
        private set
    private var isReset = true

    private var baseNodeSyncCurrentRetryCount = 0
    private var baseNodeSyncMaxRetryCount = 3
    private var connectionCheckStartTimeMs = 0L

    // connection status check (internet, tor, base node) timeout period
    private val timeoutPeriodMs = 40 * 1000L
    private val minStateDisplayPeriodMs = 3 * 1000L
    private var baseNodeSyncTimeoutSubscription: Disposable? = null
    private var baseNodeSyncRequestId: BigInteger? = null

    private val listenerWeakReference: WeakReference<Listener> = WeakReference(listener)

    private lateinit var walletService: TariWalletService

    private var numberOfReceivedTxs = 0
    private var numberOfBroadcastTxs = 0
    private var numberOfCancelledTxs = 0
    private var isWaitingOnBaseNodeSync = false
    private lateinit var currentTextView: TextView

    private var torBootstrapStatusSubscription: Disposable? = null

    init {
        UiUtil.setProgressBarColor(progressBar, view.color(R.color.purple))
        progressBar.invisible()
        subscribeToEventBus()
    }

    fun reset() {
        if (isReset) {
            return
        }
        baseNodeSyncCurrentRetryCount = 0
        // emojis
        UiUtil.setTopMargin(hourglassIconTextView, 0)
        hourglassIconTextView.alpha = 1f
        handshakeIconTextView.alpha = 1f
        // text views
        checkingForUpdatesTextView.visible()
        UiUtil.setTopMargin(checkingForUpdatesTextView, 0)
        receivingTxsTextView.visible()
        UiUtil.setTopMargin(receivingTxsTextView, 0)
        completingTxsTextView.visible()
        UiUtil.setTopMargin(completingTxsTextView, 0)
        updatingTxsTextView.visible()
        UiUtil.setTopMargin(updatingTxsTextView, 0)
        upToDateTextView.visible()
        UiUtil.setTopMargin(upToDateTextView, 0)
        upToDateTextView.alpha = 1f
        isReset = true
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Wallet.BaseNodeSyncComplete>(this) { event ->
            Logger.i("Received BaseNodeSyncComplete in state %s.", state)
            if (state == State.RUNNING && isWaitingOnBaseNodeSync) {
                onBaseNodeSyncCompleted(event)
            }
        }
        EventBus.subscribe<Event.Wallet.TxReceived>(this) {
            if (state == State.RUNNING || state == State.RECEIVING) {
                numberOfReceivedTxs++
            }
        }
        EventBus.subscribe<Event.Wallet.TxBroadcast>(this) {
            if (state == State.RUNNING || state == State.RECEIVING) {
                numberOfBroadcastTxs++
            }
        }
        EventBus.subscribe<Event.Wallet.TxCancelled>(this) {
            if (state == State.RUNNING || state == State.RECEIVING) {
                numberOfCancelledTxs++
            }
        }
    }

    private fun connectionCheckHasTimedOut(): Boolean {
        return System.currentTimeMillis() > (connectionCheckStartTimeMs + timeoutPeriodMs)
    }

    fun start(walletService: TariWalletService) {
        this.walletService = walletService
        Logger.d("Start update.")
        progressBar.visible()
        isReset = false
        numberOfReceivedTxs = 0
        numberOfCancelledTxs = 0
        numberOfBroadcastTxs = 0
        state = State.RUNNING
        currentTextView = checkingForUpdatesTextView
        connectionCheckStartTimeMs = System.currentTimeMillis()
        checkNetworkConnectionStatus()
    }

    private fun checkNetworkConnectionStatus() {
        Logger.d("Start connection check.")
        val networkConnectionState = EventBus.networkConnectionStateSubject.value
        if (networkConnectionState != NetworkConnectionState.CONNECTED) {
            Logger.e("Update error: not connected to the internet.")
            view.postDelayed({
                fail(FailureReason.NETWORK_CONNECTION_ERROR)
            }, minStateDisplayPeriodMs)
            return
        }
        checkTorBootstrapStatus()
    }

    @SuppressLint("CheckResult")
    private fun checkTorBootstrapStatus() {
        Logger.d("Check bootstrap status.")
        torBootstrapStatusSubscription?.dispose()
        // check for expiration
        if (connectionCheckHasTimedOut()) {
            fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
            return
        }
        val torProxyState = EventBus.torProxyStateSubject.value
        // check whether Tor proxy is running
        if (torProxyState !is TorProxyState.Running) {
            // either not connected or Tor proxy is not running
            Logger.e("Update error: Tor proxy is not running.")
            view.postDelayed({
                fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
            }, minStateDisplayPeriodMs)
            return
        }
        // check Tor bootstrap status
        if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.maxProgress) {
            Logger.d(
                "Tor bootstrap not complete. Try again in %d seconds.",
                minStateDisplayPeriodMs
            )
            // check again after a wait period
            torBootstrapStatusSubscription = Observable
                .timer(minStateDisplayPeriodMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { checkTorBootstrapStatus() }
            return
        }
        syncWithBaseNode()
    }

    private fun syncWithBaseNode() {
        // check for expiration
        if (connectionCheckHasTimedOut()) {
            fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
            return
        }
        baseNodeSyncCurrentRetryCount++
        // sync base node
        Logger.d(
            "Try to sync with base node - retry count %d.",
            baseNodeSyncCurrentRetryCount
        )
        val walletError = WalletError()
        // long running call
        launch(Dispatchers.IO) {
            baseNodeSyncRequestId = walletService.syncWithBaseNode(walletError).value
            if (isActive && walletError.code != WalletErrorCode.NO_ERROR) {
                Logger.e("Base node sync has failed with err code %d.", walletError.code)
                fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
            }
        }
        isWaitingOnBaseNodeSync = true
        // setup expiration timer
        val toTimeoutMs =
            (connectionCheckStartTimeMs + timeoutPeriodMs) - System.currentTimeMillis()
        baseNodeSyncTimeoutSubscription = Observable
            .timer(toTimeoutMs, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { baseNodeSyncTimedOut() }
    }

    private fun onBaseNodeSyncCompleted(event: Event.Wallet.BaseNodeSyncComplete) {
        if (event.requestId.value != baseNodeSyncRequestId) {
            Logger.i("Request id mismatch - ignore base node sync event.")
            return
        }
        isWaitingOnBaseNodeSync = false
        baseNodeSyncTimeoutSubscription?.dispose()
        if (!event.success) {
            if (baseNodeSyncCurrentRetryCount >= baseNodeSyncMaxRetryCount) {
                // failed
                fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
            } else {
                syncWithBaseNode()
            }
            return
        }
        // base node sync successful - start listening for events
        Logger.d("Connection tests passed. Start listening for wallet events.")
        state = State.RECEIVING
        view.postDelayed({
            displayReceivingTxs()
        }, minStateDisplayPeriodMs)
    }

    private fun displayReceivingTxs() {
        if (numberOfReceivedTxs == 0) {
            receivingTxsTextView.gone()
            displayCompletingTxs()
            return
        }
        animateToTextView(receivingTxsTextView)
        view.postDelayed({
            displayCompletingTxs()
        }, minStateDisplayPeriodMs)
    }

    private fun displayCompletingTxs() {
        if (numberOfBroadcastTxs == 0) {
            completingTxsTextView.gone()
            displayUpdatingTxs()
            return
        }
        animateToTextView(completingTxsTextView)
        view.postDelayed({
            displayUpdatingTxs()
        }, minStateDisplayPeriodMs)
    }

    private fun displayUpdatingTxs() {
        if (numberOfCancelledTxs == 0) {
            updatingTxsTextView.gone()
            displayUpToDate()
            return
        }
        animateToTextView(updatingTxsTextView)
        view.postDelayed({
            displayUpToDate()
        }, minStateDisplayPeriodMs)
    }

    private fun displayUpToDate() {
        animateToTextView(upToDateTextView)
        progressBar.invisible()
        view.postDelayed({
            complete()
        }, Constants.UI.xLongDurationMs)
    }

    private fun complete() {
        state = State.IDLE
        listenerWeakReference.get()?.updateHasCompleted(
            this,
            numberOfReceivedTxs,
            numberOfCancelledTxs
        )
    }

    private fun animateToTextView(nextTextView: TextView, onComplete: (() -> Unit)? = null) {
        // animate text view
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener {
            val value = it.animatedValue as Float
            UiUtil.setTopMargin(
                currentTextView,
                (-currentTextView.height * value).toInt()
            )
            if (currentTextView == checkingForUpdatesTextView
                && nextTextView != upToDateTextView
            ) {
                UiUtil.setTopMargin(
                    hourglassIconTextView,
                    (-hourglassIconTextView.height * value).toInt()
                )
            } else if (nextTextView == upToDateTextView) {
                hourglassIconTextView.alpha = 1f - value
                handshakeIconTextView.alpha = 1f - value
            }
        }
        anim.duration = Constants.UI.mediumDurationMs
        anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        anim.start()
        anim.addListener(onEnd = {
            anim.removeAllListeners()
            currentTextView = nextTextView
            onComplete?.let { callback -> callback() }
        })
        // animate emoji view if required
    }

    private fun baseNodeSyncTimedOut() {
        Logger.e("Base node sync timed out.")
        fail(FailureReason.BASE_NODE_CONNECTION_ERROR)
    }

    private fun fail(failureReason: FailureReason) {
        state = State.IDLE
        listenerWeakReference.get()?.updateHasFailed(this, failureReason)
        isWaitingOnBaseNodeSync = false
        view.post {
            progressBar.invisible()
        }
    }

    fun destroy() {
        EventBus.unsubscribe(this)
        baseNodeSyncTimeoutSubscription?.dispose()
        cancel()
    }

    interface Listener {

        fun updateHasFailed(source: UpdateProgressViewController, failureReason: FailureReason)

        fun updateHasCompleted(
            source: UpdateProgressViewController,
            receivedTxCount: Int,
            cancelledTxCount: Int
        )

    }

}
