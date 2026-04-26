package com.example.myecomapp.ui.fragments.settings

import android.content.Intent
import android.net.Uri
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
import com.bumptech.glide.Glide
import com.example.myecomapp.R
import com.example.myecomapp.data.User
import com.example.myecomapp.databinding.FragmentProfileBinding
import com.example.myecomapp.ui.activities.LoginRegisterActivity
import com.example.myecomapp.ui.dialogs.showResetPasswordDialog
import com.example.myecomapp.ui.viewmodels.LoginViewModel
import com.example.myecomapp.ui.viewmodels.UserAccountViewModel
import com.example.myecomapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val userAccountViewModel by viewModels<UserAccountViewModel>()
    private val loginViewModel by viewModels<LoginViewModel>()
    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            imageProfileClose.setOnClickListener {
                findNavController().navigateUp()
            }

            buttonEditProfile.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("user", user)
                }
                findNavController().navigate(
                    R.id.action_profileFragment_to_userAccountFragment,
                    bundle
                )
            }

            layoutMyOrders.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_orderListFragment)
            }

            layoutAllOrders.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_allOrderListFragment)
            }

            layoutBillingAddress.setOnClickListener {
                val action = ProfileFragmentDirections.actionProfileFragmentToBillingFragment(
                    0f,
                    emptyArray(),
                    false
                )
                findNavController().navigate(action)
            }

            layoutAddNewProduct.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_newProductFragment)
            }

            layoutChangePassword.setOnClickListener {
                showResetPasswordDialog { inputEmail ->

                    val currentEmail = FirebaseAuth.getInstance().currentUser?.email

                    if (currentEmail.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT)
                            .show()
                        return@showResetPasswordDialog
                    }

                    if (inputEmail != currentEmail) {
                        Toast.makeText(
                            requireContext(),
                            "Email does not match your account",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        loginViewModel.resetPassword(inputEmail, requireContext())
                    }
                }

            }

            layoutInformation.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://github.com/sengsotheara64-collab/myecom.git".toUri()
                }
                startActivity(intent)
            }

            layoutLogout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                startActivity(
                    Intent(
                        requireContext(),
                        LoginRegisterActivity::class.java
                    )
                )
                requireActivity().finishAffinity()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    userAccountViewModel.user.collect {
                        when (it) {
                            is Resource.Loading -> {
                                progressbarProfile.visibility = View.VISIBLE
                            }

                            is Resource.Success -> {
                                user = it.data
                                progressbarProfile.visibility = View.INVISIBLE
                                Glide.with(this@ProfileFragment)
                                    .load(user?.imagePath)
                                    .error(R.drawable.no_user_image)
                                    .into(imageUser)
                                val fullName = "${user?.firstName} ${user?.lastName}"
                                tvUserName.text = fullName
                                tvEmail.text = user?.email
                            }

                            is Resource.Error -> {
                                progressbarProfile.visibility = View.INVISIBLE
                            }

                            else -> {
                                Unit
                            }
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    userAccountViewModel.isAdmin.collect {
                        when (it) {
                            is Resource.Loading -> {
                            }

                            is Resource.Success -> {
                                if (it.data == true) {
                                    layoutAllOrders.visibility = View.VISIBLE
                                    layoutAddNewProduct.visibility = View.VISIBLE
                                } else {
                                    layoutAllOrders.visibility = View.GONE
                                    layoutAddNewProduct.visibility = View.GONE
                                }
                            }

                            is Resource.Error -> {
                                layoutAllOrders.visibility = View.GONE
                                layoutAddNewProduct.visibility = View.GONE
                            }

                            else -> {
                                Unit
                            }
                        }
                    }
                }
            }
        }
    }
}