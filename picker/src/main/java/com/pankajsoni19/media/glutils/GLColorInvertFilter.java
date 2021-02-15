package com.pankajsoni19.media.glutils;

/**
 * Invertフィルター.
 * <p>
 * Created by u1aryz on 2015/08/11.
 */
@SuppressWarnings("WeakerAccess")
public class GLColorInvertFilter extends GLDrawer2D {

    public static final String COLOR_INVERT_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    \n" +
            "    gl_FragColor = vec4((1.0 - textureColor.rgb), textureColor.w);\n" +
            "}";

    public GLColorInvertFilter() {
        super(NO_FILTER_VERTEX_SHADER, COLOR_INVERT_FRAGMENT_SHADER);
    }
}