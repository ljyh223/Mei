package com.ljyh.mei.ui.component.player.component.mesh

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.sin

data class ControlPoint(
    var x: Float,
    var y: Float,
    var r: Float = 0f,
    var g: Float = 0f,
    var b: Float = 0f,
    var ur: Float = 0f,
    var vr: Float = 0f,
    var up: Float = 1f,
    var vp: Float = 1f
) {
    val uTangent: FloatArray
        get() = floatArrayOf(
            up * cos(ur),
            up * sin(ur)
        )

    val vTangent: FloatArray
        get() = floatArrayOf(
            -vp * sin(vr),
            vp * cos(vr)
        )
}

private val H = floatArrayOf(
    2f, -2f, 1f, 1f,
    -3f, 3f, -2f, -1f,
    0f, 0f, 1f, 0f,
    1f, 0f, 0f, 0f
)

private val H_T = FloatArray(16).also {
    for (i in 0 until 4) {
        for (j in 0 until 4) {
            it[i * 4 + j] = H[j * 4 + i]
        }
    }
}

private fun mat4Mul(out: FloatArray, a: FloatArray, b: FloatArray) {
    for (i in 0 until 4) {
        for (j in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += a[i + k * 4] * b[k + j * 4]
            }
            out[i + j * 4] = sum
        }
    }
}

private fun mat4Transpose(out: FloatArray, m: FloatArray) {
    for (i in 0 until 4) {
        for (j in 0 until 4) {
            out[i * 4 + j] = m[j * 4 + i]
        }
    }
}

