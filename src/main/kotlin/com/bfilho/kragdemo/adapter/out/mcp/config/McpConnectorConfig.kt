package com.bfilho.kragdemo.adapter.out.mcp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class McpConnectorConfig(
    private val properties: McpConnectorProperties
) {

    @Bean
    fun mcpRestTemplate(): RestTemplate {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.timeoutMs)
            setReadTimeout(properties.timeoutMs)
        }

        return RestTemplate(requestFactory)
    }
}
