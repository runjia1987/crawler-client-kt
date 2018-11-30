package org.jackJew.biz.engine.client

import com.rabbitmq.client.MessageProperties
import org.jackJew.biz.engine.util.GsonUtils
import org.jackJew.biz.engine.util.PropertyReader
import org.jackJew.biz.task.Reply
import java.util.concurrent.LinkedBlockingQueue

class MessagePushService {

  companion object {
    val exchangeName = PropertyReader.getProperty("exchangeName")
    val queueNameReply = PropertyReader.getProperty("queueNameReply")
    val INSTANCE by lazy { MessagePushService() }
  }

  val connection = EngineClient.CONN_FACTORY.newConnection()!!
  private val replyQueue = LinkedBlockingQueue<Reply>()

  init {
    val channel = connection.createChannel()
    Thread(Runnable {
      while (true) {
        replyQueue.take().also {
          channel.basicPublish(exchangeName, queueNameReply, MessageProperties.BASIC, GsonUtils.toJson(it).toByteArray())
        }
      }
    }, "publishThread").also {
      it.isDaemon = true
      it.start()
    }
  }

  /**
   * Asynchronously submit reply message.
   */
  fun submit(reply: Reply) = replyQueue.offer(reply)
}