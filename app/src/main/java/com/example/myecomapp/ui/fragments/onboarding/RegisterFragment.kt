package com.example.myecomapp.ui.fragments.onboarding

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
import com.example.myecomapp.R
import com.example.myecomapp.data.User
import com.example.myecomapp.databinding.FragmentRegisterBinding
import com.example.myecomapp.ui.viewmodels.RegisterViewModel
import com.example.myecomapp.utils.validations.LoginRegisterValidation
import com.example.myecomapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel by viewModels<RegisterViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            facebookRegister.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            googleRegister.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            tvLogIn.setOnClickListener {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }

            buttonRegister.setOnClickListener {
                viewModel.createUserWithEmailAndPassword(
                    User(
                        firstName = edFirstNameRegister.text.toString().trim(),
                        lastName = edLastNameRegister.text.toString().trim(),
                        email = edEmailRegister.text.toString().trim()
                    ),
                    edPasswordRegister.text.toString()
                )
            }

            // Register Flow
            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.register.collect { result ->

                        when (result) {
                            is Resource.Loading -> {
                                setLoadingState(true)
                            }

                            is Resource.Success -> {
                                setLoadingState(false)

                                val bundle = Bundle().apply {
                                    putString("email", result.data?.email)
                                }

                                findNavController().navigate(
                                    R.id.action_registerFragment_to_loginFragment,
                                    bundle
                                )
                            }

                            is Resource.Error -> {
                                setLoadingState(false)

                                Toast.makeText(
                                    requireContext(),
                                    result.message.toString(),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> Unit
                        }
                    }
                }
            }

            //  Validation Flow
            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.validation.collect { validation ->

                        if (validation.email is LoginRegisterValidation.Invalid) {
                            edEmailRegister.apply {
                                requestFocus()
                                error = validation.email.error
                            }
                        }

                        if (validation.password is LoginRegisterValidation.Invalid) {
                            edPasswordRegister.apply {
                                requestFocus()
                                error = validation.password.error
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.apply {
            buttonRegister.isEnabled = !isLoading
            buttonRegister.text = if (isLoading) {
                getString(R.string.loading)
            } else {
                getString(R.string.register)
            }
        }
    }
}