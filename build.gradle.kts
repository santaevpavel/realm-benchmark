buildscript {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("io.realm:realm-gradle-plugin:10.18.0")
        //classpath("io.realm.kotlin:gradle-plugin:2.3.0")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
}