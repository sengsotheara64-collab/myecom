package com.example.myecomapp.ui.fragments.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myecomapp.R
import com.example.myecomapp.data.Address
import com.example.myecomapp.data.CartProduct
import com.example.myecomapp.data.Order
import com.example.myecomapp.data.OrderStatus
import com.example.myecomapp.databinding.FragmentBillingBinding
import com.example.myecomapp.ui.adapters.AddressAdapter
import com.example.myecomapp.ui.adapters.BillingProductAdapter
import com.example.myecomapp.ui.dialogs.showAlertDialog
import com.example.myecomapp.ui.viewmodels.BillingViewModel
import com.example.myecomapp.ui.viewmodels.OrderViewModel
import com.example.myecomapp.utils.HorizontalItemDecoration
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.getBottomNavView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BillingFragment : Fragment() {

    private lateinit var binding: FragmentBillingBinding
    private val addressAdapter by lazy { AddressAdapter() }
    private val billingProductAdapter by lazy { BillingProductAdapter() }
    private val billingViewModel by viewModels<BillingViewModel>()
    private val args by navArgs<BillingFragmentArgs>()
    private var products = emptyList<CartProduct>()
    private var totalPrice = 0f
    private var isPayment = true
    private var selectedAddress: Address? = null
    private val orderViewModel by viewModels<OrderViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        products = args.product.toList()
        totalPrice = args.totalPrice
        isPayment = args.isPayment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBillingProductsRv()
        setupAddressRv()

        binding.apply {
            if (!isPayment) {
                middleLine.visibility = View.GONE
                rvProducts.visibility = View.GONE
                bottomLine.visibility = View.GONE
                totalBoxContainer.visibility = View.GONE
                buttonPlaceOrder.visibility = View.GONE
            }

            val price = "$${totalPrice}"
            tvTotalPrice.text = price

            addressAdapter.onClick = {
                selectedAddress = it

                if (!isPayment) {
                    val bundle = Bundle().apply {
                        putParcelable("address", selectedAddress)
                    }
                    findNavController().navigate(
                        R.id.action_billingFragment_to_addressFragment,
                        bundle
                    )
                    addressAdapter.selectedAddress = -1
                    addressAdapter.notifyItemChanged(addressAdapter.selectedAddress)
                }
            }

            buttonPlaceOrder.setOnClickListener {
                if (selectedAddress == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please select an address",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                showOrderConfirmationDialog()
            }

            imageAddAddress.setOnClickListener {
                findNavController().navigate(R.id.action_billingFragment_to_addressFragment)
            }

            imageCloseBilling.setOnClickListener {
                findNavController().navigateUp()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    billingViewModel.address.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                progressbarBilling.visibility = View.VISIBLE
                            }

                            is Resource.Success -> {
                                progressbarBilling.visibility = View.INVISIBLE
                                addressAdapter.differ.submitList(it.data)
                            }

                            is Resource.Error -> {
                                progressbarBilling.visibility = View.INVISIBLE
                                Toast.makeText(
                                    requireContext(),
                                    it.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                Unit
                            }
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    orderViewModel.order.collectLatest { result ->

                        when (result) {

                            is Resource.Loading -> {
                                binding.buttonPlaceOrder.isEnabled = false
                                binding.progressPlaceOrder.visibility = View.VISIBLE
                                binding.buttonPlaceOrder.text = ""
                            }

                            is Resource.Success -> {
                                binding.buttonPlaceOrder.isEnabled = true
                                binding.progressPlaceOrder.visibility = View.GONE
                                binding.buttonPlaceOrder.text = getString(R.string.place_order)

                                findNavController().navigateUp()

                                val snackbar = Snackbar.make(
                                    requireView(),
                                    "Your order has been placed",
                                    Snackbar.LENGTH_LONG
                                )

                                snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE
                                snackbar.setAnchorView(getBottomNavView())
                                snackbar.show()
                            }

                            is Resource.Error -> {
                                binding.buttonPlaceOrder.isEnabled = true
                                binding.progressPlaceOrder.visibility = View.GONE
                                binding.buttonPlaceOrder.text = getString(R.string.place_order)

                                Toast.makeText(
                                    requireContext(),
                                    result.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        billingProductAdapter.differ.submitList(products)
    }

    private fun showOrderConfirmationDialog() {
        var address =
            "${selectedAddress!!.address}, ${selectedAddress!!.street}, ${selectedAddress!!.city}, ${selectedAddress!!.state}"
        if (address[address.length - 1] != '.') {
            address += "."
        }
        showAlertDialog(
            requireContext(),
            "Confirm order?",
            "Your order will be delivered to $address",
            "Yes",
            onPositiveClick = {
                orderViewModel.placeOrder(
                    Order(
                        OrderStatus.Pending.status,
                        totalPrice,
                        products,
                        selectedAddress!!
                    )
                )
            },
            "Cancel",
            onNegativeClick = {}
        )
    }

    private fun setupBillingProductsRv() {
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.HORIZONTAL,
                false
            )
            adapter = billingProductAdapter
            addItemDecoration(HorizontalItemDecoration())
        }
    }

    private fun setupAddressRv() {
        binding.rvAddress.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.HORIZONTAL,
                false
            )
            adapter = addressAdapter
            addItemDecoration(HorizontalItemDecoration())
        }
    }
}