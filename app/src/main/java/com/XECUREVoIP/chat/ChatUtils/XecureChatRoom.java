package com.XECUREVoIP.chat.ChatUtils;

import android.provider.Settings;
import android.util.Log;

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
    private Chat mChat;
    private XecureDH mDH;
    private boolean keyExchanged = false;
    private boolean accepted = false;

    public XecureChatRoom(String entry) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = null;
        mDH = new XecureDH();
        mDH.generateKeys();
    }

    public XecureChatRoom(String entry, Chat chat, String subject) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
        mChat = chat;
        mDH = new XecureDH();
        mDH.generateKeys();
        receivePublicKey(subject);
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
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            mChat = chatManager.chatWith(jid);
            Message newMessage = new Message();
            try {
                byte[] publicKeyBytes = org.bouncycastle.util.encoders.Base64.encode(mDH.getPublicKey().getEncoded());
                String pubKey = new String(publicKeyBytes);
                newMessage.setSubject(pubKey);
                newMessage.setBody("");
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
            keyExchagned();
        }
    }

    public void keyExchagned(){
        keyExchanged = true;
        mKey = new String(mDH.getKey(), StandardCharsets.US_ASCII);
    }

    public void keyExchange(){
        keyExchanged = true;
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
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            mChat = chatManager.chatWith(jid);
            Message newMessage = new Message();
            try {
                if (!keyExchanged){
                    byte[] publicKeyBytes = org.bouncycastle.util.encoders.Base64.encode(mDH.getPublicKey().getEncoded());
                    String pubKey = new String(publicKeyBytes);
                    newMessage.setSubject(pubKey);
                }
                SecurityUtils utils = new SecurityUtils(mKey);
                newMessage.setBody(utils.encrypt(message));
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
        }
        return true;
    }

    public void setXecureKey(String xecureKey) {
        mKey = xecureKey;
    }
    public String getXecureKey() {
        return mKey;
    }

    public void init(Chat chat) {
        mChat = chat;
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
}