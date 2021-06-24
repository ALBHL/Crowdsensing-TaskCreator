package com.example.taskcreator

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.outbox_file_row.view.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    val PROVIDER_NAME = "com.example.collector/AcronymProvider"
    val URL = "content://$PROVIDER_NAME/Inbox"
    val CONTENT_URI = Uri.parse(URL)

    val TABLEIN_NAME = "Inbox"
    val COL_NAME = "name"
    val COL_AGE = "age"
    val COL_ID = "id"
    val COL_URL = "imageurl"
    val COL_PROFILE = "profileimg"
    val COL_STAGE = "current_stage"
    val COL_IMAGE_BIT = "picturetaken"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_to_add.setOnClickListener {
            val intent = Intent(this, CreatTaskActivity::class.java)
            startActivity(intent)
        }

        val context = this

        val result = contentResolver.query(
            CONTENT_URI,
            arrayOf(COL_ID, COL_NAME, COL_STAGE, COL_AGE, COL_PROFILE, COL_URL, COL_IMAGE_BIT),
            null, null, null
        )
        val data = result?.let { readInData(it) }
        if (data != null) {
            fetchUsers(data)
        }
    }

    private fun readInData(result: Cursor): MutableList<User> {
        val list: MutableList<User> = ArrayList()
        while (result.moveToNext()) {
            val user = User()
            user.id = result.getString(0).toInt()
            user.name = result.getString(1)
            user.age = result.getString(3).toInt()
            user.imageurl = result.getString(5)
            user.cur_stage = result.getString(2)
            user.profileurl = result.getString(4)
            list.add(user)
        }
        return list
    }

    private fun fetchUsers(data: MutableList<User>) {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val adapter = GroupAdapter<ViewHolder>()
                    var tasks = ArrayList<Task>()
                    p0.children.forEach {
                        Log.d("NewMessage", it.toString())
                        val creator = it.getValue(Creator::class.java)
                        it.child("tasks").children.forEach {
                            it.getValue(Task::class.java)?.let { it1 -> tasks.add(it1) }
                        }
                        if (creator != null && tasks != null) {
                            for (i in 0 until tasks.size) {
                                if (tasks[i].cur_stage == "to collect") {
                                    adapter.add(CreatorItem(tasks[i], creator))
                                }
                            }
                        }
                    }

                    recycleview_inbox.adapter = adapter
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

//         USING local sqlite db instead of fire base
//        val adapter = GroupAdapter<ViewHolder>()
//
//        for (i in 0 until data.size) {
//            if (data[i].cur_stage == "to collect") {
//                adapter.add(UserItem(data[i]))
//            }
//        }
//
//        recycleview_inbox.adapter = adapter
    }
}

class UserItem(val user: User) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        // will be called in the list of user object
        viewHolder.itemView.outbox_query_title.text = user.name
        val imageURL = user.profileurl
        if (imageURL != "") {
            Picasso.get().load(imageURL).into(viewHolder.itemView.imageViewprofile)
        }
    }

    override fun getLayout(): Int {
        return R.layout.outbox_file_row
    }
}

class CreatorItem(val task: Task, val creator: Creator) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        // will be called in the list of user object
        Log.d("creater", creator.toString())
        Log.d("tasks", Json.encodeToString(task))
        viewHolder.itemView.outbox_query_title.text = task.task_name
        viewHolder.itemView.outbox_query_description.text = task.task_description
        val imageURL = creator.profileurl
        if (imageURL != "") {
            Picasso.get().load(imageURL).into(viewHolder.itemView.imageViewprofile)
        }
    }

    override fun getLayout(): Int {
        return R.layout.outbox_file_row
    }
}

