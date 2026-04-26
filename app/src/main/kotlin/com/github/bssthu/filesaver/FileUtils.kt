package com.github.bssthu.filesaver

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FileUtils {

    /**
     * 从 content:// URI 获取文件显示名称
     */
    fun getFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        cursor.getString(idx)?.let { return it }
                    }
                }
            }
        // Fallback: 从 URI 路径提取
        return uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    /**
     * 将 srcUri 的内容复制到 SAF 目录树下，返回是否成功
     */
    suspend fun copyToTree(
        context: Context,
        srcUri: Uri,
        treeUri: Uri,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val destDir = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IOException("Cannot access directory")

            // 使用 application/octet-stream 保持用户选择的完整文件名
            val destFile = destDir.createFile("application/octet-stream", fileName)
                ?: throw IOException("Cannot create file: $fileName")

            context.contentResolver.openInputStream(srcUri)?.use { input ->
                context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                    input.copyTo(output, bufferSize = 8192)
                } ?: throw IOException("Cannot open output stream")
            } ?: throw IOException("Cannot open input stream")

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 通过 MediaStore 直接写入系统 Downloads 根目录。
     */
    suspend fun copyToDownloads(
        context: Context,
        srcUri: Uri,
        fileName: String,
        mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        var destUri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            destUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Cannot create Downloads item")

            context.contentResolver.openInputStream(srcUri)?.use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output, bufferSize = 8192)
                } ?: throw IOException("Cannot open output stream")
            } ?: throw IOException("Cannot open input stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(destUri, publish, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            destUri?.let { context.contentResolver.delete(it, null, null) }
            false
        }
    }
}
