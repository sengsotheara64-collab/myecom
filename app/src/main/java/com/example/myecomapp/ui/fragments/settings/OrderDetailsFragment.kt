package com.example.myecomapp.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myecomapp.R
import com.example.myecomapp.data.Order
import com.example.myecomapp.data.OrderStatus
import com.example.myecomapp.data.getOrderStatus
import com.example.myecomapp.databinding.FragmentOrderDetailsBinding
import com.example.myecomapp.ui.adapters.BillingProductAdapter
import com.example.myecomapp.ui.viewmodels.OrderViewModel
import com.example.myecomapp.ui.viewmodels.UserAccountViewModel
import com.example.myecomapp.utils.HorizontalItemDecoration
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.getPriceCalculatingOfferAsCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderDetailsFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailsBinding
    private val billingProductAdapter by lazy { BillingProductAdapter() }
    private val args by navArgs<OrderDetailsFragmentArgs>()
    private val orderViewModel by viewModels<OrderViewModel>()
    private val userAccountViewModel by viewModels<UserAccountViewModel>()
    private var order = Order()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        order = args.order
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOrderRv()

        billingProductAdapter.onProductClick = {
            val bundle = Bundle().apply {
                putParcelable("product", it)
            }
            findNavController().navigate(
                R.id.action_orderDetailsFragment_to_productDetailsFragment,
                bundle
            )
        }

        binding.apply {

            val id = "#${order.orderId}"
            tvOrderId.text = id

            val orderStates = listOf(
                OrderStatus.Pending.status,
                OrderStatus.Confirmed.status,
                OrderStatus.Shipped.status,
                OrderStatus.Delivered.status
            )

            // 🔥 Determine current step
            val currentStep = when (getOrderStatus(order.orderStatus)) {
                is OrderStatus.Pending -> 0
                is OrderStatus.Confirmed -> 1
                is OrderStatus.Shipped -> 2
                is OrderStatus.Delivered -> 3
                else -> 0
            }

            // 🔥 Update UI step
            fun updateStepUI(step: Int) {
                val activeColor = ContextCompat.getColor(requireContext(), R.color.g_green)
                val inactiveColor = ContextCompat.getColor(requireContext(), R.color.g_gray700)

                val steps = listOf(step1, step2, step3, step4)

                steps.forEachIndexed { index, textView ->
                    textView.setTextColor(if (index <= step) activeColor else inactiveColor)
                }
            }

            updateStepUI(currentStep)

            //  Address info
            tvFullName.text = order.address.label
            val address =
                "${order.address.address}, ${order.address.street}, ${order.address.city}, ${order.address.state}"
            tvAddress.text = address
            tvPhoneNumber.text = order.address.phone
            tvTotalPrice.text = getPriceCalculatingOfferAsCurrency(order.totalPrice, 0f)

            // Handle admin click
            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    userAccountViewModel.isAdmin.collect { result ->

                        val steps = listOf(step1, step2, step3, step4)

                        when (result) {

                            is Resource.Loading -> {
                                steps.forEach { it.setOnClickListener(null) }
                            }

                            is Resource.Success -> {
                                if (result.data == true) {

                                    steps.forEachIndexed { index, view ->
                                        view.setOnClickListener {

                                            orderViewModel.updateOrderStatus(
                                                order.copy(
                                                    orderStatus = orderStates[index]
                                                )
                                            )

                                            updateStepUI(index)
                                        }
                                    }

                                } else {
                                    steps.forEach { it.setOnClickListener(null) }
                                }
                            }

                            is Resource.Error -> {
                                steps.forEach { it.setOnClickListener(null) }
                            }

                            else -> Unit
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    orderViewModel.order.collect {
                        when (it) {
                            is Resource.Loading -> {
                                progressbarOrderDetails.visibility = View.VISIBLE
                            }

                            is Resource.Success -> {
                                progressbarOrderDetails.visibility = View.INVISIBLE
                            }

                            is Resource.Error -> {
                                progressbarOrderDetails.visibility = View.INVISIBLE
                                Toast.makeText(
                                    requireContext(),
                                    it.message,
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
        }

        billingProductAdapter.differ.submitList(order.products)
    }

    private fun setupOrderRv() {
        binding.rvProducts.apply {
            adapter = billingProductAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            addItemDecoration(HorizontalItemDecoration())
        }
    }
}