package com.ceco.gm2.gravitybox;

import android.os.Bundle;
import android.text.TextWatcher;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModMms {
    public static final String PACKAGE_NAME = "com.android.mms";
    private static final String TAG = "GB:ModMms";
    private static final String CLASS_COMPOSE_MSG_ACTIVITY = "com.android.mms.ui.ComposeMessageActivity";
    private static final boolean DEBUG = false;

    private static UnicodeFilter mUnicodeFilter;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> composeMsgActivityClass = 
                    XposedHelpers.findClass(CLASS_COMPOSE_MSG_ACTIVITY, classLoader);

            XposedHelpers.findAndHookMethod(composeMsgActivityClass, 
                    "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    final String uniStrMode = prefs.getString(
                            GravityBoxSettings.PREF_KEY_MMS_UNICODE_STRIPPING, 
                            GravityBoxSettings.UNISTR_LEAVE_INTACT);

                    if (uniStrMode.equals(GravityBoxSettings.UNISTR_LEAVE_INTACT)) {
                        mUnicodeFilter = null;
                        if (DEBUG) log("onCreate: Unicode stripping disabled, returning");
                        return;
                    }

                    if (DEBUG) log("ComposeMessageActivity created. Hooking to TextEditorWatcher");

                    mUnicodeFilter = new UnicodeFilter(
                            uniStrMode.equals(GravityBoxSettings.UNISTR_NON_ENCODABLE));

                    final TextWatcher textEditorWatcher = (TextWatcher) XposedHelpers.getObjectField(
                            param.thisObject, "mTextEditorWatcher");
                    if (textEditorWatcher != null) {
                        XposedHelpers.findAndHookMethod(textEditorWatcher.getClass(), "onTextChanged", 
                                CharSequence.class, int.class, int.class, int.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param2) throws Throwable {
                                if (param2.thisObject != textEditorWatcher) return;

                                CharSequence s = (CharSequence) param2.args[0];
                                if (DEBUG) log ("TextEditorWatcher.onTextChanged: original ='" + s + "'");
                                s = stripUnicode(s);
                                if (DEBUG) log ("TextEditorWatcher.onTextChanged: stripped ='" + s + "'");
                                XposedHelpers.callMethod(param.thisObject, "updateCounter",
                                        s, param2.args[1], param2.args[2], param2.args[3]); 
                            }
                        });
                    }
                }
            });

            XposedHelpers.findAndHookMethod(composeMsgActivityClass, "sendMessage",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("ComposeMessageActivity.sendMessage called");
                    Object workingMessage = XposedHelpers.getObjectField(param.thisObject, "mWorkingMessage");
                    CharSequence msg = (CharSequence) XposedHelpers.callMethod(workingMessage, "getText");
                    if (DEBUG) log("mWorkingMessage.getText returned: '" + msg + "'");
                    msg = stripUnicode(msg);
                    if (DEBUG) log("Stripped message: '" + msg + "'");
                    XposedHelpers.callMethod(workingMessage, "setText", msg);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static CharSequence stripUnicode(CharSequence text) {
        if (mUnicodeFilter != null) {
            text = mUnicodeFilter.filter(text);
        }
        return text;
    }
}
