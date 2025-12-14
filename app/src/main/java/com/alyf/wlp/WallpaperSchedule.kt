package com.alyf.wlp

import android.net.Uri

data class WallpaperSchedule(
    val uri: Uri,
    val hour: Int,
    val minute: Int
)
