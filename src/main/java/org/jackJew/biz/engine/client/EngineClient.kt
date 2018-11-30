package org.jackJew.biz.engine.client

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.jackJew.biz.engine.util.PropertyReader
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * receive messages from rabbitMQ. run in each JVM process.
 * @author Jack
 *
 */
class EngineClient {

  companion object {
    val logger = LoggerFactory.getLogger(EngineClient::class.java)!!
    var CLIENT_NAME: String = ""

    private val threadPoolSize = Integer.valueOf(PropertyReader.getProperty("threadPoolSize"))
    private val pool  =ThreadPoolExecutor(threadPoolSize, threadPoolSize,
        0L, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val mqFactoryUri = PropertyReader.getProperty("mqFactoryUri")
    private val queueName = PropertyReader.getProperty("queueName")

    val CONN_FACTORY = ConnectionFactory().also {
      it.setUri(mqFactoryUri)
      it.isAutomaticRecoveryEnabled = true
      it.isTopologyRecoveryEnabled = true
    }

    @JvmStatic
    fun main(args: Array<String>) {
      if (args.isEmpty()) {
        println("Error: argument is required to indicate `clientName`.")
        exitProcess(1)
      }
      CLIENT_NAME = args[0]
      logger.info("$CLIENT_NAME is starting... on queue $queueName")

      val conn = CONN_FACTORY.newConnection(pool)
      val channel = conn.createChannel()
      channel.basicQos(threadPoolSize, false)

      val consumer = object: DefaultConsumer(channel) {
        override fun handleDelivery(consumerTag: String, envelope: Envelope,
                                    properties: AMQP.BasicProperties, body: ByteArray) {
          Task(body).process()
          channel.basicAck(envelope.deliveryTag, false)
        }

        override fun handleCancel(consumerTag: String) =
            logger.error("$CLIENT_NAME consumer on queue $queueName get cancel signal.")
      }
      channel.basicConsume(queueName, false, consumer)
      // add hook when process exits or is interrupted.
      Runtime.getRuntime().addShutdownHook(thread {
        conn.close()
        MessagePushService.INSTANCE.connection.close()
        ScriptCacheService.INSTANCE.connection.close()
        pool.shutdownNow()
      })
      logger.info("$CLIENT_NAME is started.")
    }
  }

}