package com.example.utils

object ValidationUtils {
    fun isValidThaiNationalId(id: String): Boolean {
        if (id.length != 13 || !id.all { it.isDigit() }) return false
        
        var sum = 0
        for (i in 0..11) {
            sum += id[i].digitToInt() * (13 - i)
        }
        val checkDigit = (11 - (sum % 11)) % 10
        return checkDigit == id[12].digitToInt()
    }
}
