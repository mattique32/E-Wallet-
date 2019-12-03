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

import java.util.*

/**
 * Wrapper for native byte vector type.
 *
 * @author Kutsal Kaan Bilgin
 */
class ByteVector(ptr: ByteVectorPtr) {

    /**
     * JNI functions.
     */
    private external fun byteVectorGetLengthJNI(pByteVector: ByteVectorPtr): Int
    private external fun byteVectorGetAtJNI(pByteVector: ByteVectorPtr, index: Int): Char
    private external fun byteVectorDestroyJNI(pByteVector: ByteVectorPtr)

    var ptr: ByteVectorPtr
        private set

    init {
        this.ptr = ptr
    }

    companion object {

        /**
         * JNI static functions.
         */
        @JvmStatic
        private external fun byteVectorCreateJNI(string: String): ByteVectorPtr

        fun create(string: String): ByteVector {
            return ByteVector(byteVectorCreateJNI(string))
        }

    }

    val length: Int
        get() {
            return byteVectorGetLengthJNI(ptr)
        }

    /**
     * Hex string representation.
     */
    val hexString: String
        get() {
            var string = ""
            for (i in 0 until length) {
                string += String.format("%02X", getAt(i).toUpperCase().toByte())
            }
            return string
        }

    fun getAt(index: Int): Char {
        return byteVectorGetAtJNI(ptr, index)
    }

    fun destroy() {
        byteVectorDestroyJNI(ptr)
        ptr = NULL_POINTER
    }

    protected fun finalize() {
        destroy()
    }

}