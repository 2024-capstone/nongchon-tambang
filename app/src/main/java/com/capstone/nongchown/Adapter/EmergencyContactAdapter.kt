package com.capstone.nongchown.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.capstone.nongchown.R

class EmergencyContactAdapter(
    private val contacts: MutableList<String>,
    private val onContactChanged: (Int, String) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactEditText: EditText = itemView.findViewById(R.id.emergency_contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.emergency_contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.contactEditText.setText(contact)
        holder.contactEditText.addTextChangedListener {
            onContactChanged(position, it.toString())
        }
    }

//    데이터의 전체 길이를 반환
    override fun getItemCount(): Int = contacts.size

    fun addContact(contact: String) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun removeContact(position: Int) {
        contacts.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateContacts(newContacts: List<String>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }
}
