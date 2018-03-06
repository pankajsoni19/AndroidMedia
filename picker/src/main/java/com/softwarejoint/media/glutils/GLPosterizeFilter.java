package com.softwarejoint.media.glutils;

/**
 * ポスタライズフィルター.
 * <p>
 * Created by u1aryz on 2015/08/11.
 */
@SuppressWarnings("WeakerAccess")
public class GLPosterizeFilter extends GLDrawer2D {

    public static final String POSTERIZE_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "float colorLevels = 10.0;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "   highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   \n" +
            "   gl_FragColor = floor((textureColor * colorLevels) + vec4(0.5)) / colorLevels;\n" +
            "}";

    public GLPosterizeFilter() {
        super(NO_FILTER_VERTEX_SHADER, POSTERIZE_FRAGMENT_SHADER);
    }
}