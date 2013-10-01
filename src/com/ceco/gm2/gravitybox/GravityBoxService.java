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

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class GravityBoxService extends IntentService {
    public static final String ACTION_TOGGLE_SYNC = "gravitybox.intent.action.TOGGLE_SYNC";
    public static final String ACTION_GET_SYNC_STATUS = "gravitybox.intent.action.GET_SYNC_STATUS";

    public static final int RESULT_SYNC_STATUS = 0;
    public static final String KEY_SYNC_STATUS = "syncStatus";

    public GravityBoxService() {
        super("GravityBoxService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_TOGGLE_SYNC)) {
            ContentResolver.setMasterSyncAutomatically(!ContentResolver.getMasterSyncAutomatically());
        } else if (intent.getAction().equals(ACTION_GET_SYNC_STATUS)) {
            boolean syncStatus = ContentResolver.getMasterSyncAutomatically();
            ResultReceiver receiver = intent.getParcelableExtra("receiver");
            Bundle data = new Bundle();
            data.putBoolean(KEY_SYNC_STATUS, syncStatus);
            receiver.send(RESULT_SYNC_STATUS, data);
        }
    }

}
