package com.pankajsoni19.media.glutils;

/**
 * 1977フィルター.
 * <p>
 * Created by shunfujiwara on 2015/08/11.
 */
@SuppressWarnings("WeakerAccess")
public class GL1977Filter extends GLDrawer2D {

    public static final String FRAGMENT_1977_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec2 uv = textureCoordinate;\n" +
            "    \n" +
            "    vec4 color = texture2D(inputImageTexture, uv);\n" +
            "    \n" +
            "    color.r = 0.235741910343433 * color.r * color.r + 0.813975259606267 * color.r - 0.0296158073142616;\n"
            +
            "    \n" +
            "    color.g = 0.00784313725490196 + (8.59393872059068/(1.0+pow(pow(color.g/7.5209198891438, -2.83148960230052), 0.387341567967874)));\n"
            +
            "    \n" +
            "    color.b = 0.472243328919063 * color.b * color.b + 0.459102389209912 * color.b - 0.0332794379846503;\n"
            +
            "    \n" +
            "    gl_FragColor = vec4(color.rgb, 1.0);\n" +
            "}";

    public GL1977Filter() {
        super(NO_FILTER_VERTEX_SHADER, FRAGMENT_1977_SHADER);
    }
}