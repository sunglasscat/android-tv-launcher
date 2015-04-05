package com.droid.network;

import android.util.Log;
import com.droid.application.ClientApplication;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;


public class Request {
	private static final int CONNECTTIMEOUT = 30000;
	private static final int READTIMEOUT = 20000;
    private static boolean d = true;
	public static String request( String json,String requestType ) {return "";}
	public static String requestIp(String MacAddress) {return "";}
}