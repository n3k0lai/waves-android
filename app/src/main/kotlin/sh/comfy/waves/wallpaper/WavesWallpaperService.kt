package sh.comfy.waves.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sh.comfy.waves.R
import sh.comfy.waves.data.SettingsRepository
import sh.comfy.waves.data.WallpaperSettings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class WavesWallpaperService : WallpaperService() {

    companion object {
        /** Broadcast action to toggle animation from quick settings tile or notification */
        const val ACTION_TOGGLE_ANIMATION = "sh.comfy.waves.TOGGLE_ANIMATION"
        const val ACTION_SET_ANIMATION = "sh.comfy.waves.SET_ANIMATION"
        const val EXTRA_ENABLED = "enabled"

        private val QUAD_VERTICES: FloatBuffer = createFloatBuffer(
            floatArrayOf(
                -1f, -1f,
                1f, -1f,
                -1f, 1f,
                1f, 1f,
            )
        )

        private val DEFAULT_TEX_COORDS = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        private fun createFloatBuffer(data: FloatArray): FloatBuffer {
            return ByteBuffer.allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(data)
                    position(0)
                }
        }
    }

    override fun onCreateEngine(): Engine = WavesEngine()

    inner class WavesEngine : Engine() {

        private var player: ExoPlayer? = null
        private val mainHandler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private val settingsRepo by lazy { SettingsRepository(this@WavesWallpaperService) }

        private var settings = WallpaperSettings()
        private var videoWidth = 0
        private var videoHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // Animation toggle (controlled by quick settings tile)
        private var animationEnabled = true

        // Foldable support: hinge region to avoid rendering content across
        private var hingeRect: android.graphics.Rect? = null

        // GL state
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var glProgram = 0
        private var textureId = 0
        private var videoTexture: SurfaceTexture? = null
        private var videoSurface: Surface? = null
        private var frameAvailable = false

        private val toggleReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_TOGGLE_ANIMATION -> {
                        animationEnabled = !animationEnabled
                        applyAnimationState()
                    }
                    ACTION_SET_ANIMATION -> {
                        animationEnabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
                        applyAnimationState()
                    }
                }
            }
        }

        private fun applyAnimationState() {
            if (animationEnabled) {
                player?.play()
            } else {
                player?.pause()
                // Draw one last frame so it freezes on current position (not black)
                if (frameAvailable) drawFrame()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Register for animation toggle broadcasts
            val filter = IntentFilter().apply {
                addAction(ACTION_TOGGLE_ANIMATION)
                addAction(ACTION_SET_ANIMATION)
            }
            this@WavesWallpaperService.registerReceiver(toggleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

            scope.launch {
                settingsRepo.wallpaperSettings.collectLatest { newSettings ->
                    settings = newSettings
                    // Re-render with new focal point
                    if (frameAvailable) drawFrame()
                }
            }
            // Detect foldable hinge position for fold-aware rendering
            scope.launch {
                try {
                    val windowManager = androidx.window.layout.WindowInfoTracker
                        .getOrCreate(this@WavesWallpaperService)
                    windowManager.windowLayoutInfo(this@WavesWallpaperService)
                        .collectLatest { layoutInfo ->
                            val foldFeature = layoutInfo.displayFeatures
                                .filterIsInstance<androidx.window.layout.FoldingFeature>()
                                .firstOrNull()
                            hingeRect = foldFeature?.bounds
                            if (frameAvailable) drawFrame()
                        }
                } catch (_: Exception) {
                    // Not a foldable device or WindowInfoTracker unavailable
                    hingeRect = null
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height

            // Tear down old GL context if surface changes
            releaseGl()
            initGl(holder)
            initPlayer()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
            releaseGl()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible && animationEnabled) {
                player?.play()
            } else {
                player?.pause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                this@WavesWallpaperService.unregisterReceiver(toggleReceiver)
            } catch (_: Exception) {}
            releasePlayer()
            releaseGl()
            scope.cancel()
        }

        // region GL Setup

        private fun initGl(holder: SurfaceHolder) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(
                eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0
            )

            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, configs[0], holder.surface, intArrayOf(EGL14.EGL_NONE), 0
            )

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            // Create external texture for video frames
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )

            // Create SurfaceTexture for ExoPlayer output
            videoTexture = SurfaceTexture(textureId).apply {
                setOnFrameAvailableListener {
                    frameAvailable = true
                    mainHandler.post { drawFrame() }
                }
            }
            videoSurface = Surface(videoTexture)

            // Compile shader program
            glProgram = createProgram()
        }

        private fun createProgram(): Int {
            val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return program
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            return shader
        }

        private fun releaseGl() {
            videoSurface?.release()
            videoSurface = null
            videoTexture?.release()
            videoTexture = null

            if (glProgram != 0) {
                GLES20.glDeleteProgram(glProgram)
                glProgram = 0
            }
            if (textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                textureId = 0
            }
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }

        // endregion

        // region Rendering

        private fun drawFrame() {
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            if (frameAvailable) {
                videoTexture?.updateTexImage()
                frameAvailable = false
            }

            // Foldable hinge-aware rendering
            val hinge = hingeRect
            if (hinge != null && hinge.width() > 0 && surfaceWidth > 0) {
                // Render two viewports: left of hinge, right of hinge
                // Left panel
                GLES20.glViewport(0, 0, hinge.left, surfaceHeight)
                renderQuad()
                // Right panel
                GLES20.glViewport(hinge.right, 0, surfaceWidth - hinge.right, surfaceHeight)
                renderQuad()
                // Black out the hinge area
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(hinge.left, 0, hinge.width(), surfaceHeight)
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            } else {
                // Standard single-screen rendering
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                renderQuad()
            }

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        private fun renderQuad() {
            GLES20.glUseProgram(glProgram)

            val texCoords = calculateCropTexCoords()
            val texCoordBuffer = createFloatBuffer(texCoords)

            val positionHandle = GLES20.glGetAttribLocation(glProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, QUAD_VERTICES)

            val texCoordHandle = GLES20.glGetAttribLocation(glProgram, "aTexCoord")
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

            val textureHandle = GLES20.glGetUniformLocation(glProgram, "uTexture")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUniform1i(textureHandle, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }

        /**
         * Computes texture coordinates for a center-crop that respects the focal point.
         */
        private fun calculateCropTexCoords(): FloatArray {
            if (videoWidth == 0 || videoHeight == 0 || surfaceWidth == 0 || surfaceHeight == 0) {
                return DEFAULT_TEX_COORDS
            }

            val videoAspect = videoWidth.toFloat() / videoHeight
            val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight

            var texWidth = 1f
            var texHeight = 1f

            if (videoAspect > surfaceAspect) {
                texWidth = surfaceAspect / videoAspect
            } else {
                texHeight = videoAspect / surfaceAspect
            }

            val maxOffsetX = 1f - texWidth
            val maxOffsetY = 1f - texHeight

            val offsetX = maxOffsetX * settings.focalPointX
            val offsetY = maxOffsetY * (1f - settings.focalPointY)

            val left = offsetX
            val right = offsetX + texWidth
            val bottom = offsetY
            val top = offsetY + texHeight

            return floatArrayOf(
                left, bottom,
                right, bottom,
                left, top,
                right, top,
            )
        }

        // endregion

        // region Player

        private fun initPlayer() {
            releasePlayer()

            val exoPlayer = ExoPlayer.Builder(this@WavesWallpaperService).build().apply {
                val uri = "rawresource:///${R.raw.waves}"
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f

                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(size: VideoSize) {
                        videoWidth = size.width
                        videoHeight = size.height
                    }
                })

                videoSurface?.let { setVideoSurface(it) }
                prepare()
                if (animationEnabled) play()
            }

            player = exoPlayer
        }

        private fun releasePlayer() {
            player?.let {
                it.stop()
                it.release()
            }
            player = null
        }

        // endregion
    }
}
