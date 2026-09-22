package com.projectortrace.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.projectortrace.model.ChannelMixerMode
import com.projectortrace.model.ColorPaletteMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImagePipelineTest {
    @Test fun everyFilterHandlesTinyAndNormalImagesWithoutRecyclingInput() = runBlocking {
        for (size in listOf(1, 2, 64)) {
            val input = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            input.eraseColor(Color.RED)
            val outputs = listOf(
                createLineArtBitmap(input), createEdgeOutlineBitmap(input, 1f, 1f, 1f),
                createMagicOutlineBitmap(input, 1f, 1f), createClarityBitmap(input),
                createThresholdBitmap(input, .5f, ChannelMixerMode.Normal),
                createPaintBitmap(input, 1f), createPosterizeBitmap(input, 1f),
                createVolumeBitmap(input, 1f), createNoiseReductionBitmap(input, 1f),
            )
            outputs.forEach {
                assertNotNull("Filter failed at ${size}x$size", it)
                assertFalse(it!!.isRecycled)
                assertTrue(it.width > 0 && it.height > 0)
            }
            assertFalse(input.isRecycled)
            assertEquals(Color.RED, input.getPixel(0, 0))
            assertTrue(extractColorPalette(input, 3, ColorPaletteMode.Balanced).isNotEmpty())
        }
    }

    @Test fun panoramicFileIsDecodedWithinViewportBudget() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "test-panorama.png")
        try {
            val source = Bitmap.createBitmap(12000, 100, Bitmap.Config.ARGB_8888)
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            source.recycle()
            val loaded = loadScaledBitmap(context, Uri.fromFile(file), 1920, 1080)
            assertNotNull(loaded)
            assertTrue(loaded!!.width <= 3072)
            assertTrue(loaded.height <= 2160)
        } finally { file.delete() }
    }

    @Test fun corruptImageReturnsFailureInsteadOfCrashing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "invalid-image.jpg")
        try {
            file.writeText("not an image")
            assertNull(loadScaledBitmap(context, Uri.fromFile(file), 1920, 1080))
        } finally { file.delete() }
    }
}
