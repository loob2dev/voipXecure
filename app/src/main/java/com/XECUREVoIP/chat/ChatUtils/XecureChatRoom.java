package com.XECUREVoIP.chat.ChatUtils;

import android.util.Log;

import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;

public class XecureChatRoom {
    private String mEntryId;
    private ArrayList<XecureChatMessage> mMessages;
    private final String TAG = "XECURE CHAT";

    public XecureChatRoom(String entry) {
        mEntryId = entry;
        mMessages = new ArrayList<XecureChatMessage>();
    }

    public XecureChatMessage sendMessage(String message) {
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
            newMessage.setBody(message);

            try {
                chat.send(newMessage);

            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
            mMessages.add(xecureMessage);
        }
        return xecureMessage;
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
}
