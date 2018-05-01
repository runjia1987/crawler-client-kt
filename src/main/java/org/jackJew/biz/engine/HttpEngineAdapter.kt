package org.jackJew.biz.engine

import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HttpContext
import org.jackJew.biz.engine.util.IOUtils
import org.jackJew.biz.engine.util.PropertyReader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.HashMap

/**
 * http adapter (safe singleton for concurrency usage), based on HttpClient 4.4.1 library.
 *
 */
class HttpEngineAdapter {

  companion object {
    const val CONFIG_BIZ_TYPE = "bizType"
    const val CONFIG_HEADER_CHARSET = "charset"
    const val CONFIG_KEY_PROXY_HOST = "proxyHost"
    const val CONFIG_KEY_PROXY_PORT = "proxyPort"

    private const val max_retry_times = 3
    private val connectionManager = PoolingHttpClientConnectionManager().apply {
      maxTotal = 200
      defaultMaxPerRoute = 200
    }

    private const val CONFIG_KEY_USER_AGENT = "User-Agent"
    private val DEFAULT_USER_AGENT = PropertyReader.getProperty(CONFIG_KEY_USER_AGENT)!!
    private const val DEFAULT_TIMEOUT = 30000
    private const val LONG_TIMEOUT = 50000
    private val LONG_TIME_BIZ_TYPES = PropertyReader.getProperty("Long-Time-BizTypes")?.split(",") ?: listOf()

    private val log = LoggerFactory.getLogger(HttpEngineAdapter::class.java)!!
    val INSTANCE by lazy { HttpEngineAdapter() }
  }

  private fun createHttpClient(config: MutableMap<String, String>): CloseableHttpClient {
    val bizType = config.remove(CONFIG_BIZ_TYPE)
    val timeout = bizType?.let {
      if (LONG_TIME_BIZ_TYPES.contains(bizType)) LONG_TIMEOUT else DEFAULT_TIMEOUT
    } ?: DEFAULT_TIMEOUT

    val requestConfig = RequestConfig.custom()
        .setSocketTimeout(timeout)
        .setConnectionRequestTimeout(timeout)
        .setConnectTimeout(timeout)
        .build()
    config.getOrPut(CONFIG_KEY_USER_AGENT, { DEFAULT_USER_AGENT })
    val host = config.remove(CONFIG_KEY_PROXY_HOST)
    val port = config.remove(CONFIG_KEY_PROXY_PORT)

    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setConnectionManagerShared(true)
        .addInterceptorFirst(CustomHttpRequestInterceptor(config))
        .setDefaultRequestConfig(requestConfig)
        .setRetryHandler(DefaultHttpRequestRetryHandler(0, false))
        .run {
          host?.also { setProxy(HttpHost(host, port!!.toInt())) }
          build()
        }
  }

  fun get(url: String) = get(url, mapOf(), mapOf())

  fun get(url: String, config: Map<String, String>, headers: Map<String, String>) =
    HttpGet(url).run {
      headers.forEach { addHeader(it.key, it.value) }
      processResponse(config, this, url)
    }

  fun post(url: String, params: Map<String, String>?) =
      post(url, mapOf(), mapOf(), params ?: mapOf())

  fun post(url: String,
           config: Map<String, String>,
           headers: Map<String, String>,
           params: Map<String, String>) =
    HttpPost(url).run {
      headers.forEach { addHeader(it.key, it.value) }
      entity = UrlEncodedFormEntity(arrayListOf<NameValuePair>().apply {
        params.forEach { add(BasicNameValuePair(it.key, it.value)) }
      })
      processResponse(config, this, url)
    }

  private fun processResponse(config: Map<String, String>,
                              request: HttpUriRequest,
                              url: String): ResponseConverter {
    var lastException: Exception? = null
    var i = 0
    while (i < max_retry_times) {
      try {
        return createHttpClient(config.toMutableMap()).run {
          execute(request).use {
            convertResponse(it, config[CONFIG_HEADER_CHARSET])
          }
        }
      } catch (ex: Exception) {
        i++
        lastException = ex
        log.error("", ex)
        if (i == max_retry_times - 1) {
          log.info("Retry failed.")
        } else {
          log.info("Retrying request.")
        }
      }
    }
    if (lastException is HttpHostConnectException)
      throw lastException
    throw HttpException("request fail $url", lastException)
  }

  private fun convertResponse(response: CloseableHttpResponse, _charset: String?) =
    ResponseConverter().apply {
      charset = _charset
      statusCode = response.statusLine?.statusCode ?: 0

      headers = response.allHeaders
          ?.associate { Pair(it.name, it.value) }
          ?: mapOf()

      bytes = IOUtils.getBytes(response.entity.content)
    }
}

/**
 * custom HttpRequestInterceptor
 *
 * @author jack.zhu
 */
private class CustomHttpRequestInterceptor(val config: Map<String, String>) : HttpRequestInterceptor {

  @Throws(HttpException::class, IOException::class)
  override fun process(request: HttpRequest, context: HttpContext) {
    config.entries.forEach { request.setHeader(it.key, it.value) }
  }
}