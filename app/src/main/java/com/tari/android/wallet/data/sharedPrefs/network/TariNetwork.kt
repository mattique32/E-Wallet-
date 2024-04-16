package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.application.Network

data class TariNetwork(
    val network: Network,
    val dnsPeer: String,
    val ticker: String,
    val blockExplorerUrl: String? = null,
    val recommended: Boolean = false,
) {
    val isBlockExplorerAvailable: Boolean
        get() = blockExplorerUrl != null
}