class BHPMesh(
    var controlPointWidth: Int = 5,
    var controlPointHeight: Int = 5
) {
    var subdivisionLevel: Int = 20
        private set

    private var controlPoints: Array<Array<ControlPoint>> = Array(controlPointHeight) { row ->
        Array(controlPointWidth) { col ->
            val baseX = if (controlPointWidth == 1) 0f else col.toFloat() / (controlPointWidth - 1) * 2f - 1f
            val baseY = if (controlPointHeight == 1) 0f else row.toFloat() / (controlPointHeight - 1) * 2f - 1f
            ControlPoint(baseX, baseY)
        }
    }

    private var vertexData: FloatBuffer? = null
    private var indexData: IntBuffer? = null
    private var indexCount: Int = 0

    val buffer: FloatBuffer?
        get() = vertexData

    val indices: Int
        get() = indexCount

    fun resetSubdivision(n: Int) {
        subdivisionLevel = n.coerceIn(4, 50)
    }

    fun resizeControlPoints(w: Int, h: Int) {
        controlPointWidth = w
        controlPointHeight = h
        controlPoints = Array(h) { row ->
            Array(w) { col ->
                val baseX = if (w == 1) 0f else col.toFloat() / (w - 1) * 2f - 1f
                val baseY = if (h == 1) 0f else row.toFloat() / (h - 1) * 2f - 1f
                ControlPoint(baseX, baseY)
            }
        }
    }

    fun getControlPoint(i: Int, j: Int): ControlPoint {
        return controlPoints[j.coerceIn(0, controlPointHeight - 1)][i.coerceIn(0, controlPointWidth - 1)]
    }

    fun configureFromPreset(preset: ControlPointPreset, albumTexture: Bitmap) {
        resizeControlPoints(preset.width, preset.height)
        val uPower = 2f / (preset.width - 1)
        val vPower = 2f / (preset.height - 1)
        for (cpConf in preset.conf) {
            val cp = getControlPoint(cpConf.cx, cpConf.cy)
            cp.x = cpConf.x
            cp.y = cpConf.y
            cp.ur = cpConf.ur
            cp.vr = cpConf.vr
            cp.up = uPower * cpConf.up
            cp.vp = vPower * cpConf.vp
            val color = sampleColorFromBitmap(albumTexture, cpConf.x, cpConf.y)
            cp.r = color[0]
            cp.g = color[1]
            cp.b = color[2]
        }
        updateMesh()
    }

    private fun sampleColorFromBitmap(bitmap: Bitmap, x: Float, y: Float): FloatArray {
        val u = (x + 1f) / 2f
        val v = (y + 1f) / 2f
        val px = (u * (bitmap.width - 1)).toInt().coerceIn(0, bitmap.width - 1)
        val py = (v * (bitmap.height - 1)).toInt().coerceIn(0, bitmap.height - 1)
        val pixel = bitmap.getPixel(px, py)
        return floatArrayOf(
            ((pixel shr 16) and 0xFF) / 255f,
            ((pixel shr 8) and 0xFF) / 255f,
            (pixel and 0xFF) / 255f
        )
    }

    private fun fillMeshCoefficients(
        p00: ControlPoint, p01: ControlPoint,
        p10: ControlPoint, p11: ControlPoint,
        axIdx: Int,
        out: FloatArray
    ) {
        val (l00, l10, l01, l11) = when (axIdx) {
            0 -> Quad(p00.x, p10.x, p01.x, p11.x)
            1 -> Quad(p00.y, p10.y, p01.y, p11.y)
            2 -> Quad(p00.r, p10.r, p01.r, p11.r)
            3 -> Quad(p00.g, p10.g, p01.g, p11.g)
            4 -> Quad(p00.b, p10.b, p01.b, p11.b)
            else -> Quad(p00.x, p10.x, p01.x, p11.x)
        }
        out[0] = l00; out[1] = l01; out[4] = l10; out[5] = l11
        if (axIdx <= 1) {
            val v00 = p00.vTangent; val v10 = p10.vTangent
            val v01 = p01.vTangent; val v11 = p11.vTangent
            val u00 = p00.uTangent; val u10 = p10.uTangent
            val u01 = p01.uTangent; val u11 = p11.uTangent
            out[2] = v00[axIdx]; out[3] = v01[axIdx]
            out[6] = v10[axIdx]; out[7] = v11[axIdx]
            out[8] = u00[axIdx]; out[9] = u01[axIdx]
            out[12] = u10[axIdx]; out[13] = u11[axIdx]
            out[10] = 0f; out[11] = 0f; out[14] = 0f; out[15] = 0f
        } else {
            out[2] = l01 - l00; out[3] = l11 - l10
            out[6] = l11 - l10; out[7] = l01 - l00
            out[8] = 0f; out[9] = 0f; out[12] = 0f; out[13] = 0f
            out[10] = 0f; out[11] = 0f; out[14] = 0f; out[15] = 0f
        }
    }

    private fun precomputeMatrix(m: FloatArray, out: FloatArray) {
        val mT = FloatArray(16)
        mat4Transpose(mT, m)
        val temp = FloatArray(16)
        mat4Mul(temp, mT, H)
        mat4Mul(out, H_T, temp)
    }

    fun updateMesh() {
        val n = subdivisionLevel
        val cpW = controlPointWidth
        val cpH = controlPointHeight
        val gridW = (cpW - 1) * n
        val gridH = (cpH - 1) * n

        require(gridW * gridH > 0) { "Grid size must be positive" }

        val floatCount = gridW * gridH * 7
        val vBuf = ByteBuffer.allocateDirect(floatCount * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val subDivM1 = (n - 1).coerceAtLeast(1)
        val invTH = 1f / (subDivM1 * (cpW - 1))
        val invTW = 1f / (subDivM1 * (cpH - 1))

        val tempM = FloatArray(16)
        val accX = FloatArray(16)
        val accY = FloatArray(16)
        val accR = FloatArray(16)
        val accG = FloatArray(16)
        val accB = FloatArray(16)

        for (patchY in 0 until (cpH - 1)) {
            for (patchX in 0 until (cpW - 1)) {
                val p00 = getControlPoint(patchX, patchY)
                val p10 = getControlPoint(patchX + 1, patchY)
                val p01 = getControlPoint(patchX, patchY + 1)
                val p11 = getControlPoint(patchX + 1, patchY + 1)

                fillMeshCoefficients(p00, p01, p10, p11, 0, tempM)
                precomputeMatrix(tempM, accX)
                fillMeshCoefficients(p00, p01, p10, p11, 1, tempM)
                precomputeMatrix(tempM, accY)
                fillMeshCoefficients(p00, p01, p10, p11, 2, tempM)
                precomputeMatrix(tempM, accR)
                fillMeshCoefficients(p00, p01, p10, p11, 3, tempM)
                precomputeMatrix(tempM, accG)
                fillMeshCoefficients(p00, p01, p10, p11, 4, tempM)
                precomputeMatrix(tempM, accB)

                val sX = patchX.toFloat() / (cpW - 1)
                val sY = patchY.toFloat() / (cpH - 1)

                for (u in 0 until n) {
                    val uNorm = if (n > 1) u.toFloat() / subDivM1 else 0f
                    val uPow = floatArrayOf(uNorm * uNorm * uNorm, uNorm * uNorm, uNorm, 1f)

                    val ux = FloatArray(4)
                    val uy = FloatArray(4)
                    val ur = FloatArray(4)
                    val ug = FloatArray(4)
                    val ub = FloatArray(4)

                    transformVec4Mat4(ux, uPow, accX)
                    transformVec4Mat4(uy, uPow, accY)
                    transformVec4Mat4(ur, uPow, accR)
                    transformVec4Mat4(ug, uPow, accG)
                    transformVec4Mat4(ub, uPow, accB)

                    for (v in 0 until n) {
                        val vNorm = if (n > 1) v.toFloat() / subDivM1 else 0f
                        val vPow = floatArrayOf(vNorm * vNorm * vNorm, vNorm * vNorm, vNorm, 1f)

                        val px = dot4(vPow, ux)
                        val py = dot4(vPow, uy)
                        val pr = dot4(vPow, ur)
                        val pg = dot4(vPow, ug)
                        val pb = dot4(vPow, ub)

                        val uvU = sX + v * invTH
                        val uvV = 1f - sY - u * invTW

                        val row = patchY * n + u
                        val col = patchX * n + v
                        val idx = (row * gridW + col) * 7

                        vBuf.put(idx, px)
                        vBuf.put(idx + 1, py)
                        vBuf.put(idx + 2, pr.coerceIn(0f, 1f))
                        vBuf.put(idx + 3, pg.coerceIn(0f, 1f))
                        vBuf.put(idx + 4, pb.coerceIn(0f, 1f))
                        vBuf.put(idx + 5, uvU.coerceIn(0f, 1f))
                        vBuf.put(idx + 6, uvV.coerceIn(0f, 1f))
                    }
                }
            }
        }

        vertexData = vBuf

        val numIndices = (gridW - 1) * (gridH - 1) * 6
        indexCount = numIndices
        val idxArr = IntArray(numIndices)
        var ii = 0
        for (j in 0 until (gridH - 1)) {
            for (i in 0 until (gridW - 1)) {
                val tl = j * gridW + i
                val tr = j * gridW + i + 1
                val bl = (j + 1) * gridW + i
                val br = (j + 1) * gridW + i + 1
                idxArr[ii++] = tl; idxArr[ii++] = tr; idxArr[ii++] = bl
                idxArr[ii++] = tr; idxArr[ii++] = br; idxArr[ii++] = bl
            }
        }

        val iBuf = ByteBuffer.allocateDirect(idxArr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        iBuf.put(idxArr)
        iBuf.position(0)
        indexData = iBuf
    }

    fun generateIndexBuffer(): IntBuffer {
        return indexData ?: ByteBuffer.allocateDirect(0).asIntBuffer()
    }

    private fun transformVec4Mat4(out: FloatArray, a: FloatArray, m: FloatArray) {
        for (i in 0 until 4) {
            out[i] = a[0] * m[i] + a[1] * m[4 + i] + a[2] * m[8 + i] + a[3] * m[12 + i]
        }
    }

    private fun dot4(a: FloatArray, b: FloatArray): Float {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]
    }

    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)
}