package com.projectortrace.ui

import com.projectortrace.imaging.calculateSampleSize
import org.junit.Assert.*
import org.junit.Test

class DecodeSizingTest {
    @Test fun panoramaIsBounded() = assertEquals(16, calculateSampleSize(30000, 1000, 3072, 2160))
    @Test fun tallScanIsBounded() = assertEquals(16, calculateSampleSize(1000, 30000, 2160, 3072))
    @Test fun smallImageIsNotUpscaled() = assertEquals(1, calculateSampleSize(640, 480, 1920, 1080))
    @Test fun squareIsBoundedByShortViewportAxis() = assertEquals(4, calculateSampleSize(6000, 6000, 3072, 2160))
    @Test fun oddDimensionsRoundUp() = assertEquals(4, calculateSampleSize(4001, 2001, 2000, 1000))
    @Test(expected = IllegalArgumentException::class) fun invalidDimensionsRejected() {
        calculateSampleSize(0, 10, 10, 10)
    }
    @Test fun wideRangeStaysInsideBudget() {
        for (width in listOf(1, 17, 1919, 1920, 4097, 30000, 1000000)) {
            for (height in listOf(1, 19, 1080, 6001, 30000)) {
                val sample = calculateSampleSize(width, height, 1920, 1080)
                assertTrue((width.toLong() + sample - 1) / sample <= 1920)
                assertTrue((height.toLong() + sample - 1) / sample <= 1080)
                assertEquals(0, sample and (sample - 1))
            }
        }
    }
}
