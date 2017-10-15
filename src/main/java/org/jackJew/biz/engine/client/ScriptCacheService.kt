package org.jackJew.biz.engine.client

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.jackJew.biz.engine.JsEngine
import org.jackJew.biz.engine.util.GsonUtils
import org.jackJew.biz.engine.util.PropertyReader
import org.jackJew.biz.task.BizScript
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * local cache of crawler processing scripts
 * @author Jack
 *
 */
class ScriptCacheService {

  companion object {
    val logger = LoggerFactory.getLogger(ScriptCacheService::class.java)!!
    val SCRIPT_EXCHANGE = PropertyReader.getProperty("script_exchange")!!
    val QUERY_SCRIPT_URL = PropertyReader.getProperty("query_script_url")!!
    private val DEFAULT_USER_AGENT = PropertyReader.getProperty("User-Agent")
    val DEPRECATED = "000"
    /**
     * shared httpclient since the route will never change.
     */
    val CLIENT = HttpClients.custom().setUserAgent(DEFAULT_USER_AGENT).build()

    val INSTANCE by lazy { ScriptCacheService() }
  }

  val cache = ConcurrentHashMap<String, String>()
  val connection = EngineClient.CONN_FACTORY.newConnection()!!

  private constructor() {
    val channel = connection.createChannel()
    channel.exchangeDeclarePassive(SCRIPT_EXCHANGE)
    val queueName = channel.queueDeclare(UUID.randomUUID().toString(), true, true, true, null).queue
    channel.queueBind(queueName, SCRIPT_EXCHANGE, "")

    val consumer = object: DefaultConsumer(channel) {
      override fun handleDelivery(consumerTag: String, envelope: Envelope,
                                  properties: AMQP.BasicProperties, body: ByteArray) {
        val bizScript = GsonUtils.fromJson(
            String(body, JsEngine.DEFAULT_CHARSET), BizScript::class.java)
        bizScript.bizType?.also {
          logger.info("received script update-msg for $it")
          if (bizScript.isDeleted)
            cache.put(it, DEPRECATED)
          else
            bizScript.script?.run { cache.put(it, this) }
          logger.info("cached script update-msg for $it")
        }
      }

      override fun handleCancel(consumerTag: String) =
          EngineClient.logger.error("${EngineClient.CLIENT_NAME} consumer on queue $queueName get cancel signal.")
    }
    channel.basicConsume(queueName, consumer)
  }

  fun getScript(bizType: String) =
      cache[bizType]?.let {
        if ( it == DEPRECATED) null
        else {
          logger.info("start getByHttp of bizType: $bizType")
          getByHttp(bizType)?.apply {
            cache.put(it, this)
          }
        }
      }

  private fun getByHttp(bizType: String) =
      HttpGet(String.format(QUERY_SCRIPT_URL, bizType, System.currentTimeMillis())).let {
        CLIENT.execute(it).use {
          if (it.statusLine.statusCode == HttpStatus.SC_OK)
            String(resloveResponse(it.entity.content), JsEngine.DEFAULT_CHARSET)
          else null
        }
      }

  private fun resloveResponse(stream: InputStream): ByteArray {
    var count = 0
    val buffer = ByteArray(1 shl 8)
    val ous = ByteArrayOutputStream(1 shl 9)
    while( { count = stream.read(buffer); count }() != -1) {
      ous.write(buffer, 0, count)
    }
    return ous.toByteArray()
  }
}