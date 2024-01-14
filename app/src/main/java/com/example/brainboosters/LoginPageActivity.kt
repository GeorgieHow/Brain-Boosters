package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth


class LoginPageActivity : ComponentActivity() {

    //Establishes firebase authentication
    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        val rootLayout = findViewById<ConstraintLayout>(R.id.login_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable

        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        val enterButton = findViewById<Button>(R.id.enterButton)

        //Checks if user has an account and entered details are correct
        enterButton.setOnClickListener{
                view ->

            //Gets the ids for the fields and buttons
            val emailEntered = findViewById<EditText>(R.id.emailText)
            val passwordEntered = findViewById<EditText>(R.id.passwordText)

            val isEmailEmpty = emailEntered.text.toString()
            val isPasswordEmpty = passwordEntered.text.toString()

            if(isEmailEmpty.isNotEmpty() && isPasswordEmpty.isNotEmpty()) {

                mAuth.signInWithEmailAndPassword(
                    emailEntered.text.toString(),
                    passwordEntered.text.toString()
                )
                    .addOnCompleteListener(
                        this
                    ) { task ->
                        if (task.isSuccessful) {
                            //remove done and make them go to home page LOL.
                            Toast.makeText(this, "Account does exist",
                                Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomePageActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Account does not exist",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            else{
                Toast.makeText(this, "Don't leave fields empty", Toast.LENGTH_SHORT)
                    .show()
            }

        }


    }
}



