package com.craftstudio.launcher.plugins.renderer

import java.io.File

class LocalRendererPlugin(
    id: String,
    displayName: String,
    uniqueIdentifier: String,
    glName: String,
    eglName: String,
    path: String,
    env: Map<String, String>,
    dlopen: List<String>,
    val folderPath: File
) : RendererPlugin(
    id, displayName, uniqueIdentifier, glName, eglName, path, env, dlopen
)