package com.clement.admin

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentTime(): String {
    val now = Calendar.getInstance().time
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(now)
}
