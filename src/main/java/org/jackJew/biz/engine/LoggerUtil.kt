package org.jackJew.biz.engine

import org.jackJew.biz.engine.client.EngineClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggerUtil private constructor() {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(LoggerUtil::class.java)!!
    val INSTANCE: LoggerUtil by lazy { LoggerUtil() }

    val CLIENT_NAME = if ("" == EngineClient.CLIENT_NAME) "dryRun" else
      EngineClient.CLIENT_NAME
  }

  fun debug(log: String) = logger.debug("$CLIENT_NAME - $log")

  fun info(log: String) = logger.info("$CLIENT_NAME - $log")

  fun warn(log: String) = logger.warn("$CLIENT_NAME - $log")

  fun error(log: String) = logger.error("$CLIENT_NAME - $log")
}

class SystemUtil private constructor() {

  companion object {
    val INSTANCE by lazy { SystemUtil() }
  }

  fun sleep(millis: Long) =
      try {
        Thread.sleep(millis)
      } catch (ex: Exception) {
        LoggerUtil.INSTANCE.error(ex.message!!)
      }
}