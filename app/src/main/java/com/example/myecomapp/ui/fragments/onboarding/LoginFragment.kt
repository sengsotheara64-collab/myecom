package com.example.myecomapp.ui.fragments.onboarding

import android.content.Intent
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
import com.example.myecomapp.databinding.FragmentLoginBinding
import com.example.myecomapp.ui.activities.ShoppingActivity
import com.example.myecomapp.ui.dialogs.showResetPasswordDialog
import com.example.myecomapp.ui.dialogs.showVerifyEmailDialog
import com.example.myecomapp.ui.viewmodels.LoginViewModel
import com.example.myecomapp.utils.Resource
import com.example.myecomapp.utils.getBottomNavView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("email")?.let { email ->
            showVerifyEmailDialog(email) {
                viewModel.resendVerificationMail()
            }
        }

        binding.apply {
            tvRegister.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }

            buttonLogin.setOnClickListener {
                val email = edEmailLogin.text.toString().trim()
                val password = edPasswordLogin.text.toString()

                viewModel.login(email, password)
            }

            tvForgotPasswordLogin.setOnClickListener {
                showResetPasswordDialog {
                    viewModel.resetPassword(email = it)
                }
            }

            facebookLogin.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            googleLogin.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.resetPassword.collect {
                        when (it) {
                            is Resource.Loading -> {
                            }

                            is Resource.Success -> {
                                val snackbar = Snackbar.make(
                                    requireView(),
                                    it.data.toString(),
                                    Snackbar.LENGTH_SHORT
                                )

                                snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE

                                val bottomNav = getBottomNavView()

                                if (bottomNav != null) {
                                    snackbar.setAnchorView(bottomNav)
                                }

                                snackbar.show()
                            }

                            is Resource.Error -> {
                                val snackbar = Snackbar.make(
                                    requireView(),
                                    "Error: ${it.message.toString()}",
                                    Snackbar.LENGTH_SHORT
                                )
                                snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE
                                snackbar.show()
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

                    viewModel.login.collect { result ->

                        when (result) {

                            is Resource.Loading -> {
                                binding.buttonLogin.isEnabled = false
                                binding.progressLogin.visibility = View.VISIBLE
                                binding.buttonLogin.text = ""
                            }

                            is Resource.Success -> {
                                binding.buttonLogin.isEnabled = true
                                binding.progressLogin.visibility = View.GONE
                                binding.buttonLogin.text = getString(R.string.login)

                                Intent(
                                    requireActivity(),
                                    ShoppingActivity::class.java
                                ).also { intent ->
                                    intent.addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                    )
                                    startActivity(intent)
                                }
                            }

                            is Resource.Error -> {
                                binding.buttonLogin.isEnabled = true
                                binding.progressLogin.visibility = View.GONE
                                binding.buttonLogin.text = getString(R.string.login)

                                val message = result.message.toString()
                                val messageLower = message.lowercase(Locale.ROOT)

                                if (messageLower.contains("email") &&
                                    messageLower.contains("password")
                                ) {
                                    binding.edEmailLogin.requestFocus()
                                    binding.edPasswordLogin.requestFocus()

                                    binding.edEmailLogin.error = message
                                    binding.edPasswordLogin.error = message
                                } else {

                                    var errorShown = false

                                    if (messageLower.contains("email")) {
                                        binding.edEmailLogin.requestFocus()
                                        binding.edEmailLogin.error = message
                                        errorShown = true
                                    }

                                    if (messageLower.contains("password")) {
                                        binding.edPasswordLogin.requestFocus()
                                        binding.edPasswordLogin.error = message
                                        errorShown = true
                                    }

                                    if (!errorShown) {
                                        Toast.makeText(
                                            requireContext(),
                                            message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}