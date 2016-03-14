package me.oriley.crate

@SuppressWarnings("GroovyUnusedDeclaration")
class CrateExtension {

    def boolean debugLogging = false

    def setDebugLogging(boolean enable) {
        debugLogging = enable
    }

    def boolean getDebugLogging() {
        return debugLogging
    }
}