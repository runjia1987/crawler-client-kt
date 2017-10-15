package org.jackJew.biz.engine.client

import org.jackJew.biz.engine.HttpEngineAdapter
import org.jackJew.biz.engine.JsEngine
import org.jackJew.biz.engine.JsEngineNashorn
import org.jackJew.biz.engine.JsEngineRhino
import org.jackJew.biz.engine.util.GsonUtils
import org.jackJew.biz.engine.util.PropertyReader
import org.jackJew.biz.task.Reply
import org.jackJew.biz.task.TaskObject
import org.slf4j.LoggerFactory

class Task(private val body: ByteArray) {

  companion object {
    val logger = LoggerFactory.getLogger(Task::class.java)!!
    val JS_ENGINE: JsEngine
    private val engineType = PropertyReader.getProperty("jsEngine")

    init {
      JS_ENGINE = when (engineType) {
        "nashorn" -> JsEngineNashorn.INSTANCE
        "rhino" -> JsEngineRhino.INSTANCE
        else -> throw RuntimeException("no correct jsEngine is provided.")
      }
    }
  }

  fun process() {
    try {
      val taskObject = GsonUtils.fromJson(
          String(body, JsEngine.DEFAULT_CHARSET), TaskObject::class.java)
      ScriptCacheService.INSTANCE.getScript(taskObject.bizType)?.apply {
        logger.info("${EngineClient.CLIENT_NAME} - start to process taskId ${taskObject.taskId}")
        val args = taskObject.args ?: mutableMapOf<String, String>()
        args[HttpEngineAdapter.CONFIG_BIZ_TYPE] = taskObject.bizType

        // wrap in closure JavaScript
        val closureCall = "(function(args){$this})(${GsonUtils.toJson(args)})"
        val result = JS_ENGINE.runScript2JSON(closureCall)
        logger.info("${EngineClient.CLIENT_NAME} - received result length(${result.length}) for taskId ${taskObject.taskId}")

        // send reply
        MessagePushService.INSTANCE.submit(Reply(taskObject.taskId, taskObject.bizType, result))

      } ?: logger.warn("${EngineClient.CLIENT_NAME} - fail to find script for ${taskObject.bizType}")
    } catch (ex: Exception) {
     logger.error("Task process fail!", ex)
    }
  }

}