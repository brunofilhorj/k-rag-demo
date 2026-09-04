package com.bfilho.kragdemo.adapter.out.mcp.tool

interface WebSearchProvider {
    fun search(query: String, maxResults: Int = 5, language: String? = null): List<SearchResult>
}
