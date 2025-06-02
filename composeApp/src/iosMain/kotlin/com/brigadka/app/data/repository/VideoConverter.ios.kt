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
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait


@OptIn(ExperimentalForeignApi::class)
actual fun convertVideoToMp4(fileBytes: ByteArray, fileName: String): ByteArray {
    if (fileName.lowercase().endsWith(".mp4")) return fileBytes   // already OK

    // ─── 1. temp files ─────────────────────────────────────────
    val tmpDir    = NSTemporaryDirectory()
    val inputUrl  = NSURL.fileURLWithPath("$tmpDir/in_${NSUUID.UUID().UUIDString()}.$fileName")
    val outputUrl = NSURL.fileURLWithPath("$tmpDir/out_${NSUUID.UUID().UUIDString()}.mp4")

    val ok = fileBytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), fileBytes.size.toULong())
    }.writeToURL(inputUrl, true)

    if (!ok) return fileBytes

    try {
        val asset        = AVURLAsset.URLAssetWithURL(inputUrl, null)
        val preset       = AVAssetExportPreset1280x720      // 720 p H.264 in an .mp4 container
        val export       = AVAssetExportSession.exportSessionWithAsset(asset, preset) ?: return fileBytes

        export.outputURL                   = outputUrl
        export.outputFileType              = AVFileTypeMPEG4
        export.shouldOptimizeForNetworkUse = true

        // ─── 2. Custom video composition (optional) ────────────
//        run {
//            val track = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() ?: return@run
//            val instruction = AVMutableVideoCompositionInstruction.videoCompositionInstruction().apply {
//                timeRange = CMTimeRangeMake(kCMTimeZero, asset.duration)
//            }
//            val layer = AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(track).apply {
//                // Preserve original orientation / add your own transforms if needed
//                setTransform(track.preferredTransform, kCMTimeZero)
//            }
//            instruction.setLayerInstructions(listOf(layer))
//            val comp = AVMutableVideoComposition.videoComposition().apply {
//                renderSize    = CGSizeMake(1280.0, 720.0)
//                frameDuration = CMTimeMake(1, 30)   // 30 fps
//                instructions  = listOf(instruction)
//            }
//            export.videoComposition = comp
//        }

        // ─── 3. Run export synchronously (semaphore) ───────────
        val sema = dispatch_semaphore_create(0)
        export.exportAsynchronouslyWithCompletionHandler { dispatch_semaphore_signal(sema) }
        dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER)

        // ─── 4. Success? ───────────────────────────────────────
        if (export.status == AVAssetExportSessionStatusCompleted) {
            val data = NSData.dataWithContentsOfURL(outputUrl) ?: return fileBytes
            val res  = ByteArray(data.length.toInt())
            res.usePinned { data.getBytes(it.addressOf(0), data.length) }
            return res
        }
        return fileBytes          // fall back on error
    } catch (_: Exception) {
        return fileBytes
    } finally {
        NSFileManager.defaultManager.removeItemAtURL(inputUrl, null)
        NSFileManager.defaultManager.removeItemAtURL(outputUrl, null)
    }
}
