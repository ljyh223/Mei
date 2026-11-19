package com.ljyh.mei.ui.component.player.cover.mesh

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos

private const val TAG = "MeshGradientRenderer"

class MeshGradientRenderer : GLSurfaceView.Renderer {
    private data class MeshState(
        val mesh: BHPMesh,
        val textureId: Int,
        val albumKey: String,
        var alpha: Float,
        var targetAlpha: Float
    )

    private var mainProgram: Int = 0
    private var quadProgram: Int = 0

    private var fbo: Int = 0
    private var fboTexture: Int = 0

    private val meshStates = mutableListOf<MeshState>()

    private var scaledWidth: Int = 0
    private var scaledHeight: Int = 0
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    private val frameClock = MeshFrameClock()

    internal val needsAnotherFrame: Boolean
        get() = frameClock.needsAnotherFrame(
            hasArtwork = meshStates.isNotEmpty() && fbo != 0,
            transitioning = meshStates.size > 1 || meshStates.any { it.targetAlpha != 0f },
        )

    fun resetFrameTime() = frameClock.resetFrameTime()

    @Volatile
    var flowSpeed: Float = 0.25f

    @Volatile
    var renderScale: Float = 0.75f

    @Volatile
    var subdivision: Int = 50

    private var pendingAlbum: Bitmap? = null
    private var pendingAlbumKey: String? = null
    private var retainedAlbum: Bitmap? = null
    private var retainedAlbumKey: String? = null
    private var reportedAlbumKey: String? = null
    private var albumChanged: Boolean = false

    var onAlbumRendered: ((String) -> Unit)? = null

    private val random = java.util.Random()

