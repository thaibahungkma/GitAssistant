package com.example.assistantkotlin.AES

import android.os.Build
import androidx.annotation.RequiresApi
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AES {
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun encrypt(strToEncrypt: String, myKey: String): String? {
        try {
            val sha = MessageDigest.getInstance("SHA-1")
            var key: ByteArray? = myKey.toByteArray(charset("UTF-8"))
            key = sha.digest(key)
            key = Arrays.copyOf(key, 16)
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(strToEncrypt.toByteArray(charset("UTF-8"))))
        } catch (e: Exception) {
            println(e.toString())
        }
        return null
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun decrypt(strToDecrypt: String?, myKey: String): String? {
        try {
            val sha = MessageDigest.getInstance("SHA-1")
            var key: ByteArray? = myKey.toByteArray(charset("UTF-8"))
            key = sha.digest(key)
            key = Arrays.copyOf(key, 16)
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
        } catch (e: Exception) {
            println(e.toString())
        }
        return null
    }
}