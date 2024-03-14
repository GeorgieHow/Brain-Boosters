package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth


class LoginPageActivity : AppCompatActivity() {

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

        val saveLoginCheckBox = findViewById<CheckBox>(R.id.saveLoginCheckBox)

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

            val sharedPreferences = getSharedPreferences("com.example.brainboosters", MODE_PRIVATE)
            if (saveLoginCheckBox.isChecked) {
                sharedPreferences.edit().apply {
                    putString("email", emailEntered.text.toString())
                    putString("password", passwordEntered.text.toString())
                    putBoolean("saveLogin", true)
                    apply()
                }
            } else {
                // If the box is not checked, clear saved credentials and save the state of the checkbox as false
                sharedPreferences.edit().apply {
                    remove("email") // Remove the saved email
                    remove("password") // Remove the saved password
                    putBoolean("saveLogin", false) // Save the unchecked state
                    apply()
                }
            }


        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }

        val sharedPreferences = getSharedPreferences("com.example.brainboosters", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        val saveLogin = sharedPreferences.getBoolean("saveLogin", false)

        findViewById<EditText>(R.id.emailText).setText(email)
        findViewById<EditText>(R.id.passwordText).setText(password)
        saveLoginCheckBox.isChecked = saveLogin
    }
}



