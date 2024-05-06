package com.capstone.nongchown.View.Activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ScrollingView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.capstone.nongchown.R
import com.capstone.nongchown.ViewModel.UserProfileViewModel

class UserProfileActivity : AppCompatActivity() {

    private lateinit var pageScroll: ScrollView

    private lateinit var userName: EditText
    private lateinit var userEmail: EditText
    private lateinit var age: EditText
    private lateinit var gender: Spinner
    private lateinit var emergencyContacts: LinearLayout

    private lateinit var emergencyAddButton: Button
    private lateinit var saveButton: Button

    private val emergencyContactList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        pageScroll = findViewById<ScrollView>(R.id.user_profile_scroll)

        userName = findViewById<EditText>(R.id.user_name)
        userName.addTextChangedListener {
            saveButton.isEnabled = true
        }

        userEmail = findViewById<EditText>(R.id.user_email)
        userEmail.addTextChangedListener {
            saveButton.isEnabled = true
        }

        age = findViewById<EditText>(R.id.age)
        age.addTextChangedListener {
            saveButton.isEnabled = true
        }

        gender = findViewById<Spinner>(R.id.gender)
        gender.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                saveButton.isEnabled = true
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                // Do nothing
            }
        }

        emergencyContacts = findViewById<LinearLayout>(R.id.emergency_contact_list)
        emergencyContacts.children.filterIsInstance<EditText>().forEach { emergencyContact ->
            emergencyContact.addTextChangedListener {
                saveButton.isEnabled = true
            }
        }

        emergencyAddButton = findViewById<Button>(R.id.emergency_contact_addButton)
        emergencyAddButton.setOnClickListener {
            addEmergencyContact()
        }

        saveButton = findViewById<Button>(R.id.user_profile_saveButton)
        saveButton.setOnClickListener {
            val name = userName.text.toString()
            val email = userEmail.text.toString()
            val age = age.text.toString()
            val gender = gender.selectedItem.toString()

            emergencyContacts.children.filterIsInstance<EditText>().forEach { emergencyContact ->
                emergencyContactList.add(emergencyContact.text.toString())
            }

            UserProfileViewModel().userProfileSave(name, email, age, gender, emergencyContactList)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addEmergencyContact() {
        val inflater = LayoutInflater.from(this)
        val eContact = inflater.inflate(R.layout.emergency_contact_item, emergencyContacts, false)
        setupFocusListener(eContact as EditText)
        emergencyContacts.addView(eContact, emergencyContacts.childCount - 1)
    }

    private fun setupFocusListener(editText: EditText) {
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pageScroll.post {
                    pageScroll.scrollTo(0, v.top)
                }
            }
        }

        // Optional: Adjusting scroll when text changes
        editText.doOnTextChanged { text, start, before, count ->
            if (editText.hasFocus()) {
                pageScroll.post {
                    pageScroll.scrollTo(0, editText.top)
                }
            }
        }
    }
}



