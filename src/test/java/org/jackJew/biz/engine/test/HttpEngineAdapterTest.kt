package org.jackJew.biz.engine.test

import org.jackJew.biz.engine.HttpEngineAdapter
import org.jsoup.Jsoup
import org.junit.Test

class HttpEngineAdapterTest {

  @Test
  fun get() {
    val url = "https://detail.tmall.com/item.htm?spm=a220m.1000858.1000725.1.1633294cyT7uFI" +
        "&id=536362824702&areaId=310100&standard=1&user_id=2616970884&cat_id=2" +
        "&is_b=1&rn=796707929216fe4fc25068df184d2fc8"
    val content = HttpEngineAdapter.INSTANCE.get(url, mapOf(), mapOf()).getText()

    val document = Jsoup.parse(content)
    val text = document.select("div.tb-wrap div.tb-detail-hd h1 a")[0].text()

    println(text)
  }
}