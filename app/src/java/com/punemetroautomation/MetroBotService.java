package com.punemetroautomation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MetroBotService extends AccessibilityService {

    private static final String TAG = "MetroBotService";
    private static final String PUNE_METRO_PKG = "org.mah.punemetro";

    private static MetroBotService instance;
    private Handler handler = new Handler(Looper.getMainLooper());

    private int botStep = 0;
    private String fromStation = "";
    private String toStation = "";
    private boolean botRunning = false;

    private static final int STEP_IDLE = 0;
    private static final int STEP_OPEN_APP = 1;
    private static final int STEP_TAP_BOOK_TICKET = 2;
    private static final int STEP_TAP_FROM = 3;
    private static final int STEP_SELECT_FROM_STATION = 4;
    private static final int STEP_TAP_TO = 5;
    private static final int STEP_SELECT_TO_STATION = 6;
    private static final int STEP_TAP_SEARCH = 7;
    private static final int STEP_DONE = 8;

    public static MetroBotService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility Service Connected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public void startBot(String from, String to) {
        this.fromStation = from;
        this.toStation = to;
        this.botStep = STEP_OPEN_APP;
        this.botRunning = true;
        openPuneMetroApp();
    }

    private void openPuneMetroApp() {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(PUNE_METRO_PKG);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                handler.postDelayed(() -> {
                    botStep = STEP_TAP_BOOK_TICKET;
                    performNextStep();
                }, 3000);
            } else {
                BotStatusManager.updateStatus("❌ Pune Metro app not found! Is it installed?");
            }
        } catch (Exception e) {
            BotStatusManager.updateStatus("❌ Error opening app: " + e.getMessage());
        }
    }

    private void performNextStep() {
        if (!botRunning) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            handler.postDelayed(this::performNextStep, 1000);
            return;
        }

        switch (botStep) {
            case STEP_TAP_BOOK_TICKET:
                BotStatusManager.updateStatus("🔍 Looking for Book Ticket button...");
                tapNodeWithText(root, "Book Ticket", STEP_TAP_FROM, 2000);
                break;

            case STEP_TAP_FROM:
                BotStatusManager.updateStatus("🔍 Tapping From Station...");
                boolean foundFrom = tapNodeWithText(root, "From Station", STEP_SELECT_FROM_STATION, 1500);
                if (!foundFrom) tapNodeWithText(root, "Select Station Name", STEP_SELECT_FROM_STATION, 1500);
                break;

            case STEP_SELECT_FROM_STATION:
                BotStatusManager.updateStatus("🔍 Selecting: " + fromStation);
                tapNodeWithText(root, fromStation, STEP_TAP_TO, 1500);
                break;

            case STEP_TAP_TO:
                BotStatusManager.updateStatus("🔍 Tapping To Station...");
                tapNodeWithText(root, "To Station", STEP_SELECT_TO_STATION, 1500);
                break;

            case STEP_SELECT_TO_STATION:
                BotStatusManager.updateStatus("🔍 Selecting: " + toStation);
                tapNodeWithText(root, toStation, STEP_TAP_SEARCH, 1500);
                break;

            case STEP_TAP_SEARCH:
                BotStatusManager.updateStatus("🔍 Tapping Search/Proceed...");
                boolean found = tapNodeWithText(root, "Search", STEP_DONE, 2000);
                if (!found) found = tapNodeWithText(root, "Proceed", STEP_DONE, 2000);
                if (!found) found = tapNodeWithText(root, "Get Ticket", STEP_DONE, 2000);
                if (!found) tapNodeWithText(root, "Book", STEP_DONE, 2000);
                break;

            case STEP_DONE:
                BotStatusManager.updateStatus("✅ Done! Proceeding to payment...");
                botRunning = false;
                botStep = STEP_IDLE;
                break;
        }

        root.recycle();
    }

    private boolean tapNodeWithText(AccessibilityNodeInfo root, String text, int nextStep, long delayAfter) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (!clicked) {
                        AccessibilityNodeInfo parent = node.getParent();
                        if (parent != null) {
                            clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            parent.recycle();
                        }
                    }
                    if (!clicked) {
                        Rect bounds = new Rect();
                        node.getBoundsInScreen(bounds);
                        tapByCoordinates(bounds.centerX(), bounds.centerY());
                        clicked = true;
                    }
                    node.recycle();
                    if (clicked) {
                        botStep = nextStep;
                        handler.postDelayed(this::performNextStep, delayAfter);
                        return true;
                    }
                }
            }
        }
        handler.postDelayed(this::performNextStep, 1000);
        return false;
    }

    private void tapByCoordinates(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, 100);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(clickStroke);
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        botRunning = false;
    }
}
