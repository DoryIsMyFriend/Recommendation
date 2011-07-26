package com.dory.recommend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.dory.recommend.R;
import com.google.gson.Gson;
import com.sugestio.client.SugestioClient;
import com.sugestio.client.SugestioResult;
import com.sugestio.client.model.Consumption;
import com.sugestio.client.model.Item;
import com.sugestio.client.model.Recommendation;
import com.sugestio.client.model.User;


import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SugestioService extends Service{
	private static final String accountId = "improveyelp";
	private NotificationManager mNM;
	private SugestioClient sugestioClient;
	public final static String TAG = "SugestioService";
	
	
	public class LocalBinder extends Binder {
		public SugestioService getService() {
            return SugestioService.this;
        }
    }
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "!!! onBind");
		return mBinder;
		
	}
	@Override
    public void onCreate() {
		Log.i(TAG, "!!! onCreate");
		Toast.makeText(this, R.string.sugestio_service_started, Toast.LENGTH_SHORT).show();
        sugestioClient = new SugestioClient(accountId);
       
        
      //  addUser("AF2Y-J473", "F","37.779534, -122.393371", "1967-02-17");
     //   addUser("BV2Z-K412", "M", "37.779534, -122.393371", "1972-03-17");
    //	addUser("CD2Q-G578", "F", "37.779534, -122.393371", "1975-08-08");
    //	addUser("AG2O-P493", "M","37.779534, -122.393371", "1977-12-12");
    //    addUser("JZ29-O462", "F","37.779534, -122.393371", "1967-02-12"); 
    //    addUser("OF31-G271", "F","37.779534, -122.393371", "1987-10-16"); **
    //    addUser("MU41-F441", "F","37.779534, -122.393371", "1980-11-12");
    }
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "!!! Received start id " + startId + " flags: " + flags + ": "+ intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

      //  intent.getFlags();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
    	Log.i(TAG, "!!! onDestroy");
    	sugestioClient.shutdown();
        // Tell the user we stopped.
        Toast.makeText(this, R.string.sugestio_service_stopped, Toast.LENGTH_SHORT).show();
    }


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    
public  void getSimilarItems() {
		
		try {
			List<Recommendation> recommendations = sugestioClient.getSimilarItems("1", null);
			print(recommendations);
		} catch (Exception e) {	
			e.printStackTrace();
		}		
		
	}
	
	public  String  addConsumption(Consumption consumption) {		
		
		SugestioResult result = sugestioClient.addConsumption(consumption);		
		print(result);	
		
		return result.toString();
	}
	
	
	
	public String addItem(List<Item> items) {
		//adds items found in the res/raw/items.xml file
		String res = "Empty";
		for (Item item: items)
		{
			SugestioResult result = sugestioClient.addItem(item);	
			print(result);
			res = result.toString();
		}
		return res;
	}
	
	public String addItem(Item item) {
		//adds item to sugestio service
		
		SugestioResult result = sugestioClient.addItem(item);	
		print(result);
		
		return result.toString();
	}
	
	public  List<Recommendation> getRecommendations(String userId, String category) {
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("category", category);
		List<Recommendation> recommendations = null;
		try {
			recommendations = sugestioClient.getRecommendations(userId, parameters);
			print(recommendations);
			
		} catch (Exception e) {	
			e.printStackTrace();
		}
		
		return  recommendations;	
	}
	public  void addUser(String userID, String gender, String bDay) {
		
		
		User user = new User(userID);
		user.setGender(gender);
		user.setBirthday(bDay);
		
		SugestioResult result = sugestioClient.addUser(user);
	}
	
public  void addUser(String userID, String gender, String location_latlong,  String bDay) {
		
		
		User user = new User(userID);
		user.setGender(gender);
		user.setBirthday(bDay);
		user.setLocation_latlong(location_latlong);
		
		SugestioResult result = sugestioClient.addUser(user);
	}
	private  void print (List<Recommendation> recommendations) {
		
		System.out.println(recommendations.size() + " recommendations:");
		
		for (Recommendation r : recommendations) {
			System.out.println(r.getItem().getTitle() + " (" + r.getScore() + ")");
		}
	}
	
	private  void print(SugestioResult result) {		
		if (result.isOk()) {
			System.out.println("API call succeeded with response code " + result.getCode() + ". Response body:");
			System.out.println(result.getMessage());
		} else {
			System.err.println("API call failed with response code " + result.getCode() + ". Response body:");
			System.err.println(result.getMessage());
		}
	}

}
