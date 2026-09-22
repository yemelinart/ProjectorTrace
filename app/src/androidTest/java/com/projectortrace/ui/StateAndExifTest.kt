package com.projectortrace.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.KeyEvent
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.projectortrace.model.TraceMode
import com.projectortrace.model.TransformState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StateAndExifTest {
    @Test fun mirroredExifOrientationsMapEveryPixelCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "orientation.jpg")
        val pixels = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)
        val expected = mapOf(5 to listOf(0,2,4,1,3,5), 6 to listOf(4,2,0,5,3,1),
            7 to listOf(5,3,1,4,2,0), 8 to listOf(1,3,5,0,2,4))
        try {
            val seed = Bitmap.createBitmap(pixels, 2, 3, Bitmap.Config.ARGB_8888)
            file.outputStream().use { seed.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            for ((orientation, order) in expected) {
                ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString()); saveAttributes() }
                val source = Bitmap.createBitmap(pixels, 2, 3, Bitmap.Config.ARGB_8888)
                val output = source.orientedByExif(context, Uri.fromFile(file))
                assertEquals(3, output.width); assertEquals(2, output.height)
                order.forEachIndexed { i, sourceIndex -> assertEquals("EXIF $orientation pixel $i", pixels[sourceIndex], output.getPixel(i % 3, i / 3)) }
            }
        } finally { file.delete() }
    }

    @Test fun remoteControlsRemainBoundedAndSurviveSaveRestore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (mode in TraceMode.entries) {
            var state = TransformState(currentMode = mode)
            repeat(200) {
                state = state.updateForDirection(KeyEvent.KEYCODE_DPAD_RIGHT)
                state = state.updateForDirection(KeyEvent.KEYCODE_DPAD_UP)
            }
            assertTrue(state.scale.isFinite() && state.scale in .2f..5f)
            assertTrue(state.offsetX in -4000f..4000f && state.offsetY in -4000f..4000f)
            context.saveTransformState(state)
            val restored = context.loadTransformState()
            assertEquals("Scale $mode", state.scale, restored.scale, .0001f)
            assertEquals("Offset $mode", state.offsetX, restored.offsetX, .0001f)
            assertEquals("Rotation $mode", state.rotationDegrees, restored.rotationDegrees, .0001f)
        }
    }

    @Test fun lockPreventsDirectionalEdits() {
        for (mode in TraceMode.entries.filter { it != TraceMode.Lock }) {
            val state = TransformState(currentMode = mode, isLocked = true)
            assertEquals(state, state.updateForDirection(KeyEvent.KEYCODE_DPAD_RIGHT))
        }
    }
}