    fun setAlbum(bitmap: Bitmap, key: String) {
        synchronized(this) {
            retainedAlbum?.takeIf { it !== bitmap }?.recycle()
            retainedAlbum = bitmap
            retainedAlbumKey = key
            pendingAlbum = bitmap
            pendingAlbumKey = key
            albumChanged = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {

        // EGL textures and framebuffers do not survive a recreated GL context.
        // Rebuild from the retained artwork instead of drawing an empty black frame.
        frameClock.resetFrameTime()
        synchronized(this) {
            meshStates.clear()
            fbo = 0
            fboTexture = 0
            pendingAlbum = retainedAlbum
            pendingAlbumKey = retainedAlbumKey
            reportedAlbumKey = null
            albumChanged = retainedAlbum != null
        }

        Timber.tag(TAG).d("GPU 渲染器: ${GLES30.glGetString(GLES30.GL_RENDERER)}")
        Timber.tag(TAG).d("GPU 厂商: ${GLES30.glGetString(GLES30.GL_VENDOR)}")
        Timber.tag(TAG).d("GL 版本: ${GLES30.glGetString(GLES30.GL_VERSION)}")

        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        mainProgram =
            createProgram(ShaderSource.MESH_VERTEX_SHADER, ShaderSource.MESH_FRAGMENT_SHADER)
        quadProgram =
            createProgram(ShaderSource.QUAD_VERTEX_SHADER, ShaderSource.QUAD_FRAGMENT_SHADER)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        rebuildFbo()
    }

    fun rebuildFbo() {
        frameClock.resetFrameTime()
        scaledWidth = maxOf(1, (viewWidth * renderScale).toInt())
        scaledHeight = maxOf(1, (viewHeight * renderScale).toInt())
        createFbo(scaledWidth, scaledHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        processPendingAlbum()

        if (meshStates.isEmpty() || fbo == 0) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            return
        }

        // Every requested frame must fill the EGL back buffer, even when motion is frozen.
        // Buffer contents are not guaranteed to survive a swap; hosts stop scheduling instead.
        val time = frameClock.timeSeconds(System.nanoTime()) * flowSpeed

        updateMeshStates(1f / 60f)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        for (i in meshStates.lastIndex downTo 0) {
            val state = meshStates[i]
            val easeAlpha = easeInOutSine(state.alpha.coerceIn(0f, 1f))
            if (easeAlpha <= 0.0f) continue

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
            GLES30.glViewport(0, 0, scaledWidth, scaledHeight)
            GLES30.glDisable(GLES30.GL_BLEND)
            GLES30.glClearColor(0f, 0f, 0f, 0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            drawMesh(state, time)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, viewWidth, viewHeight)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFuncSeparate(
                GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA,
                GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA
            )
            drawQuad(fboTexture, easeAlpha)
        }

        meshStates.firstOrNull()?.albumKey?.let { key ->
            if (reportedAlbumKey != key) {
                reportedAlbumKey = key
                onAlbumRendered?.invoke(key)
            }
        }

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun processPendingAlbum() {
        synchronized(this) {
            if (!albumChanged) return
            albumChanged = false
            val bitmap = pendingAlbum ?: return
            val key = pendingAlbumKey ?: return
            pendingAlbum = null
            pendingAlbumKey = null

            val processed = AlbumTextureProcessor.process(bitmap)
            val textureId = uploadTexture(processed)

            val preset = selectPreset()
            val mesh = BHPMesh(preset.width, preset.height)
            mesh.resetSubdivision(subdivision)
            mesh.configureFromPreset(preset, processed)

            processed.recycle()

            val newState = MeshState(mesh, textureId, key, if (meshStates.isEmpty()) 1f else 0f, 1f)
            for (existing in meshStates) {
                existing.targetAlpha = -1f
            }
            meshStates.add(0, newState)
        }
    }

    private fun selectPreset(): ControlPointPreset {
        return if (random.nextFloat() < 0.8f) {
            CONTROL_POINT_PRESETS[random.nextInt(CONTROL_POINT_PRESETS.size)]
        } else {
            generateControlPoints(random = kotlin.random.Random(random.nextLong()))
        }
    }

    private fun updateMeshStates(dt: Float) {
        val deltaFactor = dt * 1.5f

        val iter = meshStates.iterator()
        while (iter.hasNext()) {
            val state = iter.next()
            state.alpha += deltaFactor * state.targetAlpha

            if (state.targetAlpha > 0f && state.alpha >= 1f) {
                state.alpha = 1f
                state.targetAlpha = 0f
            }

            if (state.alpha <= 0f && state.targetAlpha < 0f) {
                GLES30.glDeleteTextures(1, intArrayOf(state.textureId), 0)
                iter.remove()
            }
        }
    }

    fun setStaticMode(enable: Boolean) = frameClock.setStaticMode(enable)

    fun setPlaying(playing: Boolean) = frameClock.setPlaying(playing)

    private fun easeInOutSine(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return (1f - cos(clamped * Math.PI.toFloat())) / 2f
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texIds[0])
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_MIRRORED_REPEAT
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_MIRRORED_REPEAT
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return texIds[0]
    }

    private fun drawMesh(state: MeshState, time: Float) {
        val mesh = state.mesh
        val vertexBuffer = mesh.buffer ?: return
        val indexBuffer = mesh.generateIndexBuffer()

        GLES30.glUseProgram(mainProgram)

        val aPos = GLES30.glGetAttribLocation(mainProgram, "a_pos")
        val aColor = GLES30.glGetAttribLocation(mainProgram, "a_color")
        val aUv = GLES30.glGetAttribLocation(mainProgram, "a_uv")

        val uTexture = GLES30.glGetUniformLocation(mainProgram, "u_texture")
        val uTime = GLES30.glGetUniformLocation(mainProgram, "u_time")
        val uAspect = GLES30.glGetUniformLocation(mainProgram, "u_aspect")

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, state.textureId)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniform1f(uTime, time)
        GLES30.glUniform1f(
            uAspect,
            if (scaledHeight > 0) scaledWidth.toFloat() / scaledHeight else 1f
        )

        vertexBuffer.position(0)
        val strideBytes = 7 * 4

        GLES30.glEnableVertexAttribArray(aPos)
        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, strideBytes, vertexBuffer)

        vertexBuffer.position(2)
        GLES30.glEnableVertexAttribArray(aColor)
        GLES30.glVertexAttribPointer(aColor, 3, GLES30.GL_FLOAT, false, strideBytes, vertexBuffer)

        vertexBuffer.position(5)
        GLES30.glEnableVertexAttribArray(aUv)
        GLES30.glVertexAttribPointer(aUv, 2, GLES30.GL_FLOAT, false, strideBytes, vertexBuffer)

        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            mesh.indices,
            GLES30.GL_UNSIGNED_INT,
            indexBuffer
        )

