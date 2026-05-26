package com.craftstudio.launcher.feature.mod.parser

import com.craftstudio.launcher.setting.unit.StringSettingUnit

enum class AllModCheckSettings(val unit: StringSettingUnit) {
    TOUCH_CONTROLLER(StringSettingUnit("modCheckTouchController", "0")),
    SODIUM_OR_EMBEDDIUM(StringSettingUnit("modCheckSodiumOrEmbeddium", "0")),
    PHYSICS_MOD(StringSettingUnit("modCheckPhysics", "0")),
    MCEF(StringSettingUnit("modCheckMCEF", "0")),
    VALKYRIEN_SKIES(StringSettingUnit("modCheckValkyrienSkies", "0")),
    YES_STEVE_MODEL(StringSettingUnit("modCheckYesSteveModel", "0")),
    IM_BLOCKER(StringSettingUnit("modCheckIMBlocker", "0")),
    REPLAY_MOD(StringSettingUnit("modCheckReplayMod", "0")),
    BORDERLESS_WINDOW(StringSettingUnit("modCheckBorderlessWindow", "0"))
}