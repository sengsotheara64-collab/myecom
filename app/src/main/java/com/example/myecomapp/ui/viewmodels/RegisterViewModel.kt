package com.example.myecomapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.myecomapp.data.User
import com.example.myecomapp.utils.Constants.USER_COLLECTION
import com.example.myecomapp.utils.validations.LoginRegisterValidation
import com.example.myecomapp.utils.validations.RegisterFieldsState
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.validations.validateEmail
import com.example.myecomapp.utils.validations.validatePassword
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _register = MutableStateFlow<Resource<User>>(Resource.Unspecified())
    val register: Flow<Resource<User>> = _register

    private val _validation = Channel<RegisterFieldsState>()
    val validation = _validation.receiveAsFlow()

    fun createUserWithEmailAndPassword(user: User, password: String) {
        if (checkValidation(user, password)) {
            runBlocking {
                _register.emit(Resource.Loading())
            }

            firebaseAuth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            authResult.user?.let {
                                saveUserInfo(
                                    it.uid,
                                    user
                                )
                            }
                        }
                        ?.addOnFailureListener {
                            _register.value = Resource.Error(it.message.toString())
                        }
                }
                .addOnFailureListener {
                    _register.value = Resource.Error(it.message.toString())
                }
        } else {
            runBlocking {
                _validation.send(
                    RegisterFieldsState(
                        validateEmail(user.email),
                        validatePassword(password)
                    )
                )
            }
        }
    }

    private fun checkValidation(user: User, password: String): Boolean {
        val emailValidation = validateEmail(user.email)
        val passwordValidation = validatePassword(password)
        return emailValidation is LoginRegisterValidation.Valid && passwordValidation is LoginRegisterValidation.Valid
    }

    private fun saveUserInfo(userUid: String, user: User) {
        firestore.collection(USER_COLLECTION)
            .document(userUid)
            .set(user)
            .addOnSuccessListener {
                _register.value = Resource.Success(user)
            }
            .addOnFailureListener {
                _register.value = Resource.Error(it.message.toString())
            }
    }
}