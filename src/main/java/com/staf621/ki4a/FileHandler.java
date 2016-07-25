package com.staf621.ki4a;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FileHandler extends ArrayAdapter {
    private Context context;
    private int current;
    private List listFiles;

    public FileHandler(Context context, int i, List list) {
        super(context, i, list);
        this.context = context;
        this.current = i;
        this.listFiles = list;
    }

    public FileInfo getFile(int i) {
        return (FileInfo) this.listFiles.get(i);
    }

    public Object getItem(int i) {
        return getFile(i);
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = ((LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(this.current, null);
        }
        FileInfo file = (FileInfo) this.listFiles.get(i);
        if (file != null) {
            ImageView imageView = (ImageView) view.findViewById(R.id.file_icon);
            TextView textView = (TextView) view.findViewById(R.id.file_name);
            if (file.getFileType().equalsIgnoreCase("folder")) {
                imageView.setImageResource(R.drawable.folder);
            } else if (file.getFileType().equalsIgnoreCase("parent")) {
                imageView.setImageResource(R.drawable.parent);
            } else {
                imageView.setImageResource(R.drawable.file);
            }
            if (textView != null) {
                textView.setText(file.getFileName());
            }
        }
        return view;
    }
}
