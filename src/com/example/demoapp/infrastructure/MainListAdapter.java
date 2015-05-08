package com.example.demoapp.infrastructure;

import java.util.ArrayList;

import com.example.demoapp.R;
import com.example.demoapp.R.color;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MainListAdapter extends ArrayAdapter<ListItem> {

	private Context context;
	public static ArrayList<ListItem> items;
	public ImageView imageView;
	

	public MainListAdapter(Context context, ArrayList<ListItem> values) {
		super(context, R.layout.new_list_item, values); 
		this.context = context;
		items = values;
	}

	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.new_list_item, parent, false);
			
		    
			// Set the profile picture(picture, position and status)
			CircleImageView profilePicture = (CircleImageView) rowView.findViewById(R.id.listProfileImage);
		    profilePicture.setTag(Integer.valueOf(position));
		    ExtendedTarget loadtarget = new ExtendedTarget(profilePicture);
		    Picasso.with(context).load(items.get(position).profile_pic).into(loadtarget);
			profilePicture.setTag(loadtarget.refButton.getTag());
			
			if (items.get(position).icon_status == IconStatus.offline){
				profilePicture.setId(5);
			} else {
				profilePicture.setId(1);
			}
			
			this.notifyDataSetChanged();
			
			// Set the contact name
			TextView contactName = (TextView) rowView.findViewById(R.id.listContactName);
			contactName.setText(items.get(position).contact_name);
			
			// Set the contact location
			TextView Location = (TextView) rowView.findViewById(R.id.listLocation);
			Location.setText(items.get(position).Location);

			// Set the contact icon
			ImageButton searchIcon = (ImageButton) rowView.findViewById(R.id.listIconImage);
			Drawable IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_offline);
			searchIcon.setTag(loadtarget.refButton.getTag());
			searchIcon.setTag(new BitmapPosition(null, position));
				// Set the position and ID for onClickListIcon method
			
			switch (items.get(position).icon_status) {
			case online:
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_online_bg);
				searchIcon.setId(1);
				break;
			
			case request_sent:
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_request_sent);
				searchIcon.setId(2);
				break;
			
			case request_received:
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_request_received_bg);
				searchIcon.setId(3);
				break;
				
			case answer_received:
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_answer_received_bg);
				searchIcon.setId(4);
				break;
			default:
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_offline);
				searchIcon.setId(5);
				contactName.setTextColor(color.medium_grey);
				Location.setText(null);
				break;
			}
			/*if (items.get(position).icon_status == IconStatus.online){
				IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_online_bg);
				searchIcon.setId(1);
			} else {
				if (items.get(position).icon_status == IconStatus.request_sent){
					IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_request_sent);
					searchIcon.setId(2);
				} else { 
					if (items.get(position).icon_status == IconStatus.request_received){
						IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_request_received_bg);
						searchIcon.setId(3);
					} else { 
						if (items.get(position).icon_status == IconStatus.answer_received){
							IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_answer_received_bg);
							searchIcon.setId(4);
						} else {
								IconImageAsDrawable = context.getResources().getDrawable(R.drawable.ic_icon_offline);
								searchIcon.setId(5);
								contactName.setTextColor(color.medium_grey);
								Location.setText(null);
							}
						}
					}
				}*/
			searchIcon.setImageDrawable(IconImageAsDrawable);
			
		return rowView;
	}
	
	private class ExtendedTarget implements Target {

		CircleImageView refButton;
		
		
		public ExtendedTarget(CircleImageView refButton) {
			this.refButton = refButton;
		}
		@Override
		public void onBitmapFailed(Drawable arg0) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onBitmapLoaded(Bitmap b, LoadedFrom arg1) {
			Drawable profileImageAsDrawable = new BitmapDrawable(context.getResources(), b);
			refButton.setImageDrawable(profileImageAsDrawable);
			refButton.setTag(new BitmapPosition(b, (Integer) refButton.getTag())); // Put the bitmap and the position in refButton
		}
		@Override
		public void onPrepareLoad(Drawable arg0) {
		}
	}
}
