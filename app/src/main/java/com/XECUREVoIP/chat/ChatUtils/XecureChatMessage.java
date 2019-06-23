package com.XECUREVoIP.chat.ChatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class XecureChatMessage {
    private Long dbId;
    private String id;
    private String senderId;
    private String mBody;
    private Date mDate;
    private boolean mSent;
    private boolean mReaded;
    private boolean delivered;

    public XecureChatMessage(String message, boolean sent) {
        mBody = message;
        mSent = sent;
        mDate = Calendar.getInstance().getTime();
        delivered = false;
        mReaded = false;
        senderId = "";
        id = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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

    public void setBody(String message) {
        mBody = message;
    }

    public void delivered(){
        delivered = true;
    }

    public boolean isDelivered(){
        return delivered;
    }

    public String getId(){
        return id;
    }

    public void setSenderId(String id){
        senderId = id;
    }

    public String getSenderid(){
        return senderId;
    }

    public String getDateString() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(mDate);
    }

    public void setDate(String dateString){
        try {
            mDate = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setDbId(long id){
        dbId = id;
    }

    public Long getDbId(){
        return dbId;
    }
}
