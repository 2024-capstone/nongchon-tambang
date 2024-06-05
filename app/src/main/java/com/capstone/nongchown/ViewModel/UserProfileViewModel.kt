package com.capstone.nongchown.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.capstone.nongchown.Model.FirebaseCommunication
import com.capstone.nongchown.Model.UserInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UserProfileViewModel : ViewModel() {
    private val firebaseComm = FirebaseCommunication()

    suspend fun loadStoredData(email: String): UserInfo = suspendCancellableCoroutine { cont ->
        firebaseComm.fetchUserByDocumentId(email) { user ->
            if (user != null) {
                cont.resume(user)
            } else {
                cont.resume(UserInfo())
            }
        }
    }

    private fun validateEmail(email: String): String {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (emailRegex.matches(email)) {
            return email
        } else {
            throw IllegalArgumentException("Invalid email format")
        }
    }

    private fun validateAge(age: String): String {
        val ageRegex = "^(?:150|[1-9]?[0-9])$".toRegex()
        if (ageRegex.matches(age)) {
            return age
        } else {
            throw IllegalArgumentException("Invalid age range")
        }
    }

    private fun validatePhone(number: String): String {
        // 숫자와 하이픈만 허용
        val cleanNumber = number.filter { it.isDigit() || it == '-' }
        val digitsOnly = cleanNumber.filter { it.isDigit() }

        // 정규식을 사용하여 유효한 번호 형식인지 검사 (하이픈 포함)
        val phoneRegexWithHyphen = "^\\d{2,3}-\\d{3,4}-\\d{4}$".toRegex()
        val phoneRegexWithoutHyphen = "^\\d{10,11}$".toRegex()

        if (phoneRegexWithHyphen.matches(cleanNumber)) {
            return cleanNumber  // 이미 유효한 형식
        } else if (phoneRegexWithoutHyphen.matches(digitsOnly)) {
            // 하이픈이 없는 경우에는 적절한 위치에 하이픈 추가
            return when (digitsOnly.length) {
                10 -> "${digitsOnly.substring(0, 3)}-${
                    digitsOnly.substring(
                        3,
                        6
                    )
                }-${digitsOnly.substring(6)}"

                11 -> "${digitsOnly.substring(0, 3)}-${
                    digitsOnly.substring(
                        3,
                        7
                    )
                }-${digitsOnly.substring(7)}"

                else -> throw IllegalArgumentException("Invalid phone number length.")
            }
        } else {
            throw IllegalArgumentException("Invalid phone number format.")
        }
    }

    private fun validateUserInfo(userInfo: UserInfo): UserInfo {
        val validName = userInfo.name.trim()
        val validEmail = validateEmail(userInfo.email).trim()
        val validAge = validateAge(userInfo.age).trim()
        val validGender = userInfo.gender.trim()
        val validEmergencyContactList = mutableListOf<String>()
        userInfo.emergencyContactList.forEach { contact ->
            validEmergencyContactList.add(validatePhone(contact).trim())
        }


        Log.d("[로그]", "valid name: $validName")
        Log.d("[로그]", "valid email: $validEmail")
        Log.d("[로그]", "valid age: $validAge")
        Log.d("[로그]", "valid gender: $validGender")
        validEmergencyContactList.forEach { eContact ->
            Log.d("[로그]", "valid emergency contact: $eContact")
        }

        return UserInfo(validName, validEmail, validAge, validGender, validEmergencyContactList)
    }

    fun userProfileSave(userInfo: UserInfo): UserInfo {
        try {
            val validUserInfo = validateUserInfo(userInfo)
            firebaseComm.updateOrCreateUser(validUserInfo)
            return validUserInfo
        } catch (e: IllegalArgumentException) {
            Log.d("[에러]", "${e.message}")
            throw e
        }
    }


}