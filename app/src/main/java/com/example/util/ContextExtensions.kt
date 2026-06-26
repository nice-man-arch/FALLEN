package com.example.util

import android.content.Context
import android.os.Build

fun Context.getSafeAttributionContext(): Context {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            this.createAttributionContext("music_playback")
        } catch (e: Exception) {
            this
        }
    } else {
        this
    }
}
