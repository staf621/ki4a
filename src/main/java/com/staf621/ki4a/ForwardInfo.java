package com.staf621.ki4a;

import org.json.JSONException;
import org.json.JSONObject;

public class ForwardInfo {
    private String src_port;
    private String dest_host;
    private String dest_port;
    private boolean local;

    public ForwardInfo() {
        this.src_port = "";
        this.dest_host = "";
        this.dest_port = "";
        this.local = true;
    }

    public ForwardInfo(String src_port, String dest_host, String dest_port, boolean local) {
        this.src_port = src_port;
        this.dest_host = dest_host;
        this.dest_port = dest_port;
        this.local = local;
    }

    public static ForwardInfo getForwardInfo(JSONObject json) {
        try {
            return new ForwardInfo(json.getString("src_port"),json.getString("dest_host"),json.getString("dest_port"),json.getBoolean("local"));
        } catch (JSONException e) {
            MyLog.e(Util.TAG,"JSONException: " + e.getMessage());
            return new ForwardInfo();
        }
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("src_port", this.getSrc_port());
            obj.put("dest_host", this.getDest_host());
            obj.put("dest_port", this.getDest_port());
            obj.put("local", this.is_local());
        } catch (JSONException e) {
            MyLog.e(Util.TAG,"JSONException: " + e.getMessage());
        }
        return obj;
    }

    public String toString() {
        return this.getJSONObject().toString();
    }

    public void setSrc_port(String src_port) {
        this.src_port = src_port;
    }

    public void setDest_host(String dest_host) {
        this.dest_host = dest_host;
    }

    public void setDest_port(String dest_port) {
        this.dest_port = dest_port;
    }

    public void set_local(boolean local) {
        this.local = local;
    }

    public String getSrc_port() {
        return this.src_port;
    }

    public String getDest_host() {
        return this.dest_host;
    }

    public String getDest_port() {
        return this.dest_port;
    }

    public boolean is_local() {
        return this.local;
    }
}
