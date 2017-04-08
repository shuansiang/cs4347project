package com.tetrastudio;

import android.hardware.SensorEventListener;

// Base controller class to handle update timestamps and enabling/disabling for simple controllers
public abstract class ControllerBase implements SensorEventListener {

    protected static final float UPDATE_INTERVAL = 10;

    protected boolean mIsEnabled = false;

    protected long mPreviousUpdateTimestamp = 0;

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    protected long updateDeltaTime() {
        long timestamp = System.currentTimeMillis();
        long deltaTime = timestamp - mPreviousUpdateTimestamp;
        return deltaTime;
    }
}
