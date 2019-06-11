package com.XECUREVoIP.chat.ChatUtils;

import java.util.Calendar;
import java.util.Date;

public class XecureChatMessage {
    private String mBody;
    private boolean mSent;
    private Date mDate;
    private boolean mReaded;

    public XecureChatMessage(String message, boolean sent) {
        mBody = message;
        mSent = sent;
        mDate = Calendar.getInstance().getTime();

        mReaded = false;
    }

    public String getBody() {
        return mBody;
    }

    public Date getDate() {
        return mDate;
    }

    public boolean isRead() {
        return mReaded;
    }

    public boolean isOutgoing() {
        return mSent;
    }

    public void read() {
        mReaded = true;
    }
}
