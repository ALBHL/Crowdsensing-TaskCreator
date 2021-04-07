package com.example.taskcreator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button.setOnClickListener {
            performRegister()
        }

        already_have_account_textView.setOnClickListener {
            Log.d("RegisterActivity", "Try to show login activity")

            // launch the login page
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performRegister() {
        val email = email_edittext_reg.text.toString()
        val password = password_edittext_reg.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in email/password", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("RegisterActivity", "Email is: " + email)
        Log.d("RegisterActivity", "Password is: $password")

        // Firebase Authentication to create a user with email and password
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                // else successful
                Log.d(
                    "RegisterActivity",
                    "Successfully create user with uid: ${it.result?.user?.uid}"
                )
//                uploadDataToFirebaseStorage()
//                saveUserToFirebaseDatabase("https://google.com")
//                readTestFirebase()
            }
            .addOnFailureListener {
                Log.d("RegisterActivity", "Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

//    private fun uploadDataToFirebaseStorage() {
//    }

//    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
//        val uid = FirebaseAuth.getInstance().uid ?: ""
//        d("RegisterActivity","UUID: $uid")
//        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
//        d("RegisterActivity","Ref: $ref")
//
//
////        val user = User(uid, username_edittext_reg.text.toString(), profileImageUrl)
//
//        ref.setValue("hello")
//            .addOnCanceledListener {
//                d("RegisterActivity", "cancelled")
//            }
//            .addOnSuccessListener {
//                d("RegisterActivity", "Finally we saved the user to Firebase Database")
//            }
//            .addOnFailureListener {
//                d("RegisterActivity", "Failed to set value to database: ${it.message}")
//            }
//    }

//    private fun readTestFirebase() {
//        val database = Firebase.database
//        val myRef = database.reference
//        Log.d("RegisterActivity", "$myRef")
//        myRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                val value = dataSnapshot.getValue<String>()
//                Log.d("RegisterActivity", "Value is: $value")
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Failed to read value
//                Log.w("RegisterActivity", "Failed to read value.", error.toException())
//            }
//        })
//    }
}