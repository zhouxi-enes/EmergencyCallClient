package org.enes.lanvideocall.utils.video;

public class X264Util {

    static {
        System.loadLibrary("x264_encoder");
    }

    public static native void initYUVTool(int width, int height,int rotate);

    public static native int encode_a_nv12_frame(byte[] y_data, byte[] uv_data,
                                                 byte[] return_data_buffer);

    public static native void clean_env();

}
