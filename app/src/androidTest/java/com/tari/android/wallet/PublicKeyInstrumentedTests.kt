/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tari.android.wallet.ffi.NULL_POINTER
import com.tari.android.wallet.ffi.PrivateKey
import com.tari.android.wallet.ffi.PublicKey
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Test

/**
 * FFI private key tests.
 *
 * @author Kutsal Kaan Bilgin
 */
@RunWith(AndroidJUnit4::class)
class PublicKeyInstrumentedTests {

    private val publicKeyHexString =
        "30E1DFA197794858BFDBF96CDCE5DC8637D4BD1202DC694991040DDECBF42D40"

    @Test
    fun testCreatePublicKeyFromPrivateKey() {
        val privateKey = PrivateKey.generate()
        val publicKey = PublicKey.fromPrivateKey(privateKey)
        assertTrue(publicKey.ptr != NULL_POINTER)
        // free resources
        publicKey.destroy()
        privateKey.destroy()
    }

    @Test
    fun testCreatePublicKeyFromInvalidHexString() {
        val invalidHexString = "invalid_hex_string"
        val publicKey = PublicKey.fromHex(invalidHexString)
        assertTrue(publicKey.ptr == NULL_POINTER)
    }

    @Test
    fun testCreatePublicKeyFromHexStringAndGetBytes() {
        val publicKey = PublicKey.fromHex(publicKeyHexString)
        assertTrue(publicKey.ptr != NULL_POINTER)
        val publicKeyBytes = publicKey.bytes
        val index = 14
        assertTrue(publicKeyBytes.hexString[index] == publicKeyHexString[index])
        // free resources
        publicKeyBytes.destroy()
        publicKey.destroy()
    }

    @Test
    fun testCreatePublicKeyFromBytes() {
        val privateKey = PrivateKey.generate()
        val publicKey = PublicKey.fromPrivateKey(privateKey)
        val publicKeyBytes = publicKey.bytes
        val newPublicKey = PublicKey.create(publicKeyBytes)
        assertTrue(newPublicKey.ptr != NULL_POINTER)
        // check bytes
        val index = 14
        val newPublicKeyBytes = newPublicKey.bytes
        assertTrue(
            publicKeyBytes.hexString[index] == newPublicKeyBytes.hexString[index]
        )
        // free resources
        newPublicKeyBytes.destroy()
        newPublicKey.destroy()
        publicKeyBytes.destroy()
        publicKey.destroy()
        privateKey.destroy()
    }

}