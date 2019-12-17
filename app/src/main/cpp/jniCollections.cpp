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

#include <jni.h>
#include <android/log.h>
#include <wallet.h>
#include <string>
#include <math.h>
#include <android/log.h>
#include "jniCommon.cpp"

#define LOG_TAG "Tari Wallet"

/**
 * Log functions. Log example:
 *
 * int count = 5;
 * LOGE("Count is %d", count);
 * char[] name = "asd";
 * LOGI("Name is %s", name);
 */
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_Contacts_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariContacts *pContacts = reinterpret_cast<TariContacts *>(jpContacts);
    jint result = contacts_get_length(pContacts,r);
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contacts_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts,
        jint index,
        jobject error) {
    int i = 0;
    int* r = &i;
    auto *pContacts = reinterpret_cast<TariContacts *>(jpContacts);
    jlong result = reinterpret_cast<jlong>(contacts_get_at(pContacts, static_cast<unsigned int>(index),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Contacts_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts) {
    contacts_destroy(reinterpret_cast<TariContacts *>(jpContacts));
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariCompletedTransactions *pCompletedTransactions = reinterpret_cast<TariCompletedTransactions *>(jpCompletedTransactions);
    jint result = completed_transactions_get_length(pCompletedTransactions,r);
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions,
        jint index,
        jobject error) {
    int i = 0;
    int* r = &i;
    auto *pCompletedTransactions = reinterpret_cast<TariCompletedTransactions *>(jpCompletedTransactions);
    jlong result = reinterpret_cast<jlong>(completed_transactions_get_at(pCompletedTransactions, static_cast<unsigned int>(index),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions) {
    completed_transactions_destroy(reinterpret_cast<TariCompletedTransactions *>(jpCompletedTransactions));
}

extern "C"
JNIEXPORT jint JNICALL
        Java_com_tari_android_wallet_ffi_PendingInboundTransactions_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariPendingInboundTransactions *pInboundTxs = reinterpret_cast<TariPendingInboundTransactions *>(jpInboundTxs);
    jint result = pending_inbound_transactions_get_length(pInboundTxs,r);
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
        Java_com_tari_android_wallet_ffi_PendingInboundTransactions_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs,
        jint index,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariPendingInboundTransactions *pInboundTxs = reinterpret_cast<TariPendingInboundTransactions *>(jpInboundTxs);
    jlong result = reinterpret_cast<jlong>(pending_inbound_transactions_get_at(pInboundTxs,
                                                                               static_cast<unsigned int>(index),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransactions_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs) {
    pending_inbound_transactions_destroy(reinterpret_cast<TariPendingInboundTransactions *>(jpInboundTxs));
}


extern "C"
JNIEXPORT jint JNICALL
        Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTxs,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariPendingOutboundTransactions *pOutboundTxs = reinterpret_cast<TariPendingOutboundTransactions *>(jpOutboundTxs);
    jint result = pending_outbound_transactions_get_length(pOutboundTxs,r);
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
        Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTxs,
        jint index,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariPendingOutboundTransactions *pOutboundTxs = reinterpret_cast<TariPendingOutboundTransactions *>(jpOutboundTxs);
    jlong result = reinterpret_cast<jlong>(pending_outbound_transactions_get_at(pOutboundTxs,static_cast<unsigned int>(index),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs) {
    pending_outbound_transactions_destroy(reinterpret_cast<TariPendingOutboundTransactions *>(jpInboundTxs));
}