package org.jackJew.biz.engine.util

import java.io.InputStreamReader
import java.util.*

class PropertyReader private constructor() {

  companion object {
    private val PROPS = Properties()

    init {
      PROPS.putAll(resolve("config.properties"))
    }

    fun getProperty(key: String): String? = PROPS.getProperty(key)

    private fun resolve(fileName: String) =
        Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).let {
          InputStreamReader(it).use {
            // use = try..with in Java
            Properties().apply { this.load(it) }
          }
        }
  }
}