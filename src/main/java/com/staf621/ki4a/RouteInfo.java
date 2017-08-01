package com.staf621.ki4a;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;

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
        try {
            int prefix = Integer.parseInt(this.prefix_length);
            if (prefix >= 0 && prefix <= 32) {
                Inet4Address address = (Inet4Address) InetAddress.getByName(this.route_address);
                byte[] bytes = address.getAddress();
                int mask = 0xffffffff << (32 - prefix);
                bytes[0] = (byte)(bytes[0] & (mask >> 24));
                bytes[1] = (byte)(bytes[1] & (mask >> 16));
                bytes[2] = (byte)(bytes[2] & (mask >> 8));
                bytes[3] = (byte)(bytes[3] & (mask));
                InetAddress netAddr = InetAddress.getByAddress(bytes);
                this.route_address = netAddr.getHostAddress();
            }
        }
        catch (Exception e) {}
        return this.route_address;
    }

    public String getPrefix_length() {
        return this.prefix_length;
    }

}
