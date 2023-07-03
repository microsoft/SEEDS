package com.example.seeds.utils

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.annotation.NonNull
import com.example.seeds.model.Student
import java.util.*
import kotlin.collections.HashMap

class ContactUtils constructor(private val context: Context) {

    var contacts: List<Student>
    var contactsMap: HashMap<String, Student>

    init {
        contactsMap = getListUsersFromContacts()
        contacts = contactsMap.values.toList()
    }

    private fun getListUsersFromContacts(): HashMap<String, Student> {
        var contactUsers: HashMap<String, Student> = hashMapOf()
        val phones: Cursor?
        try {
            phones = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC")
        } catch (e: SecurityException) {
            return HashMap()
        }
        val unprocessedContacts = HashMap<String, ArrayList<String>>()
        var name: String? = null
        var phoneNumber: String? = null
        while (phones!!.moveToNext()) {
            val colIndexName = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val colIndexNumber = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if(colIndexName >= 0 && colIndexNumber >=0)
            {
                name = phones.getString(colIndexName)
                phoneNumber = phones.getString(colIndexNumber)
            }
            if(name != null && phoneNumber != null){
                if(unprocessedContacts[name] == null)
                    unprocessedContacts[name] = arrayListOf()
                unprocessedContacts[name]!!.add(phoneNumber)
                contactUsers[phoneNumber] = Student(name=name, phoneNumber=phoneNumber)
            }
        }
        contactUsers = processContactsUser(unprocessedContacts)
        phones.close()
        return contactUsers
    }

    private fun processContactsUser(contacts: HashMap<String, ArrayList<String>>): HashMap<String, Student> {
        val contactsUsers = HashMap<String, Student>()
        contacts.forEach { (name, phoneNumbers) ->
            val finalPhoneNumbers = phoneNumbers.map{
                var phone = PhoneNumberUtils.formatNumber(it, Locale.getDefault().country)
                phone?.let {
                    phone = phone.replace(" ", "").replace("-", "").replace("+", "")
                    if (phone[0] == '0') {
                        phone = phone.substring(1)
                    }
                    phone
                } ?: it
            }.toSet()
            finalPhoneNumbers.forEach {
                var phno = it
                if(it.length == 10)phno = "91$phno"
                contactsUsers[phno] = Student(name=name, phoneNumber = phno)
            }
        }
        return contactsUsers
    }

    fun getStudentsFromString(studentStrings: List<String>): List<Student> {
        return studentStrings.map {
            contactsMap[it]?: Student(it, it)
        }
    }

    fun getNameFromString(studentString: String): String {
        return contactsMap[studentString]?.name ?: studentString
    }
}
