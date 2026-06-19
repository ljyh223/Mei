package com.ljyh.mei.ui.component.player.component.mesh

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos

private const val TAG = "MeshGradientRenderer"

class MeshGradientRenderer(private val view: GLSurfaceView) : GLSurfaceView.Renderer {
    private data class MeshState(
        val mesh: BHPMesh,
        val textureId: Int,
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

    private var accumulatedPlayingNanos: Long = 0L
    private var lastFrameNanos: Long = System.nanoTime()

    @Volatile
    private var isPlaying: Boolean = true

    @Volatile
    var volume: Float = 0f

    @Volatile
    var flowSpeed: Float = 0.25f

    @Volatile
    var renderScale: Float = 0.75f

    @Volatile
    var subdivision: Int = 50

    private var staticMode: Boolean = false
    private var isStatic: Boolean = false

    private var pendingAlbum: Bitmap? = null
    private var albumChanged: Boolean = false

    private val random = java.util.Random()

    private val quadBuffer: FloatBuffer by lazy {
        val quadData = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )
        ByteBuffer.allocateDirect(quadData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quadData).position(0) }
    }

    fun setAlbum(bitmap: Bitmap) {
        synchronized(this) {
            val old = pendingAlbum
            if (old !== null && old !== bitmap) {
                old.recycle()
            }
            pendingAlbum = bitmap
            albumChanged = true
            
            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        // 【终极核武修复 2】：清屏颜色设置为全透明 (0f,0f,0f,0f)，彻底消灭由于没画图而暴露出的默认黑底！
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        mainProgram = createProgram(ShaderSource.MESH_VERTEX_SHADER, ShaderSource.MESH_FRAGMENT_SHADER)
        quadProgram = createProgram(ShaderSource.QUAD_VERTEX_SHADER, ShaderSource.QUAD_FRAGMENT_SHADER)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        rebuildFbo()
    }

    fun rebuildFbo() {
        scaledWidth = maxOf(1, (viewWidth * renderScale).toInt())
        scaledHeight = maxOf(1, (viewHeight * renderScale).toInt())
        createFbo(scaledWidth, scaledHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        processPendingAlbum()

        if (meshStates.isEmpty() || fbo == 0) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glClearColor(0f, 0f, 0f, 0f) // 保持全透明
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            return
        }

        if (staticMode && isStatic) return

        val now = System.nanoTime()
        val playing = isPlaying
        val frameDelta = now - lastFrameNanos
        lastFrameNanos = now
        if (playing) {
            accumulatedPlayingNanos += frameDelta
        }
        val time = accumulatedPlayingNanos / 1e9f * flowSpeed

        updateMeshStates(1f / 60f)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glClearColor(0f, 0f, 0f, 0f) // 保持全透明
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

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun processPendingAlbum() {
        synchronized(this) {
            if (!albumChanged) return
            albumChanged = false
            val bitmap = pendingAlbum ?: return
            pendingAlbum = null

            val processed = AlbumTextureProcessor.process(bitmap)
            val textureId = uploadTexture(processed)

            val preset = selectPreset()
            val mesh = BHPMesh(preset.width, preset.height)
            mesh.resetSubdivision(subdivision)
            mesh.configureFromPreset(preset, processed)

            processed.recycle()

            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
            
            val newState = MeshState(mesh, textureId, 0f, 1f)
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

        if (staticMode && meshStates.size == 1 && meshStates[0].alpha >= 1f) {
            if (!isStatic) {
                isStatic = true
                view.post { view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY }
            }
        }
    }

    fun setStaticMode(enable: Boolean) {
        staticMode = enable
        if (!enable) {
            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
        }
    }

    fun setPlaying(playing: Boolean) {
        if (!isPlaying && playing) {
            lastFrameNanos = System.nanoTime()
        }
        isPlaying = playing
        if (playing) {
            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
        }
    }

    private fun easeInOutSine(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return (1f - cos(clamped * Math.PI.toFloat())) / 2f
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texIds[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_MIRRORED_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_MIRRORED_REPEAT)
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
        val uVolume = GLES30.glGetUniformLocation(mainProgram, "u_volume")
        val uAspect = GLES30.glGetUniformLocation(mainProgram, "u_aspect")

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, state.textureId)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniform1f(uTime, time)
        GLES30.glUniform1f(uVolume, volume)
        GLES30.glUniform1f(uAspect, if (scaledHeight > 0) scaledWidth.toFloat() / scaledHeight else 1f)

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

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indices, GLES30.GL_UNSIGNED_INT, indexBuffer)

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
        GLES30.glEnableVertexAttribArray(aPos)
        quadBuffer.position(0)
        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)

        GLES30.glEnableVertexAttribArray(aTexCoord)
        quadBuffer.position(2)
        GLES30.glVertexAttribPointer(aTexCoord, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)

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
        
        // 【终极核武修复 3】：还原回 GL_RGBA。天玑驱动对 NPOT (非2次幂) 尺寸的 GL_RGBA8 有时会抛出校验异常。
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        
        // 关键绑定：Clamp
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTexture, 0)

        val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.tag(TAG).e("FBO 创建失败，状态码为: $fboStatus")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun release() {
        synchronized(this) {
            for (state in meshStates) {
                GLES30.glDeleteTextures(1, intArrayOf(state.textureId), 0)
            }
            meshStates.clear()
            if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            if (fboTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(fboTexture), 0)
            if (mainProgram != 0) GLES30.glDeleteProgram(mainProgram)
            if (quadProgram != 0) GLES30.glDeleteProgram(quadProgram)
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
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
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

class MeshBackgroundView(context: Context) : GLSurfaceView(context) {

    private val renderer = MeshGradientRenderer(this)

    init {
        setEGLContextClientVersion(3)
        // 【终极核武修复 4】：申请带有 16 位深度缓冲的 32 位真彩色 EGL 表面
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        // 【终极核武修复 5】：极其关键！允许 SurfaceView 背景透明（MediaOverlay），
        // 如果这里不设置，Android 16 渲染器垫底会是一片死黑，即使 GL 画出了透明度也什么都看不见！
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderMediaOverlay(true) 
        
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setAlbum(bitmap: Bitmap) {
        queueEvent { renderer.setAlbum(bitmap) }
    }

    fun updateVolume(v: Float) {
        renderer.volume = v
        if (renderMode == RENDERMODE_WHEN_DIRTY && v > 0.005f) {
            requestRender()
        }
    }

    fun setFlowSpeed(speed: Float) {
        renderer.flowSpeed = speed
    }

    fun setRenderScale(scale: Float) {
        renderer.renderScale = scale
        queueEvent { renderer.rebuildFbo() }
    }

    fun setSubdivision(level: Int) {
        renderer.subdivision = level
    }

    fun setStaticMode(enable: Boolean) {
        queueEvent { renderer.setStaticMode(enable) }
    }

    fun setPlaying(playing: Boolean) {
        renderer.setPlaying(playing)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        queueEvent { renderer.release() }
    }
}        viewWidth = width
        viewHeight = height
        rebuildFbo()
    }

    fun rebuildFbo() {
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

        // 【核心修复】：静止省电模式下绝不在方法最顶部进行阻断式 return。
        // 这会导致 Android 16 系统在用户下拉通知栏等操作重组时由于没有执行 Swap 导致退化为死黑。
        // 彻底改由下方的 WHEN_DIRTY 挂起机制，省电的同时绝对不丢帧、不黑屏。
        if (staticMode && isStatic) return

        val now = System.nanoTime()
        val playing = isPlaying
        val frameDelta = now - lastFrameNanos
        lastFrameNanos = now
        if (playing) {
            accumulatedPlayingNanos += frameDelta
        }
        val time = accumulatedPlayingNanos / 1e9f * flowSpeed

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

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun processPendingAlbum() {
        synchronized(this) {
            if (!albumChanged) return
            albumChanged = false
            val bitmap = pendingAlbum ?: return
            pendingAlbum = null

            val processed = AlbumTextureProcessor.process(bitmap)
            val textureId = uploadTexture(processed)

            val preset = selectPreset()
            val mesh = BHPMesh(preset.width, preset.height)
            mesh.resetSubdivision(subdivision)
            mesh.configureFromPreset(preset, processed)

            processed.recycle()

            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
            
            val newState = MeshState(mesh, textureId, 0f, 1f)
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

        // 【省电核心重构】：流体过渡淡入彻底稳定后，若是开启了静态模式，安全转为 WHEN_DIRTY，不再消耗任何开销。
        if (staticMode && meshStates.size == 1 && meshStates[0].alpha >= 1f) {
            if (!isStatic) {
                isStatic = true
                view.post { view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY }
            }
        }
    }

    fun setStaticMode(enable: Boolean) {
        staticMode = enable
        if (!enable) {
            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
        }
    }

    fun setPlaying(playing: Boolean) {
        if (!isPlaying && playing) {
            lastFrameNanos = System.nanoTime()
        }
        isPlaying = playing
        if (playing) {
            isStatic = false
            view.post { view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY }
        }
    }

    private fun easeInOutSine(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return (1f - cos(clamped * Math.PI.toFloat())) / 2f
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texIds[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_MIRRORED_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_MIRRORED_REPEAT)
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
        val uVolume = GLES30.glGetUniformLocation(mainProgram, "u_volume")
        val uAspect = GLES30.glGetUniformLocation(mainProgram, "u_aspect")

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, state.textureId)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniform1f(uTime, time)
        GLES30.glUniform1f(uVolume, volume)
        GLES30.glUniform1f(uAspect, if (scaledHeight > 0) scaledWidth.toFloat() / scaledHeight else 1f)

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

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indices, GLES30.GL_UNSIGNED_INT, indexBuffer)

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
        GLES30.glEnableVertexAttribArray(aPos)
        quadBuffer.position(0)
        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)

        GLES30.glEnableVertexAttribArray(aTexCoord)
        quadBuffer.position(2)
        GLES30.glVertexAttribPointer(aTexCoord, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)

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
        
        // 【核心修复一】：将原本非法的 unsized 格式 GL_RGBA 强制修改为 GLES30 核心规范强制标准的有尺寸内部格式 GL_RGBA8
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        
        // 【核心修复二】：针对屏幕非 2 次幂大小的 FBO 附着纹理（NPOT），必须将其环绕模式（Wrap）强制锁死为边缘截取，
        // 否则天玑 9300+ 的 Immortalis 硬件驱动会直接回吐不完整异常并将其重置为纯黑像素。
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTexture, 0)

        val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.tag(TAG).e("FBO 创建失败，状态码为: $fboStatus")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun release() {
        synchronized(this) {
            for (state in meshStates) {
                GLES30.glDeleteTextures(1, intArrayOf(state.textureId), 0)
            }
            meshStates.clear()
            if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            if (fboTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(fboTexture), 0)
            if (mainProgram != 0) GLES30.glDeleteProgram(mainProgram)
            if (quadProgram != 0) GLES30.glDeleteProgram(quadProgram)
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
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
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

class MeshBackgroundView(context: Context) : GLSurfaceView(context) {

    private val renderer = MeshGradientRenderer(this)

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        // 【核心修复三】：强制让底层 SurfaceHolder 的像素位宽支持 32位真彩色与混色。
        // 解决 Android 16 在多重重构的 Compose 界面树下窗口管理器无损合成失败导致的死黑。
        holder.setFormat(android.graphics.PixelFormat.RGBA_8888)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setAlbum(bitmap: Bitmap) {
        queueEvent { renderer.setAlbum(bitmap) }
    }

    fun updateVolume(v: Float) {
        renderer.volume = v
        // 挂起静止省电阶段，若捕捉到大鼓点和强烈音乐电平输入，主动唤醒并请求渲染当前这一帧，做到动效和省电两不误
        if (renderMode == RENDERMODE_WHEN_DIRTY && v > 0.005f) {
            requestRender()
        }
    }

    fun setFlowSpeed(speed: Float) {
        renderer.flowSpeed = speed
    }

    fun setRenderScale(scale: Float) {
        renderer.renderScale = scale
        queueEvent { renderer.rebuildFbo() }
    }

    fun setSubdivision(level: Int) {
        renderer.subdivision = level
    }

    fun setStaticMode(enable: Boolean) {
        queueEvent { renderer.setStaticMode(enable) }
    }

    fun setPlaying(playing: Boolean) {
        renderer.setPlaying(playing)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        queueEvent { renderer.release() }
    }
}
