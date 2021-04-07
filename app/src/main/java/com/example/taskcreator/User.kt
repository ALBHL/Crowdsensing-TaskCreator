package com.example.taskcreator

import android.graphics.Bitmap

class User {

    var id: Int = 0
    var name: String = ""
    var age: Int = 0
    var imageurl: String = ""
    var cur_stage: String = ""
    // cur_stage indicate the current stage of the sensing stage:
    //  we have: 1. ready to be collected, 2. collected 3. inferencer 3. ready to be validated
    // 4. deleted 5. validated
//    var collect: String = ""
//    var validate: String = ""
    var profileurl: String = ""
    var bmp: Bitmap? = null


    constructor(name: String, age: Int, imageurl: String) {
        this.name = name
        this.age = age
        this.imageurl = imageurl
        this.cur_stage = "ready to be validated"
//        this.collect = "false"
//        this.validate = "false"
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
        this.profileurl = profiles[ran]
    }

    constructor() {
    }
}
