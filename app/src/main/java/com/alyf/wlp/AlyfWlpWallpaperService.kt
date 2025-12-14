package com.alyf.wlp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.opengl.GLSurfaceView


import io.github.sceneview.SceneView

import java.util.concurrent.Executors

class AlyfWlpWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AlyfWlpWallpaperEngine()
    }

    inner class AlyfWlpWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val executor = Executors.newSingleThreadExecutor()
        private val handler = Handler(Looper.getMainLooper())
        
        
        private var visible = true
        private var isLocked = true // Assume locked initially
        private lateinit var glSurfaceView: GLSurfaceView
        private lateinit var renderer: AlyfWlpGLRenderer

        // OpenGL ES related variables
        
        

        private lateinit var sharedPreferences: SharedPreferences

        

        private val screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val oldIsLocked = isLocked
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isLocked = true
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        // Screen is on, but might still be locked
                        // We'll assume it's locked until ACTION_USER_PRESENT
                        isLocked = true
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isLocked = false
                    }
                }

                if (oldIsLocked != isLocked) {
                    // Handle lock state change for 3D rendering
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)

            sharedPreferences = applicationContext.getSharedPreferences("alyf_wlp_prefs", Context.MODE_PRIVATE)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            renderer = AlyfWlpGLRenderer()
            glSurfaceView = GLSurfaceView(applicationContext)
            glSurfaceView.setEGLContextClientVersion(2)
            glSurfaceView.setRenderer(renderer)
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
            glSurfaceView.setPreserveEGLContextOnPause(true)
            glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    glSurfaceView.surfaceCreated(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    glSurfaceView.surfaceChanged(holder, format, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    glSurfaceView.surfaceDestroyed(holder)
                }
            })

            updateWallpaper()
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(screenStateReceiver)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            glSurfaceView.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "current_wallpaper_uri") {
                updateWallpaper()
            }
        }

        private fun updateWallpaper() {
            executor.execute {
                val wallpaperUriString = sharedPreferences.getString("current_wallpaper_uri", null)
                if (wallpaperUriString != null) {
                    try {
                        val wallpaperUri = Uri.parse(wallpaperUriString)
                        val inputStream = contentResolver.openInputStream(wallpaperUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        handler.post {
                            renderer.setWallpaperTexture(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Optionally, set a default wallpaper or show an error message
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                glSurfaceView.onResume()
            } else {
                glSurfaceView.onPause()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            glSurfaceView.onPause()
        }
    }
}