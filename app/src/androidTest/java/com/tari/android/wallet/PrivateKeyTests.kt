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

import com.tari.android.wallet.ffi.NULL_POINTER
import com.tari.android.wallet.ffi.PrivateKey
import org.junit.Test

import org.junit.Assert.*

/**
 * FFI private key tests.
 *
 * @author Kutsal Kaan Bilgin
 */
class PrivateKeyTests {


    @Test
    fun testGenerateAndDestroyPrivateKey() {
        val privateKey = PrivateKey.generate()
        assertTrue(privateKey.ptr != NULL_POINTER)
        privateKey.destroy()
        assertTrue(privateKey.ptr == NULL_POINTER)
    }

    @Test
    fun testCreatePrivateKeyFromInvalidHexString() {
        val invalidHexString = "invalid_hex_string"
        val privateKey = PrivateKey.fromHex(invalidHexString)
        assertTrue(privateKey.ptr == NULL_POINTER)
    }

    @Test
    fun testCreatePrivateKeyFromHexStringAndGetBytes() {
        val privateKey = PrivateKey.fromHex(TestUtil.PRIVATE_KEY_HEX_STRING)
        assertTrue(privateKey.ptr != NULL_POINTER)
        val privateKeyBytes = privateKey.bytes
        assertEquals(TestUtil.PRIVATE_KEY_HEX_STRING, privateKeyBytes.hexString)
        // free resources
        privateKeyBytes.destroy()
        privateKey.destroy()
    }

    @Test
    fun testCreatePrivateKeyFromBytesAndGetBytes() {
        // generate private key and get bytes
        val privateKey = PrivateKey.fromHex(TestUtil.PRIVATE_KEY_HEX_STRING)
        val bytes = privateKey.bytes
        val privateKeyFromBytes = PrivateKey.create(bytes)
        assertTrue(privateKeyFromBytes.ptr != NULL_POINTER)
        // test bytes
        val newBytes = privateKeyFromBytes.bytes
        assertTrue(newBytes.hexString == TestUtil.PRIVATE_KEY_HEX_STRING)
        // free resources
        newBytes.destroy()
        bytes.destroy()
        privateKey.destroy()
        privateKeyFromBytes.destroy()
    }

}
