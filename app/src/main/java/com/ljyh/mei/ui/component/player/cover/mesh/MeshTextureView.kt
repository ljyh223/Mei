package com.ljyh.mei.ui.component.player.cover.mesh

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/** Controls shared by the existing SurfaceView and the composited TextureView host. */
internal interface MeshBackgroundControl {
    var onAlbumRendered: ((String) -> Unit)?
    fun setAlbum(bitmap: Bitmap, key: String)
    fun setFlowSpeed(speed: Float)
    fun setRenderScale(scale: Float)
    fun setSubdivision(level: Int)
    fun setStaticMode(enable: Boolean)
    fun setPlaying(playing: Boolean)
    fun dispose() {}
}

private data class MeshTextureSettings(
    val speed: Float = .25f,
    val scale: Float = .75f,
    val subdivision: Int = 50,
    val static: Boolean = false,
    val playing: Boolean = true,
)

/** Same mesh renderer and artwork cache; the texture participates in parent clipping and alpha. */
internal class MeshTextureView(context: Context) : TextureView(context),
    TextureView.SurfaceTextureListener, MeshBackgroundControl {
    override var onAlbumRendered: ((String) -> Unit)? = null
    private var settings = MeshTextureSettings()
    private var album: Pair<Bitmap, String>? = null
    private var session: TextureMeshSession? = null
    private var generation = 0
    private var disposed = false

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        if (disposed) return
        val current = ++generation
        session = TextureMeshSession(texture, width, height, settings) { key ->
            post { if (generation == current) onAlbumRendered?.invoke(key) }
        }.also { target ->
            album?.let { target.album(it.first, it.second) }
            target.setVisible(windowVisibility == VISIBLE)
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        session?.resize(width, height)
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        generation++
        val previous = session ?: return true
        session = null
        // The render thread releases the texture after EGL is finished with it; never join on UI.
        previous.close()
        return false
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        session?.setVisible(visibility == VISIBLE && !disposed)
    }

    override fun setAlbum(bitmap: Bitmap, key: String) {
        if (bitmap.isRecycled || (album?.first === bitmap && album?.second == key)) return
        album = bitmap to key
        session?.album(bitmap, key)
    }

    private fun configure(next: MeshTextureSettings) {
        if (settings == next) return
        settings = next
        session?.configure(next)
    }
    override fun setFlowSpeed(speed: Float) = configure(settings.copy(speed = speed))
    override fun setRenderScale(scale: Float) = configure(settings.copy(scale = scale))
    override fun setSubdivision(level: Int) = configure(settings.copy(subdivision = level))
    override fun setStaticMode(enable: Boolean) = configure(settings.copy(static = enable))
    override fun setPlaying(playing: Boolean) = configure(settings.copy(playing = playing))

    override fun dispose() {
        disposed = true
        generation++
        onAlbumRendered = null
        album = null
        session?.stop()
        // TextureView invokes onSurfaceTextureDestroyed during detach, which owns EGL cleanup.
    }
}

