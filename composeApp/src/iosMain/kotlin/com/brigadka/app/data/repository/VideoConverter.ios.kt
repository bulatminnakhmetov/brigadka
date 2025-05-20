// composeApp/src/iosMain/kotlin/com/brigadka/app/data/repository/VideoConverter.ios.kt
package com.brigadka.app.data.repository

import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.CoreGraphics.CGSizeMake

@OptIn(ExperimentalForeignApi::class)
actual fun convertVideoToMp4(fileBytes: ByteArray, fileName: String): ByteArray {
    // Skip conversion if already MP4
    if (fileName.endsWith(".mp4", ignoreCase = true)) {
        return fileBytes
    }

    // Create temporary files
    val inputFilePath = NSTemporaryDirectory() + "/input_${NSUUID.UUID().UUIDString()}.$fileName"
    val outputFilePath = NSTemporaryDirectory() + "/output_${NSUUID.UUID().UUIDString()}.mp4"

    val inputFileURL = NSURL.fileURLWithPath(inputFilePath)
    val outputFileURL = NSURL.fileURLWithPath(outputFilePath)

    // Save input bytes to file
    val nsData = fileBytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), fileBytes.size.toULong())
    }

    if (!nsData.writeToURL(inputFileURL, true)) {
        return fileBytes // Return original if we can't save to temp file
    }

    try {
        // Create asset from input file
        val asset = AVURLAsset.URLAssetWithURL(inputFileURL, null)

        // Create export session
        val exportSession = AVAssetExportSession.exportSessionWithAsset(
            asset,
            AVAssetExportPresetHEVC1920x1080 // Use H.264 preset closest to 720p
        )

        if (exportSession == null) {
            return fileBytes
        }

        // Configure export
        exportSession.outputURL = outputFileURL
        exportSession.outputFileType = AVFileTypeMPEG4
        exportSession.shouldOptimizeForNetworkUse = true

        // Video composition to resize
        val videoComposition = AVMutableVideoComposition.videoComposition().apply {
            setRenderSize(CGSizeMake(1280.0, 720.0)) // 720p
            setFrameDuration(CMTimeMake(1, 30)) // 30fps
        }

        exportSession.videoComposition = videoComposition

        // Wait for export to complete
        val semaphore = NSCondition()
        exportSession.exportAsynchronouslyWithCompletionHandler {
            semaphore.signal()
        }

        semaphore.lock()
        semaphore.wait()
        semaphore.unlock()

        // Check if export was successful
        if (exportSession.status == AVAssetExportSessionStatusCompleted) {
            // Read the converted file
            val outputData = NSData.dataWithContentsOfURL(outputFileURL)
                ?: return fileBytes

            val length = outputData.length.toInt()
            val result = ByteArray(length)

            result.usePinned { pinned ->
                outputData.getBytes(pinned.addressOf(0), length.toULong())
            }

            return result
        } else {
            return fileBytes // Return original on error
        }
    } catch (e: Exception) {
        return fileBytes
    } finally {
        // Clean up temp files
        NSFileManager.defaultManager.removeItemAtURL(inputFileURL, null)
        NSFileManager.defaultManager.removeItemAtURL(outputFileURL, null)
    }
}