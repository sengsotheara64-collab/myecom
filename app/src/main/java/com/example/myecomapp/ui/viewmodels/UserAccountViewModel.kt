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
    private val storage: StorageReference,
    app: Application
) : AndroidViewModel(app) {

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
                saveUserInformation(user, true)
            } else {
                saveUserInformationWithNewImage(user, imageUri)
            }
        } else {
            viewModelScope.launch {
                _updateInfo.emit(Resource.Error("Email is not valid"))
            }
        }
    }

    private fun saveUserInformation(user: User, retrieveOldImage: Boolean) {
        firestore.runTransaction { transaction ->
            val documentRef = firestore.collection(USER_COLLECTION)
                .document(firebaseAuth.uid!!)

            if (retrieveOldImage) {
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
            } else {
                transaction.set(documentRef, user)
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
            try {
                _updateInfo.emit(Resource.Loading())

                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(
                        getApplication<MyApplication>().contentResolver,
                        imageUri
                    )
                } else {
                    val source = ImageDecoder.createSource(
                        getApplication<MyApplication>().contentResolver,
                        imageUri
                    )
                    ImageDecoder.decodeBitmap(source)
                }

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

                val imageBytes = stream.toByteArray()

                Log.d("UPLOAD", "bytes = ${imageBytes.size}")

                val imageRef = Firebase.storage.reference.child(
                    "profileImage/${FirebaseAuth.getInstance().uid}/${UUID.randomUUID()}.jpg"
                )

                val uploadTask = imageRef.putBytes(imageBytes).await()

                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                saveUserInformation(user.copy(imagePath = downloadUrl), false)

                _updateInfo.emit(Resource.Success(user))

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", e.message.toString())
                _updateInfo.emit(Resource.Error(e.message ?: "Upload failed"))
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