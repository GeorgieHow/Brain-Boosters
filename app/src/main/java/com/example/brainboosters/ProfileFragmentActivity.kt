package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val profileImageView: ShapeableImageView = view.findViewById(R.id.profile_image_view)

        mAuth.currentUser?.let { fetchUserDetailsFromFirebase(it.uid) }

        Glide.with(this)
            .load(ContextCompat.getDrawable(requireContext(), R.drawable.profile_picture))
            .into(profileImageView)

        val editProfileButton: Button = view.findViewById(R.id.edit_profile_button)
        val cancelEditProfileButton: Button = view.findViewById(R.id.cancel_edit_profile_button)
        val confirmEditProfileButton: Button = view.findViewById(R.id.confirm_edit_profile_button)

        cancelEditProfileButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.errorColor))

        val fullNameText: TextView = view.findViewById(R.id.full_name_text)
        val ageText: TextView = view.findViewById(R.id.age_text)
        val dementiaTypeText: TextView = view.findViewById(R.id.dementia_type_text)
        val dementiaLevelText: TextView = view.findViewById(R.id.dementia_level_text)

        val editFullNameEditText: EditText = view.findViewById(R.id.full_name_edit_text)
        val editAgeEditText: EditText = view.findViewById(R.id.age_edit_text)
        val editDementiaTypeEditText: EditText = view.findViewById(R.id.dementia_type_edit_text)
        val editDementiaLevelSpinner: Spinner = view.findViewById(R.id.dementia_level_spinner)

        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.dementia_levels, // The array resource containing your items
            R.layout.spinner_item // Custom layout for items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        editDementiaLevelSpinner.adapter = adapter

        editProfileButton.setOnClickListener {
            editProfileButton.visibility = View.GONE
            cancelEditProfileButton.visibility = View.VISIBLE
            confirmEditProfileButton.visibility = View.VISIBLE

            fullNameText.visibility = View.GONE
            ageText.visibility = View.GONE
            dementiaTypeText.visibility = View.GONE
            dementiaLevelText.visibility = View.GONE

            editFullNameEditText.visibility = View.VISIBLE
            editAgeEditText.visibility = View.VISIBLE
            editDementiaTypeEditText.visibility = View.VISIBLE
            editDementiaLevelSpinner.visibility = View.VISIBLE
        }

        cancelEditProfileButton.setOnClickListener {
            editProfileButton.visibility = View.VISIBLE
            cancelEditProfileButton.visibility = View.GONE
            confirmEditProfileButton.visibility = View.GONE

            fullNameText.visibility = View.VISIBLE
            ageText.visibility = View.VISIBLE
            dementiaTypeText.visibility = View.VISIBLE
            dementiaLevelText.visibility = View.VISIBLE

            editFullNameEditText.visibility = View.GONE
            editAgeEditText.visibility = View.GONE
            editDementiaTypeEditText.visibility = View.GONE
            editDementiaLevelSpinner.visibility = View.GONE
        }

        confirmEditProfileButton.setOnClickListener {
            val fullName = editFullNameEditText.text.toString()
            val age = editAgeEditText.text.toString().toIntOrNull() // Convert to Integer
            val dementiaType = editDementiaTypeEditText.text.toString()
            val dementiaLevel = editDementiaLevelSpinner.selectedItem.toString()

            if (fullName.isNotBlank() && age != null && dementiaType.isNotBlank()) {
                updateUserData(fullName, age, dementiaType, dementiaLevel)
            } else {
                // Handle the case where some fields are not filled in properly.
                // Show an error message or a toast.
            }
        }
    }

    private fun fetchUserDetailsFromFirebase(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null) {
                val name = document.getString("fullName") ?: "N/A"
                val age = document.getLong("age")?.toInt()?.toString() ?: "N/A"
                val email = document.getString("email") ?: "N/A"
                val dementiaType = document.getString("dementiaType") ?: "N/A"
                val dementiaLevel = document.getString("dementiaLevel") ?: "N/A"

                val editDementiaLevelSpinner: Spinner = view?.findViewById(R.id.dementia_level_spinner) ?: return@addOnSuccessListener
                val adapter = editDementiaLevelSpinner.adapter

                val position = (0 until adapter.count).firstOrNull {
                    adapter.getItem(it).toString().equals(dementiaLevel, ignoreCase = true)
                } ?: 0

                editDementiaLevelSpinner.setSelection(position)

                view?.findViewById<TextView>(R.id.full_name_text)?.text = name
                view?.findViewById<TextView>(R.id.age_text)?.text = age.toString()
                view?.findViewById<TextView>(R.id.email_text)?.text = email
                view?.findViewById<TextView>(R.id.dementia_type_text)?.text = dementiaType
                view?.findViewById<TextView>(R.id.dementia_level_text)?.text = dementiaLevel

                view?.findViewById<EditText>(R.id.full_name_edit_text)?.setText(name)
                view?.findViewById<EditText>(R.id.age_edit_text)?.setText(age)
                view?.findViewById<EditText>(R.id.dementia_type_edit_text)?.setText(dementiaType)


            } else {
                Log.d("ProfileFragment", "No such document")
            }
        }.addOnFailureListener { exception ->
            Log.d("ProfileFragment", "get failed with ", exception)
        }
    }

    private fun updateUserData(fullName: String, age: Int, dementiaType: String, dementiaLevel: String) {
        val userId = mAuth.currentUser?.uid ?: return // Get the current user's ID

        val userUpdates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "age" to age,
            "dementiaType" to dementiaType,
            "dementiaLevel" to dementiaLevel
        )

        db.collection("users").document(userId)
            .set(userUpdates)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "DocumentSnapshot successfully updated!")
                revertUIAfterUpdate(fullName, age.toString(), dementiaType, dementiaLevel)
            }
            .addOnFailureListener { e ->
                Log.w("ProfileFragment", "Error updating document", e)
            }
    }

    private fun revertUIAfterUpdate(fullName: String, age: String, dementiaType: String, dementiaLevel: String) {
        view?.findViewById<Button>(R.id.edit_profile_button)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.cancel_edit_profile_button)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.confirm_edit_profile_button)?.visibility = View.GONE

        view?.findViewById<TextView>(R.id.full_name_text)?.apply {
            text = fullName
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.age_text)?.apply {
            text = age
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.dementia_type_text)?.apply {
            text = dementiaType
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.dementia_level_text)?.apply {
            text = dementiaLevel
            visibility = View.VISIBLE
        }

        // Hide EditTexts and Spinner
        view?.findViewById<EditText>(R.id.full_name_edit_text)?.visibility = View.GONE
        view?.findViewById<EditText>(R.id.age_edit_text)?.visibility = View.GONE
        view?.findViewById<EditText>(R.id.dementia_type_edit_text)?.visibility = View.GONE
        view?.findViewById<Spinner>(R.id.dementia_level_spinner)?.visibility = View.GONE
    }
}