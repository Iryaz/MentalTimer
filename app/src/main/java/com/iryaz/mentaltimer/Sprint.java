package com.iryaz.mentaltimer;

import java.util.Date;

/**
 * Created by ilya on 17.03.18.
 */

public class Sprint {

    public Sprint(int minutes) {
        mDate = new Date();
        mMinutes = minutes;
        mIsSuccess = false;
    }

    public void success() {
        mIsSuccess = true;
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public Date getDate() {
        return mDate;
    }

    public int getMinutes() {
        return mMinutes;
    }

    private int mMinutes;
    private boolean mIsSuccess;
    private Date mDate;
}
