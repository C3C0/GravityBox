package com.ceco.gm2.gravitybox;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModVolKeyCursor {
    public static final String TAG = "GB:ModVolKeyCursor";
    public static final String CLASS_IME_SERVICE = "android.inputmethodservice.InputMethodService";
    private static int mVolKeyCursorControl = GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_OFF;
    private static final boolean DEBUG = false;

    private static void log (String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {
        if (DEBUG) log("initZygote");

        try {
            final Class<?> imeClass = XposedHelpers.findClass(CLASS_IME_SERVICE, null);

            XposedHelpers.findAndHookMethod(imeClass, "onShowInputRequested", int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mVolKeyCursorControl = 
                            Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_VOL_KEY_CURSOR_CONTROL, "0")); 
                    if (DEBUG) log("onShowInputRequested: refreshing configuartion; " +
                    		"mVolKeyCursorControl = " + mVolKeyCursorControl);
                } 
            });

            XposedHelpers.findAndHookMethod(imeClass, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    InputMethodService imeService = (InputMethodService) param.thisObject; 
                    if (imeService == null) {
                        if (DEBUG) log("failed to cast param.thisObject to InputMethodService");
                        return;
                    }

                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (imeService.isInputViewShown() && 
                                mVolKeyCursorControl != GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_OFF) {
                            int newKeyCode = mVolKeyCursorControl == GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_ON_REVERSE ?
                                    KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                            imeService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                            return;
                        }
                        param.setResult(false);
                        return;
                    }

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (imeService.isInputViewShown() && 
                                mVolKeyCursorControl != GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_OFF) {
                            int newKeyCode = mVolKeyCursorControl == GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_ON_REVERSE ?
                                    KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                            imeService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                            return;
                        }
                        param.setResult(false);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(imeClass, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    InputMethodService imeService = (InputMethodService) param.thisObject; 
                    if (imeService == null) {
                        log("failed to cast param.thisObject to InputMethodService");
                        return;
                    }
                    
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (imeService.isInputViewShown() &&
                            mVolKeyCursorControl != GravityBoxSettings.VOL_KEY_CURSOR_CONTROL_OFF) {
                            param.setResult(true);
                            return;
                        }
                        param.setResult(false);
                        return;
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}