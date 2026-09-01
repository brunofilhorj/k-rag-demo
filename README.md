# K-RAG Demo 🚀

Uma aplicação de demonstração de **Retrieval-Augmented Generation (RAG)** desenvolvida em **Kotlin** e **Spring Boot**, utilizando **LangChain4j**, **Ollama** e **InMemoryEmbeddingStore**, estruturada com **Arquitetura Hexagonal (Ports and Adapters)**.

---

## 📌 Visão Geral

O **K-RAG Demo** permite consultar uma base de conhecimento dinâmica utilizando modelos de linguagem (LLMs) executados localmente através do **Ollama**. A aplicação suporta a inclusão de documentos via upload de arquivos (PDF, TXT, Markdown) ou inserção direta de texto via API REST, além de permitir o gerenciamento e deleção de documentos indexados.

### Principais Funcionalidades

- 🔍 **Perguntas e Respostas (RAG)**: Busca vetorial por similaridade nos segmentos de texto (chunks) indexados para responder a perguntas de forma contextualizada.
- 📄 **Ingestão Dinâmica de Documentos**:
  - Upload de arquivos PDF, TXT ou Markdown.
  - Ingestão de texto bruto com título customizado via API.
- ⚙️ **Ingestão Inicial Configurável**: Carga opcional de documento padrão via inicialização da aplicação (`initial-document.pdf-path`).
- 🗑️ **Gestão da Base de Conhecimento**: Listagem e remoção de documentos e seus respectivos embeddings da memória vetorial.

---

## 🏗️ Arquitetura

O projeto adota a **Arquitetura Hexagonal (Ports & Adapters)**, garantindo desacoplamento entre a regra de negócio e os frameworks/bibliotecas externas:

```
src/main/kotlin/com/bfilho/kragdemo/
├── adapter/
│   ├── in/web/              # Adaptadores de Entrada (Controllers REST, DTOs)
│   └── out/langchain4j/     # Adaptadores de Saída (Integração com LangChain4j & Ollama)
├── domain/
│   ├── model/               # Entidades de Domínio (Question, Answer, DocumentInfo)
│   ├── port/
│   │   ├── in/              # Use Cases (AskQuestionUseCase, IngestDocumentUseCase, etc.)
│   │   └── out/             # Interfaces de Saída (RagEnginePort, KnowledgeBaseStorePort)
│   └── service/             # Implementação da Lógica de Negócio
```

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Kotlin (JVM 17+)
- **Framework**: Spring Boot 3
- **RAG Framework**: LangChain4j
- **LLM / Embeddings Provider**: [Ollama](https://ollama.com/)
  - Modelo de Embeddings: `nomic-embed-text`
  - Modelo de Chat: `llama3.2`
- **Armazenamento Vetorial**: `InMemoryEmbeddingStore` (LangChain4j)
- **Parse de Documentos**: Apache PDFBox / LangChain4j Parsers

---

## 🚀 Como Executar

### 1. Pré-requisitos

- Java 17+ instalado.
- Docker e Docker Compose instalados (caso opte por rodar o Ollama via container).

### 2. Configurar o Ollama

Você pode rodar o Ollama via Docker usando o `docker-compose.yml` fornecido no repositório:

```bash
docker compose up -d
```

Após iniciar o container do Ollama, baixe os modelos necessários executando:

```bash
docker exec -it ollama-k-rag-demo ollama pull nomic-embed-text
docker exec -it ollama-k-rag-demo ollama pull llama3.2
```

*(Caso utilize o Ollama instalado nativamente no host, execute `ollama pull nomic-embed-text` e `ollama pull llama3.2` no terminal).*

### 3. Configurações da Aplicação (`application.yaml`)

As configurações de integração encontram-se em `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: k-rag-demo

initial-document:
  pdf-path: ${user.home}/repos/sample.pdf # (Opcional) Caminho para PDF inicial

ollama:
  url: http://localhost:11434
  embedding-model: nomic-embed-text
  chat-model: llama3.2
```

### 4. Executando a Aplicação

Compile e inicie a aplicação utilizando o Gradle Wrapper:

```bash
./gradlew bootRun
```

A aplicação estará disponível em `http://localhost:8080`.

---

## 📬 Endpoints da API REST

### 1. Perguntas (RAG)

- **POST** `/api/v1/questions`
  - **Body**:
    ```json
    {
      "question": "Quais são as diretrizes de arquitetura mencionadas nos documentos?"
    }
    ```
  - **Resposta**:
    ```json
    {
      "answer": "As diretrizes estabelecem o uso de Arquitetura Hexagonal..."
    }
    ```

### 2. Ingestão e Gestão da Base de Conhecimento

- **POST** `/api/v1/documents/text` (Ingerir texto bruto)
  - **Body**:
    ```json
    {
      "title": "Manual de Arquitetura",
      "text": "Conteúdo com as definições do sistema..."
    }
    ```

- **POST** `/api/v1/documents/upload` (Upload de arquivo PDF / TXT / MD)
  - **Form-Data**:
    - `file`: arquivo (PDF, TXT ou MD)
    - `title` *(opcional)*: título do documento

- **GET** `/api/v1/documents` (Listar documentos ativos)

- **DELETE** `/api/v1/documents/{id}` (Remover documento e seus vetores)

---

## 🧪 Testes e Coleção Postman

- No repositório, está incluído o arquivo `k-rag-demo.postman_collection.json` contendo todas as requisições prontas para testes.
- Para rodar a suíte de testes automatizados:
  ```bash
  ./gradlew test
  ```
