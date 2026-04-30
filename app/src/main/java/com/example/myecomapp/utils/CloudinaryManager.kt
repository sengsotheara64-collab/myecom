package com.example.myecomapp.utils


import com.cloudinary.Cloudinary
import com.example.myecomapp.utils.Constants.CONFIG_CLOUDARY
import java.util.UUID

object CloudinaryManager {

    private val cloudinary = Cloudinary(CONFIG_CLOUDARY)

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