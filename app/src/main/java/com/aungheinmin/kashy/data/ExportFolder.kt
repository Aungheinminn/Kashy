package com.aungheinmin.kashy.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Every export, on the phone or in Google Drive, lands in a folder with this name. */
const val EXPORT_FOLDER = "ရငွေ ပေးငွေ"

object ExportFolder {
    /** Where files end up, as the Files app shows it. */
    val displayPath = "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER"

    /** Before Android 10 the app needs the storage permission to write to Downloads. */
    val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Blocking; call off the main thread. */
    fun save(context: Context, fileName: String, mimeType: String, text: String) {
        val bytes = text.toByteArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, displayPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Couldn't create $fileName")
            try {
                resolver.openOutputStream(uri)!!.use { it.write(bytes) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), EXPORT_FOLDER)
            check(dir.isDirectory || dir.mkdirs()) { "Couldn't create $dir" }
            val file = uniqueFile(dir, fileName)
            file.writeBytes(bytes)
            MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf(mimeType), null)
        }
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val base = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        var file = File(dir, fileName)
        var n = 1
        while (file.exists()) file = File(dir, "$base ($n).$extension").also { n++ }
        return file
    }
}
