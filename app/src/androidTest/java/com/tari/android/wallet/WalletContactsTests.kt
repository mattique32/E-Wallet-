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

import com.tari.android.wallet.ffi.Contact
import com.tari.android.wallet.ffi.NULL_POINTER
import com.tari.android.wallet.ffi.PublicKey
import org.junit.Assert.*
import org.junit.Test

/**
 * FFI wallet contacts tests - operates on test data generated by the FFI.
 *
 * @author Kutsal Kaan Bilgin
 */
class WalletContactsTests {

    val wallet = TestUtil.testWallet

    @Test
    fun testGetAndDestroyContacts() {
        val contacts = wallet.getContacts()
        assertTrue(contacts.ptr != NULL_POINTER)
        contacts.destroy()
        assertTrue(contacts.ptr == NULL_POINTER)
    }

    @Test
    fun testGetLength() {
        val contacts = wallet.getContacts()
        assertTrue(contacts.length > 0)
        contacts.destroy()
    }

    @Test
    fun testGetAt() {
        val contacts = wallet.getContacts()
        val contact = contacts.getAt(0)
        assertTrue(contacts.ptr != NULL_POINTER)
        val publicKey = contact.publicKey
        val bytes = publicKey.bytes
        assertTrue(bytes.hexString.length == TestUtil.PUBLIC_KEY_HEX_STRING.length)
        bytes.destroy()
        publicKey.destroy()
        contact.destroy()
        contacts.destroy()
    }

    @Test
    fun testAddContact() {
        // create contact
        val alias = TestUtil.generateRandomAlphanumericString(5)
        val publicKey = PublicKey.fromHex(TestUtil.PUBLIC_KEY_HEX_STRING)
        val contact = Contact.create(alias, publicKey)
        publicKey.destroy()

        // get initial contacts length
        val preContacts = wallet.getContacts()
        val preContactsLength = preContacts.length
        preContacts.destroy()

        // add contact
        val addSuccess = wallet.addContact(contact)
        assertTrue(addSuccess)

        // get post contacts length
        val postContacts = wallet.getContacts()
        val postContactsLength = postContacts.length
        assertTrue(postContactsLength == preContactsLength + 1)

        // get new contact from wallet
        val contactFromWallet = postContacts.getAt(postContactsLength - 1)
        assertTrue(contactFromWallet.ptr != NULL_POINTER)
        postContacts.destroy()

        // compare
        assertEquals(alias, contactFromWallet.alias)

        // free resource
        contactFromWallet.destroy()
        contact.destroy()
    }

    @Test
    fun testRemoveContact() {
        // get initial contacts length
        val preContacts = wallet.getContacts()
        val preContactsLength = preContacts.length
        val contact = preContacts.getAt(preContactsLength - 1)
        assertTrue(wallet.removeContact(contact))
        contact.destroy()
        preContacts.destroy()

        val postContacts = wallet.getContacts()
        val postContactsLength = postContacts.length
        assertEquals(preContactsLength - 1, postContactsLength)
        postContacts.destroy()
    }



}