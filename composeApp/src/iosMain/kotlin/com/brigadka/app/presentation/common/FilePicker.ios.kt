package com.brigadka.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.kamel.core.utils.File
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.datetime.Clock
import platform.UIKit.UIViewController
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.posix.memcpy
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType          // iOS 14+
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeMovie


@Composable
actual fun rememberFilePickerLauncher(
    fileType: FileType,
    onFilePicked: (ByteArray, String) -> Unit,
    onError: (String) -> Unit
): FilePickerLauncher {
    val uiViewController = UIViewController.currentViewController()

    val delegate = remember {
        PhotoPickerDelegate(
            onFilePicked = { data, fileName ->
                onFilePicked(data, fileName)
            },
            onError = { error ->
                onError(error)
            }
        )
    }

    DisposableEffect(uiViewController) {
        onDispose {
            // Cleanup if needed
        }
    }

    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                val configuration = PHPickerConfiguration()

                if (fileType == FileType.VIDEO) {
                    configuration.setFilter(PHPickerFilter.videosFilter())
                } else if (fileType == FileType.IMAGE) {
                    configuration.setFilter(PHPickerFilter.imagesFilter())
                } else {
                    // TODO
                    onError("Unsupported file type: $fileType")
                    return
                }

                // For multiple selection (optional):
                // configuration.setSelectionLimit(0) // 0 means no limit

                val pickerViewController = PHPickerViewController(configuration)
                pickerViewController.delegate = delegate
                uiViewController.presentViewController(pickerViewController, true, null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val onFilePicked: (ByteArray, String) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>
    ) {
        // Grab the results *before* we dismiss – no need for a completion block.
        val first = didFinishPicking.filterIsInstance<PHPickerResult>().firstOrNull()
        picker.dismissViewControllerAnimated(true, null)

        val provider = first?.itemProvider ?: return          // user cancelled

        when {
            // ---------- VIDEO ----------
            provider.hasItemConformingToTypeIdentifier(UTTypeMovie.identifier) -> {
                provider.loadFileRepresentationForTypeIdentifier(
                    UTTypeMovie.identifier
                ) { url, error ->
                    if (error != null) {
                        onError("Error loading video: ${error.localizedDescription}")
                        return@loadFileRepresentationForTypeIdentifier
                    }
                    url?.let { videoUrl ->
                        // Read the file into memory (or just hand back the URL if that’s enough).
                        val nsData = NSData.dataWithContentsOfURL(videoUrl)
                        nsData?.let { data ->
                            val bytes = ByteArray(data.length.toInt())
                            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
                            val fileName = "video_${Clock.System.now().toEpochMilliseconds()}.mov"
                            onFilePicked(bytes, fileName)
                        }
                    }
                }
            }

            // ---------- IMAGE ----------
            provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier) -> {
                provider.loadDataRepresentationForTypeIdentifier(
                    UTTypeImage.identifier
                ) { data, error ->
                    if (error != null) {
                        onError("Error loading image: ${error.localizedDescription}")
                        return@loadDataRepresentationForTypeIdentifier
                    }
                    data?.let {
                        val bytes = ByteArray(it.length.toInt())
                        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), it.bytes, it.length) }
                        val fileName = "image_${Clock.System.now().toEpochMilliseconds()}.jpg"
                        onFilePicked(bytes, fileName)
                    }
                }
            }

            else -> onError("Unsupported UTType")
        }
    }
}

// Extension to get current UIViewController
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun UIViewController.Companion.currentViewController(): UIViewController {
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var viewController = keyWindow?.rootViewController

    while (viewController?.presentedViewController != null) {
        viewController = viewController.presentedViewController
    }

    return viewController!!
}