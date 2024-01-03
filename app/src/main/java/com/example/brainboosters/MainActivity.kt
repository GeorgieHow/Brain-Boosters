package com.example.brainboosters

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainboosters.ui.theme.BrainBoostersTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val emailEntered = findViewById<EditText>(R.id.emailText)
        val passwordEntered = findViewById<EditText>(R.id.passwordText)
        val enterButton = findViewById<Button>(R.id.enterButton)

        enterButton.setOnClickListener{view ->
            mAuth.signInWithEmailAndPassword(
                emailEntered.text.toString(),
                passwordEntered.text.toString()
            )
                .addOnCompleteListener(
                    this
                ){task ->
                    if (task.isSuccessful){
                        //remove done and make them go to home page LOL.
                        emailEntered.setText("DONE")
                    }

                }

        }


    }
}



