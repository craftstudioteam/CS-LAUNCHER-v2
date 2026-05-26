package com.craftstudio.launcher.feature.version.install

enum class Addon(val addonName: String) {
    OPTIFINE("OptiFine"),
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    FABRIC("Fabric"),
    FABRIC_API("Fabric API"),
    QUILT("Quilt"),
    QSL("QSL");

    companion object {
        private val compatibleMap = mapOf(
            OPTIFINE to setOf(OPTIFINE, FORGE),
            FORGE to setOf(OPTIFINE, FORGE),
            NEOFORGE to setOf(NEOFORGE),
            FABRIC to setOf(FABRIC, FABRIC_API),
            FABRIC_API to setOf(FABRIC, FABRIC_API),
            QUILT to setOf(QUILT, QSL),
            QSL to setOf(QUILT, QSL)
        )

        fun getCompatibles(addon: Addon) = compatibleMap[addon]
    }
}