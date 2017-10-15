package org.jackJew.biz.engine

import java.nio.charset.Charset

/**
 * interface for JsEngine,
 * <br />
 * For the sake of performance, scriptContext of all implementations are shared,
 * so gloabl variables will be visible to all.
 * <br/>
 * Ensure that follow "var v = x;" style in "use scrict" mode.
 * @author Jack
 *
 */
interface JsEngine {

  companion object {
    val DEFAULT_CHARSET = Charset.forName("UTF-8")!!
  }

  /**
   * @param script JavaScript closure style
   */
  fun runScript(script: String): Any

  /**
   * @param script JavaScript closure style
   */
  fun runScript2JSON(script: String): String

}