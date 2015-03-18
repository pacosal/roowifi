package com.pacosal.roomba;

import java.io.FileOutputStream;
import android.util.Log;

public class Util {

    public static boolean debugMode = false;

    public static MainActivity actividad = null;
    
    public static boolean start = false;
    
    public static boolean slow = false;
    
    public static boolean unaVez = false;
    
    public static String serverIP = "192.168.0.99";
     
	public static void logDebug(String message)
	{
		if (!Util.debugMode) return;
		try {
			FileOutputStream  fs = new FileOutputStream("/sdcard/roomba.txt", true);
			String date = new java.util.Date().toLocaleString();
			message += "\r\n";
			message = date + " " + message;
			Log.d("roomba", message);
			byte[] buffer = message.getBytes();
			fs.write(buffer);
			fs.close();
		} catch (Exception e) {
		}

	}      
	
		
}
