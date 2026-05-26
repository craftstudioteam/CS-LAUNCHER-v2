package com.craftstudio.launcher.plugins.renderer

data class RendererConfig(
    val pluginVersion: Int,
    var rendererId: String,
    var rendererDisplayName: String,
    var glName: String,
    var eglName: String,
    val boatEnv: Map<String, String>,
    val pojavEnv: Map<String, String>,
    val dlopenList: List<String>?
)