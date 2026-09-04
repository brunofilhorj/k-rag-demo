package com.bfilho.kragdemo.adapter.out.mcp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "mcp")
class McpConnectorProperties {
    var enabled: Boolean = false
    var serverUrl: String = ""
    var tools: List<String> = emptyList()
    var timeoutMs: Int = 5000
}
