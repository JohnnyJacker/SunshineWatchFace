package com.example.android.sunshine.sync;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


public class ListenerService extends WearableListenerService {

    private String LOG_TAG = ListenerService.class.getSimpleName();

    //  When the mobile device receives a message from the connected wearable device
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        //  Sync weather immediately
        SunshineSyncUtils.startImmediateSync(getApplicationContext());

    }

}
