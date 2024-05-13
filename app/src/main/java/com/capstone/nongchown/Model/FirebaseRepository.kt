package com.capstone.nongchown.Model

import com.google.firebase.database.FirebaseDatabase

class FirebaseRepository {

    private val databaseReference = FirebaseDatabase.getInstance().getReference("data")

    fun saveData(data: String) {
        // Firebase에 데이터 저장
        val key = databaseReference.push().key ?: return
        databaseReference.child(key).setValue(data)
    }
}
