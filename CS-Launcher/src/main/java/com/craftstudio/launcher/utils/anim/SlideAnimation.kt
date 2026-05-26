package com.craftstudio.launcher.utils.anim

import com.craftstudio.launcher.anim.AnimPlayer

interface SlideAnimation {
    fun slideIn(animPlayer: AnimPlayer)
    fun slideOut(animPlayer: AnimPlayer)
}