package com.example.taskcreator

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_creat_task.*
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class CreatTaskActivity : AppCompatActivity() {
    val PROVIDER_NAME = "com.example.collector/AcronymProvider"
    val URL = "content://$PROVIDER_NAME/Inbox"
    val CONTENT_URI = Uri.parse(URL)

    val TABLEIN_NAME="Inbox"
    val COL_NAME = "name"
    val COL_AGE = "age"
    val COL_ID = "id"
    val COL_URL = "imageurl"
    val COL_PROFILE = "profileimg"
    val COL_STAGE = "current_stage"
    val COL_IMAGE_BIT = "picturetaken"

    var modelSelected = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creat_task)

        button_back_main.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val profiles = listOf<String>("https://i.pinimg.com/originals/52/44/95/524495ccf8c05ab40f5905d852a358c2.png",
            "https://i.pinimg.com/236x/6c/39/9b/6c399b619a43f86c2d5341aacd75702e.jpg",
            "https://i.pinimg.com/564x/69/72/bd/6972bde9f6d0d45d41530c85f8379a88.jpg",
            "https://i.pinimg.com/236x/d8/d7/b6/d8d7b6e15d06590ef551a4c561e22a55.jpg",
            "https://www.cctvforum.com/uploads/monthly_2018_12/S_member_24879.png",
            "https://i.pinimg.com/236x/85/79/ec/8579ec86d8863850953fd49fb41e2a71.jpg",
            "https://i.pinimg.com/236x/65/76/d3/6576d3d4b94f08be3c082d666a48e798.jpg",
            "https://i.pinimg.com/236x/58/f7/98/58f79869d4466999f24597632d8d06e4.jpg",
            "https://i.pinimg.com/564x/d5/be/82/d5be825883f116248f5cd3a2f58874b1.jpg")
        val ran = (0 until 8).random()

        var chooseMdl : Spinner = findViewById(R.id.spinner_choosemodel)
        val models = arrayOf("Object Detection", "Face Detection", "Pose Detection", "Image Labeling", "Text Recognition", "customSeg", "From URL")
        chooseMdl.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, models)

        chooseMdl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
                editTextURL.visibility = View.GONE
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 6) { // enter custom URL
                    editTextURL.visibility = View.VISIBLE
                } else {
                    editTextURL.visibility = View.GONE
                }
            }
        }

        btnInsert.setOnClickListener {
            if (editTextName.text.toString().isNotEmpty() &&
                editTextAge.text.toString().isNotEmpty() &&
                mediaType.checkedRadioButtonId != -1
            ) {
                val cv = ContentValues()
                cv.put(COL_NAME, editTextName.text.toString())
                cv.put(COL_AGE, editTextAge.text.toString().toInt())
                cv.put(COL_URL, "")
                cv.put(COL_PROFILE, profiles[ran])
                cv.put(COL_STAGE, "to collect")
                contentResolver.insert(CONTENT_URI, cv)
                // new field
                val checkedMediaType = getMediaType()
                modelSelected = chooseMdl.selectedItemPosition
                if(modelSelected == 6) {
                    val customURL = editTextURL.text.toString()
                }

                // upload the entered things to firebase
                val uid = FirebaseAuth.getInstance().uid ?: ""
                val task_id = UUID.randomUUID().toString()
                val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/$task_id")

                val task = Task(task_id, editTextName.text.toString(), editTextDes.text.toString(), models[0], uid)

                ref.setValue(task)
                    .addOnSuccessListener {
                        Log.d("CreateTaskActivity", "Finally we saved the task to Firebase Database id: $task_id")
                    }
                    .addOnFailureListener {
                        Log.d("CreateTaskActivity", "Failed to set task value to database: ${it.message}")
                    }

            } else {
                Toast.makeText(this, "Please enter Name and/or Number and media type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to the success page
            val intent = Intent(this, SuccessActivity::class.java)
            startActivity(intent)
        }
    }

    fun getMediaType() : String {
        val id = mediaType.checkedRadioButtonId
        val checkedBtn : RadioButton = findViewById(id)
        return checkedBtn.text.toString()
    }
}