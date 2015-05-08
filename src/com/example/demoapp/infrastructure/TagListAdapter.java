package com.example.demoapp.infrastructure;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.demoapp.R;

public class TagListAdapter extends ArrayAdapter<ListTagItem> {

	private Context context;
	public static ArrayList<ListTagItem> items;

	/**
	 * Adapter for main list objects
	 * 
	 * @param context
	 * @param values
	 *        Array list of ListTagItem objects
	 */
	public TagListAdapter(Context context, ArrayList<ListTagItem> values) {
		super(context, R.layout.new_list_item, values); 
		this.context = context;
		items = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.list_tag_item, parent, false);

			// Set the tag content
			TextView content = (TextView) rowView.findViewById(R.id.listTagContent);
			content.setText(items.get(position).tag);
			
			// Set the Item position
			LinearLayout listItem = (LinearLayout) rowView.findViewById(R.id.tag_list_item);
			listItem.setTag(Integer.valueOf(position));
			
		return rowView;
	}

}
