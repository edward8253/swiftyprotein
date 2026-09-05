package com.swiftyprotein.ui.opengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.swiftyprotein.data.Atom
import com.swiftyprotein.data.Bond
import com.swiftyprotein.data.CpkColors
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class MoleculeGlRenderer(val context: Context) : GLSurfaceView.Renderer {

    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    // Gesture state
    var rotationX = 0f
    var rotationY = 0f
    var zoom = 20f
    var panX = 0f
    var panY = 0f

    private var centerX = 0f
    private var centerY = 0f
    private var centerZ = 0f

    fun resetState() {
        rotationX = 0f
        rotationY = 0f
        panX = 0f
        panY = 0f
        zoom = 20f
    }

    private var mProgram = 0
    private var mPositionHandle = 0
    private var mNormalHandle = 0
    private var mMVPMatrixHandle = 0
    private var mMVMatrixHandle = 0
    private var mColorHandle = 0
    private var mLightPosHandle = 0

    private lateinit var sphereVertices: FloatBuffer
    private lateinit var sphereNormals: FloatBuffer
    private lateinit var sphereIndices: ShortBuffer
    private var sphereIndexCount = 0

    private lateinit var cylinderVertices: FloatBuffer
    private lateinit var cylinderNormals: FloatBuffer
    private lateinit var cylinderIndices: ShortBuffer
    private var cylinderIndexCount = 0

    var atoms = listOf<Atom>()
        set(value) {
            field = value
            calculateCenter()
        }
    var bonds = listOf<Bond>()

    private fun calculateCenter() {
        if (atoms.isEmpty()) {
            centerX = 0f
            centerY = 0f
            centerZ = 0f
            zoom = 20f
            return
        }
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        atoms.forEach {
            if (it.x < minX) minX = it.x
            if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y
            if (it.y > maxY) maxY = it.y
            if (it.z < minZ) minZ = it.z
            if (it.z > maxZ) maxZ = it.z
        }

        centerX = (minX + maxX) / 2f
        centerY = (minY + maxY) / 2f
        centerZ = (minZ + maxZ) / 2f

        val deltaX = maxX - minX
        val deltaY = maxY - minY
        val deltaZ = maxZ - minZ
        val maxDim = max(deltaX, max(deltaY, deltaZ))
        
        // Adjust zoom based on molecule size. 
        // frustum is -1..1, so we want the molecule to fit comfortably.
        zoom = max(5f, maxDim * 2f) 
    }
    
    // Callback for screenshot
    var screenshotCallback: ((android.graphics.Bitmap) -> Unit)? = null

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uMVMatrix;
        attribute vec4 vPosition;
        attribute vec3 vNormal;
        varying vec3 vViewPosition;
        varying vec3 vViewNormal;

        void main() {
            vViewPosition = vec3(uMVMatrix * vPosition);
            vViewNormal = vec3(uMVMatrix * vec4(vNormal, 0.0));
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        uniform vec3 uLightPos;
        varying vec3 vViewPosition;
        varying vec3 vViewNormal;

        void main() {
            vec3 normal = normalize(vViewNormal);
            vec3 lightDir = normalize(uLightPos - vViewPosition);
            float diff = max(dot(normal, lightDir), 0.0);
            
            vec3 ambient = 0.3 * vColor.rgb;
            vec3 diffuse = diff * vColor.rgb;
            
            gl_FragColor = vec4(ambient + diffuse, vColor.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal")
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix")
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPos")

        setupSphereGeometry()
        setupCylinderGeometry()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, zoom, 0f, 0f, 0f, 0f, 1.0f, 0f)
        Matrix.translateM(mViewMatrix, 0, panX, panY, 0f)
        Matrix.rotateM(mViewMatrix, 0, rotationX, 1.0f, 0f, 0f)
        Matrix.rotateM(mViewMatrix, 0, rotationY, 0f, 1.0f, 0f)

        GLES20.glUseProgram(mProgram)
        GLES20.glUniform3f(mLightPosHandle, 5f, 5f, 5f)

        // Draw atoms
        atoms.forEach { atom ->
            drawAtom(atom)
        }

        // Draw bonds
        bonds.forEach { bond ->
            drawBond(bond)
        }
        
        // Handle screenshot
        screenshotCallback?.let { callback ->
            val bitmap = captureScreenshot()
            callback(bitmap)
            screenshotCallback = null
        }
    }

    private fun drawAtom(atom: Atom) {
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, atom.x - centerX, atom.y - centerY, atom.z - centerZ)
        Matrix.scaleM(mModelMatrix, 0, 0.4f, 0.4f, 0.4f) // Adjust sphere size

        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mvMatrix, 0)

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0)

        val color = CpkColors.getColor(atom.element)
        GLES20.glUniform4f(mColorHandle, color.red, color.green, color.blue, color.alpha)

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, sphereVertices)

        GLES20.glEnableVertexAttribArray(mNormalHandle)
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, sphereNormals)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereIndexCount, GLES20.GL_UNSIGNED_SHORT, sphereIndices)
    }

    private fun drawBond(bond: Bond) {
        val a1 = atoms.getOrNull(bond.atom1Id) ?: return
        val a2 = atoms.getOrNull(bond.atom2Id) ?: return

        val dx = a2.x - a1.x
        val dy = a2.y - a1.y
        val dz = a2.z - a1.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, a1.x - centerX, a1.y - centerY, a1.z - centerZ)

        // Rotation to align with (dx, dy, dz)
        // Cylinder starts at (0,0,0) towards (0,0,1)
        val v1 = floatArrayOf(0f, 0f, 1f)
        val v2 = floatArrayOf(dx / distance, dy / distance, dz / distance)
        
        // Cross product for rotation axis
        val axis = floatArrayOf(
            v1[1] * v2[2] - v1[2] * v2[1],
            v1[2] * v2[0] - v1[0] * v2[2],
            v1[0] * v2[1] - v1[1] * v2[0]
        )
        val axisLen = sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2])
        if (axisLen > 0.0001f) {
            val angle = acos(v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]) * 180f / PI.toFloat()
            Matrix.rotateM(mModelMatrix, 0, angle, axis[0] / axisLen, axis[1] / axisLen, axis[2] / axisLen)
        } else if (v2[2] < 0) {
            Matrix.rotateM(mModelMatrix, 0, 180f, 0f, 1f, 0f)
        }

        Matrix.scaleM(mModelMatrix, 0, 0.15f, 0.15f, distance)

        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mvMatrix, 0)

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0)
        GLES20.glUniform4f(mColorHandle, 0.7f, 0.7f, 0.7f, 1f) // Grey bonds

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cylinderVertices)

        GLES20.glEnableVertexAttribArray(mNormalHandle)
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, cylinderNormals)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cylinderIndexCount, GLES20.GL_UNSIGNED_SHORT, cylinderIndices)
    }

    private fun setupSphereGeometry() {
        val segments = 16
        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        for (i in 0..segments) {
            val lat = PI * i / segments
            val sinLat = sin(lat).toFloat()
            val cosLat = cos(lat).toFloat()

            for (j in 0..segments) {
                val lon = 2 * PI * j / segments
                val sinLon = sin(lon).toFloat()
                val cosLon = cos(lon).toFloat()

                val x = cosLon * sinLat
                val y = cosLat
                val z = sinLon * sinLat

                vertexList.add(x); vertexList.add(y); vertexList.add(z)
                normalList.add(x); normalList.add(y); normalList.add(z)
            }
        }

        for (i in 0 until segments) {
            for (j in 0 until segments) {
                val first = (i * (segments + 1) + j).toShort()
                val second = (first + segments + 1).toShort()

                indexList.add(first); indexList.add(second); indexList.add((first + 1).toShort())
                indexList.add(second); indexList.add((second + 1).toShort()); indexList.add((first + 1).toShort())
            }
        }

        sphereVertices = createFloatBuffer(vertexList.toFloatArray())
        sphereNormals = createFloatBuffer(normalList.toFloatArray())
        sphereIndices = createShortBuffer(indexList.toShortArray())
        sphereIndexCount = indexList.size
    }

    private fun setupCylinderGeometry() {
        val segments = 12
        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        for (i in 0..segments) {
            val angle = 2 * PI * i / segments
            val x = cos(angle).toFloat()
            val y = sin(angle).toFloat()

            vertexList.add(x); vertexList.add(y); vertexList.add(0.0f)
            normalList.add(x); normalList.add(y); normalList.add(0.0f)

            vertexList.add(x); vertexList.add(y); vertexList.add(1.0f)
            normalList.add(x); normalList.add(y); normalList.add(0.0f)
        }

        for (i in 0 until segments) {
            val b1 = (i * 2).toShort()
            val t1 = (i * 2 + 1).toShort()
            val b2 = ((i + 1) * 2).toShort()
            val t2 = ((i + 1) * 2 + 1).toShort()

            indexList.add(b1); indexList.add(b2); indexList.add(t1)
            indexList.add(t1); indexList.add(b2); indexList.add(t2)
        }

        cylinderVertices = createFloatBuffer(vertexList.toFloatArray())
        cylinderNormals = createFloatBuffer(normalList.toFloatArray())
        cylinderIndices = createShortBuffer(indexList.toShortArray())
        cylinderIndexCount = indexList.size
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(array)
            position(0)
        }
    }

    private fun createShortBuffer(array: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(array.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            put(array)
            position(0)
        }
    }
    
    private fun captureScreenshot(): android.graphics.Bitmap {
        val w = 1024 // Or use current view width
        val h = 1024 // Or use current view height
        val pixelBuffer = ByteBuffer.allocateDirect(w * h * 4)
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)
        
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        pixelBuffer.rewind()
        bitmap.copyPixelsFromBuffer(pixelBuffer)
        
        // Vertical flip is needed as OpenGL coordinates start from bottom-left
        val matrix = android.graphics.Matrix()
        matrix.postScale(1f, -1f)
        return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    fun pickAtom(screenX: Float, screenY: Float, width: Int, height: Int): Atom? {
        // Simple ray-casting (simplified)
        // Convert screen coordinates to world coordinates
        // For each atom, check distance to the ray
        
        // For now, let's just return the closest atom to the center as a placeholder
        // or implement a basic projection-based check.
        
        // Real implementation:
        val viewInv = FloatArray(16)
        val projInv = FloatArray(16)
        Matrix.invertM(viewInv, 0, mViewMatrix, 0)
        Matrix.invertM(projInv, 0, mProjectionMatrix, 0)
        
        val x = (2.0f * screenX / width - 1.0f)
        val y = (1.0f - 2.0f * screenY / height)
        
        val nearPoint = floatArrayOf(x, y, -1f, 1f)
        val farPoint = floatArrayOf(x, y, 1f, 1f)
        
        val nearWorld = unproject(nearPoint, projInv, viewInv)
        val farWorld = unproject(farPoint, projInv, viewInv)
        
        val rayDir = floatArrayOf(
            farWorld[0] - nearWorld[0],
            farWorld[1] - nearWorld[1],
            farWorld[2] - nearWorld[2]
        )
        val rayLen = sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
        rayDir[0] /= rayLen; rayDir[1] /= rayLen; rayDir[2] /= rayLen
        
        var closestAtom: Atom? = null
        var minDistance = Float.MAX_VALUE
        
        atoms.forEach { atom ->
            val toAtom = floatArrayOf(atom.x - centerX - nearWorld[0], atom.y - centerY - nearWorld[1], atom.z - centerZ - nearWorld[2])
            val projection = toAtom[0] * rayDir[0] + toAtom[1] * rayDir[1] + toAtom[2] * rayDir[2]
            
            if (projection > 0) {
                val distSq = (toAtom[0] * toAtom[0] + toAtom[1] * toAtom[1] + toAtom[2] * toAtom[2]) - (projection * projection)
                if (distSq < 0.25f && distSq < minDistance) { // 0.5 radius squared
                    minDistance = distSq
                    closestAtom = atom
                }
            }
        }
        
        return closestAtom
    }
    
    private fun unproject(point: FloatArray, projInv: FloatArray, viewInv: FloatArray): FloatArray {
        val temp = FloatArray(4)
        Matrix.multiplyMV(temp, 0, projInv, 0, point, 0)
        temp[0] /= temp[3]; temp[1] /= temp[3]; temp[2] /= temp[3]; temp[3] = 1f
        
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, viewInv, 0, temp, 0)
        return result
    }
}
