package com.alyf.wlp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class WallpaperScheduler : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_TICK) {
            val sharedPreferences = context.getSharedPreferences("alyf_wlp_prefs", Context.MODE_PRIVATE)
            val schedules = loadSchedules(sharedPreferences)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            var bestSchedule: WallpaperSchedule? = null
            for (schedule in schedules) {
                if (schedule.hour < currentHour || (schedule.hour == currentHour && schedule.minute <= currentMinute)) {
                    if (bestSchedule == null || schedule.hour > bestSchedule.hour || (schedule.hour == bestSchedule.hour && schedule.minute > bestSchedule.minute)) {
                        bestSchedule = schedule
                    }
                }
            }

            if (bestSchedule != null) {
                val editor = sharedPreferences.edit()
                editor.putString("current_wallpaper_uri", bestSchedule.uri.toString())
                editor.apply()
            }
        }
    }

    private fun loadSchedules(sharedPreferences: SharedPreferences): List<WallpaperSchedule> {
        val gson = Gson()
        val json = sharedPreferences.getString("wallpaper_schedules", null)
        return if (json != null) {
            val type = object : TypeToken<List<WallpaperSchedule>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
