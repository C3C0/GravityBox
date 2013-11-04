/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import java.nio.CharBuffer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixCallerIdMms {
    public static final String PACKAGE_NAME = "com.android.mms";
    public static final String ALT_PACKAGE_NAME = "com.lenovo.ideafriend";
    private static final String TAG = "GB:FixCallerIdMms";
    private static String CLASS_CONTACTS_CACHE = "com.android.mms.data.Contact$ContactsCache";
    private static final int STATIC_KEY_BUFFER_MAXIMUM_LENGTH = 5;
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader, String appUri) {
        if (DEBUG) log("init (" + appUri + ")");

        if (appUri.equals(ALT_PACKAGE_NAME)) {
            CLASS_CONTACTS_CACHE = "com.lenovo.ideafriend.mms.android.data.Contact$ContactsCache";
        }

        try {
            final Class<?> contactsCacheClass = XposedHelpers.findClass(CLASS_CONTACTS_CACHE, classLoader);

            XposedHelpers.findAndHookMethod(contactsCacheClass, "key", String.class, CharBuffer.class,
                    new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            String phoneNumber = (String) param.args[0];
                            CharBuffer keyBuffer = (CharBuffer) param.args[1];
                            keyBuffer.clear();
                            keyBuffer.mark();

                            int position = phoneNumber.length();
                            int resultCount = 0;
                            while (--position >= 0) {
                                char c = phoneNumber.charAt(position);
                                if (Character.isDigit(c)) {
                                    keyBuffer.put(c);
                                    if (++resultCount == STATIC_KEY_BUFFER_MAXIMUM_LENGTH) {
                                        break;
                                    }
                                }
                            }
                            String retVal = phoneNumber;
                            keyBuffer.reset();
                            if (resultCount > 0) {
                                retVal = keyBuffer.toString();
                            }
                            if (DEBUG) log("key(" + phoneNumber + "); retVal='" + retVal + "'");
                            return retVal;
                        }
            });

            XposedHelpers.findAndHookMethod(contactsCacheClass, "getKey", String.class, boolean.class,
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String key = (String) param.getResult();
                    if (key.length() > STATIC_KEY_BUFFER_MAXIMUM_LENGTH)
                        key = key.substring(0, key.length() - 1);
                    if (DEBUG) log("getKey(): original retVal='" + param.getResult() + "'; " +
                        "new retVal='" + key + "'");
                    param.setResult(key);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}