package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

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
            val headers = HttpHeaders().apply {
                add("X-API-Key", apiKey)
                add("User-Agent", props.userAgent)
                add("Content-Type", "application/json")
                add("Accept", "application/json")
            }

            // Build request body according to ydc-index API example
            val body = mutableMapOf<String, Any>("query" to query)
            // prefer explicit extraction highlights
            body["extraction"] = mapOf("extraction_mode" to "highlights")
            if (maxResults > 0) body["num_results"] = maxResults

            val resolvedLanguage = (language ?: props.youcom.language).takeIf { it.isNotBlank() }
            if (resolvedLanguage != null) body["language"] = resolvedLanguage

            val resolvedCountry = props.youcom.country.takeIf { it.isNotBlank() }
            if (resolvedCountry != null) body["country"] = resolvedCountry

            val entity = HttpEntity(body, headers)
            val response = restTemplate.postForEntity("https://ydc-index.io/v1/search", entity, YouComSearchResponse::class.java)
            val resp = response.body

            val resultsMap = resp?.results ?: emptyMap()
            val rawItems = resultsMap.values.flatten()
            val mapped = rawItems.mapNotNull { raw ->
                val title = raw.title ?: raw.url ?: "(no title)"
                val url = raw.url ?: ""
                val highlights = raw.contents?.highlights ?: emptyList()

                // join highlights into a cleaner content snippet: strip markdown headers and compress whitespace
                val rawContent = if (highlights.isNotEmpty()) highlights.joinToString("\n\n") else (raw.description ?: "")
                val cleaned = rawContent.replace(Regex("(?m)^#+\\s*"), "").replace(Regex("\\s{2,}"), " ").trim()
                val snippet = if (cleaned.length > 400) cleaned.substring(0, 400) + "..." else cleaned

                if (snippet.isBlank() && cleaned.isBlank()) null else SearchResult(title = title, url = url, snippet = snippet, content = cleaned)
            }
            val limit = if (maxResults > 0) maxResults else props.youcom.maxResults
            return mapped.take(limit)
        } catch (e: Exception) {
            log.warn("YouComWebSearchProvider.search failed for '{}': {}", query, e.message)
            emptyList()
        }
    }
}
