//
// Created by zhouxi on 2019-06-24.
//

#include "image_util_c.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <android/log.h>

void rotate_nv12_clockwise_90_degree(int y_width, int y_height, uint8_t * y_in,
                                     uint8_t * uv_in, uint8_t * y_out, uint8_t * uv_out) {
    int uv_height = y_height >> 1;
    int uv_width = y_width >> 1;
    int y_length = y_width * y_height;
    int uv_length = (uv_height * uv_width) << 1;

    int new_uv_width = uv_height << 1;
    int new_uv_height = uv_width;

    for(int i = 0; i < y_length; i ++) {
        int left_to_right_num = i / y_width;
        int top_to_bottom_num = i % y_width;
        int pos = (top_to_bottom_num * y_height) + (y_height - left_to_right_num) - 1;
        //y
        y_out[pos] = y_in[i];
        if(i < uv_length) {
            //往左的偏移
            left_to_right_num = i % 2 + new_uv_width - 1 - (((i / 2) / new_uv_height) << 1) - 1;
            //往下的偏移
            top_to_bottom_num = (i / 2) % new_uv_height;
            pos = new_uv_width * top_to_bottom_num + left_to_right_num;
            uv_out[pos] = uv_in[i];
        }
    }
}

void rotate_nv12_counterclockwise_90_degree(int y_width, int y_height, uint8_t * y_in,
                                            uint8_t * uv_in, uint8_t * y_out, uint8_t * uv_out) {
    int uv_height = y_height >> 1;
    int uv_width = y_width >> 1;
    int y_length = y_width * y_height;
    int uv_length = (uv_height * uv_width) << 1;


    int new_y_width = y_height;
    int new_y_height = y_width;

    int new_uv_width = uv_height << 1;
    int new_uv_height = uv_width;

    for(int i = 0; i < y_length; i ++) {
        int new_left_to_right_value = i / new_y_height;
        int new_top_to_bottom_value = (new_y_height - 1) - i % new_y_height;
        int new_pos = new_top_to_bottom_value * new_y_width + new_left_to_right_value - 1;
        //y
        y_out[new_pos] = y_in[i];
        //uv
        if(i < uv_length) {
            int tmp = i / 2; // 第下標組
            int tmp2 = i % 2; // 第0,1個
            new_top_to_bottom_value = (new_uv_height -1) - (tmp % new_uv_height); //這個組在從上往下多少的下標位置
            new_left_to_right_value = (tmp / new_uv_height) * 2 + tmp2; //個體從左到右的數值
            new_pos = new_top_to_bottom_value * new_uv_width + new_left_to_right_value;
            uv_out[new_pos] = uv_in[i];
        }
    }
}