        GLES30.glDisableVertexAttribArray(aPos)
        GLES30.glDisableVertexAttribArray(aColor)
        GLES30.glDisableVertexAttribArray(aUv)
    }

    private fun drawQuad(textureId: Int, alpha: Float) {
        GLES30.glUseProgram(quadProgram)

        val aPos = GLES30.glGetAttribLocation(quadProgram, "a_pos")
        val aTexCoord = GLES30.glGetAttribLocation(quadProgram, "a_texCoord")
        val uTexture = GLES30.glGetUniformLocation(quadProgram, "u_texture")
        val uAlpha = GLES30.glGetUniformLocation(quadProgram, "u_alpha")

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniform1f(uAlpha, alpha)

        drawFullScreenQuad(aPos, aTexCoord)
    }

    private fun drawFullScreenQuad(aPos: Int, aTexCoord: Int) {
        val quadData = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f
        )
        val buffer = ByteBuffer.allocateDirect(quadData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(quadData).position(0)

        GLES30.glEnableVertexAttribArray(aPos)
        buffer.position(0)
        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, buffer)

        GLES30.glEnableVertexAttribArray(aTexCoord)
        buffer.position(2)
        GLES30.glVertexAttribPointer(aTexCoord, 2, GLES30.GL_FLOAT, false, 16, buffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(aPos)
        GLES30.glDisableVertexAttribArray(aTexCoord)
    }

    private fun createFbo(width: Int, height: Int) {
        if (fbo != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            GLES30.glDeleteTextures(1, intArrayOf(fboTexture), 0)
        }

        val fboIds = IntArray(1)
        GLES30.glGenFramebuffers(1, fboIds, 0)
        fbo = fboIds[0]

        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        fboTexture = texIds[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTexture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, fboTexture, 0
        )

        // 【核心日志】：检查天玑 GPU 是否承认你创建的离屏 FBO
        val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.tag(TAG)
                .e("FBO 创建失败，状态码为: $fboStatus (可能因为尺寸 $width x $height 导致 Mali 硬件拒绝)")
        } else {
            Timber.tag(TAG).d("FBO 成功创建并绑定完成: $width x $height")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun release(clearArtwork: Boolean = false) {
        synchronized(this) {
            for (state in meshStates) {
                GLES30.glDeleteTextures(1, intArrayOf(state.textureId), 0)
            }
            meshStates.clear()
            if (fbo != 0) {
                GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            }
            if (fboTexture != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(fboTexture), 0)
            }
            if (mainProgram != 0) {
                GLES30.glDeleteProgram(mainProgram)
            }
            if (quadProgram != 0) {
                GLES30.glDeleteProgram(quadProgram)
            }
            if (clearArtwork) {
                retainedAlbum?.takeUnless { it.isRecycled }?.recycle()
                retainedAlbum = null
                pendingAlbum = null
                retainedAlbumKey = null
                pendingAlbumKey = null
            }
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) {
            Timber.tag(TAG).e("着色器组件编译失败，取消程序创建")
            return 0
        }

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            // 关键捕获：链接错误信息（如果网格属性和着色器in/out没对上，Mali会死在这里）
            Timber.tag(TAG).e("程序链接失败: ${GLES30.glGetProgramInfoLog(program)}")
            GLES30.glDeleteProgram(program)
            return 0
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return program
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            // 关键捕获：修正 API 后的 Shader 编译期日志
            val shaderTypeStr = if (type == GLES30.GL_VERTEX_SHADER) "顶点" else "片元"
            Timber.tag(TAG).e("$shaderTypeStr 着色器编译失败: ${GLES30.glGetShaderInfoLog(shader)}")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
internal class MeshBackgroundView(context: Context) : GLSurfaceView(context), MeshBackgroundControl {

    private val renderer = MeshGradientRenderer()
    private var submittedAlbum: Bitmap? = null
    private var submittedAlbumKey: String? = null
    override var onAlbumRendered: ((String) -> Unit)? = null
    private var appliedFlowSpeed = renderer.flowSpeed
    private var appliedRenderScale = renderer.renderScale
    private var appliedSubdivision = renderer.subdivision
    private var appliedStaticMode = false
    private var appliedPlaying = true

    init {
        renderer.onAlbumRendered = { key -> post { onAlbumRendered?.invoke(key) } }
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) =
                renderer.onSurfaceCreated(gl, config)

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) =
                renderer.onSurfaceChanged(gl, width, height)

            override fun onDrawFrame(gl: GL10?) {
                renderer.onDrawFrame(gl)
                if (renderer.needsAnotherFrame) requestRender()
            }
        })
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun setAlbum(bitmap: Bitmap, key: String) {
        if (bitmap.isRecycled || (submittedAlbum === bitmap && submittedAlbumKey == key)) return
        val owned = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        submittedAlbum = bitmap
        submittedAlbumKey = key
        updateRenderer { renderer.setAlbum(owned, key) }
    }

    override fun setFlowSpeed(speed: Float) {
        if (appliedFlowSpeed == speed) return
        appliedFlowSpeed = speed
        updateRenderer { renderer.flowSpeed = speed }
    }

    override fun setRenderScale(scale: Float) {
        if (appliedRenderScale == scale) return
        appliedRenderScale = scale
        updateRenderer {
            renderer.renderScale = scale
            renderer.rebuildFbo()
        }
    }

    override fun setSubdivision(level: Int) {
        if (appliedSubdivision == level) return
        appliedSubdivision = level
        updateRenderer { renderer.subdivision = level }
    }

    override fun setStaticMode(enable: Boolean) {
        if (appliedStaticMode == enable) return
        appliedStaticMode = enable
        updateRenderer { renderer.setStaticMode(enable) }
    }

    override fun setPlaying(playing: Boolean) {
        if (appliedPlaying == playing) return
        appliedPlaying = playing
        updateRenderer { renderer.setPlaying(playing) }
    }

    private fun updateRenderer(update: () -> Unit) {
        queueEvent {
            update()
            requestRender()
        }
    }

    override fun onDetachedFromWindow() {
        submittedAlbum = null
        super.onDetachedFromWindow()
        queueEvent { renderer.release() }
    }
}
