package com.example.notemaster

class Note(
    name: String = "NaMe",
    content: String = "CoNtEnT"
){
    var name: String = ""
    var content: String = ""

    init {
        this.name = name
        this.content = content
    }

}