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
package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.ffi.FFICompletedTx
import com.tari.android.wallet.ffi.FFIPendingInboundTx
import com.tari.android.wallet.ui.extension.readP
import com.tari.android.wallet.ui.extension.readS
import java.math.BigInteger

/**
 * Pending inbound tx model class.
 *
 * @author The Tari Development Team
 */
class PendingInboundTx() : Tx(), Parcelable {

    constructor(tx: FFICompletedTx) : this() {
        this.id = tx.getId()
        this.direction = tx.getDirection()
        this.tariContact = tx.getContact()
        this.amount = MicroTari(tx.getAmount())
        this.timestamp = tx.getTimestamp()
        this.message = tx.getMessage()
        this.status = TxStatus.map(tx.getStatus())
        tx.destroy()
    }

    constructor(tx: FFIPendingInboundTx) : this() {
        this.id = tx.getId()
        this.direction = tx.getDirection()
        this.tariContact = tx.getContact()
        this.amount = MicroTari(tx.getAmount())
        this.timestamp = tx.getTimestamp()
        this.message = tx.getMessage()
        this.status = TxStatus.map(tx.getStatus())
        tx.destroy()
    }

    override fun toString(): String {
        return "PendingInboundTx(status=$status) ${super.toString()}"
    }

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<PendingInboundTx> {

        override fun createFromParcel(parcel: Parcel): PendingInboundTx {
            return PendingInboundTx(parcel)
        }

        override fun newArray(size: Int): Array<PendingInboundTx> {
            return Array(size) { PendingInboundTx() }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(id)
        parcel.writeSerializable(direction)
        parcel.writeSerializable(tariContact.javaClass)
        parcel.writeParcelable(tariContact, flags)
        parcel.writeParcelable(amount, flags)
        parcel.writeSerializable(timestamp)
        parcel.writeString(message)
        parcel.writeSerializable(status)
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readS(BigInteger::class.java)
        direction = inParcel.readS(Direction::class.java)
        tariContact = inParcel.readP(TariContact::class.java)
        amount = inParcel.readP(MicroTari::class.java)
        timestamp = inParcel.readS(BigInteger::class.java)
        message = inParcel.readString().orEmpty()
        status = inParcel.readS(TxStatus::class.java)
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion

}
