package me.oriley.crate

import android.support.annotation.Nullable

@SuppressWarnings("GroovyUnusedDeclaration")
class CrateExtension {

    def boolean staticMode = true
    def boolean debugLogging = false
    def String packageName = null
    def String className = null

    def setStaticMode(boolean enable) {
        staticMode = enable
    }

    def boolean getStaticMode() {
        return staticMode
    }

    def setDebugLogging(boolean enable) {
        debugLogging = enable
    }

    def boolean getDebugLogging() {
        return debugLogging
    }

    def setPackageName(@Nullable String packageName) {
        this.packageName = packageName
    }

    @Nullable
    def String getPackageName() {
        return packageName
    }

    def setClassName(@Nullable String className) {
        this.className = className
    }

    @Nullable
    def String getClassName() {
        return className
    }
}