package com.brigadka.app.data.repository

import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.MediaItem
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

interface MediaRepository {
    /**
     * Uploads a media file to the server
     * @param fileBytes The binary content of the file
     * @param fileName The name of the file including extension
     * @param thumbnailBytes Optional thumbnail image bytes
     * @param thumbnailFileName Optional thumbnail file name
     * @return MediaResponse containing the ID and URLs of the uploaded media
     * @throws IOException if there's a network error
     * @throws Exception for other errors
     */
    suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        thumbnailBytes: ByteArray,
        thumbnailFileName: String
    ): MediaItem

    /**
     * Uploads a media file with an automatically generated thumbnail
     * @param fileBytes The binary content of the file
     * @param fileName The name of the file including extension
     * @return MediaResponse containing the ID and URLs of the uploaded media
     * @throws IOException if there's a network error
     * @throws Exception for other errors
     */
    suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String
    ): MediaItem
}

class MediaRepositoryImpl(
    private val api: BrigadkaApiService
) : MediaRepository {

    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        thumbnailBytes: ByteArray,
        thumbnailFileName: String
    ): MediaItem =
        withContext(Dispatchers.IO) {
            try {
                api.uploadMedia(
                    file = fileBytes,
                    fileName = fileName,
                    thumbnail = thumbnailBytes,
                    thumbnailFileName = thumbnailFileName
                )
            } catch (e: Exception) {
                // Log the error or handle specific exceptions
                throw e
            }
        }

    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String
    ): MediaItem =
        withContext(Dispatchers.Default) {
            // Convert video if needed
            val processedFileBytes = if (isVideoFile(fileName)) {
                convertVideoToMp4(fileBytes, fileName)
            } else {
                fileBytes
            }

            // Use the processed file name (ensuring mp4 extension for videos)
            val processedFileName = if (isVideoFile(fileName)) {
                ensureMp4Extension(fileName)
            } else {
                fileName
            }

            val thumbnailData = generateThumbnail(processedFileBytes, processedFileName) // TODO: fix image rotation
            val thumbnailFileName = "thumbnail_${replaceExtensionWithPng(processedFileName)}"

            withContext(Dispatchers.IO) {
                uploadMedia(processedFileBytes, processedFileName, thumbnailData, thumbnailFileName)
            }
        }
    /**
     * Generates a thumbnail from the provided file bytes
     */
    private fun generateThumbnail(fileBytes: ByteArray, fileName: String): ByteArray {
        // This will be platform-specific implementation
        return createThumbnail(fileBytes, fileName.lowercase())
    }
}

private fun isVideoFile(fileName: String): Boolean {
    return fileName.endsWith(".mp4", ignoreCase = true) ||
            fileName.endsWith(".mov", ignoreCase = true) ||
            fileName.endsWith(".webm", ignoreCase = true) ||
            fileName.endsWith(".avi", ignoreCase = true)
}

private fun ensureMp4Extension(fileName: String): String {
    val baseName = fileName.substringBeforeLast('.', fileName)
    return "$baseName.mp4"
}

fun replaceExtensionWithPng(fileName: String): String {
    val baseName = fileName.substringBeforeLast('.', fileName)
    return "$baseName.png"
}
