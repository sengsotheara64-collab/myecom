package com.example.myecomapp.firebase

import android.util.Log
import com.example.myecomapp.data.CartProduct
import com.example.myecomapp.utils.Constants.CART_COLLECTION
import com.example.myecomapp.utils.Constants.USER_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseCommon(
    private val firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth
) {

    private val cartCollection = firestore.collection(USER_COLLECTION)
        .document(firebaseAuth.currentUser?.uid ?: throw Exception("User not logged in"))
        .collection(CART_COLLECTION)

    fun addProductToCart(
        cartProduct: CartProduct,
        onResult: (CartProduct?, Exception?) -> Unit
    ) {
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

        if (user == null) {
            onResult(null, Exception("User not authenticated"))
            return
        }

        user.getIdToken(true).addOnSuccessListener {

            val cartCollection = firestore.collection(USER_COLLECTION)
                .document(user.uid)
                .collection(CART_COLLECTION)

            cartCollection.document()
                .set(cartProduct)
                .addOnSuccessListener {
                    Log.d("CART", "SUCCESS ADD")
                    onResult(cartProduct, null)
                }
                .addOnFailureListener {
                    Log.e("CART", "FAILED ADD", it)
                    onResult(null, it)
                }

        }.addOnFailureListener {
            onResult(null, it)
        }
    }

    fun increaseQuantity(
        documentId: String,
        onResult: (String?, Exception?) -> Unit
    ) {
        firestore.runTransaction { transaction ->
            val documentRef = cartCollection.document(documentId)
            val productObject = transaction.get(documentRef).toObject(CartProduct::class.java)
            productObject?.let { cartProduct ->
                val newQuantity = cartProduct.quantity + 1
                val newProductObject = cartProduct.copy(quantity = newQuantity)
                transaction.set(documentRef, newProductObject)
            }
        }
            .addOnSuccessListener {
                onResult(documentId, null)
            }
            .addOnFailureListener {
                onResult(null, it)
            }
    }

    fun decreaseQuantity(
        documentId: String,
        onResult: (String?, Exception?) -> Unit
    ) {
        firestore.runTransaction { transaction ->
            val documentRef = cartCollection.document(documentId)
            val productObject = transaction.get(documentRef).toObject(CartProduct::class.java)
            productObject?.let { cartProduct ->
                val newQuantity = cartProduct.quantity - 1
                val newProductObject = cartProduct.copy(quantity = newQuantity)
                transaction.set(documentRef, newProductObject)
            }
        }
            .addOnSuccessListener {
                onResult(documentId, null)
            }
            .addOnFailureListener {
                onResult(null, it)
            }
    }

    enum class QuantityChanging {
        INCREASE,
        DECREASE
    }
}