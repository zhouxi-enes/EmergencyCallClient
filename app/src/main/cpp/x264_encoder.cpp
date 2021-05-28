//
// Created by zhouxi on 2019-06-24.
//

#define JAVA_IN_ENCODE_TYPE X264_CSP_NV12
#define _90degree 90
#define _270degree 270

#include <jni.h>
#include <cstdlib>
#include <android/log.h>
#include <cstring>
#include "image_util_c.h"
#include "include/x264.h"

static bool encoder_can_use = false;

static x264_t * pHandle = nullptr;

static x264_param_t * pParam = nullptr;

x264_nal_t * pNals = nullptr;

int i_nal = 0;

static int video_width,video_height,video_rotate;

/**
 *
 */
void clean_mem() {
    if(encoder_can_use) {
        encoder_can_use = false;
        if (pHandle != nullptr) {
            x264_encoder_close(pHandle);
            pHandle = nullptr;
        }
        if (pParam != nullptr) {
            free(pParam);
            pParam = nullptr;
        }
    }
}

#define fps 15
void alloc_and_init_profile() {
    //alloc
    pParam = (x264_param_t *)malloc(sizeof(x264_param_t));
    //init
    int status = x264_param_default_preset(pParam,x264_preset_names[1],x264_tune_names[7]);
    if(status >= 0) {
        pParam->i_threads = 1;
        int screen_width = video_width;
        int screen_height = video_height;
        pParam->i_width = screen_width;
        pParam->i_height = screen_height;
        pParam->i_fps_num = fps;
        pParam->i_fps_den = 1;
        pParam->i_keyint_max = fps;
        pParam->b_intra_refresh = 1;
        pParam->rc.i_rc_method = X264_RC_CRF;
        pParam->rc.f_rf_constant = 25;
        pParam->rc.f_rf_constant_max = 45;
        pParam->b_repeat_headers = 1;
        pParam->b_annexb = 1;
        pParam->i_csp = JAVA_IN_ENCODE_TYPE;
        pParam->i_bframe = 0;
//        pParam->i_log_level = X264_LOG_DEBUG;
        status = x264_param_apply_profile(pParam, x264_profile_names[1]);
    }
}

void alloc_x264_t() {
    pHandle = x264_encoder_open(pParam);
}

void initTool(int width,int height, int rotate) {
    if(rotate == 0 || rotate == 180){
        video_width = width;
        video_height = height;
    }else if(rotate == _90degree) {
        video_width = height;
        video_height = width;
    }else if(rotate == _270degree) {
        video_width = height;
        video_height = width;
    }
    video_rotate = rotate;
    alloc_and_init_profile();
    alloc_x264_t();
    i_nal = 0;
    pNals = nullptr;
    encoder_can_use = true;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_enes_lanvideocall_utils_video_X264Util_initYUVTool(JNIEnv *env, jclass type, jint width,
                                                            jint height, jint rotate) {
    __android_log_print(ANDROID_LOG_ERROR,"jni","Java_org_enes_lanvideocall_utils_video_X264Util_initYUVTool");
    clean_mem();
    initTool(width,height,rotate);
}


extern "C"
JNIEXPORT jint JNICALL
Java_org_enes_lanvideocall_utils_video_X264Util_encode_1a_1nv12_1frame(JNIEnv *env, jclass type,
                                                                       jbyteArray y_data_,
                                                                       jbyteArray uv_data_,
                                                                       jbyteArray return_data_buffer_
                                                                       ) {
    jbyte *y_data = env->GetByteArrayElements(y_data_, nullptr);
    jbyte *uv_data = env->GetByteArrayElements(uv_data_, nullptr);
    bool is_release_jni_data = false;
    //
    int return_size = 0;
    if(encoder_can_use) {
        jsize y_data_length = env->GetArrayLength(y_data_);
        jsize uv_data_length = env->GetArrayLength(uv_data_);
        int y_data_length_int = y_data_length;
        int uv_data_length_int = uv_data_length;

        x264_picture_t pPic_in;
        x264_picture_t pPic_out;

        x264_picture_init(&pPic_out);
        x264_picture_alloc(&pPic_in, JAVA_IN_ENCODE_TYPE, video_width, video_height);
        pPic_in.img.i_plane = 2;

        if(video_rotate == _90degree) {
            rotate_nv12_clockwise_90_degree(video_height,video_width,
                    (uint8_t *)y_data,(uint8_t *)uv_data,pPic_in.img.plane[0],pPic_in.img.plane[1]);
        }else if(video_rotate == _270degree) {
            rotate_nv12_counterclockwise_90_degree(video_height,video_width,
                    (uint8_t *)y_data,(uint8_t *)uv_data,pPic_in.img.plane[0],pPic_in.img.plane[1]);
        }else {
            memcpy(pPic_in.img.plane[0],y_data,y_data_length_int * sizeof(uint8_t));
            memcpy(pPic_in.img.plane[1],uv_data,uv_data_length_int * sizeof(uint8_t));
        }
        //release
        env->ReleaseByteArrayElements(y_data_, y_data, 0);
        env->ReleaseByteArrayElements(uv_data_, uv_data, 0);
        is_release_jni_data = true;
        //
        int i_frame_size = x264_encoder_encode(pHandle,&pNals,&i_nal,&pPic_in,&pPic_out);
        if(i_frame_size > 0) {
            return_size = i_frame_size;
            jbyte *return_data_buffer = env->GetByteArrayElements(return_data_buffer_, nullptr);
            jsize return_data_buffer_length = env->GetArrayLength(return_data_buffer_);
            if(return_data_buffer_length > i_frame_size) {
                //write_data
                int offset = 0;
                for(int i = 0 ; i < i_nal ; i ++ ) {
                    uint8_t * p_payload = (pNals + i)->p_payload;
                    int i_payload = (pNals + i)->i_payload;
                    memcpy(((uint8_t *)return_data_buffer)+offset,p_payload,(size_t)i_payload);
                    offset += i_payload;
                }
            }
            env->ReleaseByteArrayElements(return_data_buffer_, return_data_buffer, 0);
        }
        pNals = nullptr;
        x264_picture_clean(&pPic_in);
    }
    if(!is_release_jni_data) {
        env->ReleaseByteArrayElements(y_data_, y_data, 0);
        env->ReleaseByteArrayElements(uv_data_, uv_data, 0);
    }
    return return_size;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_enes_lanvideocall_utils_video_X264Util_clean_1env(JNIEnv *env, jclass type) {
    __android_log_print(ANDROID_LOG_ERROR,"jni","Java_org_enes_lanvideocall_utils_video_X264Util_clean_1env");
    clean_mem();
}