package org.jackJew.biz.engine

import org.mozilla.intl.chardet.nsDetector
import org.mozilla.intl.chardet.nsICharsetDetectionObserver
import org.mozilla.intl.chardet.nsPSMDetector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Converter for http response with charset detection, based on mozilla jchardet.
 *
 */
class ResponseConverter {

  var charset: String? = null
  lateinit var headers: Map<String, String>
  var statusCode: Int = 0
  lateinit var bytes: ByteArray

  fun detectAndResolve(ins: InputStream): String {
    val nsDetector = nsDetector(nsPSMDetector.CHINESE)
    val charsetObserverNotify = CharsetObserverNotify()
    nsDetector.Init(charsetObserverNotify)

    var found = false
    var isAscii = true
    val buffer = ByteArray(1 shl 8)
    var count: Int = -1
    val bous = ByteArrayOutputStream()
    while ({ count = ins.read(buffer); count }() != -1) {
      bous.write(buffer, 0, count)
      // Check if the stream is only ascii.
      if (isAscii) {
        isAscii = nsDetector.isAscii(buffer, count)
      }
      // DoIt if non-ascii and not found yet.
      if (!isAscii && !found) {
        found = nsDetector.DoIt(buffer, count, false)
      }
    }
    nsDetector.DataEnd()

    if (isAscii) {
      charsetObserverNotify.found = true
    }
    if (!charsetObserverNotify.found) {
      nsDetector.probableCharsets.also {
        if (it.size > 0) charsetObserverNotify.charset = it[0]
      }
    }
    return charsetObserverNotify.charset?.let {
      String(bous.toByteArray(), Charset.forName(charset))
    } ?: String(bous.toByteArray())
  }

  fun getText() =
      charset?.let {
        try {
          String(bytes, Charset.forName(charset))
        } catch (ex: UnsupportedEncodingException) {
          // charset is wrong, then call jchardet.
          detectAndResolve(ByteArrayInputStream(bytes))
        }
      } ?: detectAndResolve(ByteArrayInputStream(bytes))

  private inner class CharsetObserverNotify : nsICharsetDetectionObserver {
    var found = false
    var charset: String? = null

    override fun Notify(charset: String) {
      this.found = true
      this.charset = charset
    }
  }
}