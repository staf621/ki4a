package com.staf621.ki4a;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

public class ForwardHandler extends ArrayAdapter<ForwardInfo> {
    private List<ForwardInfo> listForwards;

    private static class ViewHolder {
        TextView text_src_port;
        TextView text_dest_host;
        TextView text_dest_port;
        RadioButton radio_local;
        RadioButton radio_remote;
        Button delete_button;
    }

    public ForwardHandler(Context context, List<ForwardInfo> list) {
        super(context, R.layout.forward_item, list);
        this.listForwards = list;
    }

    public void add(ForwardInfo fi) {
        this.listForwards.add(fi);
        notifyDataSetChanged();
    }

    public void delete(ForwardInfo fi) {
        this.listForwards.remove(fi);
        notifyDataSetChanged();
    }

    public List<ForwardInfo>getListForwards() {
        return this.listForwards;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        ForwardInfo fi = getItem(i);
        final int myI = i;

        ViewHolder viewHolder; // view lookup cache stored in tag
        if (view == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.forward_item, viewGroup, false);
            viewHolder.text_src_port = (TextView) view.findViewById(R.id.source_Port);
            viewHolder.text_dest_host = (TextView) view.findViewById(R.id.dest_Host);
            viewHolder.text_dest_port = (TextView) view.findViewById(R.id.dest_Port);
            viewHolder.radio_local = (RadioButton) view.findViewById(R.id.local_Radio);
            viewHolder.radio_remote = (RadioButton) view.findViewById(R.id.remote_Radio);
            viewHolder.delete_button = (Button) view.findViewById(R.id.delete_button);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if (fi != null) {
            if (viewHolder.text_src_port != null) {
                viewHolder.text_src_port.setText(fi.getSrc_port());
                viewHolder.text_src_port.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            getItem(myI).setSrc_port(((EditText) v).getText().toString());
                        }
                    }
                });
            }
            if (viewHolder.text_dest_host != null) {
                viewHolder.text_dest_host.setText(fi.getDest_host());
                viewHolder.text_dest_host.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            getItem(myI).setDest_host(((EditText) v).getText().toString());
                        }
                    }
                });
            }
            if (viewHolder.text_dest_port != null) {
                viewHolder.text_dest_port.setText(fi.getDest_port());
                viewHolder.text_dest_port.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            getItem(myI).setDest_port(((EditText) v).getText().toString());
                        }
                    }
                });
            }
            if(viewHolder.radio_local != null && viewHolder.radio_remote != null) {
                if(fi.is_local()) viewHolder.radio_local.setChecked(true);
                else viewHolder.radio_remote.setChecked(true);

                viewHolder.radio_local.setOnClickListener(new CompoundButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getItem(myI).set_local(((RadioButton) v).isChecked());
                    }
                });

                viewHolder.radio_remote.setOnClickListener(new CompoundButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getItem(myI).set_local(!((RadioButton) v).isChecked());
                    }
                });
            }
            if(viewHolder.delete_button !=null) {
                final ForwardHandler myFH = this;
                final ViewHolder myVH = viewHolder;
                viewHolder.delete_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remove focus change listeners
                        myVH.text_src_port.setOnFocusChangeListener(null);
                        myVH.text_dest_host.setOnFocusChangeListener(null);
                        myVH.text_dest_port.setOnFocusChangeListener(null);
                        // Delete array element
                        myFH.delete(getItem(myI));
                    }
                });
            }
        }

        return view;
    }

}
