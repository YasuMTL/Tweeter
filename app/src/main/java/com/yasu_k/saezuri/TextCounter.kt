package com.yasu_k.saezuri

import java.nio.charset.StandardCharsets

class TextCounter {
    fun getTextLength(enteredText: CharSequence): Int {
        val writtenText = enteredText.toString()
        var textLength = 0
        for (element in writtenText)
        {
            val oneChar = element.toString()
            val utf8Bytes = oneChar.toByteArray(StandardCharsets.UTF_8)
            val oneCharBytes = utf8Bytes.size

            if (oneCharBytes == 3)
            {
                //Count two if the character is Chinese, Japanese, Korean or Emoji.
                textLength += 2
            }
            else
            {
                //Count one if the character is NOT Chinese, Japanese, Korean or Emoji.
                textLength++
            }
        }
        return textLength
    }
}