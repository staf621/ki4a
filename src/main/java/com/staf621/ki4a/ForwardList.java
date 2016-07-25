package com.staf621.ki4a;

import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ForwardList extends ListFragment {

    private static ForwardHandler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            List<ForwardInfo> arrayList = ForwardList.loadForwardInfo(getActivity());
            if(arrayList.isEmpty()) arrayList.add(new ForwardInfo());
            handler = new ForwardHandler(getActivity(), arrayList);
        }
        setListAdapter(handler);
    }

    protected static void addForward() {
        handler.add(new ForwardInfo());
    }

    protected static String getForwardString(Context context) {
        String forwardString = "";
        List<ForwardInfo> arrayList  = ForwardList.loadForwardInfo(context);
        for(ForwardInfo info : arrayList) {
            forwardString += ((info.is_local()?" -L ":" -R ") + info.getSrc_port()+":"+info.getDest_host()+":"+info.getDest_port());
        }
        return forwardString;
    }

    protected static List<ForwardInfo> loadForwardInfo(Context context) {
        List<ForwardInfo> arrayList = new ArrayList<>();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonString = settings.getString("port_forward_json", "");
        if(jsonString.isEmpty()) return arrayList;
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                arrayList.add(ForwardInfo.getForwardInfo(obj));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    protected static void saveForwardInfo(Context context) {
        List<ForwardInfo> infoList = handler.getListForwards();
        JSONArray jsonArray = new JSONArray();
        for(ForwardInfo info : infoList) {
            if(!info.getSrc_port().isEmpty() && !info.getDest_host().isEmpty() && !info.getDest_port().isEmpty())
                jsonArray.put(info.getJSONObject());
        }
        MyLog.d(Util.TAG,"ForwardJson = " + jsonArray.toString());
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString("port_forward_json",jsonArray.toString()).commit();
    }

}
