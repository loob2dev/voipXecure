package com.XECUREVoIP.chat.ChatUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.contacts.ContactsManager;
import com.XECUREVoIP.security.SecurityUtils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class XecureChatRoom {
    private long mIdDB;
    private String mEntryId;
    private ArrayList<XecureChatMessage> mMessages;
    private final String TAG = "XECURE CHAT";
    private String mKey = "xecurechat";
    private XecureDH mDH;
    private boolean keyExchanged = false;
    private boolean accepted = false;
    private Context mCnt;

    static public final String KEY = "pub_key";
    static public final String PUB_KEY = "pub_key";
    static public final String PUB_KEY_ACCEPT = "pub_key_accept";
    static public final String PUB_KEY_REQ = "pub_key_req";
    static public final String PUB_KEY_RES = "pub_key_res";
    static public final String DELIVERED = "delivered";

    public XecureChatRoom(String entry, Context context) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mDH = new XecureDH();
        mDH.generateKeys();
        mCnt = context;

        setDataFromDB();
    }

    public void setDataFromDB() {
        ChatMessageDBHelper chat_dbHelper = new ChatMessageDBHelper(mCnt, mEntryId);
        Cursor cursor = chat_dbHelper.getAllData();
        if (cursor.getColumnIndex(ChatMessageDBHelper.COL_MSG_ID) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_SENDER_ID) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_BODY) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_DATE) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_SENT) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_READ) > 0
                && cursor.getColumnIndex(ChatMessageDBHelper.COL_DELIVER) > 0){
            try {
                while (cursor.moveToNext()){
                    long id = cursor.getLong(0);
                    String msgId = cursor.getString(1);
                    String senderId = cursor.getString(2);
                    String body = cursor.getString(3);
                    String date = cursor.getString(4);
                    boolean sent = cursor.getString(5).compareTo("0") == 0 ? false : true;
                    boolean read = cursor.getString(6).compareTo("0") == 0 ? false : true;
                    boolean deliver = cursor.getString(7).compareTo("0") == 0 ? false : true;
                    XecureChatMessage message = new XecureChatMessage(body, sent);
                    if (read)
                        message.read();
                    if (deliver) {
                        message.delivered();
                    }
                    message.setDbId(id);

                    mMessages.add(message);
                }
            }catch (Exception e){
                org.linphone.mediastream.Log.e(e);
            }finally {
                cursor.close();
            }
        }
        else
            mCnt.deleteDatabase(mEntryId + ".db");
    }

    public boolean isAccept(){
        return accepted;
    }

    public boolean isExchanged(){
        return keyExchanged;
    }

    public void accept(){
        accepted = true;
    }

    public void receivePublicKey(String key){
        byte[] publicKeyBytes = org.bouncycastle.util.encoders.Base64.decode(key.getBytes());

        KeyFactory keyFactory = null;
        PublicKey pubKey = null;
        try {
            pubKey = mDH.loadEcPublicKey(publicKeyBytes);
            mDH.receivePublicKeyFrom(pubKey);
            mDH.generateCommonSecretKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public void sendPublicKey(String type){
        //share public key
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(mEntryId + "@chat.xecu.re");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        AbstractXMPPConnection connection = XecureManager.getInstance().getSmackConnection();
        if(connection != null) {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            Chat chat = chatManager.chatWith(jid);
            Message newMessage = new Message();
            try {
                byte[] publicKeyBytes = org.bouncycastle.util.encoders.Base64.encode(mDH.getPublicKey().getEncoded());
                newMessage.setSubject(type);
                String pubKey = new String(publicKeyBytes);
                newMessage.setBody(pubKey);
                chat.send(newMessage);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void keyExchagned(){
        keyExchanged = true;
        try {
            mKey = new String(mDH.getKey(), StandardCharsets.US_ASCII);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void keyExchange(){
        keyExchanged = true;
    }

    public void sendMessage(String message, Context context) {
        checkConnection();
        XecureChatMessage xecureMessage = new XecureChatMessage(message, true);
        xecureMessage.read();
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(mEntryId + "@chat.xecu.re");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        AbstractXMPPConnection connection = XecureManager.getInstance().getSmackConnection();
        if(connection != null) {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            Chat chat = chatManager.chatWith(jid);
            Message newMessage = new Message();
            try {
                newMessage.setSubject(xecureMessage.getId());
                SecurityUtils utils = new SecurityUtils(mKey);
                newMessage.setBody(utils.encrypt(message));
                newMessage.setType(Message.Type.chat);
                chat.send(newMessage);

            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMessages.add(xecureMessage);
        }
        //store db
//        ChatMessageDBHelper dbHelper = new ChatMessageDBHelper(context, mEntryId);
//        xecureMessage.setDbId(dbHelper.insertData(
//                xecureMessage.getId(),
//                xecureMessage.getSenderid(),
//                xecureMessage.getBody(),
//                xecureMessage.getDateString(),
//                xecureMessage.isOutgoing(),
//                xecureMessage.isRead(),
//                xecureMessage.isDelivered()));
    }

    private void checkConnection() {
        ArrayList<XecureChatMessage> unDeliveredMsgs = getUnDeliveredMessages();
        if (XecureService.isReady() && unDeliveredMsgs.size() > 0){
            XecureService.instance().connectOpenfire();
        }
    }

    public ArrayList<XecureChatMessage> getUnDeliveredMessages(){
        ArrayList<XecureChatMessage> unDeliveredMsgs = new ArrayList<XecureChatMessage>();
        for (int i = 0; i < mMessages.size(); i++){
            if (!mMessages.get(i).isDelivered() && mMessages.get(i).isOutgoing()){
                unDeliveredMsgs.add(mMessages.get(i));
            }
        };
        return unDeliveredMsgs;
    }

    public XecureChatMessage getLastMessage() {
        if (mMessages.size() == 0)
            return null;
        return mMessages.get(mMessages.size() - 1);
    }

    public String getAddress() {
        XecureContact contact = ContactsManager.getInstance().findContactFromNumber(mEntryId);
        if (contact == null)
            return mEntryId;

        return contact.getFullName();
    }

    public int getUnreadMessagesCount() {
        int count = 0;
        for (int i = 0; i < mMessages.size(); i++){
            if (mMessages.get(i).isRead())
                continue;
            count++;
        }
        return count;
    }

    public ArrayList<XecureChatMessage> getHistory() {
        return mMessages;
    }

    public boolean createNewMessage(XecureChatMessage message) {
        SecurityUtils utils = new SecurityUtils(mKey);
        try {
            message.setBody(utils.decrypt(message.getBody()));
            mMessages.add(message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void setXecureKey(String xecureKey) {
        mKey = xecureKey;
    }
    public String getXecureKey() {
        return mKey;
    }

    public String getId() {
        return mEntryId;
    }

    public void setDbId(long idDB){
        mIdDB = idDB;
    }

    public long getDbId(){
        return mIdDB;
    }

    public void reSendMissedMessages() {
        ArrayList<XecureChatMessage> unDeliveredMsgs = getUnDeliveredMessages();
        for (int i = 0; i < unDeliveredMsgs.size(); i++){
            XecureChatMessage xecureMessage = unDeliveredMsgs.get(i);
            xecureMessage.read();
            EntityBareJid jid = null;
            try {
                jid = JidCreate.entityBareFrom(mEntryId + "@chat.xecu.re");
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            AbstractXMPPConnection connection = XecureManager.getInstance().getSmackConnection();
            if(connection != null) {
                try {
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    Chat chat = chatManager.chatWith(jid);
                    Message newMessage = new Message();
                    newMessage.setSubject(xecureMessage.getId());
                    SecurityUtils utils = new SecurityUtils(mKey);
                    newMessage.setBody(utils.encrypt(xecureMessage.getBody()));
                    newMessage.setType(Message.Type.chat);
                    chat.send(newMessage);

                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void replyDelver() {
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(mEntryId + "@chat.xecu.re");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        AbstractXMPPConnection connection = XecureManager.getInstance().getSmackConnection();
        if(connection != null) {
            for (int i = 0; i< mMessages.size(); i++){
                if  (mMessages.get(i).isOutgoing())
                    continue;
                try {
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    Chat chat = chatManager.chatWith(jid);
                    Message response = new Message();
                    response.setSubject(XecureChatRoom.DELIVERED);
                    response.setBody(mMessages.get(i).getSenderid());
                    chat.send(response);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}