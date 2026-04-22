package com.example.myecomapp.ui.dialogs

import android.annotation.SuppressLint
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.myecomapp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

fun Fragment.showVerifyEmailDialog(
    email: String,
    onResendClick: (String) -> Unit
) {
    val dialog = BottomSheetDialog(requireContext(), R.style.DialogStyle)
    val view = layoutInflater.inflate(R.layout.dialog_verify_email, null)
    dialog.setContentView(view)
    dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    dialog.show()

    val buttonResend = view.findViewById<AppCompatButton>(R.id.buttonSendVerifyEmail)
    val buttonClose = view.findViewById<AppCompatButton>(R.id.buttonCancelVerifyEmail)

    buttonResend.setOnClickListener {
        if (email.isNotEmpty()) {
            onResendClick(email)
            dialog.dismiss()
        } else {
            Toast.makeText(context, "Email not available", Toast.LENGTH_SHORT).show()
        }
    }

    buttonClose.setOnClickListener {
        dialog.dismiss()
    }
}