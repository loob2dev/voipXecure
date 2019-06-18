package com.XECUREVoIP.chat.ChatUtils;

import android.util.Base64;
import android.util.Log;

import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.security.SecurityUtils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.security.PublicKey;
import java.util.ArrayList;

public class XecureChatRoom {
    private String mEntryId;
    private ArrayList<XecureChatMessage> mMessages;
    private final String TAG = "XECURE CHAT";
    private byte[] mKey;
    private Chat mChat;
    private XecureDH mDH;
    private boolean keyExchanged = false;

    public XecureChatRoom(String entry) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = null;
        mDH = new XecureDH();
        mDH.generateKeys();
    }

    public boolean isAccept(){
        return keyExchanged;
    }

    public XecureChatRoom(String entry, Chat chat, String subject) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = chat;
        mDH = new XecureDH();
        mDH.generateKeys();
        if (subject == null){

        }
    }

    public void sendMessage(String message) {
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
            if (mChat == null){
                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                mChat = chatManager.chatWith(jid);
            }
            Message newMessage = new Message();
            try {
                if (!keyExchanged){
                    byte[] publicKeyBytes = Base64.encode(mDH.getPublicKey().getEncoded(),0);
                    String pubKey = new String(publicKeyBytes);
                    newMessage.setSubject(pubKey);
                    //encrypt message
                    SecurityUtils utils = new SecurityUtils("xecurechat");
                    newMessage.setBody(utils.encrypt(message));
                } else {

                }
                mChat.send(newMessage);

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
    }

    public XecureChatMessage getLastMessage() {
        if (mMessages.size() == 0)
            return null;
        return mMessages.get(mMessages.size() - 1);
    }

    public String getAddress() {
        return mEntryId;
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

    public void createNewMessage(XecureChatMessage message) {
        mMessages.add(message);
    }

    public void setXecureKey(byte[] xecureKey) {
        mKey = xecureKey;
    }

    public void init(Chat chat) {
        mChat = chat;
    }
}
