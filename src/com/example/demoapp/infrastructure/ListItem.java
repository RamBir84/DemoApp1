package com.example.demoapp.infrastructure;


// This object represent a contacts list
public class ListItem {

	public String contact_name;
	public String Location;   
	public String profile_pic; // URL of the facebook profile picture
	public IconStatus icon_status; //online, request_sent, request_received, answer_received, offline.
	public String uId;
	public String gcm_id;
	public String tagDateTime = "";
	public String school_name;

	//Constructor
	public ListItem(String contact_name, String Location, String profile_pic, IconStatus icon_status, String ID, String gcm_id, String tagDateTime, String school_name) {
		this.contact_name = contact_name;
		this.Location = Location;
		this.profile_pic = profile_pic;
		this.icon_status = icon_status;
		this.uId = ID;
		this.gcm_id = gcm_id;
		this.tagDateTime = tagDateTime;
		this.school_name = school_name;
	}
}
