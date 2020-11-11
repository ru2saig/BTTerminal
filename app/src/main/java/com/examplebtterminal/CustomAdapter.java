package com.examplebtterminal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {

    Context c;
    ArrayList<SingleRow> deviceList;

    CustomAdapter(Context c, ArrayList<SingleRow> arrayList){
        this.c  = c;
        this.deviceList = arrayList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        LayoutInflater inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.custom_listview,parent,false);
        TextView deviceNameText = (TextView) row.findViewById(R.id.deviceName);
        TextView macAddressText = (TextView) row.findViewById(R.id.macAddress);
        SingleRow temp_obj  = deviceList.get(position);
        deviceNameText.setText(temp_obj.deviceName);
        macAddressText.setText(temp_obj.deviceAddress);

        return row;

    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }
}
