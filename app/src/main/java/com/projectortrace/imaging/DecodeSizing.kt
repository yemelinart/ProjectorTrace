package com.projectortrace.imaging

/** A power-of-two sample that bounds BOTH axes, including panoramas and scans. */
internal fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    require(width > 0 && height > 0 && targetWidth > 0 && targetHeight > 0)
    var sample = 1
    while ((width.toLong() + sample - 1) / sample > targetWidth ||
        (height.toLong() + sample - 1) / sample > targetHeight
    ) {
        if (sample >= (1 shl 30)) return sample
        sample *= 2
    }
    return sample
}
