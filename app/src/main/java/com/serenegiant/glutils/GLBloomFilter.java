package com.serenegiant.glutils;

/**
 * Bloomフィルター.
 * <p>
 * Created by shunfujiwara on 2015/08/11.
 */
@SuppressWarnings("WeakerAccess")
public class GLBloomFilter extends GLDrawer2D {

    public static final String POSTERIZE_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "     vec4 sum = vec4(0);\n" +
            "     vec2 texcoord = textureCoordinate;\n" +
            "     \n" +
            "     for( int i= -4 ;i < 4; i++)\n" +
            "     {\n" +
            "         for ( int j = -3; j < 3; j++)\n" +
            "         {\n" +
            "             sum += texture2D(inputImageTexture, texcoord + vec2(j, i)*0.004) * 0.25;\n" +
            "         }\n" +
            "     }\n" +
            "     if (texture2D(inputImageTexture, texcoord).r < 0.3)\n" +
            "     {\n" +
            "         gl_FragColor = sum*sum*0.012 + texture2D(inputImageTexture, texcoord);\n" +
            "     }\n" +
            "     else\n" +
            "     {\n" +
            "         if (texture2D(inputImageTexture, texcoord).r < 0.5)\n" +
            "         {\n" +
            "             gl_FragColor = sum*sum*0.009 + texture2D(inputImageTexture, texcoord);\n" +
            "         }\n" +
            "         else\n" +
            "         {\n" +
            "             gl_FragColor = sum*sum*0.0075 + texture2D(inputImageTexture, texcoord);\n" +
            "         }\n" +
            "     }\n" +
            "}";

    public GLBloomFilter() {
        super(NO_FILTER_VERTEX_SHADER, POSTERIZE_FRAGMENT_SHADER);
    }
}