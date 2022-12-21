package com.yasu_k.saezuri

//import com.nhaarman.mockitokotlin2.spy

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class TextCounterTest {
    lateinit var textCounter: TextCounter

    @Before
    fun setUp() {
        textCounter = TextCounter()
    }

    @Test
    fun getTextLength_InputTest_returnsTrue() {
        val enteredText: CharSequence = "test"
        val numberOfText = textCounter.getTextLength(enteredText)
        System.out.println("Length of $enteredText: $numberOfText")
        assertThat(numberOfText).isEqualTo(4)
    }

    @Test
    fun getTextLength_InputNothing_returnsTrue() {
        val enteredText: CharSequence = ""
        val numberOfText = textCounter.getTextLength(enteredText)
        System.out.println("Length of nothing: $numberOfText")
        assertThat(numberOfText).isEqualTo(0)
    }

    @Test//Japanese
    fun getTextLength_InputArigato_returnsTrue() {
        val enteredText: CharSequence = "ありがとう"
        val numberOfText = textCounter.getTextLength(enteredText)
        System.out.println("Length of $enteredText: $numberOfText")
        assertThat(numberOfText).isEqualTo(10)
    }

    @Test//Korean
    fun getTextLength_InputKorean_LengthEquals() {
        val enteredText: CharSequence = "감사"
        val numberOfText = textCounter.getTextLength(enteredText)
        System.out.println("Length of $enteredText: $numberOfText")
        assertThat(numberOfText).isEqualTo(4)
    }

    @Test//Chinese
    fun getTextLength_InputChinese_returnsTrue() {
        val enteredText: CharSequence = "謝謝"
        val numberOfText = textCounter.getTextLength(enteredText)
        System.out.println("Length of $enteredText: $numberOfText")
        assertThat(numberOfText).isEqualTo(4)
    }

//    @Test//Emoji
//    fun getTextLength_InputEmoji_returnsTrue() {
//        val enteredText: CharSequence = "ありがとう"
//        val numberOfText = textCounter.getTextLength(enteredText)
//        System.out.println("Length of ありがとう: $numberOfText")
//        assertThat(numberOfText).isEqualTo(10)
//    }
}