package com.XECUREVoIP.compatibility;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;

/*ApiNineteenPlus.java
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

@TargetApi(19)
public class ApiNineteenPlus {
	public static void scheduleAlarm(AlarmManager alarmManager, int type, long triggerAtMillis, PendingIntent operation) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			alarmManager.setExact(type, triggerAtMillis, operation);
		} else {
			alarmManager.set(type, triggerAtMillis, operation);
		}
	}
}
