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
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_jniCreate(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jAlias,
        jlong jpPublicKey,
        jobject error) {
    int i = 0;
    int* r = &i;
    const char *pAlias = jEnv->GetStringUTFChars(jAlias, JNI_FALSE);
    auto *pPublicKey = reinterpret_cast<TariPublicKey *>(jpPublicKey);
    TariContact *pContact = contact_create(pAlias, pPublicKey,r);
    setErrorCode(jEnv,error,i);
    jEnv->ReleaseStringUTFChars(jAlias, pAlias);
    return reinterpret_cast<jlong>(pContact);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_Contact_jniGetAlias(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariContact *pContact = reinterpret_cast<TariContact *>(jpContact);
    const char *pAlias = contact_get_alias(pContact,r);
    setErrorCode(jEnv,error,i);
    return jEnv->NewStringUTF(pAlias);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_jniGetPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariContact *pContact = reinterpret_cast<TariContact *>(jpContact);
    jlong result = reinterpret_cast<jlong>(contact_get_public_key(pContact,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Contact_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    contact_destroy( reinterpret_cast<TariContact *>(jpContact));
}
