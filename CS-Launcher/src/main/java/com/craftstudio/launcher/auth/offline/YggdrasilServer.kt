package com.craftstudio.launcher.auth.offline

import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson

class YggdrasilServer(port: Int = 0) : NanoHTTPD(port) {

    data class Character(
        val uuid: String,
        val username: String,
        val loadedSkin: LoadedSkin? = null
    )

    private val characters = mutableMapOf<String, Character>()
    private val textures = mutableMapOf<String, ByteArray>()
    private val gson = Gson()

    init {
        start(SOCKET_READ_TIMEOUT, true)
    }

    fun addCharacter(character: Character) {
        characters[character.uuid] = character
        character.loadedSkin?.let { loaded ->
            loaded.skin?.let { texture -> textures[texture.hash] = texture.data }
            loaded.cape?.let { texture -> textures[texture.hash] = texture.data }
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        if (uri.startsWith("/sessionserver/session/minecraft/profile/")) {
            val uuidStr = uri.removePrefix("/sessionserver/session/minecraft/profile/").trimEnd('/')
            val character = characters.values.find { it.uuid.replace("-", "") == uuidStr }
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{}")
            val profileJson = gson.toJson(buildProfile(character))
            return newFixedLengthResponse(Response.Status.OK, "application/json", profileJson)
        }

        if (uri.startsWith("/textures/")) {
            val hash = uri.removePrefix("/textures/").trimEnd('/')
            val bytes = textures[hash]
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "image/png", "")
            return newFixedLengthResponse(Response.Status.OK, "image/png",
                bytes.inputStream(), bytes.size.toLong())
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json",
            """{"meta":{"serverName":"CS Launcher"}}""")
    }

    private fun buildProfile(character: Character): Map<String, Any> {
        val textureMap = mutableMapOf<String, Any>()
        character.loadedSkin?.let { loaded ->
            loaded.skin?.let { texture ->
                // ✅ Fix: explicit <String, Any> taaki Map value bhi store ho sake
                val skinEntry = mutableMapOf<String, Any>("url" to "http://127.0.0.1:$listeningPort/textures/${texture.hash}")
                if (loaded.textureModel == TextureModel.ALEX) {
                    skinEntry["metadata"] = mapOf("model" to "slim")
                }
                textureMap["SKIN"] = skinEntry
            }
            loaded.cape?.let { texture ->
                textureMap["CAPE"] = mapOf("url" to "http://127.0.0.1:$listeningPort/textures/${texture.hash}")
            }
        }

        val texturesValue = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "profileId" to character.uuid.replace("-", ""),
            "profileName" to character.username,
            "textures" to textureMap
        )

        val encoded = java.util.Base64.getEncoder()
            .encodeToString(gson.toJson(texturesValue).toByteArray())

        return mapOf(
            "id" to character.uuid.replace("-", ""),
            "name" to character.username,
            "properties" to listOf(mapOf("name" to "textures", "value" to encoded))
        )
    }
}
