package com.bfilho.kragdemo.adapter.out.mcp.tool

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InternetSearchServiceTest {

    private val restTemplate = org.springframework.web.client.RestTemplate()
    private val props = InternetSearchProperties().apply {
        ttlMs = 1000L
        defaultTargets = listOf("web")
        defaultLanguage = "pt"
        youcom = InternetSearchProperties.YouComProperties(apiKey = "", maxResults = 3, language = "pt-BR")
    }
    private val meterRegistry = SimpleMeterRegistry()

    // fake provider
    private class FakeProvider : WebSearchProvider {
        var calls = 0
        override fun search(query: String, maxResults: Int, language: String?): List<SearchResult> {
            calls++
            return listOf(SearchResult(title = "T1", url = "http://example.com/1", snippet = "S1", content = "C1"))
        }
    }

    @Test
    fun `structured fetch populates and caches results and metrics`() {
        val provider = FakeProvider()
        val service = InternetSearchService(restTemplate, props, meterRegistry, provider)

        val first = service.fetchFromTargetsStructured("query 1")
        assertTrue(first.containsKey("web"))
        assertEquals(1, first["web"]?.size)
        // provider called
        assertEquals(1, provider.calls)

        // call again -> should hit cache (provider not called again)
        val second = service.fetchFromTargetsStructured("query 1")
        assertEquals(1, provider.calls)

        // metrics
        val requests = meterRegistry.find("internet.search.requests").tags("target", "web").counter()?.count() ?: 0.0
        assertTrue(requests >= 1.0)
        val hits = meterRegistry.find("internet.search.cache_hits").tags("target", "web").counter()?.count() ?: 0.0
        assertTrue(hits >= 0.0)
    }

    @Test
    fun `fetchFromTargets returns snippets map`() {
        val provider = FakeProvider()
        val service = InternetSearchService(restTemplate, props, meterRegistry, provider)

        val res = service.fetchFromTargets("my query")
        assertTrue(res.containsKey("web"))
        assertEquals("S1", res["web"])
    }
}
