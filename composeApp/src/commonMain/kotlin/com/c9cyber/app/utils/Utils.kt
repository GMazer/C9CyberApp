package com.c9cyber.app.utils

import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.GERMANY) // Germany use dot separation
    return formatter.format(amount)
}