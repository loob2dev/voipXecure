/*
FirebaseIdService.java
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

package com.XECUREVoIP.firebase;

import com.XECUREVoIP.UIThreadDispatcher;
import com.XECUREVoIP.XecurePreferences;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class FirebaseIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        android.util.Log.i("FirebaseIdService", "[Push Notification] Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(final String refreshedToken) {
        android.util.Log.i("FirebaseIdService", "[Push Notification] Send token to server: " + refreshedToken);
        UIThreadDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                XecurePreferences.instance().setPushNotificationRegistrationID(refreshedToken);
            }
        });
    }
}
