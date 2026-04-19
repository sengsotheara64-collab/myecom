package com.example.myecomapp.utils


import android.net.Uri
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resumeWithException

object CloudinaryManager {

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dknt0thb6",
            "api_key" to "569385867159738",
            "api_secret" to "Gos2IQrBQOhNbUr100AhBjhgsBI"
        )
    )


    suspend fun uploadImage(uri: Uri, folder: String): String =
        suspendCancellableCoroutine { continuation ->

            MediaManager.get()
                .upload(uri)
                .option("folder", folder)
                .callback(object : com.cloudinary.android.callback.UploadCallback {

                    override fun onStart(requestId: String?) {}

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            continuation.resume(url, null)
                        } else {
                            continuation.resumeWithException(Exception("Upload failed"))
                        }
                    }

                    override fun onError(
                        requestId: String?,
                        error: com.cloudinary.android.callback.ErrorInfo?
                    ) {
                        continuation.resumeWithException(
                            Exception(error?.description ?: "Upload error")
                        )
                    }

                    override fun onReschedule(
                        requestId: String?,
                        error: com.cloudinary.android.callback.ErrorInfo?
                    ) {}
                })
                .dispatch()
        }

    fun uploadImage(
        imageBytes: ByteArray,
        folder: String
    ): String {
        val publicId = UUID.randomUUID().toString()

        val result = cloudinary.uploader().upload(
            imageBytes,
            mapOf(
                "folder" to folder,
                "public_id" to publicId
            )
        )

        return result["secure_url"] as String
    }
}