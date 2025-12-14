package com.alyf.wlp

import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.os.Handler
import android.os.Looper

class Live3dWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return Live3dWallpaperEngine()
    }

    inner class Live3dWallpaperEngine : Engine() {

        private lateinit var glSurfaceView: GLSurfaceView
        private lateinit var renderer: AlyfWlpGLRenderer
        private val handler = Handler(Looper.getMainLooper())
        private val gson = Gson()

        private val layoutObserver = Observer<String> { json ->
            json?.let {
                val type = object : TypeToken<Map<Int, List<IconInfo>>>() {}.type
                val layout = gson.fromJson<Map<Int, List<IconInfo>>>(it, type)
                renderer.updateIconLayout(layout)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            glSurfaceView = GLSurfaceView(applicationContext)
            renderer = AlyfWlpGLRenderer()
            glSurfaceView.setEGLContextClientVersion(2)
            glSurfaceView.setRenderer(renderer)
            handler.post {
                SharedViewModel.capturedLayoutJson.observeForever(layoutObserver)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer.onSurfaceChanged(null, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.post {
                SharedViewModel.capturedLayoutJson.removeObserver(layoutObserver)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                glSurfaceView.onResume()
            } else {
                glSurfaceView.onPause()
            }
        }
    }
}
