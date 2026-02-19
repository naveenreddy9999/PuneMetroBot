package com.punemetroautomation;

public class BotStatusManager {
    private static StatusCallback callback;

    public interface StatusCallback {
        void onStatusUpdate(String status);
    }

    public static void setCallback(StatusCallback cb) {
        callback = cb;
    }

    public static void updateStatus(String status) {
        if (callback != null) {
            callback.onStatusUpdate(status);
        }
    }
}
