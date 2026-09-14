plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jooq.jooq-codegen-gradle") version "3.21.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "AI Personal Workspace"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // HTTP API
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Authentication / authorization
    implementation("org.springframework.boot:spring-boot-starter-security")
    // JWT encode / decode via Nimbus
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    // Jakarta Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Type-safe SQL
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    // Schema migrations
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Refresh token / cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Health probes
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // JDBC driver
    runtimeOnly("org.postgresql:postgresql")

    // Generate jOOQ classes from Flyway SQL without a live database
    jooqCodegen("org.jooq:jooq-meta-extensions")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                inputSchema = "PUBLIC"
                includes = ".*"
                excludes = "flyway_schema_history"
                properties.addAll(
                    listOf(
                        org.jooq.meta.jaxb.Property().withKey("scripts").withValue("src/main/resources/db/migration/*.sql"),
                        org.jooq.meta.jaxb.Property().withKey("sort").withValue("flyway"),
                        org.jooq.meta.jaxb.Property().withKey("defaultNameCase").withValue("lower"),
                    ),
                )
            }
            target {
                packageName = "com.example.workspace.infrastructure.database.jooq"
                directory = layout.buildDirectory.dir("generated-sources/jooq").get().asFile.absolutePath
            }
        }
    }
}

tasks.named("jooqCodegen") {
    inputs.files(fileTree("src/main/resources/db/migration"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
