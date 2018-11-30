package org.jackJew.biz.engine

import jdk.nashorn.api.scripting.ClassFilter
import jdk.nashorn.api.scripting.NashornScriptEngine
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.jackJew.biz.engine.util.GsonUtils
import org.jackJew.biz.engine.util.IOUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.script.ScriptContext
import javax.script.SimpleBindings
import javax.script.SimpleScriptContext

/**
 * JS script engine, based on JDK 8 Nashorn. --optimistic-types=true
 * {@link https://blogs.oracle.com/nashorn/nashorn-architecture-and-performance-improvements-in-the-upcoming-jdk-8u40-release }
 * @author Jack
 *
 */
class JsEngineNashorn: JsEngine {

  companion object {
    private val log = LoggerFactory.getLogger(JsEngineNashorn::class.java)!!

    private val classWhiteList = mutableSetOf<String>()
    private val packageWhiteList = mutableSetOf<String>()

    const val CONFIG_PACKAGE = "org/jackJew/biz/engine/config/nashorn/"

    val INSTANCE by lazy { JsEngineNashorn() }

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
    }
  }

  private val scriptEngine = NashornScriptEngineFactory().scriptEngine as NashornScriptEngine
  private val scriptContext = SimpleScriptContext()

  init {
    SimpleBindings().also {
      it["\$\$http"] = HttpEngineAdapter.INSTANCE
      it["\$\$system"] = SystemUtil.INSTANCE
      it["log"] = LoggerUtil.INSTANCE
      scriptContext.setBindings(it, ScriptContext.GLOBAL_SCOPE)
      scriptEngine.context = scriptContext
    }
    Thread.currentThread().contextClassLoader.getResourceAsStream("${CONFIG_PACKAGE}json_util.js").use {
      IOUtils.toString(it, JsEngine.DEFAULT_CHARSET).apply {
        scriptEngine.compile(this).eval(scriptContext)
      }
    }

    Thread.currentThread().contextClassLoader.getResourceAsStream("${CONFIG_PACKAGE}core.js").use {
      IOUtils.toString(it, JsEngine.DEFAULT_CHARSET).apply {
        scriptEngine.compile(this).eval(scriptContext)
      }
    }
  }

  override fun runScript(script: String) = scriptEngine.eval(script, scriptContext)

  override fun runScript2JSON(script: String): String {
    val result = scriptEngine.eval(script, scriptContext)
    return when (result) {
      is ScriptObjectMirror -> { transform(result); GsonUtils.toJson(result) }
      else -> scriptEngine.invokeFunction("\$\$stringify", result).toString()
    }
  }

  private fun transform(objectMirror: ScriptObjectMirror) {
    for (key in objectMirror.keys) {
      val value = objectMirror[key]
      when(value) {
        is ScriptObjectMirror -> {
          if (value.isArray) objectMirror[key] = value.values
          transform(value) // we assume that the depth of recursion is tolerable
        }
      }
    }
  }

  /**
   * secutiry filter by className, supported in JDK 1.8u40 and later.
   * {@link http://www.oracle.com/technetwork/java/javase/8u40-relnotes-2389089.html#newft }
   *
   */
  inner class ClassNameFilter: ClassFilter {
    /**
     * @param name the name of the Java class or package
     */
    override fun exposeToScripts(name: String) =
        classWhiteList.contains(name) || packageWhiteList.any { name.startsWith(it)}
  }
}