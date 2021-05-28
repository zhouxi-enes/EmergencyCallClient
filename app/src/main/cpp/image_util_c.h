//
// Created by zhouxi on 2019-06-24.
//

#ifndef LANVIDEOCALL_IMAGE_UTIL_C_H
#define LANVIDEOCALL_IMAGE_UTIL_C_H


#include <jni.h>

void rotate_nv12_clockwise_90_degree(int y_width, int y_height, uint8_t * y_in,
                                     uint8_t * uv_in, uint8_t * y_out, uint8_t * uv_out);

void rotate_nv12_counterclockwise_90_degree(int y_width, int y_height, uint8_t * y_in,
                                     uint8_t * uv_in, uint8_t * y_out, uint8_t * uv_out);

#endif //LANVIDEOCALL_IMAGE_UTIL_C_H