package com.lsid.listener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TzbDataChild{
	public String activity_datetime = "";
	public String open_id = "";
	public String nick_name = "";
	public String mobile = "";
	public String action = "money_add";
	public String action_amount = "";

	void setactivity_datetime(String millis) {
		activity_datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(millis)));
	}

	void setopen_id(String oi) {
		open_id = oi;
	}

	void setnick_name(String nn) {
		nick_name = nn;
	}

	void setaction_amount(String aa) {
		action_amount = aa;
	}

}
