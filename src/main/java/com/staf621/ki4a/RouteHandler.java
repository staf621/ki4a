package com.staf621.ki4a;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class RouteHandler extends ArrayAdapter<RouteInfo> {
    private List<RouteInfo> listRoutes;

    private static class ViewHolder {
        TextView text_route_address;
        TextView text_prefix_length;
        Button delete_button;
    }

    public RouteHandler(Context context, List<RouteInfo> list) {
        super(context, R.layout.route_item, list);
        this.listRoutes = list;
    }

    public void add(RouteInfo ri) {
        this.listRoutes.add(ri);
        notifyDataSetChanged();
    }

    public void delete(RouteInfo ri) {
        this.listRoutes.remove(ri);
        notifyDataSetChanged();
    }

    public List<RouteInfo>getListRoutes() {
        return this.listRoutes;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        RouteInfo ri = getItem(i);
        final int myI = i;

        ViewHolder viewHolder; // view lookup cache stored in tag
        if (view == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.route_item, viewGroup, false);
            viewHolder.text_route_address = (TextView) view.findViewById(R.id.route_address);
            viewHolder.text_prefix_length = (TextView) view.findViewById(R.id.prefix_length);
            viewHolder.delete_button = (Button) view.findViewById(R.id.delete_button);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if (ri != null) {
            if (viewHolder.text_route_address != null) {
                viewHolder.text_route_address.setText(ri.getRoute_address());
                viewHolder.text_route_address.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            getItem(myI).setRoute_address(((EditText) v).getText().toString());
                        }
                    }
                });
            }
            if (viewHolder.text_prefix_length != null) {
                viewHolder.text_prefix_length.setText(ri.getPrefix_length());
                viewHolder.text_prefix_length.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            getItem(myI).setPrefix_length(((EditText) v).getText().toString());
                        }
                    }
                });
            }
            if(viewHolder.delete_button !=null) {
                final RouteHandler myRH = this;
                final ViewHolder myVH = viewHolder;
                viewHolder.delete_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remove focus change listeners
                        myVH.text_route_address.setOnFocusChangeListener(null);
                        myVH.text_prefix_length.setOnFocusChangeListener(null);
                        // Delete array element
                        myRH.delete(getItem(myI));
                    }
                });
            }
        }

        return view;
    }

}
