package com.example.myecomapp.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myecomapp.MyApplication
import com.example.myecomapp.data.User
import com.example.myecomapp.utils.Constants.ADMIN_COLLECTION
import com.example.myecomapp.utils.Constants.PROFILE_IMAGE_COLLECTION
import com.example.myecomapp.utils.Constants.USER_COLLECTION
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.validations.LoginRegisterValidation
import com.example.myecomapp.utils.validations.validateEmail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UserAccountViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    app: Application
) : AndroidViewModel(app)
{
    private val _user = MutableStateFlow<Resource<User>>(Resource.Unspecified())
    val user = _user.asStateFlow()

    private val _updateInfo = MutableStateFlow<Resource<User>>(Resource.Unspecified())
    val updateInfo = _updateInfo.asStateFlow()

    private val _isAdmin = MutableStateFlow<Resource<Boolean>>(Resource.Unspecified())
    val isAdmin = _isAdmin.asStateFlow()

    init {
        checkAdminStatus()
        getUserInformation()
    }

    private fun getUserInformation() {
        viewModelScope.launch {
            _user.emit(Resource.Loading())
        }

        firestore.collection(USER_COLLECTION)
            .document(firebaseAuth.uid!!)
            .addSnapshotListener { value, error ->
                if (value != null && error == null) {
                    val user = value.toObject(User::class.java)
                    user?.let {
                        viewModelScope.launch {
                            _user.emit(Resource.Success(user))
                        }
                    }
                }
            }
    }

    fun updateUser(user: User, imageUri: Uri?) {
        if (validateInput(user)) {
            viewModelScope.launch {
                _updateInfo.emit(Resource.Loading())
            }

            if (imageUri == null) {
                saveUserInformation(user)
            } else {
                saveUserInformationWithNewImage(user, imageUri)
            }
        } else {
            viewModelScope.launch {
                _updateInfo.emit(Resource.Error("Email is not valid"))
            }
        }
    }

    private fun saveUserInformation(user: User) {
        firestore.runTransaction { transaction ->
            val documentRef = firestore.collection(USER_COLLECTION)
                .document(firebaseAuth.uid!!)
            val currentUser = transaction.get(documentRef).toObject(User::class.java)
             currentUser?.let {
                val newUser = user.copy(
                    firstName = user.firstName.ifEmpty { currentUser.firstName },
                    lastName = user.lastName.ifEmpty { currentUser.lastName },
                    email = user.email.ifEmpty { currentUser.email },
                    imagePath = user.imagePath.ifEmpty { currentUser.imagePath }
                )
                transaction.set(documentRef, newUser)
            }

        }
            .addOnSuccessListener {
                viewModelScope.launch {
                    _updateInfo.emit(Resource.Success(user))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _updateInfo.emit(Resource.Error(it.message.toString()))
                }
            }
    }

    @Suppress("deprecation")
    private fun saveUserInformationWithNewImage(user: User, imageUri: Uri) {
        viewModelScope.launch {
            _updateInfo.emit(Resource.Loading())

            try {
                com.cloudinary.android.MediaManager.get()
                    .upload(imageUri)
                    .option("folder", "profile_images")
                    .option("public_id", "user_${firebaseAuth.uid}")
                    .option("overwrite", true)
                    .callback(object : com.cloudinary.android.callback.UploadCallback {

                        override fun onStart(requestId: String?) {}

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(
                            requestId: String?,
                            resultData: MutableMap<Any?, Any?>?
                        ) {
                            val imageUrl = resultData?.get("secure_url").toString()

                            viewModelScope.launch {
                                saveUserInformation(
                                    user.copy(imagePath = imageUrl)
                                )
                            }
                        }

                        override fun onError(
                            requestId: String?,
                            error: com.cloudinary.android.callback.ErrorInfo?
                        ) {
                            viewModelScope.launch {
                                _updateInfo.emit(
                                    Resource.Error(error?.description ?: "Upload failed")
                                )
                            }
                        }

                        override fun onReschedule(
                            requestId: String?,
                            error: com.cloudinary.android.callback.ErrorInfo?
                        ) {}
                    })
                    .dispatch()

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", e.message.toString())
                _updateInfo.emit(Resource.Error(e.message ?: "Error"))
            }
        }
    }

    private fun validateInput(user: User): Boolean {
        return user.email.isEmpty() || validateEmail(user.email) is LoginRegisterValidation.Valid
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            _isAdmin.emit(Resource.Loading())
        }

        val userId = firebaseAuth.uid
        if (userId != null) {
            val adminRef = firestore.collection(ADMIN_COLLECTION).document(userId)
            adminRef.get()
                .addOnSuccessListener {
                    val isAdmin = it.exists()
                    viewModelScope.launch {
                        _isAdmin.emit(Resource.Success(isAdmin))
                    }
                }
                .addOnFailureListener {
                    viewModelScope.launch {
                        _isAdmin.emit(Resource.Error(it.message.toString()))
                    }
                }
        } else {
            viewModelScope.launch {
                _isAdmin.emit(Resource.Success(false))
            }
        }
    }
}