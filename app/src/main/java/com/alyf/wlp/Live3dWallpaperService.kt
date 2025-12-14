package com.alyf.wlp

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Live3dWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return Live3dWallpaperEngine()
    }

    inner class Live3dWallpaperEngine : Engine() {

        private val handlerThread = HandlerThread("WallpaperRenderer")
        private lateinit var handler: Handler
        private lateinit var renderer: AlyfWlpGLRenderer
        private val gson = Gson()
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var eglConfig: EGLConfig? = null

        private val layoutObserver = Observer<String> { json ->
            json?.let {
                val type = object : TypeToken<Map<Int, List<IconInfo>>>() {}.type
                val layout = gson.fromJson<Map<Int, List<IconInfo>>>(it, type)
                handler.post { renderer.updateIconLayout(layout) }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            handlerThread.start()
            handler = Handler(handlerThread.looper)
            renderer = AlyfWlpGLRenderer(applicationContext)
            handler.post {
                initEGL(holder)
                renderer.onSurfaceCreated(null, null)
                SharedViewModel.capturedLayoutJson.observeForever(layoutObserver)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            handler.post {
                renderer.onSurfaceChanged(null, width, height)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.post {
                SharedViewModel.capturedLayoutJson.removeObserver(layoutObserver)
                destroyEGL()
            }
            handlerThread.quitSafely()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                handler.post { startRendering() }
            } else {
                handler.post { stopRendering() }
            }
        }

        private fun initEGL(holder: SurfaceHolder) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val numConfigs = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
            eglConfig = configs[0]
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            val surfaceAttribs = intArrayOf(
                EGL14.EGL_NONE
            )
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, holder, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }

        private fun destroyEGL() {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }

        private fun startRendering() {
            handler.post(object : Runnable {
                override fun run() {
                    if (isVisible) {
                        renderer.onDrawFrame(null)
                        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                        handler.postDelayed(this, 16)
                    }
                }
            })
        }

        private fun stopRendering() {
            // No need to do anything here, the rendering loop will stop when isVisible is false
        }
    }
}
