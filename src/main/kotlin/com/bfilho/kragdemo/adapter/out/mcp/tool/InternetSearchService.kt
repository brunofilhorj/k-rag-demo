package com.bfilho.kragdemo.adapter.out.mcp.tool

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Internet search wrapper: configurable TTL cache, metrics and multiple targets.
 * Uses WebSearchProvider for generic web searches (You.com implementation provided separately).
 */
@Component
class InternetSearchService(
    private val restTemplate: RestTemplate,
    private val props: InternetSearchProperties,
    private val meterRegistry: MeterRegistry,
    private val webSearchProvider: WebSearchProvider
) {
    private val log = LoggerFactory.getLogger(InternetSearchService::class.java)

    private data class CacheEntry<T>(val value: T, val timestamp: Long)

    // cache holds lists of SearchResult per key
    private val cache = ConcurrentHashMap<String, CacheEntry<List<SearchResult>>>()
    private val ttlMs: Long
        get() = props.ttlMs

    private fun counter(name: String, target: String) =
        meterRegistry.counter(name, Tags.of("target", target))

    /**
     * Backwards-compatible: returns Map<target, simpleText> as before.
     * Internally prefers structured results via fetchFromTargetsStructured.
     */
    fun fetchFromTargets(title: String, targets: List<String>? = null): Map<String, String?> {
        val structured = fetchFromTargetsStructured(title, targets)
        return structured.mapValues { (_, list) ->
            list.firstOrNull()?.snippet
        }
    }

    /**
     * New structured method: returns Map<target, List<SearchResult>>
     */
    fun fetchFromTargetsStructured(title: String, targets: List<String>? = null): Map<String, List<SearchResult>> {
        val chosen = targets ?: props.defaultTargets
        val results = mutableMapOf<String, List<SearchResult>>()

        for (t in chosen) {
            val target = t.lowercase().trim()
            counter("internet.search.requests", target).increment()

            try {
                when (target) {
                        "wikipedia", "wiki" -> {
                            val key = "wikipedia:${props.defaultLanguage}:${title.trim().lowercase()}"
                            val now = System.currentTimeMillis()
                            val cached = cache[key]
                            if (cached != null && now - cached.timestamp <= ttlMs) {
                                counter("internet.search.cache_hits", target).increment()
                                results[target] = listOf(SearchResult(title = title, url = "https://$ {props.defaultLanguage}.wikipedia.org/wiki/$title", snippet = cached.value.joinToString(" \n ") { it.snippet }, content = cached.value.joinToString("\n\n") { it.content ?: it.snippet }))
                                continue
                            }
                            counter("internet.search.cache_misses", target).increment()

                            val extract = fetchWikipediaExtract(title, props.defaultLanguage)
                            val searchResults = if (extract != null) {
                                val sr = SearchResult(title = title, url = "https://$ {props.defaultLanguage}.wikipedia.org/wiki/$title", snippet = extract.take(1000), content = extract)
                                listOf(sr)
                            } else emptyList()

                            if (searchResults.isNotEmpty()) cache[key] = CacheEntry(searchResults, now)
                            results[target] = searchResults
                        }

                        "url" -> {
                            val key = "url:${title.trim()}"
                            val now = System.currentTimeMillis()
                            val cached = cache[key]
                            if (cached != null && now - cached.timestamp <= ttlMs) {
                                counter("internet.search.cache_hits", target).increment()
                                results[target] = cached.value
                                continue
                            }
                            counter("internet.search.cache_misses", target).increment()

                            val content = fetchUrlText(title)
                            val searchResults = if (content != null) listOf(SearchResult(title = title, url = title, snippet = content.take(300), content = content)) else emptyList()
                            if (searchResults.isNotEmpty()) cache[key] = CacheEntry(searchResults, now)
                            results[target] = searchResults
                        }

                        "web" -> {
                            // generic web search via provider
                            val key = "web:${props.youcom.language}:${props.youcom.country}:${title.trim().lowercase()}"
                            val now = System.currentTimeMillis()
                            val cached = cache[key]
                            if (cached != null && now - cached.timestamp <= ttlMs) {
                                counter("internet.search.cache_hits", target).increment()
                                results[target] = cached.value
                                continue
                            }

                            counter("internet.search.cache_misses", target).increment()

                            // call provider
                            val list = try {
                                webSearchProvider.search(title, props.youcom.maxResults, props.youcom.language)
                            } catch (e: Exception) {
                                counter("internet.search.failures", target).increment()
                                log.warn("WebSearchProvider failed for '{}': {}", title, e.message)
                                emptyList<SearchResult>()
                            }

                            if (list.isNotEmpty()) cache[key] = CacheEntry(list, now)
                            results[target] = list
                        }

                        else -> {
                            // unknown target -> map to web provider by default
                            val key = "web:${props.youcom.language}:${props.youcom.country}:${title.trim().lowercase()}"
                            val now = System.currentTimeMillis()
                            val cached = cache[key]
                            if (cached != null && now - cached.timestamp <= ttlMs) {
                                counter("internet.search.cache_hits", target).increment()
                                results[target] = cached.value
                                continue
                            }
                            counter("internet.search.cache_misses", target).increment()
                            val list = try {
                                webSearchProvider.search(title, props.youcom.maxResults, props.youcom.language)
                            } catch (e: Exception) {
                                counter("internet.search.failures", target).increment()
                                log.warn("WebSearchProvider failed for '{}': {}", title, e.message)
                                emptyList<SearchResult>()
                            }
                            if (list.isNotEmpty()) cache[key] = CacheEntry(list, now)
                            results[target] = list
                        }
                }
            } catch (e: Exception) {
                // ensure failures increment only once per target on exception
                counter("internet.search.failures", target).increment()
                log.warn("InternetSearchService target '{}' failed for '{}': {}", target, title, e.message)
                results[target] = emptyList()
            }
        }

        return results
    }

    private fun fetchWikipediaExtract(title: String, language: String = "pt"): String? {
        return try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val url = "https://$language.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&format=json&titles=$encoded&redirects=true"

            val headers = org.springframework.http.HttpHeaders().apply {
                add("User-Agent", props.userAgent)
            }
            val entity = org.springframework.http.HttpEntity<String>(headers)
            val responseEntity = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map::class.java)
            val resp = responseEntity.body as? Map<*, *>

            val extract = resp
                ?.get("query")
                .let { q ->
                        val queryMap = q as? Map<*, *>
                        val pages = queryMap?.get("pages") as? Map<*, *>
                        val firstPage = pages?.values?.firstOrNull() as? Map<*, *>
                        firstPage?.get("extract") as? String
                }

            extract?.replace(Regex("<[^>]*>"), "").orEmpty().trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            counter("internet.search.failures", "wikipedia").increment()
            log.warn("InternetSearchService fetchWikipediaExtract failed for '{}': {}", title, e.message)
            null
        }
    }

    private fun fetchUrlText(url: String): String? {
        return try {
            val headers = org.springframework.http.HttpHeaders().apply {
                add("User-Agent", props.userAgent)
            }
            val entity = org.springframework.http.HttpEntity<String>(headers)
            val resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String::class.java).body
            resp?.replace(Regex("<[^>]*>"), "").orEmpty().trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            counter("internet.search.failures", "url").increment()
            log.warn("InternetSearchService fetchUrlText failed for '{}': {}", url, e.message)
            null
        }
    }

    fun metricsSummary(): Map<String, Map<String, Double>> {
        val names = listOf("internet.search.requests", "internet.search.cache_hits", "internet.search.cache_misses", "internet.search.failures")
        val summary = mutableMapOf<String, Map<String, Double>>()

        val targetsSeen = mutableSetOf<String>()
        meterRegistry.meters.forEach { meter ->
            val targetTag = meter.id.tags.firstOrNull { it.key == "target" }?.value
            if (targetTag != null) targetsSeen += targetTag
        }

        for (t in targetsSeen) {
            val map = mutableMapOf<String, Double>()
            for (n in names) {
                val count = meterRegistry.find(n).tag("target", t).counter()?.count() ?: 0.0
                map[n] = count
            }
            summary[t] = map
        }

        return summary
    }
}
