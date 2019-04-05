package com.sercanturkmen.mobilesegmenter

import android.graphics.Bitmap
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.sercanturkmen.mobilesegmenter", appContext.packageName)

        val config = appContext.assets
            .open("model_config.json")
            .bufferedReader()
            .use {
                val json = it.readText()
                Segmenter.Config.fromJson(json)
            }

        if (config == null) {
            fail("Config was null.")
            return
        }

        val segmenter = Segmenter(appContext, config)

        val result = segmenter.segment(Bitmap.createBitmap(config.inputWidth, config.inputHeight, Bitmap.Config.ARGB_8888))

        segmenter.close()
    }
}
