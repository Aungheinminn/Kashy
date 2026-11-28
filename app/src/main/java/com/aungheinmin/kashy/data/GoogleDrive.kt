package com.aungheinmin.kashy.data

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DriveHttpException(val code: Int, body: String) : Exception("Drive returned $code: $body")

class DriveFile(val name: String, val link: String?)

/**
 * Uploads backups to a "Kashy" folder in the user's Drive. The drive.file scope only lets the app
 * see files it created itself, so it never reads anything else in the user's Drive.
 */
object GoogleDrive {
    private val SCOPE = Scope("https://www.googleapis.com/auth/drive.file")
    private const val API = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
    private const val FOLDER_NAME = "Kashy"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    private const val SHEET_MIME = "application/vnd.google-apps.spreadsheet"

    /** When the result has a resolution, the user still has to pick an account or grant access. */
    suspend fun authorize(context: Context): AuthorizationResult =
        Identity.getAuthorizationClient(context)
            .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(SCOPE)).build())
            .await()

    fun resultFromIntent(context: Context, data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    suspend fun disconnect(context: Context, email: String) {
        Identity.getAuthorizationClient(context)
            .revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email, "com.google")).setScopes(listOf(SCOPE)).build())
            .await()
    }

    suspend fun accountEmail(context: Context, token: String): String = call(context, token) { t ->
        JSONObject(request("GET", "$API/about?fields=user(emailAddress)", t)).getJSONObject("user").getString("emailAddress")
    }

    /** A CSV is converted to a Google Sheet on upload; JSON stays a plain file so it can be imported back. */
    suspend fun upload(context: Context, token: String, fileName: String, text: String, format: BackupFormat): DriveFile =
        call(context, token) { t ->
            val metadata = JSONObject().put("parents", JSONArray().put(folderId(t)))
            val content = when (format) {
                BackupFormat.JSON -> {
                    metadata.put("name", fileName).put("mimeType", format.mimeType)
                    text
                }
                BackupFormat.CSV -> {
                    metadata.put("name", fileName.removeSuffix(".${format.extension}")).put("mimeType", SHEET_MIME)
                    // Sheets would keep the BOM as part of the first header cell.
                    text.removePrefix("﻿")
                }
            }
            val boundary = "kashy-${UUID.randomUUID()}"
            val body = buildString {
                append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString())
                append("\r\n--$boundary\r\nContent-Type: ${format.mimeType}; charset=UTF-8\r\n\r\n")
                append(content)
                append("\r\n--$boundary--\r\n")
            }
            val file = JSONObject(
                request(
                    "POST",
                    "$UPLOAD_API/files?uploadType=multipart&fields=name,webViewLink",
                    t,
                    "multipart/related; boundary=$boundary",
                    body.toByteArray(),
                )
            )
            DriveFile(file.getString("name"), file.optString("webViewLink").ifEmpty { null })
        }

    private fun folderId(token: String): String {
        val query = URLEncoder.encode("name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false", "UTF-8")
        val found = JSONObject(request("GET", "$API/files?q=$query&spaces=drive&fields=files(id)", token)).getJSONArray("files")
        if (found.length() > 0) return found.getJSONObject(0).getString("id")
        val folder = JSONObject().put("name", FOLDER_NAME).put("mimeType", FOLDER_MIME)
        return JSONObject(request("POST", "$API/files?fields=id", token, "application/json; charset=UTF-8", folder.toString().toByteArray()))
            .getString("id")
    }

    /** Play services can hand back a cached token that has since expired or been revoked; on 401 it is cleared and fetched once more. */
    private suspend fun <T> call(context: Context, token: String, block: (String) -> T): T =
        try {
            withContext(Dispatchers.IO) { block(token) }
        } catch (e: DriveHttpException) {
            if (e.code != HttpURLConnection.HTTP_UNAUTHORIZED) throw e
            val client = Identity.getAuthorizationClient(context)
            client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
            val fresh = authorize(context).accessToken ?: throw e
            withContext(Dispatchers.IO) { block(fresh) }
        }

    private fun request(method: String, url: String, token: String, contentType: String? = null, body: ByteArray? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw DriveHttpException(code, connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
}
