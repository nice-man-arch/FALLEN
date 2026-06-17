package com.example.util

import android.content.Context
import android.os.Build

fun Context.getSafeAttributionContext(): Context {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            this.createAttributionContext("default")
        } catch (e: Exception) {
            this
        }
    } else {
        this
    }
}
