package org.jackJew.biz.engine.util

import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

class GsonUtils {

  companion object {
    private val GSON = GsonBuilder().disableHtmlEscaping().create()

    fun <T> fromJson(content: String, cls: Class<T>) = GSON.fromJson(content, cls)

    fun toJson(obj: Any) = GSON.toJson(obj)
  }
}

class IOUtils {

  companion object {
    fun toString(ins: InputStream, charset: Charset) =
      StringBuilder(1 shl 8).also {
        val buff = ByteArray(1 shl 8)
        var count: Int
        while (ins.read(buff).let { count = it; count != -1 }) {
          it.append(String(buff, 0, count, charset))
        }
      }.toString()

    fun getBytes(ins: InputStream) =
        ByteArrayOutputStream(1 shl 8).also {
          val buff = ByteArray(1 shl 8)
          var count: Int
          while (ins.read(buff).let { count = it; count != -1 }) {
            it.write(buff, 0, count)
          }
        }.toByteArray()
  }
}

class ValidationException(message: String): RuntimeException(message) {

  @Synchronized override fun fillInStackTrace(): Throwable  = this
}