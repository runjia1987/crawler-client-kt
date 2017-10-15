package org.jackJew.biz.engine

import org.jackJew.biz.engine.util.GsonUtils
import org.jackJew.biz.engine.util.IOUtils
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.ScriptableObject
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * JS script engine, based on Mozilla Rhino.
 * @author Jack
 *
 */
class JsEngineRhino: JsEngine {

  companion object {
    private val log = LoggerFactory.getLogger(JsEngineRhino::class.java)!!

    private val classWhiteList = mutableSetOf<String>()
    private val packageWhiteList = mutableSetOf<String>()

    var globalContextFactory: ContextFactory
    var sharedScriptObject: ScriptableObject
    private val CONFIG_PACKAGE = "org/jackJew/biz/engine/config/rhino/"

    val INSTANCE by lazy { JsEngineRhino() }
    init {
      val cl = Thread.currentThread().contextClassLoader
      try {
        BufferedReader(InputStreamReader(
            cl.getResourceAsStream(CONFIG_PACKAGE + "classWhiteList"))).useLines {
          it.forEach { log.info("class: $it"); classWhiteList.add(it) }
        }
      } catch (ex: Exception) {
        log.error("fail to read classWhiteList", ex)
      }
      try {
        BufferedReader(InputStreamReader(
            cl.getResourceAsStream(CONFIG_PACKAGE + "packageWhiteList"))).useLines {
          it.forEach { log.info("package: $it"); packageWhiteList.add(it) }
        }
      } catch (ex: Exception) {
        log.error("fail to read packageWhiteList", ex)
      }

      globalContextFactory = object: ContextFactory() {
        override fun makeContext() =
            Context().also {
              it.setClassShutter { className ->
                classWhiteList.contains(className) || packageWhiteList.any { className.startsWith(it) }
              }
              it.optimizationLevel = 5
              it.instructionObserverThreshold = 20000
          }
      }
      ContextFactory.initGlobal(globalContextFactory)
      sharedScriptObject = globalContextFactory.enterContext().initStandardObjects().also {
        ScriptableObject.putConstProperty(it, "\$\$http", HttpEngineAdapter.INSTANCE)
        ScriptableObject.putConstProperty(it, "\$\$system", SystemUtil.INSTANCE)
        ScriptableObject.putConstProperty(it, "log", LoggerUtil.INSTANCE)
      }
    }
  }

  private constructor() {
    Thread.currentThread().contextClassLoader.also {
      it.getResourceAsStream(CONFIG_PACKAGE + "json_util.js").use {
        runScript(IOUtils.toString(it, JsEngine.DEFAULT_CHARSET))
      }

      it.getResourceAsStream(CONFIG_PACKAGE + "core.js").use {
        runScript(IOUtils.toString(it, JsEngine.DEFAULT_CHARSET))
      }
    }
  }

  override fun runScript(script: String) =
      try {
        globalContextFactory.enterContext().evaluateString(sharedScriptObject,
            script, null, 0, null)
      } finally {
        Context.exit()
      }

  override fun runScript2JSON(script: String) =
      GsonUtils.toJson(runScript(script))
}