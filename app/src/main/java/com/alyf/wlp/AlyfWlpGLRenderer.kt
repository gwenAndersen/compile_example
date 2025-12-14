package com.alyf.wlp

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AlyfWlpGLRenderer : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 a_texCoord;
        varying vec2 v_texCoord;
        void main() {
          gl_Position = vPosition;
          v_texCoord = a_texCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D u_texture;
        varying vec2 v_texCoord;
        void main() {
          gl_FragColor = texture2D(u_texture, v_texCoord);
        }
    """.trimIndent()

    private val quadVertices = floatArrayOf(
        -1.0f,  1.0f,  // top left
        -1.0f, -1.0f,  // bottom left
         1.0f, -1.0f,  // bottom right
         1.0f,  1.0f   // top right
    )

    private val textureCoordinates = floatArrayOf(
        0.0f, 0.0f,     // top left
        0.0f, 1.0f,     // bottom left
        1.0f, 1.0f,     // bottom right
        1.0f, 0.0f      // top right
    )

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer
    private lateinit var drawListBuffer: java.nio.ShortBuffer

    private var program: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureUniformHandle: Int = 0
    private var textureId: Int = 0

    private var wallpaperBitmap: Bitmap? = null

    fun setWallpaperTexture(bitmap: Bitmap) {
        wallpaperBitmap = bitmap
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        setupBuffers()
        setupShaders()
        setupTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)

        if (wallpaperBitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, wallpaperBitmap, 0)
            wallpaperBitmap = null
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun setupBuffers() {
        val bb = ByteBuffer.allocateDirect(quadVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(quadVertices)
        vertexBuffer.position(0)

        val tb = ByteBuffer.allocateDirect(textureCoordinates.size * 4)
        tb.order(ByteOrder.nativeOrder())
        textureBuffer = tb.asFloatBuffer()
        textureBuffer.put(textureCoordinates)
        textureBuffer.position(0)

        val dlb = ByteBuffer.allocateDirect(drawOrder.size * 2)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(drawOrder)
        drawListBuffer.position(0)
    }

    private fun setupShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "u_texture")
    }

    private fun setupTexture() {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
