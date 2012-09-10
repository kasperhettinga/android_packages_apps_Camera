/*
 * Copyright (C) 2012 The Android Open Source Project
 *
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

package com.android.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


public class CameraSwitcher extends ScrollerView {

    private static final String TAG = "CAM_Switcher";

    private static final long FLING_DURATION = 250;

    private static final int MSG_SNAP = 1;
    private static final int MSG_SET_CAM = 2;

    public interface CameraSwitchListener {

        public void onScroll();

        public void onCameraSelected(int i);
    }

    private LinearLayout mContent;
    private CameraSwitchListener mListener;
    private int mCurrentIndex;
    private int mChildSize;
    private int mOffset;
    private boolean mHorizontal = true;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SNAP:
                snap();
                break;
            case MSG_SET_CAM:
                if (mListener != null) {
                    mListener.onCameraSelected(mCurrentIndex);
                }
                break;
            }
        }
    };

    public CameraSwitcher(Context context) {
        super(context);
        init(context);
    }

    public CameraSwitcher(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public CameraSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        setFillViewport(true);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        mContent = new LinearLayout(context);
        addView(mContent, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        updateOrientation();
        // initialize to non-zero
        mChildSize = 100;
        mOffset = 100;
    }

    private void updateOrientation() {
        Configuration config = getContext().getResources().getConfiguration();
        switch(config.orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
        case Configuration.ORIENTATION_UNDEFINED:
            setOrientation(LinearLayout.VERTICAL);
            break;
        case Configuration.ORIENTATION_PORTRAIT:
            setOrientation(LinearLayout.HORIZONTAL);
            break;
        }
    }
    public void setOrientation(int orientation) {
        mContent.setOrientation(orientation);
        if (orientation == LinearLayout.HORIZONTAL) {
            mHorizontal = true;
            mContent.setLayoutParams(
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        } else {
            mHorizontal = false;
            mContent.setLayoutParams(
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        super.setOrientation(orientation);
    }


    public void setCurrentModule(int i) {
        mCurrentIndex = i;
        reposition();

    }

    public void setSwitchListener(CameraSwitchListener l) {
        mListener = l;
    }

    @Override
    public void fling(int velocity) {
        super.fling(velocity);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SNAP), FLING_DURATION);
    }

    @Override
    public void onScrollChanged(int sx, int sy, int oldx, int oldy) {
        if (!ignoreScroll()) {
            if (mListener != null) {
                mListener.onScroll();
            }
        }
    }

    public void add(View view, ViewGroup.LayoutParams lp) {
        mContent.addView(view, lp);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // child width
        int n = mContent.getChildCount();
        if (mHorizontal) {
            // layout left to right
            int w = r - l;
            mChildSize = w * 2 / 5;
            mOffset = (w - mChildSize) / 2;
            mContent.layout(0, 0, mChildSize * n + 2 * mOffset, (b - t));
            int cl = mOffset;
            int h = b - t;
            for (int i = 0; i < n; i++) {
                View c = mContent.getChildAt(i);
                c.layout(cl, 0, cl + mChildSize, h);
                cl += mChildSize;
            }
        } else {
            // layout bottom to top
            int h = b - t;
            mChildSize = h * 2 / 5;
            mOffset = (h - mChildSize) / 2;
            mContent.layout(0, 0, (r - l), mChildSize * n + 2 * mOffset);
            int cl = mOffset;
            int w = r - l;
            for (int i = n - 1; i >= 0; i--) {
                View c = mContent.getChildAt(i);
                c.layout(0, cl, w, cl + mChildSize);
                cl += mChildSize;
            }
        }
        reposition();
    }

    @Override
    protected void onScrollUp() {
        snap();
    }

    private void reposition() {
        if (mHorizontal) {
            scrollTo(mCurrentIndex * mChildSize, 0);
        } else {
            scrollTo(0, (mContent.getChildCount() - 1 - mCurrentIndex) * mChildSize);
        }
    }

    private void snap() {
        int cx = mHorizontal ? getScrollX() + getWidth() / 2 - mOffset :
            getScrollY() + getHeight() / 2 - mOffset ;
        if (mChildSize != 0) {
            final int centerpos = cx / mChildSize;
            if (mHorizontal) {
                smoothScrollTo(centerpos * mChildSize, 0);
                mCurrentIndex = centerpos;
            } else {
                smoothScrollTo(0, centerpos * mChildSize);
                // top to bottom
                mCurrentIndex = mContent.getChildCount() - 1 - centerpos;
            }
            mHandler.sendEmptyMessageDelayed(MSG_SET_CAM, 100);
        }
    }

}