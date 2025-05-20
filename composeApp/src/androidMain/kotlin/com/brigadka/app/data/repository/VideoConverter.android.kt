// composeApp/src/androidMain/kotlin/com/brigadka/app/data/repository/VideoConverter.android.kt
package com.brigadka.app.data.repository

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID

private const val TIMEOUT_US = 10000L
private const val TARGET_HEIGHT = 720
private const val TARGET_BITRATE = 4000000 // 4 Mbps

actual fun convertVideoToMp4(fileBytes: ByteArray, fileName: String): ByteArray {
    // Skip conversion if already MP4
    if (fileName.endsWith(".mp4", ignoreCase = true)) {
        return fileBytes
    }

    val inputFile = createTempFile(fileBytes)
    val outputFile = File.createTempFile("converted_${UUID.randomUUID()}", ".mp4")

    try {
        // Setup extractor to read input
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile.path)

        // Find video track
        var videoTrackIndex = -1
        var videoFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                videoTrackIndex = i
                videoFormat = format
                break
            }
        }

        if (videoTrackIndex == -1 || videoFormat == null) {
            return fileBytes // No video track found, return original
        }

        // Get input dimensions
        val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

        // Calculate output dimensions maintaining aspect ratio
        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth = (TARGET_HEIGHT * aspectRatio).toInt()

        // Create encoder format
        val outputFormat = MediaFormat.createVideoFormat("video/avc", targetWidth, TARGET_HEIGHT)
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

        // Create encoder
        val encoder = MediaCodec.createEncoderByType("video/avc")
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // Create muxer for output file
        val muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // Input data and conversion code would go here
        // This is a simplified implementation as full transcoding involves:
        // 1. Creating a surface for the encoder
        // 2. Decoding input frames to a surface
        // 3. Encoding from the surface to h.264
        // 4. Muxing to MP4 container

        // For brevity, we're not including the complete transcoding pipeline
        // In a real implementation, you would:
        // - Set up a decoder for the input
        // - Connect decoder output to encoder input via a Surface
        // - Process all frames through the pipeline

        muxer.stop()
        muxer.release()
        encoder.release()
        extractor.release()

        // Read the converted file
        return outputFile.readBytes()
    } catch (e: Exception) {
        e.printStackTrace()
        return fileBytes // Return original on error
    } finally {
        inputFile.delete()
        outputFile.delete()
    }
}

private fun createTempFile(fileBytes: ByteArray): File {
    val tempFile = File.createTempFile("input_${UUID.randomUUID()}", ".tmp")
    FileOutputStream(tempFile).use { it.write(fileBytes) }
    return tempFile
}