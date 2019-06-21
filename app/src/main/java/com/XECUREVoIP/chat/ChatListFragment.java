package com.XECUREVoIP.chat;

/*
ChatListFragment.java
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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecureUtils;
import com.XECUREVoIP.chat.ChatUtils.XecureChatMessage;
import com.XECUREVoIP.chat.ChatUtils.XecureChatRoom;

import org.linphone.core.LinphoneCoreListenerBase;

import java.util.ArrayList;
import java.util.Date;

public class ChatListFragment extends Fragment implements OnClickListener, OnItemClickListener {
	private LayoutInflater mInflater;
	private ListView chatList;
	private TextView noChatHistory;
	private ImageView edit, selectAll, deselectAll, delete, newDiscussion, cancel, backInCall;
	private LinearLayout editList, topbar;
	private boolean isEditMode = false;
	private ChatRoomAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		mInflater = inflater;

		View view = inflater.inflate(R.layout.chatlist, container, false);
		chatList = (ListView) view.findViewById(R.id.chatList);
		chatList.setOnItemClickListener(this);
		adapter = new ChatRoomAdapter(XecureManager.getInstance().getXecureChatRooms());
		chatList.setAdapter(adapter);

		noChatHistory = (TextView) view.findViewById(R.id.noChatHistory);

		editList = (LinearLayout) view.findViewById(R.id.edit_list);
		topbar = (LinearLayout) view.findViewById(R.id.top_bar);

		cancel = (ImageView) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		edit = (ImageView) view.findViewById(R.id.edit);
		edit.setOnClickListener(this);

		newDiscussion = (ImageView) view.findViewById(R.id.new_discussion);
		newDiscussion.setOnClickListener(this);

		selectAll = (ImageView) view.findViewById(R.id.select_all);
		selectAll.setOnClickListener(this);

		deselectAll = (ImageView) view.findViewById(R.id.deselect_all);
		deselectAll.setOnClickListener(this);

		backInCall = (ImageView) view.findViewById(R.id.back_in_call);
		backInCall.setOnClickListener(this);

		delete = (ImageView) view.findViewById(R.id.delete);
		delete.setOnClickListener(this);

		XecureService.instance().setChatListNofity(mChatListUpdate);

        if (adapter.getCount() == 0) {
            noChatHistory.setVisibility(View.VISIBLE);
            chatList.setVisibility(View.GONE);
            edit.setEnabled(false);
        }
		XecureService.instance().removeMessageNotification();

		return view;
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id){
			case R.id.new_discussion:
				XecureActivity.instance().displayChat(null, null, null);
				break;
			case R.id.edit:
				topbar.setVisibility(View.GONE);
				editList.setVisibility(View.VISIBLE);
				isEditMode = true;
				hideAndDisplayMessageIfNoChat();
				enabledDeleteButton(false);
				refresh();
				break;
            case R.id.cancel:
                quitEditMode();
                selectAllList(false);
                refresh();
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
			case R.id.delete:
				final Dialog dialog = XecureActivity.instance().displayDialog(getString(R.string.delete_text));
				Button delete = (Button) dialog.findViewById(R.id.delete_button);
				Button cancel = (Button) dialog.findViewById(R.id.cancel);

				delete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						removeChatsConversation();
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
		}
	}

	public void displayFirstChat(){
		XecureActivity.instance().displayEmptyFragment();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		Animation animation = new AlphaAnimation(0.3f, 1.0f);
		animation.setDuration(100);
		view.startAnimation(animation);
		final String sipUri = ((XecureChatRoom)adapter.getItem(i)).getId();
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (XecureActivity.isInstanciated() && !isEditMode) {
					XecureActivity.instance().displayChat(sipUri, null, null);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
	}


	class ChatRoomAdapter extends BaseAdapter{
		private class ViewHolder {
			public TextView lastMessageView;
			public TextView date;
			public TextView displayName;
			public TextView unreadMessages;
			public CheckBox select;
			public ImageView contactPicture;

			public ViewHolder(View view) {
				lastMessageView = (TextView) view.findViewById(R.id.lastMessage);
				date = (TextView) view.findViewById(R.id.date);
				displayName = (TextView) view.findViewById(R.id.sipUri);
				unreadMessages = (TextView) view.findViewById(R.id.unreadMessages);
				select = (CheckBox) view.findViewById(R.id.delete_chatroom);
				contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
			}
		}


		private ArrayList<XecureChatRoom> chatRooms;

		public ChatRoomAdapter(ArrayList<XecureChatRoom> rooms) {
			super();
			chatRooms = rooms;
		}

		@Override
		public int getCount() {
			return chatRooms.size();
		}

		@Override
		public Object getItem(int i) {
			return chatRooms.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(final int i, View view, ViewGroup viewGroup) {
			view = mInflater.inflate(R.layout.chatlist_cell, viewGroup, false);
			ViewHolder holder = new ViewHolder(view);
			view.setTag(holder);

			XecureChatRoom chatRoom = chatRooms.get(i);
			if (chatRoom.getLastMessage() != null){
				XecureChatMessage lastMessage = chatRoom.getLastMessage();
				String message = lastMessage.getBody();
				Date date = lastMessage.getDate();
				holder.date.setText(XecureUtils.timestampToHumanDate(getActivity(), date.getTime(), getString(R.string.messages_list_date_format)));
				holder.lastMessageView.setText(message);
			}
			holder.displayName.setSelected(true); // For animation
			holder.displayName.setText(chatRoom.getAddress());

			int unreadMessagesCount = chatRoom.getUnreadMessagesCount();
			if (unreadMessagesCount > 0) {
				holder.unreadMessages.setVisibility(View.VISIBLE);
				holder.unreadMessages.setText(String.valueOf(unreadMessagesCount));
				if (unreadMessagesCount > 99) {
					holder.unreadMessages.setTextSize(12);
				}
				holder.displayName.setTypeface(null, Typeface.BOLD);
			} else {
				holder.unreadMessages.setVisibility(View.GONE);
				holder.displayName.setTypeface(null, Typeface.NORMAL);
			}
			if (isEditMode) {
				holder.unreadMessages.setVisibility(View.GONE);
				holder.select.setVisibility(View.VISIBLE);
				holder.select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						chatList.setItemChecked(i, b);
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
				if(chatList.isItemChecked(i)) {
					holder.select.setChecked(true);
				} else {
					holder.select.setChecked(false);
				}
			} else {
				if (unreadMessagesCount > 0) {
					holder.unreadMessages.setVisibility(View.VISIBLE);
				}
			}

			return view;
		}

		public void addAll(ArrayList<XecureChatRoom> rooms) {
			chatRooms.addAll(rooms);
		}
	}

	public Handler mChatListUpdate = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			adapter.notifyDataSetChanged();
			refresh();
		}
	};

	private void hideAndDisplayMessageIfNoChat() {
		if (adapter.getCount() == 0) {
			noChatHistory.setVisibility(View.VISIBLE);
			chatList.setVisibility(View.GONE);
			edit.setEnabled(false);
		} else {
			noChatHistory.setVisibility(View.GONE);
			chatList.setVisibility(View.VISIBLE);
			chatList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
			edit.setEnabled(true);
		}
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
		int size = chatList.getAdapter().getCount();
		int nb = 0;
		for(int i=0; i<size; i++) {
			if(chatList.isItemChecked(i)) {
				nb ++;
			}
		}
		return nb;
	}

    public void quitEditMode(){
        isEditMode = false;
        editList.setVisibility(View.GONE);
        topbar.setVisibility(View.VISIBLE);
        refresh();
        if(getResources().getBoolean(R.bool.isTablet)){
            displayFirstChat();
        }
    }

    public void refresh() {
        hideAndDisplayMessageIfNoChat();
		adapter.notifyDataSetChanged();
    }
	private void selectAllList(boolean isSelectAll){
		int size = chatList.getAdapter().getCount();
		for(int i=0; i<size; i++) {
			chatList.setItemChecked(i,isSelectAll);
		}
	}

	private void removeChatsConversation() {
		int size = chatList.getAdapter().getCount();
		ArrayList<XecureChatRoom> willRemove = new ArrayList<XecureChatRoom>();
		for (int i = 0; i < size; i++) {
			if (chatList.isItemChecked(i)) {
				XecureManager.getInstance().getDeletedRooms().add(XecureManager.getInstance().getXecureChatRooms().get(i));
				XecureManager.getInstance().getXecureChatRooms().get(i).getHistory().clear();
			}
		}
		XecureManager.getInstance().getXecureChatRooms().removeAll(	XecureManager.getInstance().getDeletedRooms());
		quitEditMode();
		XecureActivity.instance().updateMissedChatCount();
	}
}