package com.yasunari_k.saezuri

import android.text.TextUtils
import java.lang.IllegalArgumentException

class InputChecker {
    fun isValid(text: String): Boolean {
        //if(text == null) throw IllegalArgumentException("Cannot be null")
        if (TextUtils.isEmpty(text)) throw IllegalArgumentException("Cannot be blank")
        return text.length >= 3 && text.matches(Regex("[a-zA-Z0-9]+"))
    }
}