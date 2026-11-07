package com.example.pricelistapp.data.local

import java.security.SecureRandom

object UlidGenerator {
    private val RANDOM = SecureRandom()
    private val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun generate(): String {
        val time = System.currentTimeMillis()
        val timeChars = encodeTime(time, 10)
        val randChars = encodeRandom(16)
        return timeChars + randChars
    }

    private fun encodeTime(time: Long, length: Int): String {
        var t = time
        val chars = CharArray(length)
        for (i in length - 1 downTo 0) {
            chars[i] = ENCODING[(t % 32).toInt()]
            t /= 32
        }
        return String(chars)
    }

    private fun encodeRandom(length: Int): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = ENCODING[RANDOM.nextInt(32)]
        }
        return String(chars)
    }
}