/** EGL and all renderer mutations belong to this session's Looper, including final cleanup. */
private class TextureMeshSession(
    private val texture: SurfaceTexture,
    width: Int,
    height: Int,
    initialSettings: MeshTextureSettings,
    private val onRendered: (String) -> Unit,
) {
    private val thread = HandlerThread("MeiMeshTexture").apply { start() }
    private val handler = Handler(thread.looper)
    private val closing = AtomicBoolean(false)
    private val renderer = MeshGradientRenderer()
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var window: EGLSurface = EGL14.EGL_NO_SURFACE
    private var surface: Surface? = null
    private var clock: Choreographer? = null
    private var ready = false
    private var running = true
    private var pendingKey: String? = null
    private var frameScheduled = false

    private val drawFrame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameScheduled = false
            if (closing.get() || !running || !ready) return
            try {
                renderer.onDrawFrame(null)
                check(EGL14.eglSwapBuffers(display, window)) { "Mesh texture swap failed" }
                // Readiness is published only after the first album frame has been submitted.
                pendingKey?.let { pendingKey = null; onRendered(it) }
                if (renderer.needsAnotherFrame) requestFrame()
            } catch (error: Exception) {
                running = false
                Timber.tag("MeshTextureView").e(error, "Mesh rendering stopped")
                releaseEgl()
            }
        }
    }

    init {
        handler.post {
            if (!closing.get()) try {
                openEgl()
                renderer.onAlbumRendered = { pendingKey = it }
                renderer.flowSpeed = initialSettings.speed
                renderer.renderScale = initialSettings.scale
                renderer.subdivision = initialSettings.subdivision
                renderer.onSurfaceCreated(null, null)
                renderer.onSurfaceChanged(null, width.coerceAtLeast(1), height.coerceAtLeast(1))
                renderer.setStaticMode(initialSettings.static)
                renderer.setPlaying(initialSettings.playing)
                ready = true
                clock = Choreographer.getInstance()
                requestFrame()
            } catch (error: Exception) {
                running = false
                Timber.tag("MeshTextureView").e(error, "Unable to initialize mesh texture")
                releaseEgl()
            }
        }
    }

    fun album(bitmap: Bitmap, key: String) {
        if (closing.get() || bitmap.isRecycled) return
        val owned = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        if (!handler.post {
            if (ready && !closing.get()) {
                renderer.setAlbum(owned, key)
                requestFrame()
            } else owned.recycle()
        }) owned.recycle()
    }

    fun configure(settings: MeshTextureSettings) {
        if (!closing.get()) handler.post {
            if (ready && !closing.get()) {
                renderer.flowSpeed = settings.speed
                renderer.subdivision = settings.subdivision
                if (renderer.renderScale != settings.scale) {
                    renderer.renderScale = settings.scale
                    renderer.rebuildFbo()
                }
                renderer.setStaticMode(settings.static)
                renderer.setPlaying(settings.playing)
                requestFrame()
            }
        }
    }

    fun resize(width: Int, height: Int) {
        if (!closing.get()) handler.post {
            if (ready && !closing.get()) {
                renderer.onSurfaceChanged(null, width.coerceAtLeast(1), height.coerceAtLeast(1))
                requestFrame()
            }
        }
    }

    fun setVisible(visible: Boolean) {
        if (!closing.get()) handler.post {
            if (!closing.get()) {
                val resume = visible && !running && ready
                running = visible
                if (!visible) {
                    clock?.removeFrameCallback(drawFrame)
                    frameScheduled = false
                } else if (resume) {
                    renderer.resetFrameTime()
                    requestFrame()
                }
            }
        }
    }

    // Called only on the render Looper. Coalesce settings/album/resize wake-ups into one frame.
    private fun requestFrame() {
        if (closing.get() || !running || !ready || frameScheduled) return
        val choreographer = clock ?: return
        frameScheduled = true
        choreographer.postFrameCallback(drawFrame)
    }

    fun stop() = setVisible(false)

    fun close() {
        if (!closing.compareAndSet(false, true)) return
        handler.post {
            clock?.removeFrameCallback(drawFrame)
            releaseEgl()
            texture.release()
            thread.quitSafely()
        }
    }

    private fun openEgl() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY)
        check(EGL14.eglInitialize(display, null, 0, null, 0))
        val configurations = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, 0x40, // EGL_OPENGL_ES3_BIT_KHR
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_NONE,
        ), 0, configurations, 0, 1, count, 0) && count[0] > 0)
        val configuration = checkNotNull(configurations[0])
        context = EGL14.eglCreateContext(display, configuration, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
        check(context != EGL14.EGL_NO_CONTEXT)
        surface = Surface(texture)
        window = EGL14.eglCreateWindowSurface(display, configuration, surface,
            intArrayOf(EGL14.EGL_NONE), 0)
        check(window != EGL14.EGL_NO_SURFACE)
        check(EGL14.eglMakeCurrent(display, window, window, context))
        EGL14.eglSwapInterval(display, 1)
    }

    private fun releaseEgl() {
        running = false
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (context != EGL14.EGL_NO_CONTEXT) runCatching { renderer.release(clearArtwork = true) }
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (window != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, window)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
            EGL14.eglReleaseThread()
        }
        ready = false
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
        window = EGL14.EGL_NO_SURFACE
        surface?.release()
        surface = null
    }
}
