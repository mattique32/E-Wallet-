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

import com.tari.android.wallet.ffi.NULL_POINTER
import org.junit.Assert.*
import org.junit.Test

class WalletBasicTests {

    val wallet = TestUtil.testWallet

    @Test
    fun testCreateWallet() {
        assertTrue(wallet.ptr != NULL_POINTER)
    }

    @Test
    fun testGetPublicKey() {
        val publicKey = wallet.getPublicKey()
        val bytes = publicKey.bytes
        assertEquals(TestUtil.PUBLIC_KEY_HEX_STRING, bytes.hexString)
        bytes.destroy()
        publicKey.destroy()
    }

    /**
    @Test
    fun testBalances() {
        // this returns non-zero after test data generation
        assertEquals(0, wallet.getAvailableBalance())
        assertEquals(0, wallet.getPendingIncomingBalance())
        assertEquals(0, wallet.getPendingOutgoingBalance())
    }
    */

}