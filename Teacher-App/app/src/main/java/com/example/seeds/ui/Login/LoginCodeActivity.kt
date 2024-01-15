package com.example.seeds.ui.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.seeds.MainActivity
import com.example.seeds.R
import com.example.seeds.databinding.ActivityLoginBinding
import com.example.seeds.databinding.ActivityLoginCodeBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LoginCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginCodeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.proceedBtn.setOnClickListener{
            if (validCode() && validName() && validPhone()){
                // store in shared preferences
                val sharedPref = getSharedPreferences("sharedPref", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("name", binding.editTextName.text.toString())
                editor.putString("phone", binding.editTextPhone.text.toString())
                editor.putString("code", binding.editCode.text.toString())
                editor.apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validCode(): Boolean{
        val codePattern = "1110"
        return codePattern == binding.editCode.text.toString()
    }

    private fun validName(): Boolean{
        val namePattern = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*\$"
        return Pattern.matches(namePattern, binding.editTextName.text.toString()) && binding.editTextName.text.toString().length > 2
    }

    private fun validPhone(): Boolean{
        val phonePattern = "[0-9]{10}"
        return Pattern.matches(phonePattern, binding.editTextPhone.text.toString())
    }
}