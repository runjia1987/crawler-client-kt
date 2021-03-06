package org.jackJew.biz.engine.test

import org.jackJew.biz.engine.JsEngine
import org.jackJew.biz.engine.JsEngineNashorn
import org.jackJew.biz.engine.JsEngineRhino
import org.jackJew.biz.engine.util.IOUtils
import org.junit.Test

import com.google.gson.JsonObject

/**
 * when MAX is below 50, Rhino is better than Nashorn;
 * <br></br>
 * when MAX is 100, Nashorn is better than Rhino.
 * @author Jack
 */
const val MAX = 200
class JsEngineTest {

  @Test
  fun testNashornJS() {
    val cl = Thread.currentThread().contextClassLoader
    cl.getResourceAsStream("site_source.html").use {
      val content = IOUtils.toString(it, JsEngine.DEFAULT_CHARSET)
      val config = JsonObject().also {
        it.addProperty("content", content)
      }

      val script = cl.getResourceAsStream("org/jackJew/biz/engine/test/config/test.js").use {
        IOUtils.toString(it, JsEngine.DEFAULT_CHARSET)
      }

      val startTime = System.currentTimeMillis()
      var i = 0
      while (i < MAX) {
        val result = JsEngineNashorn.INSTANCE.runScript2JSON(
            "(function(args){$script})($config);")
        if (i++ == 0) {
          println(result)
        }
      }
      println("testNashornJS time cost: " + (System.currentTimeMillis() - startTime) + " ms.")
    }
  }

  @Test
  fun testRhinoJS() {
    val cl = Thread.currentThread().contextClassLoader
    cl.getResourceAsStream("site_source.html").use {
      val content = IOUtils.toString(it, JsEngine.DEFAULT_CHARSET)

      val config = JsonObject().also{ it.addProperty("content", content)}

      val script = cl.getResourceAsStream("org/jackJew/biz/engine/test/config/test.js").use {
        IOUtils.toString(it, JsEngine.DEFAULT_CHARSET)
      }

      val startTime = System.currentTimeMillis()
      var i = 0
      while (i < MAX) {
        val result = JsEngineRhino.INSTANCE.runScript2JSON(
            "(function(args){$script})($config);")
        if (i++ == 0) {
          println(result)
        }
      }
      println("testRhinoJS time cost: " + (System.currentTimeMillis() - startTime) + " ms.")
    }
  }
}
