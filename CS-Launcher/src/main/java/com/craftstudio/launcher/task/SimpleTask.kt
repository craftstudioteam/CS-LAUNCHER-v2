package com.craftstudio.launcher.task

import java.util.concurrent.Callable

class SimpleTask<V>(private val callable: Callable<V>) : Task<V>() {
    override fun performMainTask() {
        setResult(callable.call())
    }
}