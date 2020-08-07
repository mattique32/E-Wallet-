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

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.home_selected_nav_item
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.GiphyEcosystem
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.service.connection.TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED
import com.tari.android.wallet.service.connection.TariWalletServiceConnection.TariWalletServiceConnectionFactory
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.activity.settings.BackupSettingsActivity
import com.tari.android.wallet.ui.activity.tx.TxDetailsActivity
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.profile.WalletInfoFragment
import com.tari.android.wallet.ui.fragment.settings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.store.StoreFragment
import com.tari.android.wallet.ui.fragment.tx.TxListFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import javax.inject.Inject

internal class HomeActivity : AppCompatActivity(), AllSettingsFragment.AllSettingsRouter,
    TxListFragment.TxListRouter {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var giphy: GiphyEcosystem

    private lateinit var ui: ActivityHomeBinding
    private lateinit var serviceConnection: TariWalletServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        appComponent.inject(this)
        if (!sharedPrefsWrapper.isAuthenticated) {
            startActivity(Intent(this, SplashActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            finish()
            return
        }
        giphy.enable()
        serviceConnection = ViewModelProvider(this, TariWalletServiceConnectionFactory(this))
            .get(TariWalletServiceConnection::class.java)
        ui = ActivityHomeBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setupViewPagerListener()
        if (savedInstanceState == null) {
            enableNavigationView(ui.homeImageView)
            checkDeepLinkWhenServiceIsConnected()
        } else {
            val index = savedInstanceState.getInt(KEY_PAGE)
            ui.viewPager.setCurrentItem(index, false)
            enableNavigationView(index)
        }
        setupUi()
    }

    private fun setupViewPagerListener() {
        ui.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // No-op
            }

            override fun onPageScrollStateChanged(state: Int) {
                // No-op
            }

            override fun onPageSelected(position: Int) {
                // No-op
                if (position == INDEX_HOME) {
                    ui.bottomElevationView.visible()
                    ui.bottomNavigationView.visible()
                    ui.sendTariCtaView.visible()
                } else {
                    ui.bottomElevationView.gone()
                    ui.bottomNavigationView.gone()
                    ui.sendTariCtaView.gone()
                }
            }

        })
    }

    private fun checkDeepLinkWhenServiceIsConnected() {
        serviceConnection.connection.observe(this, Observer {
            if (it.status == CONNECTED) {
                ui.root.postDelayed(Constants.UI.mediumDurationMs) {
                    processIntentDeepLink(it.service!!, intent)
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (serviceConnection.currentState.status == CONNECTED) {
            processIntentDeepLink(serviceConnection.currentState.service!!, intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, ui.viewPager.currentItem)
    }

    override fun onBackPressed() {
        if (ui.viewPager.currentItem == INDEX_HOME) {
            super.onBackPressed()
        } else {
            ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.homeImageView)
        }
    }

    private fun setupUi() {
        setupBottomNavigation()
        setupCTAs()
    }

    private fun setupCTAs() {
        ui.sendTariCtaView.setOnClickListener {
            if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
                showInternetConnectionErrorDialog(this)
            } else {
                startActivity(Intent(this, SendTariActivity::class.java))
                overridePendingTransition(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left
                )
            }
        }
    }

    private fun setupBottomNavigation() {
        enableNavigationView(ui.homeImageView)
        ui.viewPager.adapter = HomeAdapter(supportFragmentManager)
        ui.viewPager.offscreenPageLimit = 3
        ui.homeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.homeImageView)
        }
        ui.storeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_STORE, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.storeImageView)
        }
        ui.walletInfoView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_PROFILE, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.walletInfoImageView)
        }
        ui.settingsView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.settingsImageView)
        }
    }

    private fun enableNavigationView(index: Int) {
        val view: ImageView = when (index) {
            INDEX_HOME -> ui.homeImageView
            INDEX_STORE -> ui.storeImageView
            INDEX_PROFILE -> ui.walletInfoImageView
            INDEX_SETTINGS -> ui.settingsImageView
            else -> error("Unexpected index: $index")
        }
        enableNavigationView(view)
    }

    private fun enableNavigationView(view: ImageView) {
        arrayOf(ui.homeImageView, ui.storeImageView, ui.walletInfoImageView, ui.settingsImageView)
            .forEach { it.clearColorFilter() }
        view.setColorFilter(color(home_selected_nav_item))
    }

    override fun toTxDetails(tx: Tx) = startActivity(TxDetailsActivity.createIntent(this, tx))

    override fun toTTLStore() = ui.viewPager.setCurrentItem(INDEX_STORE, NO_SMOOTH_SCROLL)

    override fun toAllSettings() = ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)

    override fun toBackupSettings() =
        startActivity(Intent(this, BackupSettingsActivity::class.java))

    private fun processIntentDeepLink(service: TariWalletService, intent: Intent) {
        DeepLink.from(intent.data?.toString() ?: "")?.let { deepLink ->
            val pubkey = when (deepLink.type) {
                DeepLink.Type.EMOJI_ID -> service.getPublicKeyFromEmojiId(deepLink.identifier)
                DeepLink.Type.PUBLIC_KEY_HEX -> service.getPublicKeyFromHexString(deepLink.identifier)
            }
            pubkey?.let { publicKey -> sendTariToUser(service, publicKey, deepLink.parameters) }
        }
    }

    private fun sendTariToUser(
        service: TariWalletService,
        recipientPublicKey: PublicKey,
        parameters: Map<String, String>
    ) {
        val error = WalletError()
        val contacts = service.getContacts(error)
        val recipientUser = when (error.code) {
            WalletErrorCode.NO_ERROR -> contacts
                .firstOrNull { it.publicKey == recipientPublicKey } ?: User(recipientPublicKey)
            else -> User(recipientPublicKey)
        }
        val intent = Intent(this, SendTariActivity::class.java)
        intent.putExtra("recipientUser", recipientUser)
        parameters[DeepLink.PARAMETER_NOTE]?.let { intent.putExtra(DeepLink.PARAMETER_NOTE, it) }
        parameters[DeepLink.PARAMETER_AMOUNT]?.toDoubleOrNull()
            ?.let { intent.putExtra(DeepLink.PARAMETER_AMOUNT, it) }
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
    }

    private class HomeAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment =
            when (position) {
                INDEX_HOME -> TxListFragment()
                INDEX_STORE -> StoreFragment.newInstance()
                INDEX_PROFILE -> WalletInfoFragment()
                INDEX_SETTINGS -> AllSettingsFragment.newInstance()
                else -> error("Unexpected position: $position")
            }

        override fun getCount(): Int = 4

    }

    companion object {
        private const val KEY_PAGE = "key_page"
        private const val INDEX_HOME = 0
        private const val INDEX_STORE = 1
        private const val INDEX_PROFILE = 2
        private const val INDEX_SETTINGS = 3
        private const val NO_SMOOTH_SCROLL = false
    }
}
