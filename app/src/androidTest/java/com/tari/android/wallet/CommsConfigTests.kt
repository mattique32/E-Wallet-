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

import androidx.test.platform.app.InstrumentationRegistry
import com.tari.android.wallet.ffi.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import java.lang.StringBuilder

/**
 * FFI comms config tests.
 *
 * @author The Tari Development Team
 */
class CommsConfigTests {

    private val dbName = "tari_test_db"
    private val controlServiceAddress = NetAddressString("127.0.0.1",80)
    private val listenerAddress = NetAddressString("0.0.0.0",80)
    private val datastorePath =
        StringBuilder()
            .append(TestUtil.WALLET_DATASTORE_PATH)
            .append("/")
            .append(dbName).toString()

    @Test
    fun testCommsConfig() {
        TestUtil.clearTestFiles(StringBuilder().append(datastorePath).toString())
        val privateKey = PrivateKey(HexString(TestUtil.PRIVATE_KEY_HEX_STRING))
        val commsConfig = CommsConfig(
            controlServiceAddress,
            listenerAddress,
            dbName,
            datastorePath,
            privateKey
        )
        assertTrue(commsConfig.getPointer() != nullptr)
        commsConfig.destroy()
        privateKey.destroy()
    }

    @Test(expected = FileSystemException::class)
    fun testByteVectorException() {
        TestUtil.clearTestFiles(StringBuilder().append(datastorePath).toString())
        val privateKey = PrivateKey(HexString(TestUtil.PRIVATE_KEY_HEX_STRING))
        val commsConfig = CommsConfig(
            controlServiceAddress,
            listenerAddress,
            dbName,
            StringBuilder().append(datastorePath).append("bad_dir").toString(),
            privateKey
        )
    }

}