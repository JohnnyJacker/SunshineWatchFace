package com.example.android.sunshine.sync;

import android.content.Context;
import android.net.sip.SipAudioCall;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.MainActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;


public class ListenerService extends WearableListenerService {

    private String LOG_TAG = ListenerService.class.getSimpleName();


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

//        showToast(messageEvent.getPath());

//        SunshineSyncUtils.startImmediateSync(this);

        Log.d(LOG_TAG, messageEvent.getPath());

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
