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
package com.tari.android.wallet.ffi

import java.lang.RuntimeException
import java.math.BigInteger
import java.util.*

/**
 * Wrapper for native byte vector type.
 *
 * @author The Tari Development Team
 */

typealias ByteVectorPtr = Long

class ByteVector constructor(pointer:ByteVectorPtr) {

    private external fun jniGetLength(pByteVector: ByteVectorPtr, error: LibError): Int
    private external fun jniGetAt(pByteVector: ByteVectorPtr, index: Int, error: LibError): Int
    private external fun jniDestroy(pByteVector: ByteVectorPtr)
    private external fun jniCreate(byteArray: ByteArray, error: LibError): ByteVectorPtr

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    constructor(hex:HexString) : this(nullptr) {
        if (hex.toString().length == 64) {
            val byteArray = BigInteger(hex.toString(),16).toByteArray()
            var error = LibError()
            ptr = jniCreate(byteArray, error)
            if (error.code != 0)
            {
                throw RuntimeException()
            }
        } else
        {
            throw InvalidPropertiesFormatException("Argument is invalid")
        }
    }

    fun getLength(): Int
    {
        var error = LibError()
        val len = jniGetLength(ptr, error)
        if (error.code != 0)
        {
            throw RuntimeException()
        }
        return len
    }

    override fun toString(): String {
        return HexString(this).toString()
    }

    fun getPointer() : ByteVectorPtr {
        return ptr
    }

    fun getAt(index: Int): Int {
        var error = LibError()
        val byte = jniGetAt(ptr, index, error)
        if (error.code != 0)
        {
            throw RuntimeException()
        }
        return byte
    }

    fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}