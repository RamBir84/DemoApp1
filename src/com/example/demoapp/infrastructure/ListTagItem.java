package com.example.demoapp.infrastructure;

import android.location.Location;


public class ListTagItem {

	public String tag;
	public Location tag_location;
	
	public ListTagItem(String tag, Location tag_location){
		this.tag = tag;
		this.tag_location = tag_location;
	}
	
	

}

