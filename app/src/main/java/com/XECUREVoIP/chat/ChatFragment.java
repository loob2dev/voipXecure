package com.XECUREVoIP.chat;

/*
ChatFragment.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecureUtils;
import com.XECUREVoIP.chat.ChatUtils.XecureChatMessage;
import com.XECUREVoIP.chat.ChatUtils.XecureChatRoom;
import com.XECUREVoIP.chat.ChatUtils.XecureDH;
import com.XECUREVoIP.compatibility.Compatibility;
import com.XECUREVoIP.contacts.ContactEditorFragment;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jxmpp.jid.EntityBareJid;

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;

public class ChatFragment extends Fragment implements OnClickListener{
	private String sipUri;
	private EditText message;
	private ImageView edit, selectAll, deselectAll, startCall, delete, sendImage, sendMessage, cancel;
	private TextView contactName, remoteComposing, waiting_accept;
	private ImageView back, backToCall;
	private EditText searchContactField;
	private LinearLayout topBar, editList, footer, button_group;
	private ListView messagesList, resultContactsSearch;
	private Button accept, block;
	private LayoutInflater inflater;
	private Bitmap defaultBitmap;

	private boolean isEditMode = false;
	private XecureContact contact;
	private Uri imageToUploadUri;
	private String filePathToUpload;
	private TextWatcher textWatcher;

	private XecureChatRoom mChatRoom;

	private boolean newChatConversation = false;

	private XecureChatMessageAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final View view = inflater.inflate(R.layout.chat, container, false);

		contactName = (TextView) view.findViewById(R.id.contact_name);
		messagesList = (ListView) view.findViewById(R.id.chat_message_list);
		registerForContextMenu(messagesList);

		searchContactField = (EditText) view.findViewById(R.id.search_contact_field);
		resultContactsSearch = (ListView) view.findViewById(R.id.result_contacts);

		editList = (LinearLayout) view.findViewById(R.id.edit_list);
		topBar = (LinearLayout) view.findViewById(R.id.top_bar);
		footer = (LinearLayout) view.findViewById(R.id.footer);
		button_group = (LinearLayout) view.findViewById(R.id.button_group);

		sendMessage = (ImageView) view.findViewById(R.id.send_message);
		sendMessage.setOnClickListener(this);
		sendMessage.setEnabled(false);

		remoteComposing = (TextView) view.findViewById(R.id.remote_composing);
		remoteComposing.setVisibility(View.GONE);

		waiting_accept = (TextView) view.findViewById(R.id.waiting_accept);

		cancel = (ImageView) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		edit = (ImageView) view.findViewById(R.id.edit);
		edit.setOnClickListener(this);

		startCall = (ImageView) view.findViewById(R.id.start_call);
		startCall.setOnClickListener(this);

		backToCall = (ImageView) view.findViewById(R.id.back_to_call);
		backToCall.setOnClickListener(this);

		selectAll = (ImageView) view.findViewById(R.id.select_all);
		selectAll.setOnClickListener(this);

		deselectAll = (ImageView) view.findViewById(R.id.deselect_all);
		deselectAll.setOnClickListener(this);

		delete = (ImageView) view.findViewById(R.id.delete);
		delete.setOnClickListener(this);

		back = (ImageView) view.findViewById(R.id.back);
		back.setOnClickListener(this);

		message = (EditText) view.findViewById(R.id.message);
		message.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				if (charSequence.length() == 0)
					sendMessage.setEnabled(false);
				else
					sendMessage.setEnabled(true);
			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		accept = (Button) view.findViewById(R.id.accept);
		accept.setOnClickListener(this);
		block = (Button) view.findViewById(R.id.block);
		block.setOnClickListener(this);

		if(getArguments() == null || getArguments().getString("entryId") == null) {
			newChatConversation = true;
		} else {
			//Retrieve parameter from intent
			sipUri = getArguments().getString("entryId");
			newChatConversation = false;
		}

		if (newChatConversation) {
			initNewChatConversation();
		}else{
			String address = getArguments().getString("entryId");
			ArrayList<XecureChatRoom> chatRooms = XecureManager.getInstance().getXecureChatRooms();
			for (XecureChatRoom room : chatRooms){
				if (room.getAddress().compareTo(address) != 0)
					continue;
				mChatRoom = room;
				for (XecureChatMessage message : mChatRoom.getHistory()){
					message.read();
				}
				break;
			}
			exitNewConversationMode();

			adapter = new XecureChatMessageAdapter(getActivity(), mChatRoom.getHistory());
			messagesList.setAdapter(adapter);
			messagesList.deferNotifyDataSetChanged();
			messagesList.setSelection(adapter.getCount() - 1);
			((XecureActivity)getActivity()).updateMissedChatCount();
		}

		XecureService.instance().setChatHandler(mReceiveMessage);
		return view;
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id){
			case R.id.back:
				getFragmentManager().popBackStackImmediate();
				XecureService.instance().setChatHandler(null);
				break;
			case R.id.send_message:
				sendTextMessage();
				break;
			case R.id.edit:
				topBar.setVisibility(View.INVISIBLE);
				editList.setVisibility(View.VISIBLE);
				isEditMode = true;
				redrawMessageList();
				break;
			case R.id.select_all:
				deselectAll.setVisibility(View.VISIBLE);
				selectAll.setVisibility(View.GONE);
				enabledDeleteButton(true);
				selectAllList(true);
				break;
			case R.id.deselect_all:
				deselectAll.setVisibility(View.GONE);
				selectAll.setVisibility(View.VISIBLE);
				enabledDeleteButton(false);
				selectAllList(false);
				break;
			case R.id.cancel:
				quitEditMode();
				break;
			case R.id.delete:
				final Dialog dialog = XecureActivity.instance().displayDialog(getString(R.string.delete_text));
				Button delete = (Button) dialog.findViewById(R.id.delete_button);
				Button cancel = (Button) dialog.findViewById(R.id.cancel);

				delete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						removeChats();
						dialog.dismiss();
						quitEditMode();
					}
				});

				cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						dialog.dismiss();
						quitEditMode();
					}
				});
				dialog.show();
				break;
			case R.id.start_call:
				XecureActivity.instance().setAddresGoToDialerAndCall(XecureUtils.getDisplayableUsernameFromAddress(sipUri), sipUri, null);
				break;
			case R.id.accept:
				mChatRoom.accept();
				mChatRoom.sendPublicKey();
				exitNewConversationMode();
				break;
			case R.id.block:
				break;
		}
	}

	private void sendTextMessage() {
		if (newChatConversation) {
			mChatRoom = new XecureChatRoom(searchContactField.getText().toString());
			XecureManager.getInstance().getXecureChatRooms().add(mChatRoom);
			adapter = new XecureChatMessageAdapter(getActivity(), mChatRoom.getHistory());
			messagesList.setAdapter(adapter);
			mChatRoom.accept();
			exitNewConversationMode();
			sipUri = searchContactField.getText().toString();
		}
		mChatRoom.sendMessage(message.getText().toString());
		message.setText("");
		adapter.notifyDataSetChanged();
		messagesList.setSelection(adapter.getCount() - 1);
	}

	public void changeDisplayedChat(String newSipUri, String displayName, String pictureUri, String message, String fileUri) {

	}

	private void initNewChatConversation(){
		newChatConversation = true;
		messagesList.setVisibility(View.GONE);
		edit.setVisibility(View.INVISIBLE);
		startCall.setVisibility(View.INVISIBLE);
		contactName.setVisibility(View.INVISIBLE);
		resultContactsSearch.setVisibility(View.VISIBLE);
		searchContactField.setVisibility(View.VISIBLE);
		searchContactField.setText("");
//		searchContactField.requestFocus();
	}

	private Spanned getTextWithHttpLinks(String text) {
		if (text.contains("<")) {
			text = text.replace("<", "&lt;");
		}
		if (text.contains(">")) {
			text = text.replace(">", "&gt;");
		}
		if (text.contains("\n")) {
			text = text.replace("\n", "<br>");
		}
		if (text.contains("http://")) {
			int indexHttp = text.indexOf("http://");
			int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
			String link = text.substring(indexHttp, indexFinHttp);
			String linkWithoutScheme = link.replace("http://", "");
			text = text.replaceFirst(Pattern.quote(link), "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
		}
		if (text.contains("https://")) {
			int indexHttp = text.indexOf("https://");
			int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
			String link = text.substring(indexHttp, indexFinHttp);
			String linkWithoutScheme = link.replace("https://", "");
			text = text.replaceFirst(Pattern.quote(link), "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
		}

		return Compatibility.fromHtml(text);
	}

	private class XecureChatMessageAdapter extends BaseAdapter{
		class ViewHolder{
			public int id;
			public RelativeLayout bubbleLayout;
			public CheckBox delete;
			public LinearLayout background;
			public ImageView contactPicture;
			public TextView contactName;
			public TextView messageText;
			public ImageView messageImage;
			public RelativeLayout fileTransferLayout;
			public ProgressBar fileTransferProgressBar;
			public Button fileTransferAction;
			public ImageView messageStatus;
			public ProgressBar messageSendingInProgress;
			public ImageView contactPictureMask;
			public LinearLayout imdmLayout;
			public ImageView imdmIcon;
			public TextView imdmLabel;
			public TextView fileExtensionLabel;
			public TextView fileNameLabel;

			public ViewHolder(View view) {
				id = view.getId();
				bubbleLayout = (RelativeLayout) view.findViewById(R.id.bubble);
				delete = (CheckBox) view.findViewById(R.id.delete_message);
				background = (LinearLayout) view.findViewById(R.id.background);
				contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
				contactName = (TextView) view.findViewById(R.id.contact_header);
				messageText = (TextView) view.findViewById(R.id.message);
				messageImage = (ImageView) view.findViewById(R.id.image);
				fileTransferLayout = (RelativeLayout) view.findViewById(R.id.file_transfer_layout);
				fileTransferProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
				fileTransferAction = (Button) view.findViewById(R.id.file_transfer_action);
				messageStatus = (ImageView) view.findViewById(R.id.status);
				messageSendingInProgress = (ProgressBar) view.findViewById(R.id.inprogress);
				contactPictureMask = (ImageView) view.findViewById(R.id.mask);
				imdmLayout = (LinearLayout) view.findViewById(R.id.imdmLayout);
				imdmIcon = (ImageView) view.findViewById(R.id.imdmIcon);
				imdmLabel = (TextView) view.findViewById(R.id.imdmText);
				fileExtensionLabel = (TextView) view.findViewById(R.id.file_extension);
				fileNameLabel = (TextView) view.findViewById(R.id.file_name);
			}
		}
		ArrayList<XecureChatMessage> messages = null;
		Context context;

		public XecureChatMessageAdapter(Context cnt, ArrayList<XecureChatMessage> msgs) {
			super();
			messages = msgs;
			context = cnt;
		}

		@Override
		public int getCount() {
			return messages.size();
		}

		@Override
		public Object getItem(int i) {
			return messages.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(final int i, View view, ViewGroup viewGroup) {
			XecureChatMessage message = messages.get(i);
			view = LayoutInflater.from(context).inflate(R.layout.chat_bubble, null);

			String displayName = mChatRoom.getAddress();

			final ViewHolder holder = new ViewHolder(view);
			view.setTag(holder);
			holder.delete.setVisibility(View.GONE);
			holder.messageText.setVisibility(View.GONE);
			holder.messageImage.setVisibility(View.GONE);
			holder.fileExtensionLabel.setVisibility(View.GONE);
			holder.fileNameLabel.setVisibility(View.GONE);
			holder.fileTransferLayout.setVisibility(View.GONE);
			holder.fileTransferProgressBar.setProgress(0);
			holder.fileTransferAction.setEnabled(true);
			holder.messageStatus.setVisibility(View.INVISIBLE);
			holder.messageSendingInProgress.setVisibility(View.GONE);

			holder.contactName.setText(timestampToHumanDate(context, message.getDate().getTime()) + " - " + displayName);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if(message.isOutgoing()){
				holder.imdmLayout.setVisibility(View.VISIBLE);
				holder.imdmIcon.setImageResource(R.drawable.chat_delivered);
				holder.imdmLabel.setText(R.string.delivered);
				holder.imdmLabel.setTextColor(getResources().getColor(R.color.colorD));
				holder.messageText.setTextColor(R.color.colorQ);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				layoutParams.setMargins(100, 10, 10, 10);
				holder.background.setBackgroundResource(R.drawable.resizable_chat_bubble_outgoing);
				Compatibility.setTextAppearance(holder.contactName, getActivity(), R.style.font3);
				Compatibility.setTextAppearance(holder.fileTransferAction, getActivity(), R.style.font15);
				holder.fileTransferAction.setBackgroundResource(R.drawable.resizable_confirm_delete_button);
				holder.contactPictureMask.setImageResource(R.drawable.avatar_chat_mask_outgoing);

			}else {
				holder.messageText.setTextColor(R.color.colorR);
				holder.imdmLayout.setVisibility(View.INVISIBLE);
				holder.fileTransferAction.setText(getString(R.string.accept));
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				layoutParams.setMargins(10, 10, 100, 10);
				holder.background.setBackgroundResource(R.drawable.resizable_chat_bubble_incoming);
				Compatibility.setTextAppearance(holder.contactName, getActivity(), R.style.font9);
				Compatibility.setTextAppearance(holder.fileTransferAction, getActivity(), R.style.font8);
				holder.fileTransferAction.setBackgroundResource(R.drawable.resizable_assistant_button);
				holder.contactPictureMask.setImageResource(R.drawable.avatar_chat_mask);
			}
			holder.bubbleLayout.setLayoutParams(layoutParams);

			String msg = message.getBody();
			if (msg != null) {
				Spanned text = getTextWithHttpLinks(msg);
				holder.messageText.setText(text);
				holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
				holder.messageText.setVisibility(View.VISIBLE);
			}
			if (isEditMode) {
				holder.delete.setVisibility(View.VISIBLE);
				holder.delete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						messagesList.setItemChecked(i, b);
						if (getNbItemsChecked() == getCount()) {
							deselectAll.setVisibility(View.VISIBLE);
							selectAll.setVisibility(View.GONE);
							enabledDeleteButton(true);
						} else {
							if (getNbItemsChecked() == 0) {
								deselectAll.setVisibility(View.GONE);
								selectAll.setVisibility(View.VISIBLE);
								enabledDeleteButton(false);
							} else {
								deselectAll.setVisibility(View.GONE);
								selectAll.setVisibility(View.VISIBLE);
								enabledDeleteButton(true);
							}
						}
					}
				});

				if (messagesList.isItemChecked(i)) {
					holder.delete.setChecked(true);
				} else {
					holder.delete.setChecked(false);
				}
			}

			return view;
		}

		private String timestampToHumanDate(Context context, long timestamp) {
			try {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(timestamp);

				SimpleDateFormat dateFormat;
				if (isToday(cal)) {
					dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.today_date_format));
				} else {
					dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.messages_date_format));
				}

				return dateFormat.format(cal.getTime());
			} catch (NumberFormatException nfe) {
				return String.valueOf(timestamp);
			}
		}
		private boolean isToday(Calendar cal) {
			return isSameDay(cal, Calendar.getInstance());
		}

		private boolean isSameDay(Calendar cal1, Calendar cal2) {
			if (cal1 == null || cal2 == null) {
				return false;
			}

			return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
					cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
					cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
		}
	}
	private void exitNewConversationMode() {
		searchContactField.setVisibility(View.GONE);
		resultContactsSearch.setVisibility(View.GONE);
		messagesList.setVisibility(View.VISIBLE);
		contactName.setVisibility(View.VISIBLE);
		edit.setVisibility(View.VISIBLE);
		startCall.setVisibility(View.VISIBLE);
		if (mChatRoom.isAccept() && !mChatRoom.isExchanged()){
			footer.setVisibility(View.VISIBLE);
			waiting_accept.setText(R.string.waiting_accept);
			waiting_accept.setVisibility(View.VISIBLE);
			button_group.setVisibility(View.GONE);
		}else if(!mChatRoom.isAccept() && !mChatRoom.isExchanged()){
			footer.setVisibility(View.GONE);
			waiting_accept.setText(mChatRoom.getAddress() + getResources().getString(R.string.accept_waiting));
			waiting_accept.setVisibility(View.VISIBLE);
			button_group.setVisibility(View.VISIBLE);
		}else{
			footer.setVisibility(View.VISIBLE);
			waiting_accept.setText(mChatRoom.getAddress() + " " + getResources().getString(R.string.accept_waiting));
			waiting_accept.setVisibility(View.GONE);
			button_group.setVisibility(View.GONE);
		}


		if(getResources().getBoolean(R.bool.isTablet)){
			back.setVisibility(View.INVISIBLE);
		} else {
			back.setOnClickListener(this);
		}

		newChatConversation = false;
	}

	private Handler mReceiveMessage = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String id = bundle.getString("idFrom");

			if (id != null && id.compareTo(mChatRoom.getAddress()) == 0){
				XecureChatMessage message = (XecureChatMessage) msg.obj;
				message.read();
				adapter.notifyDataSetChanged();
				messagesList.deferNotifyDataSetChanged();
				messagesList.setSelection(adapter.getCount() - 1);

			}
			XecureActivity.instance().updateMissedChatCount();
			super.handleMessage(msg);
		}
	};

	private void redrawMessageList() {
		if (adapter != null) {
			adapter.notifyDataSetInvalidated();
		}
		//messagesList.invalidateViews();
	}

	public void enabledDeleteButton(Boolean enabled){
		if(enabled){
			delete.setEnabled(true);
		} else {
			if (getNbItemsChecked() == 0){
				delete.setEnabled(false);
			}
		}
	}

	public int getNbItemsChecked(){
		int size = messagesList.getAdapter().getCount();
		int nb = 0;
		for(int i=0; i<size; i++) {
			if(messagesList.isItemChecked(i)) {
				nb ++;
			}
		}
		return nb;
	}
	private void selectAllList(boolean isSelectAll) {
		int size = messagesList.getAdapter().getCount();
		for (int i = 0; i < size; i++) {
			messagesList.setItemChecked(i, isSelectAll);
		}
	}

	public void quitEditMode(){
		isEditMode = false;
		editList.setVisibility(View.GONE);
		topBar.setVisibility(View.VISIBLE);
		redrawMessageList();
	}

	private void removeChats(){
		int size = messagesList.getAdapter().getCount();
		ArrayList<XecureChatMessage> willRemove = new ArrayList<XecureChatMessage>();
		for (int i = 0; i < size; i++) {
			if (messagesList.isItemChecked(i)) {
				willRemove.add(mChatRoom.getHistory().get(i));
			}
		}
//		if (willRemove.size() == mChatRoom.getHistory().size()){
//		    XecureManager.getInstance().getXecureChatRooms().remove(mChatRoom);
//            getFragmentManager().popBackStackImmediate();
//            XecureService.instance().setChatHandler(null);
//            return;
//        }
		mChatRoom.getHistory().removeAll(willRemove);
		adapter.notifyDataSetChanged();
	}
}
