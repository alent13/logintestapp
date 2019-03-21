package com.applexis.logintestapp

import android.util.Patterns

fun String.isValidEmail(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isPasswordStrong(): Boolean =
    Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{6,}").toPattern().matcher(this).matches()
