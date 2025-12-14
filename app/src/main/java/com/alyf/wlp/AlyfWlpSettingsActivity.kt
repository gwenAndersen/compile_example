package com.alyf.wlp

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class AlyfWlpSettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var layoutJsonTextView: TextView
    private lateinit var wallpaperScheduleList: RecyclerView
    private lateinit var adapter: WallpaperScheduleAdapter
    private val schedules = mutableListOf<WallpaperSchedule>()
    private val gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    private var mHour: Int = 0
    private var mMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("alyf_wlp_prefs", Context.MODE_PRIVATE)

        loadSchedules()

        wallpaperScheduleList = findViewById(R.id.wallpaper_schedule_list)
        wallpaperScheduleList.layoutManager = LinearLayoutManager(this)
        adapter = WallpaperScheduleAdapter(schedules, {
            position ->
            schedules.removeAt(position)
            adapter.notifyItemRemoved(position)
            saveSchedules()
        }) { position ->
            editWallpaperSchedule(position)
        }
        wallpaperScheduleList.adapter = adapter

        val callback = ItemMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(wallpaperScheduleList)

        val addWallpaperScheduleButton: Button = findViewById(R.id.add_wallpaper_schedule_button)
        addWallpaperScheduleButton.setOnClickListener { addWallpaperSchedule() }

        val captureLayoutButton: Button = findViewById(R.id.capture_layout_button)
        layoutJsonTextView = findViewById(R.id.layout_json_textview)
        val copyJsonButton: Button = findViewById(R.id.copy_json_button)

        captureLayoutButton.setOnClickListener {
            val intent = Intent(AlyfWlpAccessibilityService.ACTION_CAPTURE_LAYOUT)
            sendBroadcast(intent)
            Toast.makeText(this, "Requesting layout capture...", Toast.LENGTH_SHORT).show()
        }

        copyJsonButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Captured Layout", layoutJsonTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        SharedViewModel.capturedLayoutJson.observe(this) {
            layoutJsonTextView.text = "Captured Layout:\n" +
                    (it ?: "No layout captured")
            Toast.makeText(this, "Layout captured!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWallpaperSchedule() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                mHour = hourOfDay
                mMinute = minute
                openFilePicker()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                val mimeType = contentResolver.getType(it)
                if (mimeType?.startsWith("image/") == true) {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    schedules.add(WallpaperSchedule(it, mHour, mMinute))
                    adapter.notifyDataSetChanged()
                    saveSchedules()
                } else {
                    Toast.makeText(this, "Please select a valid image file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun editWallpaperSchedule(position: Int) {
        val schedule = schedules[position]
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, schedule.hour)
        calendar.set(Calendar.MINUTE, schedule.minute)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                mHour = hourOfDay
                mMinute = minute
                openFilePickerForEdit(position)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun openFilePickerForEdit(position: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra("position", position)
        }
        pickImageLauncherForEdit.launch(intent)
    }

    private val pickImageLauncherForEdit = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                val mimeType = contentResolver.getType(it)
                if (mimeType?.startsWith("image/") == true) {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val position = data.getIntExtra("position", -1)
                    if (position != -1) {
                        schedules[position] = WallpaperSchedule(it, mHour, mMinute)
                        adapter.notifyItemChanged(position)
                        saveSchedules()
                    }
                } else {
                    Toast.makeText(this, "Please select a valid image file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveSchedules() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(schedules)
        editor.putString("wallpaper_schedules", json)
        editor.apply()
    }

    private fun loadSchedules() {
        val json = sharedPreferences.getString("wallpaper_schedules", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<WallpaperSchedule>>() {}.type
            schedules.clear()
            schedules.addAll(gson.fromJson(json, type))
        }
    }
}