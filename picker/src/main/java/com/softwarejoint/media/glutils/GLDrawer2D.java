package com.softwarejoint.media.glutils;
import android.graphics.Rect;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
@SuppressWarnings("WeakerAccess")
public class GLDrawer2D {

    private static String TAG = "GLDrawer2D";

    /**
     * create external texture
     *
     * @return texture ID
     */
    public static int initTex() {
        Log.d(TAG, "initTex:");

        final int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        return tex[0];
    }

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "attribute highp vec4 position;\n" +
            "attribute highp vec4 inputTextureCoordinate;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "void main() {\n" +
            "	   gl_Position = uMVPMatrix * position;\n" +
            "	   textureCoordinate = (uTexMatrix * inputTextureCoordinate).xy;\n" +
            "}\n";

    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private static final float[] VERTICES = {
            1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f
    };

    private static final float[] TEXCOORD = {
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f
    };

    private static final int FLOAT_SZ = Float.SIZE / 8;

    private static final int VERTEX_NUM = 4;
    private static final int VERTEX_SZ = VERTEX_NUM * 2;

    private final float[] mMvpMatrix = new float[16];

    int mGLAttribPosition;
    int mGLAttribTextureCoordinate;
    int muMVPMatrixLoc;
    int muTexMatrixLoc;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;
    private int mGLProgId = -1;
    private String mVertexShader;
    private String mFragmentShader;
    private Rect rect = new Rect();

    /**
     * Constructor
     * this should be called in GL context
     */
    public GLDrawer2D() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    /**
     * Constructor
     * this should be called in GL context
     */
    protected GLDrawer2D(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        pVertex.put(VERTICES);
        pVertex.flip();

        pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        pTexCoord.put(TEXCOORD);
        pTexCoord.flip();
    }

    /**
     * delete specific texture
     */
    public static void deleteTex(final int hTex) {
        Log.d(TAG, "deleteTex:");
        final int[] tex = new int[]{hTex};
        GLES20.glDeleteTextures(1, tex, 0);
        GLES20.glFlush();
    }

    public int init() {
        //Log.d(TAG, "init: " + mGLProgId);
        mGLProgId = createProgram(mVertexShader, mFragmentShader);
        onInit(mGLProgId);
        return mGLProgId;
    }

    public GLDrawer2D createCopy() {
        return new GLDrawer2D(mVertexShader, mFragmentShader);
    }

    private void onInit(int programId) {
        GLES20.glUseProgram(programId);
        mGLAttribPosition = GLES20.glGetAttribLocation(programId, "position");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(programId, "inputTextureCoordinate");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(programId, "uTexMatrix");

        Matrix.setIdentityM(mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    }

    /**
     * terminating, this should be called in GL context
     */
    public void release() {
        if (mGLProgId >= 0) GLES20.glDeleteProgram(mGLProgId);
        mGLProgId = -1;
    }

    /**
     * draw specific texture with specific texture matrix
     *
     * @param texId     texture ID
     * @param texMatrix texture matrix、if this is null, the last one use(we don't check size of this
     *                  array and needs at least 16 of float)
     */
    public void draw(final int texId, final float[] texMatrix) {
        draw(mGLProgId, texId, texMatrix);
    }

    private void draw(int mGLProgId, final int texId, final float[] texMatrix) {
        GLES20.glUseProgram(mGLProgId);
        if (texMatrix != null) GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);

        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

    protected void onDrawArraysPre() {
    }

    /**
     * Set model/view/projection transform matrix
     */
    @SuppressWarnings("SameParameterValue")
    public void setMatrix(final float[] matrix, final int offset) {
        if ((matrix != null) && (matrix.length >= offset + 16)) {
            System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
        } else {
            Matrix.setIdentityM(mMvpMatrix, 0);
        }
    }

    public void setRect(int startX, int startY, int width, int height) {
        rect.left = startX;
        rect.top = startY;
        rect.right = rect.left + width;
        rect.bottom = rect.top + height;
    }

    public int getStartX() {
        return rect.left;
    }

    public int getStartY() {
        return rect.top;
    }

    public int width() {
        return rect.width();
    }

    public int height() {
        return rect.height();
    }

    public Rect getRect() {
        return rect;
    }

    /**
     * Create program
     * */
    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("Could not compile shader " + shaderType + ":" + info);
            }
        }
        return shader;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                String info = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        return program;
    }

    @SuppressWarnings("SameParameterValue")
    private static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}