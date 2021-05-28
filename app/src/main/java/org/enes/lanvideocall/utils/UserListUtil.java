package org.enes.lanvideocall.utils;

import org.enes.lanvideocall.pojos.User;

import java.util.ArrayList;
import java.util.List;

public class UserListUtil {

    private static UserListUtil instance;

    public static UserListUtil getInstance() {
        if(instance == null) {
            instance = new UserListUtil();
        }
        return instance;
    }

    private static List<User> userList;

    private UserListUtil() {
        userList = new ArrayList<>();
    }

    public List<User> getUsers() {
        return userList;
    }

    public void updateList(List<User> new_user_list_from_server) {
        synchronized (userList) {
            userList.clear();
            userList.addAll(new_user_list_from_server);
        }
    }

}
