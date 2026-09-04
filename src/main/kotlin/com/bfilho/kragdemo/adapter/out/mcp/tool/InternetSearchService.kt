package com.bfilho.kragdemo.adapter.out.mcp.tool

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight internet search wrapper to centralize HTTP calls, parsing and caching for MCP tools.
 * - Provides a configurable TTL cache
 * - Supports multiple targets (wikipedia, url)
 * - Tracks metrics via Micrometer and logs detailed events
 */
@Component
class InternetSearchService(
    private val restTemplate: RestTemplate,
    private val props: InternetSearchProperties,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(InternetSearchService::class.java)

    private data class CacheEntry(val value: String, val timestamp: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs: Long
        get() = props.ttlMs

    private fun counter(name: String, target: String) =
        meterRegistry.counter(name, io.micrometer.core.instrument.Tags.of("target", target))

    fun fetchFromTargets(title: String, targets: List<String>? = null): Map<String, String?> {
        val chosen = targets ?: props.defaultTargets
        val results = mutableMapOf<String, String?>()

        for (t in chosen) {
            try {
                counter("internet.search.requests", t).increment()
                val res = when (t.lowercase().trim()) {
                    "wikipedia", "wiki" -> fetchWikipediaExtract(title, props.defaultLanguage, t)
                    "url" -> {
                        fetchUrlText(title)
                    }
                    else -> {
                        // unknown target - attempt wikipedia fallback
                        fetchWikipediaExtract(title, props.defaultLanguage, t)
                    }
                }

                if (!res.isNullOrBlank()) {
                    results[t] = res
                } else {
                    results[t] = null
                }
            } catch (e: Exception) {
                counter("internet.search.failures", t).increment()
                log.warn("InternetSearchService target '{}' failed for '{}': {}", t, title, e.message)
                results[t] = null
            }
        }

        return results
    }

    private fun fetchWikipediaExtract(title: String, language: String = "pt", targetName: String = "wikipedia"): String? {
        val key = "wikipedia:$language:${title.trim().lowercase()}"
        val now = System.currentTimeMillis()

        val cached = cache[key]
        if (cached != null && now - cached.timestamp <= ttlMs) {
            counter("internet.search.cache_hits", targetName).increment()
            log.debug("cache hit for {} (target={})", title, targetName)
            return cached.value
        }
        counter("internet.search.cache_misses", targetName).increment()
        log.debug("cache miss for {} (target={})", title, targetName)

        return try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val url = "https://$language.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&format=json&titles=$encoded&redirects=true"
            val resp = restTemplate.getForObject(url, Map::class.java) as? Map<*, *>

            val extract = resp
                ?.get("query")
                .let { q ->
                    val queryMap = q as? Map<*, *>
                    val pages = queryMap?.get("pages") as? Map<*, *>
                    val firstPage = pages?.values?.firstOrNull() as? Map<*, *>
                    firstPage?.get("extract") as? String
                }

            val cleaned = extract?.replace(Regex("<[^>]*>"), "").orEmpty().trim().takeIf { it.isNotBlank() }

            if (!cleaned.isNullOrBlank()) {
                cache[key] = CacheEntry(cleaned, now)
            }

            cleaned
        } catch (e: Exception) {
            counter("internet.search.failures", "wikipedia").increment()
            log.warn("InternetSearchService fetchWikipediaExtract failed for '{}': {}", title, e.message)
            null
        }
    }

    private fun fetchUrlText(url: String): String? {
        val key = "url:$url"
        val now = System.currentTimeMillis()
        val cached = cache[key]
        if (cached != null && now - cached.timestamp <= ttlMs) {
            counter("internet.search.cache_hits", "url").increment()
            log.debug("cache hit for url {}", url)
            return cached.value
        }
        counter("internet.search.cache_misses", "url").increment()
        log.debug("cache miss for url {}", url)

        return try {
            val resp = restTemplate.getForObject(url, String::class.java)
            val cleaned = resp?.replace(Regex("<[^>]*>"), "").orEmpty().trim().takeIf { it.isNotBlank() }
            if (!cleaned.isNullOrBlank()) cache[key] = CacheEntry(cleaned, now)
            cleaned
        } catch (e: Exception) {
            counter("internet.search.failures", "url").increment()
            log.warn("InternetSearchService fetchUrlText failed for '{}': {}", url, e.message)
            null
        }
    }

    fun metricsSummary(): Map<String, Map<String, Double>> {
        val names = listOf("internet.search.requests", "internet.search.cache_hits", "internet.search.cache_misses", "internet.search.failures")
        val summary = mutableMapOf<String, Map<String, Double>>()

        // query meters with tag 'target'
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
