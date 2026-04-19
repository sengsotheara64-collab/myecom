package com.example.myecomapp.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myecomapp.data.Product
import com.example.myecomapp.utils.CloudinaryManager
import com.example.myecomapp.utils.Constants.PRODUCT_COLLECTION
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.validations.NewProductFieldsState
import com.example.myecomapp.utils.validations.NewProductValidation
import com.example.myecomapp.utils.validations.validateNewProductCategory
import com.example.myecomapp.utils.validations.validateNewProductImages
import com.example.myecomapp.utils.validations.validateNewProductName
import com.example.myecomapp.utils.validations.validateNewProductOfferPercentage
import com.example.myecomapp.utils.validations.validateNewProductPrice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewProductViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private val _addProduct = MutableStateFlow<Resource<Product>>(Resource.Unspecified())
    val addProduct: Flow<Resource<Product>> = _addProduct

    private val _validation = Channel<NewProductFieldsState>()
    val validation = _validation.receiveAsFlow()

    fun saveNewProduct(
        id: String,
        name: String,
        category: String,
        price: String,
        offerPercentage: Float? = null,
        description: String? = null,
        colors: List<Int>? = null,
        sizes: List<String>? = null,
        special: Boolean = false,
        imagesByteArray: List<ByteArray>?
    ) {
        if (checkValidation(name, category, price, offerPercentage, imagesByteArray)) {

            viewModelScope.launch {
                _addProduct.emit(Resource.Loading())
            }

            imagesByteArray?.let {
                uploadImagesAndSaveProduct(
                    id, name, category, price.toFloat(),
                    offerPercentage, description, colors, sizes, special, it
                )
            }

        } else {
            viewModelScope.launch {
                _validation.send(
                    NewProductFieldsState(
                        validateNewProductName(name),
                        validateNewProductCategory(category),
                        validateNewProductPrice(price),
                        validateNewProductOfferPercentage(offerPercentage),
                        validateNewProductImages(imagesByteArray)
                    )
                )
            }
        }
    }

    private fun uploadImagesAndSaveProduct(
        id: String,
        name: String,
        category: String,
        price: Float,
        offerPercentage: Float? = null,
        description: String? = null,
        colors: List<Int>? = null,
        sizes: List<String>? = null,
        special: Boolean = false,
        imagesByteArray: List<ByteArray>
    ) {
        val images = mutableListOf<String>()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val deferredImages = imagesByteArray.map { bytes ->
                        async {
                            CloudinaryManager.uploadImage(bytes, "products")
                        }
                    }

                    images.addAll(deferredImages.awaitAll())

                } catch (e: Exception) {
                    _addProduct.value = Resource.Error("Upload failed: ${e.message}")
                    return@withContext
                }

                val product = Product(
                    id = id,
                    name = name,
                    category = category,
                    price = price,
                    offerPercentage = offerPercentage,
                    description = description,
                    colors = colors,
                    sizes = sizes,
                    images = images,
                    special = special
                )

                firestore.collection(PRODUCT_COLLECTION)
                    .add(product)
                    .addOnSuccessListener {
                        _addProduct.value = Resource.Success(product)
                    }
                    .addOnFailureListener {
                        _addProduct.value = Resource.Error(it.message.toString())
                    }
            }
        }
    }

    private fun checkValidation(
        name: String,
        category: String,
        price: String,
        offerPercentage: Float? = null,
        imagesByteArray: List<ByteArray>?
    ): Boolean {
        val nameValidation = validateNewProductName(name)
        val categoryValidation = validateNewProductCategory(category)
        val priceValidation = validateNewProductPrice(price)
        val offerPercentageValidation = validateNewProductOfferPercentage(offerPercentage)
        val imagesValidation = validateNewProductImages(imagesByteArray)

        return nameValidation is NewProductValidation.Valid &&
                categoryValidation is NewProductValidation.Valid &&
                priceValidation is NewProductValidation.Valid &&
                offerPercentageValidation is NewProductValidation.Valid &&
                imagesValidation is NewProductValidation.Valid
    }
}