package com.craftstudio.launcher.plugins.renderer

class ApkRendererPlugin(
    id: String,
    displayName: String,
    uniqueIdentifier: String,
    glName: String,
    eglName: String,
    path: String,
    env: Map<String, String>,
    dlopen: List<String>,
    val packageName: String
) : RendererPlugin(
    id, displayName, uniqueIdentifier, glName, eglName, path, env, dlopen
)