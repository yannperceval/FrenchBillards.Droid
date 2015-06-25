package com.example.testopencv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

public class SendMessage extends AsyncTask<Void, Void, Void> {
	Socket client;
	public static Context context;
	public double puissance;
	public double angle;
	public double tempsCalculMs;
	public String calculCoup;
	
	private boolean isNetworkAvailable(){
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
    @Override
    protected Void doInBackground(Void... params) {
    	if(!isNetworkAvailable()){
    		System.out.println("[TEST] Pas internet");
    		return null;
    	}else{
    		System.out.println("[TEST] Internet OK");
    	}
    	
    	
        try {

            client = new Socket("37.59.114.164", 14789); // connect to the server

            OutputStream out = client.getOutputStream();
            InputStream in = client.getInputStream();

            byte[] buffer = new byte[1024];
            @SuppressWarnings("unused")
			int bytesRead;

            String device = android.os.Build.BRAND + "::" + android.os.Build.DEVICE;
            out.write(device.getBytes(Charset.forName("UTF-8"))); // send device type           
            bytesRead = in.read(buffer); // receive OK

            out.write(String.valueOf(this.angle).getBytes(Charset.forName("UTF-8")));
            bytesRead = in.read(buffer); // receive OK

            out.write(String.valueOf(this.puissance).getBytes(Charset.forName("UTF-8")));
            bytesRead = in.read(buffer); // receive OK

            out.write(String.valueOf((double)this.tempsCalculMs).getBytes(Charset.forName("UTF-8")));
            bytesRead = in.read(buffer); // receive OK

            out.write(calculCoup.getBytes(Charset.forName("UTF-8")));

            client.close(); // closing the connection

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}