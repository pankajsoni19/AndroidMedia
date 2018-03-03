package com.softwarejoint.media.glutils;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: GLDrawer2D.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
@SuppressWarnings("WeakerAccess")
public class GLDrawer2D {

    public static final int NO_TEXTURE = -1;

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

    private static final String TAG = "GLDrawer2D";
    private static final float[] VERTICES = {1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f};
    private static final float[] TEXCOORD = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
    private static final int FLOAT_SZ = Float.SIZE / 8;
    private static final int VERTEX_NUM = 4;
    private static final int VERTEX_SZ = VERTEX_NUM * 2;

    private final float[] mMvpMatrix = new float[16];
    private final LinkedList<Runnable> mRunOnDraw = new LinkedList<>();

    int mGLAttribPosition;
    int mGLAttribTextureCoordinate;
    int muMVPMatrixLoc;
    int muTexMatrixLoc;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;
    private int mGLProgId;
    private String mVertexShader;
    private String mFragmentShader;

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

    /**
     * delete specific texture
     */
    public static void deleteTex(final int hTex) {
        Log.d(TAG, "deleteTex:");
        final int[] tex = new int[]{hTex};
        GLES20.glDeleteTextures(1, tex, 0);
        GLES20.glFlush();
    }

    /**
     * load, compile and link shader
     *
     * @param vss source of vertex shader
     * @param fss source of fragment shader
     */
    public static int loadShader(final String vss, final String fss) {
        Log.d(TAG, "loadShader:");

        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vs, vss);
        GLES20.glCompileShader(vs);
        final int[] compiled = new int[1];
        GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d(TAG, "Failed to compile vertex shader:" + GLES20.glGetShaderInfoLog(vs));
            GLES20.glDeleteShader(vs);
            vs = 0;
        }

        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fs, fss);
        GLES20.glCompileShader(fs);
        GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d(TAG, "Failed to compile fragment shader:" + GLES20.glGetShaderInfoLog(fs));
            GLES20.glDeleteShader(fs);
            fs = 0;
        }

        final int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        return program;
    }

    public int init() {
        mGLProgId = loadShader(mVertexShader, mFragmentShader);
        onInit(mGLProgId);
        return mGLProgId;
    }

    protected void onInit(int programId) {
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
    @Deprecated
    public void release() {
        if (mGLProgId >= 0) GLES20.glDeleteProgram(mGLProgId);
        mGLProgId = -1;
    }

    /**
     * terminating, this should be called in GL context
     */
    public void release(int programId) {
        if (programId >= 0) {
            GLES20.glDeleteProgram(programId);
        }
    }

    @Deprecated
    public int getProgram() {
        return mGLProgId;
    }

    /**
     * draw specific texture with specific texture matrix
     *
     * @param texId     texture ID
     * @param texMatrix texture matrix、if this is null, the last one use(we don't check size of this
     *                  array and needs at least 16 of float)
     */
    @Deprecated
    public void draw(final int texId, final float[] texMatrix) {
        GLES20.glUseProgram(mGLProgId);
        if (texMatrix != null) GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

    /**
     * draw specific texture with specific texture matrix
     *
     * @param programId program ID
     * @param texId     texture ID
     * @param texMatrix texture matrix、if this is null, the last one use(we don't check size of this
     *                  array and needs at least 16 of float)
     */
    public void draw(final int programId, final int texId, final float[] texMatrix) {
        GLES20.glUseProgram(programId);
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
    public void setMatrix(final float[] matrix, final int offset) {
        if ((matrix != null) && (matrix.length >= offset + 16)) {
            System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
        } else {
            Matrix.setIdentityM(mMvpMatrix, 0);
        }
    }
}