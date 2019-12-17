/**
 * Copyright 2019 The Tari Project
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
package com.tari.android.wallet.ffi

import java.lang.RuntimeException

/**
 * Tari pending inbound transactions wrapper.
 *
 * @author The Tari Development Team
 */
typealias PendingInboundTransactionsPtr = Long

class PendingInboundTransactions constructor(pointer: PendingInboundTransactionsPtr) {

    private external fun jniGetLength(ptr: PendingInboundTransactionsPtr, libError: LibError): Int
    private external fun jniGetAt(ptr: PendingInboundTransactionsPtr, index: Int, libError: LibError): PendingInboundTransactionPtr
    private external fun jniDestroy(ptr: PendingInboundTransactionsPtr)

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    fun getLength(): Int {
        var error = LibError()
        val result = jniGetLength(ptr,error)
        if (error.code != 0)
        {
            throw RuntimeException()
        }
        return result
    }

    fun getPointer() : PendingInboundTransactionsPtr
    {
        return ptr
    }

    fun getAt(index: Int): PendingInboundTransaction {
        var error = LibError()
        val result = PendingInboundTransaction(jniGetAt(ptr, index,error))
        if (error.code != 0)
        {
            throw RuntimeException()
        }
        return result
    }

    fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}