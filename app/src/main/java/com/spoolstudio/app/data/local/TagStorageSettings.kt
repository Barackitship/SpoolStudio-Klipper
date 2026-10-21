package com.spoolstudio.app.data.local

import android.content.Context

/** Persisted per server: changing a printer profile must not change another server's policy. */
object TagStorageSettings {
    private var context: Context? = null
    fun initialize(context: Context) { this.context = context.applicationContext }
    private fun key(url: String) = "native_tags_only:" + url.trim().trimEnd('/')
    fun nativeOnly(url: String): Boolean = context?.getSharedPreferences("tag_storage", Context.MODE_PRIVATE)
        ?.getBoolean(key(url), false) ?: false
    fun setNativeOnly(url: String, value: Boolean) {
        context?.getSharedPreferences("tag_storage", Context.MODE_PRIVATE)?.edit()
            ?.putBoolean(key(url), value)?.apply()
    }
}
