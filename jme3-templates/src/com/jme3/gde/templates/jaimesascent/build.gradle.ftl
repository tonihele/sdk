plugins {
    id 'java'
    id 'application'
}

group 'com.JaimesAscent'
version '1.0'

mainClassName = "com.JaimesAscent.JaimesAscent"

repositories {
    mavenCentral()
}

project.ext {
  jmeVer = '3.9.0-stable'
}

project(":assets") {
    apply plugin: "java"

    buildDir = rootProject.file("build/assets")

    sourceSets {
        main {
            resources {
                srcDir '.'
            }
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {

  implementation "org.jmonkeyengine:jme3-core:$jmeVer"
  implementation "org.jmonkeyengine:jme3-desktop:$jmeVer"
  implementation "org.jmonkeyengine:jme3-lwjgl:$jmeVer"
  implementation "org.jmonkeyengine:jme3-lwjgl:$jmeVer"
  implementation "com.github.stephengold:Heart:9.3.0"
  implementation "com.github.stephengold:Minie:9.0.3"
  implementation project("assets")

}

jar {
    manifest {
        attributes 'Main-Class': "$mainClassName"
    }
}

wrapper {
    gradleVersion = '9.2.1'
}
