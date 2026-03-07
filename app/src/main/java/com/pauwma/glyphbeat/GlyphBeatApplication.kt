package com.pauwma.glyphbeat

import android.app.Application
import com.pauwma.glyphbeat.utils.DebugLogger

class GlyphBeatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
    }
}
