pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // Newer Boot 4 / Testcontainers 2 artifacts are often missing from Aliyun.
        mavenCentral()
        maven("https://repo.huaweicloud.com/repository/maven/")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/spring")
    }
}

rootProject.name = "workspace"
