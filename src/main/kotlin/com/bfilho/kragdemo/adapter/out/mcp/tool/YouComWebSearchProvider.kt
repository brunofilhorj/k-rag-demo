package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class YouComWebSearchProvider(
    private val restTemplate: RestTemplate,
    private val props: InternetSearchProperties
) : WebSearchProvider {

    private val log = LoggerFactory.getLogger(YouComWebSearchProvider::class.java)

    override fun search(query: String, maxResults: Int, language: String?): List<SearchResult> {
        val apiKey = props.youcom.apiKey
        if (apiKey.isBlank()) {
            log.warn("YouCom API key not configured; web searches will return no results.")
            return emptyList()
        }

        return try {
            // Build request according to You.com API (common pattern: Authorization: Bearer <key>)
            val headers = HttpHeaders().apply {
                add("Authorization", "Bearer $apiKey")
                add("User-Agent", props.userAgent)
                add("Accept", "application/json")
            }

            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.you.com/search?q=$encoded&num=$maxResults&locale=${language ?: props.youcom.language}"

            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(url, HttpMethod.GET, entity, YouComSearchResponse::class.java)
            val resp = response.body

            val items = resp?.results.orEmpty()
            return items.mapNotNull { item ->
                val title = item.title ?: item.url ?: "(no title)"
                val url = item.url ?: ""
                val snippet = item.snippet ?: item.highlights?.firstOrNull() ?: item.content?.take(300) ?: ""
                val content = item.content
                if (snippet.isBlank() && content.isNullOrBlank()) null else SearchResult(title = title, url = url, snippet = snippet, content = content)
            }.take(maxResults)
        } catch (e: Exception) {
            log.warn("YouComWebSearchProvider.search failed for '{}': {}", query, e.message)
            emptyList()
        }
    }
}
