package com.XECUREVoIP;

/*
DozeReceiver.java
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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;


import com.XECUREVoIP.Service.XecureService;

import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

/*
 * Purpose of this receiver is to disable keep alives when device is on idle
 * */
public class DozeReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /*PowerManager pm;
        if (!XecureService.isReady()) return;

        boolean isDebugEnabled = XecurePreferences.instance().isDebugEnabled();
        LinphoneCoreFactory.instance().enableLogCollection(isDebugEnabled);
        LinphoneCoreFactory.instance().setDebugMode(isDebugEnabled, context.getString(R.string.app_name));
        LinphoneCore lc = XecureManager.getLcIfManagerNotDestroyedOrNull();
        if (lc == null) return;

        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean dozeM = pm.isDeviceIdleMode();
            Log.i("[DozeReceiver] Idle Mode: " + dozeM);
            XecureManager.getInstance().setDozeModeEnabled(dozeM);
            XecureManager.getInstance().updateNetworkReachability();
        }*/
    }
}
