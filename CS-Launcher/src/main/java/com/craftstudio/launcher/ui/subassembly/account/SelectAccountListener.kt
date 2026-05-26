package com.craftstudio.launcher.ui.subassembly.account

import com.craftstudio.launcher.value.MinecraftAccount

interface SelectAccountListener {
    fun onSelect(account: MinecraftAccount)
}
