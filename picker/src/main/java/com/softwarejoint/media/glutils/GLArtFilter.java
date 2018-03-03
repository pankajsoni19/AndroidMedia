package com.softwarejoint.media.glutils;

/**
 * Artフィルター.
 * <p>
 * Created by shunfujiwara on 2015/08/11.
 */
@SuppressWarnings("WeakerAccess")
public class GLArtFilter extends GLDrawer2D {

    public static final String ART_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "float level( in float value, in float min, in float max ) {\n" +
            "     return min / 255.0 + ( max - min ) * value / 255.0;\n" +
            "}\n" +
            "\n" +
            "float gamma( in float value, in float g ) {\n" +
            "     return pow( value, 1.0 / g );\n" +
            "}\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "     vec4 color = texture2D( inputImageTexture, textureCoordinate );\n" +
            "     float r = color.r;\n" +
            "     float g = color.g;\n" +
            "     float b = color.b;\n" +
            "     r = level( r, 0.0, 255.0 ); \n" +
            "     g = level( g, 0.0, 184.0 ); \n" +
            "     b = level( b, 0.0, 113.0 ); \n" +
            "     r = gamma( r, 1.10 ); \n" +
            "     g = gamma( g, 0.95 ); \n" +
            "     b = gamma( b, 1.04 ); \n" +
            "     r = level( r, 10.0, 240.0 ); \n" +
            "     g = level( g, 10.0, 240.0 ); \n" +
            "     b = level( b, 10.0, 240.0 ); \n" +
            "     r = gamma( r, 0.87 ); \n" +
            "     g = gamma( g, 0.87 ); \n" +
            "     b = gamma( b, 0.87 ); \n" +
            "     float yL = .2126 * color.r + .7152 * color.g + .0722 * color.b;\n" +
            "     r += yL; g += yL; b += yL;\n" +
            "     gl_FragColor = vec4( r, g, b, color.a );\n" +
            "}";

    public GLArtFilter() {
        super(NO_FILTER_VERTEX_SHADER, ART_FRAGMENT_SHADER);
    }
}