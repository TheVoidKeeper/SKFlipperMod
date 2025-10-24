package com.thevoidkeeper.skflipper.config

/**
 * Placeholder for YACL v3 config integration.
 * We keep a plain data holder now; wire into a YACL screen later to avoid relying on wrong API surface.
 */
data class ModConfig(
    val openKeyDefault: Int = 71 // GLFW.GLFW_KEY_G
)