package com.chatapp.repository;

public final class RedisKeys {
    private RedisKeys() {}

    public static String channel(String roomId) {
        return "chatroom:" + roomId;
    }

    public static String meta(String roomId) {
        return channel(roomId) + ":meta";
    }

    public static String participants(String roomId) {
        return channel(roomId) + ":participants";
    }

    public static String messages(String roomId) {
        return channel(roomId) + ":messages";
    }
}
