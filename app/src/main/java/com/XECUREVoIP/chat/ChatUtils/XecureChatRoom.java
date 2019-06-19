package com.XECUREVoIP.chat.ChatUtils;

import android.util.Log;

import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.security.SecurityUtils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

public class XecureChatRoom {
    private String mEntryId;
    private ArrayList<XecureChatMessage> mMessages;
    private final String TAG = "XECURE CHAT";
    private byte[] mKey;
    private Chat mChat;
    private XecureDH mDH;
    private SecurityUtils mAES256;
    private boolean keyExchanged = false;
    private boolean accepted = false;

    public XecureChatRoom(String entry) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = null;
        mDH = new XecureDH();
        mDH.generateKeys();
        mAES256 = new SecurityUtils("xecurechat");
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
        EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getBytes());
        KeyFactory keyFactory = null;
        PublicKey pubKey = null;
        try {
            keyFactory = KeyFactory.getInstance("DH");
            pubKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }finally {
            mDH.receivePublicKeyFrom(pubKey);
            mDH.generateCommonSecretKey();
        }
    }

    public void sendPublicKey(){
        //share public key
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
                byte[] publicKeyBytes = Base64.encode(mDH.getPublicKey().getEncoded());
                String pubKey = new String(publicKeyBytes);
                newMessage.setSubject(pubKey);
                newMessage.setType(Message.Type.normal);
                mChat.send(newMessage);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mDH.generateCommonSecretKey();
            }
        }
    }

    public void keyExchagned(){
        keyExchanged = true;
    }

    public XecureChatRoom(String entry, Chat chat, String subject) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = chat;
        mDH = new XecureDH();
        mDH.generateKeys();
        mAES256 = new SecurityUtils("xecurechat");
        receivePublicKey(subject);
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
                    byte[] publicKeyBytes = mDH.getPublicKey().getEncoded();
                    String pubKey = new String(publicKeyBytes);
                    newMessage.setSubject(pubKey);
                    //encrypt message
                    newMessage.setBody(mAES256.encrypt(message));
                } else {
                    newMessage.setBody(mDH.encrypt(message));
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
        if (keyExchanged){
            message.setBody(mDH.decrypt(message.getBody()));
            mMessages.add(message);
        }else {
            try {
                message.setBody(mAES256.decrypt(message.getBody()));
                mMessages.add(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setXecureKey(byte[] xecureKey) {
        mKey = xecureKey;
    }

    public void init(Chat chat) {
        mChat = chat;
    }
}
