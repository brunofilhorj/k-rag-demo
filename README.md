# K-RAG Demo 🚀

A Kotlin + Spring Boot demo for a Retrieval-Augmented Generation (RAG) workflow with local Ollama models, vector search, and an MCP-style external context layer.

The project combines:

- local document indexing and retrieval
- LLM-backed question answering
- live tool execution via MCP-style adapters
- structured routing for Jira/GitHub/system context before final answer generation

---

## Overview

This application is designed to answer questions using two sources of context:

1. Local knowledge-base documents indexed in memory via LangChain4j
2. External context retrieved from MCP-compatible tools (Jira, GitHub, status checks, etc.)

The idea is not to replace RAG with tool calling, but to enrich it. The traditional flow remains:

- document chunks are retrieved from the local vector store
- relevant MCP tool results are appended as external facts
- the final prompt is built with all context
- the LLM answers using the direct tool context when it is present

This is especially useful for “current state” questions such as:

- “O Bruno tem alguma issue?”
- “Tem pull request aberto para João?”
- “Há issues abertas em Jira?”
- “Qual é o status do sistema?”

---

## Main features

- 🔍 RAG over indexed documents using LangChain4j + Ollama
- 📄 Document ingestion via raw text or uploaded PDF/TXT/Markdown
- 🗂️ Knowledge-base management: list, delete, and inspect indexed documents
- 🔌 MCP connector layer for external context
- 🧭 Tool intent routing for Jira/GitHub/system queries
- ⚙️ Demo Jira dataset with multiple issues and statuses
- 🧪 Regression tests for MCP routing and result normalization

---

## Architecture

The project follows a Hexagonal Architecture with ports and adapters.

```text
src/main/kotlin/com/bfilho/kragdemo/
├── adapter/
│   ├── in/
│   │   └── web/                 # HTTP controllers and DTOs
│   └── out/
│       ├── langchain4j/         # RAG + Ollama integration
│       └── mcp/                 # MCP connector, routing, registry and tools
│           ├── config/
│           ├── contract/
│           ├── controller/
│           ├── router/
│           └── tool/
├── domain/
│   ├── model/                   # Question, Answer, DocumentInfo, ExternalContextFact
│   └── port/
│       ├── in/
│       └── out/
└── KRagDemoApplication.kt
```

The MCP layer is intentionally separated from the core RAG logic:

- `ExternalContextPort` defines how external facts are requested
- `McpConnectorAdapter` calls the configured MCP server
- `ToolIntentRouter` decides which tool should run for a given question
- `McpToolRegistry` exposes the available tools
- each tool implementation returns normalized text for the LLM prompt

---

## MCP behavior in this project

This repo does not use a fully external vendor-standard MCP server in the strictest sense. Instead, it exposes an MCP-compatible HTTP interface inside the same Spring Boot app and uses it as a lightweight demo layer.

The flow is:

1. user asks a question
2. `ToolIntentRouter` decides which tool matches the intent
3. `McpConnectorAdapter` posts to `/mcp/tools/call`
4. the tool returns structured text
5. the result is added as external context before the LLM answers

Examples of supported routing patterns:

- direct Jira key: `ABC-123`, `abc-123`, `issueKey=abc-123`
- issue-related questions: `Fulano tem issues?`, `Fulano tem issues abertas?`
- list queries: `busque todas as issues`, `issues fechadas`
- PR queries: `tem PR aberto?`, `pull request`, `branch`, `commit`, `release`
- system queries: `status`, `health`, `saúde`

---

## Technologies used

- Kotlin
- Spring Boot 4.1.1
- Java 21
- LangChain4j 0.35.0
- Ollama
- InMemoryEmbeddingStore
- Apache PDFBox

---

## Prerequisites

- Java 21+
- Ollama running locally on `http://localhost:11434`
- optional: a PDF in `${user.home}/repos/sample.pdf` if you want startup ingestion enabled

---

## Configuration

The main config file is:

`src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: k-rag-demo

initial-document:
  pdf-path: ${user.home}/repos/sample.pdf

ollama:
  url: http://localhost:11434
  embedding-model: nomic-embed-text
  chat-model: llama3.2

mcp:
  enabled: true
  server-url: http://localhost:8080
  tools:
    - jira.get_issue
    - jira.list_issues
    - github.search_prs
    - system.status
  timeout-ms: 5000

prompt:
  variant: BASELINE
  template:
    user: |
      Use the context below as the source of truth.
      If the context contains a direct answer from a tool, external system, or retrieved document, answer directly from it.
      Do not say you lack information when the answer is already present in the context.
      If the context does not contain the answer, say the information is not available in the knowledge base.

      CONTEXT:

      {{context}}

      QUESTION:

      {{question}}
```

Important note:

- `server-url` can point to the same app (`http://localhost:8080`) in this demo
- the app exposes both the REST API and the MCP endpoints on the same Spring Boot instance

---

## Run locally

Start Ollama and pull the required models:

```bash
ollama pull nomic-embed-text
ollama pull llama3.2
```

Then start the app:

```bash
./gradlew bootRun
```

The app will run on:

```text
http://localhost:8080
```

---

## REST API

### POST `/api/v1/questions`

Ask a question using the local RAG flow + MCP external context.

Request body:

```json
{
  "question": "O Bruno tem alguma issue?"
}
```

Response example:

```json
{
  "question": "O Bruno tem alguma issue?",
  "answer": "Sim, o Bruno tem issues abertas...",
  "retrievedChunks": [
    "[External source: MCP]\n[Tool: jira.list_issues]\nStatus: success\nSummary: ..."
  ]
}
```

### Document endpoints

- `POST /api/v1/documents/text` — add raw text to the knowledge base
- `POST /api/v1/documents/upload` — upload PDF/TXT/Markdown
- `GET /api/v1/documents` — list indexed documents
- `DELETE /api/v1/documents/{id}` — delete a document and its vectors

---

## MCP endpoints

The app exposes a lightweight MCP-compatible interface directly in the same service.

### List tools

```bash
curl -X POST http://localhost:8080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Invoke a tool

```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "jira.list_issues",
    "arguments": {
      "assignee": "Bruno",
      "status": "all"
    }
  }'
```

Example response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "ABC-100 | owner=Bruno | status=OPEN | summary=Login page crash on Safari"
      }
    ]
  }
}
```

Available demo tools:

- `jira.get_issue`
- `jira.list_issues`
- `github.search_prs`
- `system.status`

---

## Demo Jira dataset

The app includes a small in-memory Jira dataset used to validate the issue-routing flow. It contains several sample issues across different owners and statuses.

This dataset is intentionally simple and designed to demo how tool intent detection and external context enrichment behave in a real app flow.

---

## Testing

Run the suite:

```bash
./gradlew test
```

The project includes regression tests around MCP routing and tool extraction, including scenarios such as:

- explicit Jira keys
- generic issue questions
- `issueKey=abc-123` style parsing
- issue-list detection
- error-payload filtering

---

## Notes

This project is a demo implementation and not a production-ready external MCP server implementation. It is intended to show the architectural pattern clearly:

- keep the domain and RAG core independent
- plug in external context through a structured connector layer
- use tool results as authoritative context when present

That makes it easy to replace the demo Jira/GitHub logic with real integrations later.
