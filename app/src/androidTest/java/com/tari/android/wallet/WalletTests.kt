/**
 * Copyright 2019 The Tari Project
 *
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
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
package com.tari.android.wallet

import com.tari.android.wallet.ffi.CommsConfig
import com.tari.android.wallet.ffi.NULL_POINTER
import com.tari.android.wallet.ffi.PrivateKey
import com.tari.android.wallet.ffi.Wallet
import org.junit.Assert.*
import org.junit.Test
import java.io.File



class WalletTests {

    fun clearTestFiles(path: String): Boolean
    {
        var fileDirectory = File(path)
        var del = fileDirectory.deleteRecursively()
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdir()
        }
        return del
    }
/*
    @Test
    fun testCreateAndDestroyWallet() {
        var clean = clearTestFiles(TestUtil.WALLET_DATASTORE_PATH)
        assertTrue(clean == true)
        val privateKey = PrivateKey.fromHex(TestUtil.PRIVATE_KEY_HEX_STRING)
        val commsConfig = CommsConfig.create(
            TestUtil.WALLET_CONTROL_SERVICE_ADDRESS,
            TestUtil.WALLET_LISTENER_ADDRESS,
            TestUtil.WALLET_DB_NAME,
            TestUtil.WALLET_DATASTORE_PATH,
            privateKey
        )
        val wallet = Wallet.create(commsConfig, TestUtil.WALLET_LOG_FILE_PATH)
        assertTrue(wallet.ptr != NULL_POINTER)
        wallet.destroy()
        assertTrue(wallet.ptr == NULL_POINTER)
        // free resources
        commsConfig.destroy()
        privateKey.destroy()
    }
*/
    @Test
    fun testGenerateTestData() {
        var clean = clearTestFiles(TestUtil.WALLET_DATASTORE_PATH)
        assertTrue(clean)
        val privateKey = PrivateKey.fromHex(TestUtil.PRIVATE_KEY_HEX_STRING)
        val commsConfig = CommsConfig.create(
            TestUtil.WALLET_CONTROL_SERVICE_ADDRESS,
            TestUtil.WALLET_LISTENER_ADDRESS,
            TestUtil.WALLET_DB_NAME,
            TestUtil.WALLET_DATASTORE_PATH,
            privateKey
        )
        val wallet = Wallet.create(commsConfig, TestUtil.WALLET_LOG_FILE_PATH)
        //TestUtil.printFFILogFile()
        assertTrue(wallet.ptr != NULL_POINTER)
        // should generate test data
        val success = wallet.generateTestData(TestUtil.WALLET_DATASTORE_PATH)
        assertTrue(success)
        // free resources
        commsConfig.destroy()
        privateKey.destroy()
        wallet.destroy()
    }
}