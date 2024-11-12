package com.pocket.util.android;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.HashMap;
import java.util.Map;

public class AccessibilityUtils {

    /**
     * Internally, {@link BottomSheetBehavior} expects that the {@link CoordinatorLayout} the Bottom Sheet View appears in will also be used to display the rest of the content on screen,
     * and contains logic to hide content behind it from accessibility tools while it is expanded.
     *
     * Because of the way that our {@link BottomSheetBehavior}'s have been implemented, as discrete drop in Views in their own right, Views behind them are not excluded from
     * accessibility tools, such as Talkback, and thus Views underneath the drawer would be read aloud.
     *
     * To fix this, {@link BottomSheetHelper} does the same work that {@link BottomSheetBehavior} does internally, only using the parent of the {@link CoordinatorLayout} from which our Bottom Sheets
     * are composed.
     *
     * To use in a Bottom Sheet implementation *that does not itself host the rest of the screen content*, create an instance of BottomSheetHelper and call {@link BottomSheetHelper#updateAccessibilityState} within a
     * {@link BottomSheetBehavior.BottomSheetCallback} on the Bottom Sheet's {@link BottomSheetBehavior}.
     */
    public static class BottomSheetHelper {

        private Map<View, Integer> importantForAccessibilityMap;

        /**
         * Updates the accessibility tools state of the parent view tree, based on the new state of the BottomSheetBehavior.
         * Adapted from {@link BottomSheetBehavior}'s setStateInternal method logic.
         */
        public void updateAccessibilityState(@NonNull View drawer, int newState, boolean hideCollapsed) {
            if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED ||
                    newState == BottomSheetBehavior.STATE_EXPANDED ||
                    (hideCollapsed && newState == BottomSheetBehavior.STATE_COLLAPSED)) {
                updateImportantForAccessibility(drawer, true);
            } else {
                // update only on STATE_HIDDEN or STATE_COLLAPSED (when applicable), skip doing anything while dragging / settling
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    updateImportantForAccessibility(drawer, false);
                }
            }
        }

        /**
         * This method is adapted from {@link BottomSheetBehavior}'s internal updateImportantForAccessibility method, which loops through all of the Views in the Bottom Sheet's
         * parent, except for the Bottom Sheet itself, and marks them either IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, or back to their original state, depending on whether
         * the drawer is open / closed.
         */
        private void updateImportantForAccessibility(View drawer, boolean hideFromAccessibility) {
            ViewParent viewParent = drawer.getParent();
            if (viewParent instanceof ViewGroup) {
                int childCount = ((ViewGroup) viewParent).getChildCount();
                if (hideFromAccessibility) {
                    if (importantForAccessibilityMap != null) {
                        return;
                    }
                    importantForAccessibilityMap = new HashMap<>(childCount);
                }
                for (int i = 0; i < childCount; ++i) {
                    View child = ((ViewGroup) viewParent).getChildAt(i);
                    if (child != drawer) {
                        if (!hideFromAccessibility) {
                            if (importantForAccessibilityMap != null && importantForAccessibilityMap.containsKey(child)) {
                                ViewCompat.setImportantForAccessibility(child, importantForAccessibilityMap.get(child));
                            }
                        } else {
                            importantForAccessibilityMap.put(child, child.getImportantForAccessibility());
                            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                        }
                    }
                }
                if (!hideFromAccessibility) {
                    importantForAccessibilityMap = null;
                }
            }
        }
    }
}
