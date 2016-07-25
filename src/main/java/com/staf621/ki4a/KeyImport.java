package com.staf621.ki4a;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyImport extends ListActivity {
    private File currentDir;
    private FileHandler handler;

    private void show(File file) {
        File[] listFiles = file.listFiles();
        setTitle(file.getPath());
        List arrayList = new ArrayList();
        try {
            for (File file2 : listFiles) {
                if (!file2.isHidden()) {
                    arrayList.add(new FileInfo(file2.getName(), file2.isDirectory()?"Folder":"File", file2.getAbsolutePath(), file2.isDirectory(), false));
                }
            }
        } catch (Exception e) {
        }
        Collections.sort(arrayList);
        if (!(file.getName().equalsIgnoreCase("/") || file.getParentFile() == null)) {
            arrayList.add(0, new FileInfo("..", "Parent", file.getParent(), false, true));
        }

        this.handler = new FileHandler(this, R.layout.file_view, arrayList);
        setListAdapter(this.handler);
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.currentDir = new File("/sdcard/");
        show(this.currentDir);
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 4) {
            return super.onKeyDown(i, keyEvent);
        }
        if (this.currentDir.getName().equals("/") || this.currentDir.getParentFile() == null) {
            finish();
        } else {
            this.currentDir = this.currentDir.getParentFile();
            show(this.currentDir);
        }
        return false;
    }

    @SuppressLint("CommitPrefEdits")
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        super.onListItemClick(listView, view, i, j);
        FileInfo file = this.handler.getFile(i);
        if (file.isDirectory() || file.isParent()) {
            this.currentDir = new File(file.getFilePath());
            show(this.currentDir);
        } else {
            //A file has been selected, let's open it and change the value for key field
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            try {
                byte[] buffer = new byte[4096];
                InputStream in = new FileInputStream(file.getFilePath());

                if(in.read(buffer) > 0)
                    settings.edit().putString("key_text",new String(buffer)).commit();

                in.close();
            } catch (Exception e) {
                MyLog.e(Util.TAG, "Error importing key file", e);
            }
            finish();
        }
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        finish();
        return super.onOptionsItemSelected(menuItem);
    }
}