package org.enes.lanvideocall.utils;

public class Defines {

    public static final String LAN_SERVER_ADDRESS = "192.168.1.43";// for mac

//    public static final String LAN_SERVER_ADDRESS = "192.168.0.199";

    public static final int LAN_SERVER_PORT = 8080;

    public static final String LAN_SERVER_WEB_SERVICE_NAME = "LanVideoCallServer_war";

    public static final long HTTP_TIMEOUT = 3000;



    public static final String http_api_check_version = "v";

    public static final String http_api_add_user = "add";

    public static final String http_api_get_user_list = "u";



    public static final int AUDIO_SAMPLE_RATE = 44100;

    public static final int AUDIO_SERVER_PORT = 60000;

    public static final int VIDEO_SERVER_PORT = AUDIO_SERVER_PORT + 1;



    public static final int CONTROL_SERVER_PORT = VIDEO_SERVER_PORT + 1;


    /**
     * 等待對方客戶端返回3秒
     */
    public static final long Dialing_WAITING_TIME = 5 * 1000;

    /**
     * 一次電話等待50秒放響鈴
     */
    public static final long SEND_RINGING_PULSE_TIME = 50 * 1000;

    /**
     * 發送脈衝中的間隔
     */
    public static final long SEND_RINGING_PULSE_INTERVAL = 3 * 1000;

    /**
     * 檢查脈衝中的間隔
     */
    public static final long CHECK_RECEIVED_RINGING_PULSE_INTERVAL = 7 * 1000;

    /**
     * 發送心跳包的間隔
     */
    public static final long SEND_ALIVE_PACKAGE_INTERVAL = 3 * 1000;

    /**
     * 檢查心跳包的間隔
     */
    public static final long CHECK_RECEIVED_ALIVE_PACKAGE_INTERVAL = 7 * 1000;

    /**
     * 拒絕
     */
    public static final int CALL_REJECT_CALLING_NOW = -1;

    /**
     * 同意
     */
    public static final int CALL_ACCEPT_CALLING_NOW = 0;


    /**
     * 廣播的動作
     */
    public static final String CALL_BROADCAST_ACTION = "CALL_BROADCAST_ACTION";

    public static final String CALL_BROADCAST_KEY = "CALL_BROADCAST_KEY";

    /**
     * 沒有電話
     */
    public static final int CALL_BROADCAST_NO_CALLING_NOW = -1;

    /**
     * 正在撥號
     */
    public static final int CALL_BROADCAST_DIALING_NOW = 0;

    /**
     * 正在響鈴
     */
    public static final int CALL_BROADCAST_RINGING_NOW = 1;

    /**
     * 掛斷
     */
    public static final int CALL_BROADCAST_CLOSE = 2;

    /**
     * 聯通了
     */
    public static final int CALL_BROADCAST_CONNECTED_NOW = 3;

    /**
     * 對方忙
     */
    public static final int CALL_BROADCAST_BUSY = 4;

    /**
     * 直到脈衝結束用戶都沒有接
     */
    public static final int CALL_BROADCAST_USER_NOT_ACCEPT_STILL_PULSE_FINISH = 5;

    /**
     * 發送命令關掉接電話的試圖
     */
    public static final int CALL_BROADCAST_CLOSE_INCOMING_VIEW = 6;

    /**
     * 發送心跳包
     */
    public static final int CALL_BROADCAST_SEND_ALIVE_PACKAGE = 7;

    /**
     * timeout
     */
    public static final int CALL_BROADCAST_SEND_ALIVE_PACKAGE_TIME_OUT = 8;

    public static final int CALL_BROADCAST_NORMAL_CLOSE_CALL = 9;


    public static final int CALL_JSON_TYPE_REQ = 0;

    public static final int CALL_JSON_TYPE_RESP = 1;

    /**
     * 對方同意鏈接
     */
    public static final int CALL_OTHER_ONE_ACCEPT = 0;

    /**
     * 對方不同意
     */
    public static final int CALL_OTHER_ONE_IGNORE = 1;

    /**
     * 打開用到的線程
     */
    public static final int OPEN_CALL_THREAD = 11;


    public static final String MESSAGE_SERVER_ADDRESS = "http://157.65.30.36/demo/";

    public static final String API_MESSAGE = "apiMessage.php";

    public static final String API_LOCATION = "index.php";

}
