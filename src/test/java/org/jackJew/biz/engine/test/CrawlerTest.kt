package org.jackJew.biz.engine.test

import com.google.gson.JsonObject
import org.jackJew.biz.engine.JsEngine
import org.jackJew.biz.engine.JsEngineNashorn
import org.jackJew.biz.engine.util.IOUtils
import org.junit.Test

class CrawlerTest {

  @Test
  fun crawl() {
    val start = System.currentTimeMillis()
    val scriptFile = "org/jackJew/biz/engine/test/config/dd_shop.js"
    val url = "http://category.dangdang.com/cid4001867.html"
    val cl = Thread.currentThread().contextClassLoader
    cl.getResourceAsStream(scriptFile).use {
      val script = IOUtils.toString(it, JsEngine.DEFAULT_CHARSET)
      val config = JsonObject().apply { addProperty("url", url) }
      val result = JsEngineNashorn.INSTANCE.runScript2JSON(
          String.format("(function(args){%s})(%s);", script, config.toString())
      )
      println(result)
      println("time cost: ${System.currentTimeMillis() - start} ms.")
    }
  }
}