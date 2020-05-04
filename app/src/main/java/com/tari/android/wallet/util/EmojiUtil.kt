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
package com.tari.android.wallet.util

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UProperty
import com.ibm.icu.text.BreakIterator
import com.tari.android.wallet.extension.applyColorStyle
import com.tari.android.wallet.extension.applyLetterSpacingStyle
import com.tari.android.wallet.extension.applyRelativeTextSizeStyle

/**
 * String code points as list.
 */
private fun String.codePointsAsList(): List<Int> {
    val codePoints = mutableListOf<Int>()
    this.codePoints().forEachOrdered {
        for (i in 0 until UCharacter.charCount(it)) {
            codePoints.add(it)
        }
    }
    return codePoints
}

/**
 * Checks whether the unicode code point is some short of an emoji character.
 */
private fun codePointHasEmojiProperty(codePoint: Int): Boolean {
    return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_COMPONENT)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER_BASE)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_PRESENTATION)
}

/**
 * @return true if the string is emoji-only and the length is equal to the emoji-id length
 */
internal fun String.isPossiblyEmojiId(): Boolean {
    return !this.containsNonEmoji()
            && this.numberOfEmojis() == Constants.Wallet.emojiIdLength
}

/**
 * Number of emojis in a string.
 */
internal fun String.numberOfEmojis(): Int {
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        var isEmoji = true
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            // check if current code point is a part of an emoji
            isEmoji = isEmoji && codePointHasEmojiProperty(codePoint)
        }
        if (isEmoji) {
            emojiCount++
        }
        previous = it.current()
    }
    // no emojis found
    return emojiCount
}

/**
 * @return false if there is at least 1 non-emoji character in the string.
 */
internal fun String.containsNonEmoji(): Boolean {
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            if (!codePointHasEmojiProperty(codePoint)) {
                return true
            }
        }
        previous = it.current()
    }
    // no emojis found
    return false
}

/**
 * @return false if there is at least 1 non-emoji character in the string.
 */
internal fun String.extractEmojis(): String {
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var previous = 0
    val stringBuilder = StringBuilder()
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            // append if has emoji id property
            if (codePointHasEmojiProperty(codePoint)) {
                stringBuilder.append(this[i])
            }
        }
        previous = it.current()
    }
    return stringBuilder.toString()
}

/**
 * Checks whether a given number of first characters of the string are emojis.
 */
internal fun String.firstNCharactersAreEmojis(n: Int): Boolean {
    // prepare a map of codepoints for each character
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            // check if current code point is a part of an emoji
            if (!codePointHasEmojiProperty(codePoint)) {
                return false
            }
        }
        if (++emojiCount >= n) {
            return true
        }
        previous = it.current()
    }
    // didn't reach the number of emojis (n)
    return false
}

/**
 * Emoji utility functions.
 *
 * @author The Tari Development Team
 */
internal class EmojiUtil {

    companion object {

        /**
         * Masking-related: get the indices of current chunk separators.
         *
         * @param string possibly chunked string
         * @param emojiIdChunkSeparator chunk separator sequence
         */
        fun getExistingChunkSeparatorIndices(
            string: String,
            emojiIdChunkSeparator: String
        ): ArrayList<Int> {
            val existingIndices = ArrayList<Int>()
            var currentIndex = 0
            // prep the iterator
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            while (it.next() != BreakIterator.DONE) {
                val builder = StringBuilder()
                val itemIndex = currentIndex
                for (i in previous until it.current()) {
                    builder.append(string[i])
                    currentIndex++
                }
                val item = builder.toString()
                if (item == emojiIdChunkSeparator) {
                    existingIndices.add(itemIndex)
                }
                previous = it.current()
            }
            return existingIndices
        }

        /**
         * Masking-related: calculate the indices of separators for a string.
         *
         * @param string non-chunked string
         */
        fun getNewChunkSeparatorIndices(string: CharSequence): ArrayList<Int> {
            val newIndices = ArrayList<Int>()
            var currentIndex = 0
            // prep the iterator
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            var noOfElements = 0
            while (it.next() != BreakIterator.DONE) {
                val builder = StringBuilder()
                for (i in previous until it.current()) {
                    builder.append(string[i])
                    currentIndex++
                }
                noOfElements++
                if (currentIndex < string.length
                    && noOfElements % Constants.Wallet.emojiFormatterChunkSize == 0
                ) {
                    newIndices.add(currentIndex)
                }
                previous = it.current()
            }
            return newIndices
        }

        fun getStartIndexOfItemEndingAtIndex(string: String, endIndex: Int): Int {
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            while (it.next() != BreakIterator.DONE) {
                if (it.current() == endIndex) {
                    return previous
                }
                previous = it.current()
            }
            return -1
        }

        private fun getChunkedEmojiId(emojiId: CharSequence, separator: String): String {
            // make chunks
            val separatorIndices = getNewChunkSeparatorIndices(emojiId)
            val builder = StringBuilder(emojiId)
            for ((i, index) in separatorIndices.iterator().withIndex()) {
                builder.insert((index + i * separator.length), separator)
            }
            return builder.toString()
        }

        fun getChunkSeparatorSpannable(
            separator: String,
            color: Int
        ): SpannableString {
            val spannable = SpannableString(separator)
            spannable.setSpan(
                ForegroundColorSpan(color),
                0,
                separator.length,
                Spanned.SPAN_INTERMEDIATE
            )
            spannable.applyRelativeTextSizeStyle(
                separator,
                Constants.UI.emojiIdChunkSeparatorRelativeScale
            )
            spannable.applyLetterSpacingStyle(
                separator,
                Constants.UI.emojiIdChunkSeparatorLetterSpacing
            )
            return spannable
        }

        fun getFullEmojiIdSpannable(
            emojiId: String,
            separator: String,
            darkColor: Int,
            lightColor: Int
        ): SpannableString {
            val spannable = getChunkedEmojiId(
                emojiId,
                separator
            ).applyColorStyle(
                darkColor,
                separator,
                lightColor,
                applyToOnlyFirstOccurence = false
            )
            spannable.applyLetterSpacingStyle(
                separator,
                Constants.UI.emojiIdChunkSeparatorLetterSpacing
            )
            spannable.applyRelativeTextSizeStyle(
                separator,
                Constants.UI.emojiIdChunkSeparatorRelativeScale,
                applyToOnlyFirstOccurence = false
            )
            return spannable
        }

    }

}
