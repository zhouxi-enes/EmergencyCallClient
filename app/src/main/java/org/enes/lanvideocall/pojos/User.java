package org.enes.lanvideocall.pojos;

import java.io.Serializable;

public class User implements Serializable {

    public String uuid;

    public String name;

    public String ip;

    public long last_online_time;

    public boolean is_online;

}
