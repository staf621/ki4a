package com.staf621.ki4a;

import org.json.JSONException;
import org.json.JSONObject;

public class RouteInfo {
    private String route_address;
    private String prefix_length;

    public RouteInfo() {
        this.route_address = "";
        this.prefix_length = "";
    }

    public RouteInfo(String route_address, String prefix_length) {
        this.route_address = route_address;
        this.prefix_length = prefix_length;
    }

    public static RouteInfo getRouteInfo(JSONObject json) {
        try {
            return new RouteInfo(json.getString("route_address"),json.getString("prefix_length"));
        } catch (JSONException e) {
            MyLog.e(Util.TAG,"JSONException: " + e.getMessage());
            return new RouteInfo();
        }
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("route_address", this.getRoute_address());
            obj.put("prefix_length", this.getPrefix_length());
        } catch (JSONException e) {
            MyLog.e(Util.TAG,"JSONException: " + e.getMessage());
        }
        return obj;
    }

    public String toString() {
        return this.getJSONObject().toString();
    }

    public void setRoute_address(String route_address) {
        this.route_address = route_address;
    }

    public void setPrefix_length(String prefix_length) {
        this.prefix_length = prefix_length;
    }

    public String getRoute_address() {
        return this.route_address;
    }

    public String getPrefix_length() {
        return this.prefix_length;
    }

}
