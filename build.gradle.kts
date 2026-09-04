plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.bfilho"
version = "0.0.1-SNAPSHOT"
description = "k-rag-demo"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus:1.11.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("dev.langchain4j:langchain4j:0.35.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:0.35.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.35.0")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:0.35.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Load .env.local into the environment when running bootRun so local env vars like YOU_COM_API_KEY are available

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    doFirst {
        val envFile = file(".env.local")
        if (envFile.exists()) {
            val lines = envFile.readLines()
            val envMap = lines.mapNotNull { raw ->
                val line = raw.trim()
                if (line.isBlank() || line.startsWith("#")) return@mapNotNull null
                val idx = line.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1)
                key to value
            }.toMap()

            // Copy into the Gradle task environment map
            environment.putAll(envMap)
            println("Loaded ${envMap.size} entries from .env.local into bootRun environment")
        }
    }
}
