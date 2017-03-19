package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sample digital watch face with blinking colons and seconds. In ambient mode, the seconds are
 * replaced with an AM/PM indicator and the colons don't blink. On devices with low-bit ambient
 * mode, the text is drawn without anti-aliasing in ambient mode. On devices which require burn-in
 * protection, the hours are drawn in normal rather than bold. The time is drawn with less contrast
 * and without seconds in mute mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        static final int MSG_UPDATE_TIME = 0;
        private final String TAG = Engine.class.getSimpleName();

        //Handler to update the time periodically in interactive mode.
        final Handler mUpdateTimeHandler = new Handler() {

            @Override
            public void handleMessage(Message message) {

                switch (message.what) {

                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {

                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;


        boolean mAmbient;

        Calendar mCalendar;

        private int specW, specH;
        private View mLayout;

        private TextView time;
        private TextView date;
        private TextView high;
        private TextView low;
        private ImageView imageView;
        private final Point displaySize = new Point();
        float mXOffset;
        float mYOffset;
        private String highTemp;
        private String lowTemp;
        private int iconInt;
        private GoogleApiClient mGoogleApiClient;
        private final String PATH = getResources().getString(R.string.weather_data_path);
        private final String PATH_DATA_REQUEST = getResources().getString(R.string.weather_data_request_path);
        private final String KEY_WEATHER_ID = getResources().getString(R.string.key_weather_id);
        private final String KEY_TEMP_HIGH = getResources().getString(R.string.key_high_temperature);
        private final String KEY_TEMP_LOW = getResources().getString(R.string.key_low_temperature);

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);


            mCalendar = Calendar.getInstance();

            LayoutInflater inflater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = inflater.inflate(R.layout.round_activity_main, null);

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);

            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);

            time = (TextView) mLayout.findViewById(R.id.time);
            date = (TextView) mLayout.findViewById(R.id.date);
            high = (TextView) mLayout.findViewById(R.id.high);
            low = (TextView) mLayout.findViewById(R.id.low);
            imageView = (ImageView) mLayout.findViewById(R.id.image);
            iconInt = R.drawable.art_clear;
            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }


        @Override
        public void onConnected(Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            Log.d(TAG, getResources().getString(R.string.wearable_add_listener_connected));

            new Thread(new Runnable() {
                @Override
                public void run() {

                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage
                                (mGoogleApiClient, node.getId(), PATH_DATA_REQUEST, (getResources()
                                        .getString(R.string.wearable_data_request_message)).getBytes()).await();

                        String displayName = node.getDisplayName();

                        if (!result.getStatus().isSuccess()) {
                            Log.d(TAG, getResources().getString(R.string.wearable_send_message_error) + " " + displayName);
                        } else {
                            Log.d(TAG, getResources().getString(R.string.wearable_send_message_success) + " " + displayName);
                        }
                    }

                }
            }).start();


        }


        @Override
        public void onConnectionSuspended(int cause) {
            Log.e(TAG, getResources().getString(R.string.on_connection_suspended) + " " + String.valueOf(cause));

        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, getResources().getString(R.string.on_connection_failed) + " " + result);

        }

        //  This is only called if the data is changed since the last update
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {

            Log.d(TAG, getResources().getString(R.string.on_data_changed));


            for (DataEvent event : dataEvents) {


                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();


                    if (item.getUri().getPath().equals(PATH)) {

                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        //  Update the weather
                        updateWeather(dataMap);
                    }


                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    // DataItem deleted
                }
            }
        }

        public void updateWeather(DataMap dataMap) {
            //  If the datamap isn't null
            if (dataMap != null) {
                //  If the datamap contains any of the KEYs
                if (dataMap.containsKey(KEY_WEATHER_ID) && dataMap.containsKey(KEY_TEMP_HIGH)
                        && dataMap.containsKey(KEY_TEMP_LOW)) {
                    //  Set the member variables equal to the data from the datamap based on the KEYs
                    highTemp = dataMap.getString(KEY_TEMP_HIGH);
                    lowTemp = dataMap.getString(KEY_TEMP_LOW);
                    iconInt = SunshineWatchFaceUtils.getIcon(dataMap.getInt(KEY_WEATHER_ID));
                }
            }
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            //  If visible is true
            if (visible) {
                //  Register the timezone receiver
                registerReceiver();
                //  Connect the GoogleApiClient
                mGoogleApiClient.connect();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                //  Refresh the data
                invalidate();
            } else {
                //  If visible is not true unregister the timezone receiver
                unregisterReceiver();
                //  If GoogleApiClient is not null and it is connected
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    //  Disconnect the GoogleApiClient
                    mGoogleApiClient.disconnect();
                    //  Remove the wearable listener
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);


        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {

                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mCalendar.setTimeInMillis(System.currentTimeMillis());
            String timeString;
            String dateString;
            if (mCalendar.get(Calendar.MINUTE) < 10) {

                timeString = mCalendar.get(Calendar.HOUR_OF_DAY) + ":0" + mCalendar.get(Calendar.MINUTE);
            } else {

                timeString = mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE);
            }
            dateString = SunshineWatchFaceUtils.getDate(mCalendar);
            time.setText(timeString);
            date.setText(dateString);
            high.setText(highTemp);
            low.setText(lowTemp);
            imageView.setImageResource(iconInt);

            mLayout.measure(specW, specH);
            mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
            mLayout.draw(canvas);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

    }
}


