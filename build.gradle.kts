plugins {
    kotlin("multiplatform") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "edu.kit.iti.formal.flavium"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    /*js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }*/
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val ktor_version = "2.2.1"
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-call-logging:$ktor_version")
                implementation("io.ktor:ktor-server-status-pages:$ktor_version")
                implementation("io.ktor:ktor-server-html-builder:$ktor_version")
                implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
                implementation("io.ktor:ktor-server-default-headers:$ktor_version")

                //implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation("com.charleskorn.kaml:kaml:0.49.0")
                implementation("ch.qos.logback:logback-classic:1.4.4")

                implementation("com.h2database:h2:2.1.214")
                //implementation("org.xerial:sqlite-jdbc:3.40.0.0")

                val exposedVersion = "0.40.1"
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
            }
        }
        val jvmTest by getting
        /*val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.346")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.346")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion:11.9.3-pre.346")
            }
        }
        val jsTest by getting
        */
    }
}

application {
    mainClass.set("edu.kit.iti.formal.flavium.ServerKt")
}

/*tasks.named<Copy>("jvmProcessResources") {
    val jsBrowserDistribution = tasks.named("jsBrowserDistribution")
    from(jsBrowserDistribution)
}
*/

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}
dependencies {
    implementation("io.ktor:ktor-server-call-logging-jvm:2.2.1")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:2.2.1")
    implementation("io.ktor:ktor-server-core-jvm:2.2.1")
    implementation("io.ktor:ktor-server-host-common-jvm:2.2.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.2.1")
}
