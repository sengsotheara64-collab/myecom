package com.example.myecomapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myecomapp.data.Order
import com.example.myecomapp.utils.Constants.ORDER_COLLECTION
import com.example.myecomapp.utils.Constants.USER_COLLECTION
import com.example.myecomapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _myOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Unspecified())
    val myOrders = _myOrders.asStateFlow()

    private val _allOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Unspecified())
    val allOrders = _allOrders.asStateFlow()

    init {
        getMyOrders()
        getAllOrders()
    }

    private fun getMyOrders() {
        val userId = firebaseAuth.currentUser?.uid
            ?: run {
                viewModelScope.launch {
                    _myOrders.emit(Resource.Error("User not logged in"))
                }
                return
            }

        viewModelScope.launch {
            _myOrders.emit(Resource.Loading())
        }

        firestore.collection(USER_COLLECTION)
            .document(userId)
            .collection(ORDER_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                viewModelScope.launch {
                    if (error != null) {
                        _myOrders.emit(Resource.Error(error.message ?: "Error"))
                    } else {
                        val orders = value?.toObjects(Order::class.java) ?: emptyList()
                        _myOrders.emit(Resource.Success(orders))
                    }
                }
            }
    }

    private fun getAllOrders() {
        viewModelScope.launch {
            _allOrders.emit(Resource.Loading())
        }

        firestore.collection(ORDER_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                viewModelScope.launch {
                    if (error != null) {
                        _allOrders.emit(Resource.Error(error.message ?: "Error"))
                    } else {
                        val orders = value?.toObjects(Order::class.java) ?: emptyList()
                        _allOrders.emit(Resource.Success(orders))
                    }
                }
            }
    }
}