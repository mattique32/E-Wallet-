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

import com.tari.android.wallet.ffi.nullptr
import com.tari.android.wallet.ffi.PrivateKey
import com.tari.android.wallet.ffi.PublicKey
import com.tari.android.wallet.ffi.ByteVector
import com.tari.android.wallet.ffi.HexString
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * FFI private key tests.
 *
 * @author The Tari Development Team
 */
class PublicKeyTests {

    private val str = TestUtil.PUBLIC_KEY_HEX_STRING
    private val str2 = "A03DB4"

    @Test
    fun testCreatePublicKeyFromPrivateKey() {
        val privateKey = PrivateKey()
        val publicKey = PublicKey(privateKey)
        assertTrue(publicKey.getPointer() != nullptr)
        publicKey.destroy()
        privateKey.destroy()
        val publicKey2 = PublicKey(ByteVector(HexString(str)))
        assertTrue(publicKey2.getPointer() != nullptr)
        assertTrue(publicKey2.toString() == str)
        publicKey2.destroy()
        val publicKey3 = PublicKey(HexString(str))
        assertTrue(publicKey3.getPointer() != nullptr)
        assertTrue(publicKey3.toString() == str)
        publicKey3.destroy()
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun testHexStringException() {
        val publickey = PublicKey(ByteVector(HexString(str2)))
    }
}