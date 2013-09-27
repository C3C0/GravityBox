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
    private static final String CLASS_WORKING_MESSAGE = "com.android.mms.data.WorkingMessage";
    private static final String CLASS_DIALOG_MODE_ACTIVITY = "com.android.mms.ui.DialogModeActivity";
    private static final boolean DEBUG = false;

    private static UnicodeFilter mUnicodeFilter;
    private static XSharedPreferences mPrefs;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;

            final Class<?> composeMsgActivityClass = 
                    XposedHelpers.findClass(CLASS_COMPOSE_MSG_ACTIVITY, classLoader);
            final Class<?> workingMessageClass = 
                    XposedHelpers.findClass(CLASS_WORKING_MESSAGE, classLoader);

            XposedHelpers.findAndHookMethod(composeMsgActivityClass, 
                    "onCreate", Bundle.class, activityOnCreateHook);

            if (Utils.hasGeminiSupport()) {
                XposedHelpers.findAndHookMethod(workingMessageClass, "send",
                        String.class, int.class, workingMessageSendHook);
                try {
                    if (DEBUG) log ("Trying to hook on Dialog Mode activity (quickmessage)");
                    final Class<?> dialogModeActivityClass = XposedHelpers.findClass(
                            CLASS_DIALOG_MODE_ACTIVITY, classLoader);
                    XposedHelpers.findAndHookMethod(dialogModeActivityClass, "onCreate",
                            Bundle.class, activityOnCreateHook);
                } catch (Throwable t) {
                    XposedBridge.log("Error hooking to quick message dialog. Ignoring.");
                }
            } else {
                XposedHelpers.findAndHookMethod(workingMessageClass, "send",
                        String.class, workingMessageSendHook);
            }

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook activityOnCreateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                if (!prepareUnicodeFilter()) return;

                if (DEBUG) log("ComposeMessageActivity created. Hooking to TextEditorWatcher");

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
                            s = mUnicodeFilter.filter(s);
                            if (DEBUG) log ("TextEditorWatcher.onTextChanged: stripped ='" + s + "'");
                            XposedHelpers.callMethod(param.thisObject, "updateCounter",
                                    s, param2.args[1], param2.args[2], param2.args[3]); 
                        }
                    });
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static XC_MethodHook workingMessageSendHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (mUnicodeFilter == null) return;

                CharSequence msg = (CharSequence) XposedHelpers.getObjectField(param.thisObject, "mText");
                if (DEBUG) log("WorkingMessage.send called; mText = '" + msg + "'");
                msg = mUnicodeFilter.filter(msg);
                if (DEBUG) log("Stripped mText = '" + msg + "'");
                XposedHelpers.setObjectField(param.thisObject, "mText", msg);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static boolean prepareUnicodeFilter() {
        mPrefs.reload();
        final String uniStrMode = mPrefs.getString(
                GravityBoxSettings.PREF_KEY_MMS_UNICODE_STRIPPING, 
                GravityBoxSettings.UNISTR_LEAVE_INTACT);

        if (uniStrMode.equals(GravityBoxSettings.UNISTR_LEAVE_INTACT)) {
            mUnicodeFilter = null;
            if (DEBUG) log("prepareUnicodeFilter: Unicode stripping disabled");
            return false;
        } else {
            mUnicodeFilter = new UnicodeFilter(uniStrMode.equals(
                    GravityBoxSettings.UNISTR_NON_ENCODABLE));
            if (DEBUG) log("prepareUnicodeFilter: Unicode filter prepared; mode=" + uniStrMode);
            return true;
        }
    }
}
