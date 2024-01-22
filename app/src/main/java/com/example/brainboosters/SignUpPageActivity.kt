package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth

class SignUpPageActivity : AppCompatActivity() {

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        val rootLayout = findViewById<ConstraintLayout>(R.id.signup_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable

        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        val signUpButton = findViewById<Button>(R.id.signUpButton)

        signUpButton.setOnClickListener{
                view ->

            val emailEntered = findViewById<EditText>(R.id.emailText)
            val passwordEntered = findViewById<EditText>(R.id.passwordText)

            val passwordRetyped = findViewById<EditText>(R.id.passwordRetypeText)

            val emailString = emailEntered.text.toString()
            val passwordString = passwordEntered.text.toString()

            val passwordRetypedString = passwordRetyped.text.toString()

            if(emailString.isNotEmpty() && passwordString.isNotEmpty()){
                if(passwordString == passwordRetypedString){

                    mAuth.createUserWithEmailAndPassword(emailString, passwordString)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Account Created",
                                    Toast.LENGTH_SHORT).show()
                            }
                            else{
                                Toast.makeText(this, "Account Unsuccessful ",
                                    Toast.LENGTH_SHORT).show()
                            }

                        }
                }
            }
            else{
                Toast.makeText(this, "Don't leave fields empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}