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

public class RouteList extends ListFragment {

    private static RouteHandler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            List<RouteInfo> arrayList = RouteList.loadRouteInfo(getActivity());
            if(arrayList.isEmpty()) arrayList.add(new RouteInfo());
            handler = new RouteHandler(getActivity(), arrayList);
        }
        setListAdapter(handler);
    }

    protected static void addRoute() {
        handler.add(new RouteInfo());
    }

    protected static List<RouteInfo> getRoutes(Context context) {
        return RouteList.loadRouteInfo(context);
    }

    protected static List<RouteInfo> loadRouteInfo(Context context) {
        List<RouteInfo> arrayList = new ArrayList<>();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonString = settings.getString("routes_json", "");
        if(jsonString.isEmpty()) return arrayList;
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                arrayList.add(RouteInfo.getRouteInfo(obj));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    protected static void saveRouteInfo(Context context) {
        List<RouteInfo> infoList = handler.getListRoutes();
        JSONArray jsonArray = new JSONArray();
        for(RouteInfo info : infoList) {
            if(!info.getRoute_address().isEmpty() && !info.getPrefix_length().isEmpty())
                jsonArray.put(info.getJSONObject());
        }
        MyLog.d(Util.TAG,"RouteJson = " + jsonArray.toString());
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString("routes_json",jsonArray.toString()).commit();
    }

}
