package com.dory.recommend;

import java.util.ArrayList;
import java.util.List;

import com.dory.recommend.service.SugestioService;
import com.dory.recommend.service.YelpClient;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.sugestio.client.model.Consumption;
import com.sugestio.client.model.Item;
import com.sugestio.client.model.Recommendation;
import com.yelp.v2.Business;
import com.yelp.v2.YelpSearchResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import android.os.Message;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class CurrentLocation extends MapActivity {

	public YelpSearchResult places;
	private Context mContext;
	private Button findMeAction;
	private Button recommendAction;
	private Button previousYelpFind;
	private Button nextYelpFind;
	private int yelpOffset = 0;
	public static final String TAG = "location";
	private List<Overlay> mapOverlays;

	private GeoPoint p;
	private LocationManager locationManager;
	private String provider;
	private String currentLat;
	private String currentLng;
	private int lat;
	private int lng;
	private MapView mapView;
	// private List<OverlayItem> listOfOverlays;
	private overlayedItems yelpMapOverlay;
	private overlayedItems recMapOverlay;
	public static final String RECOMMEND_PREFS = "recommend";
	private String myId = "OF31-G271";

	private SugestioService sugestioService;
	private ServiceConnection mConnection = null;
	private boolean mIsBound;
	Intent sugestioInterface;

	void doBindSugestioService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(CurrentLocation.this, SugestioService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindSugestioService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindSugestioService();
		sugestioService = null;
		stopService(sugestioInterface);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.location);

		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				// This is called when the connection with the service has been
				// established, giving us the service object we can use to
				// interact with the service. Because we have bound to a
				// explicit
				// service that we know is running in our own process, we can
				// cast its IBinder to a concrete class and directly access it.
				sugestioService = ((SugestioService.LocalBinder) service)
						.getService();

				// Tell the user about this for our demo.
				Toast.makeText(mContext, R.string.sugestio_service_connected,
						Toast.LENGTH_SHORT).show();
			}

			public void onServiceDisconnected(ComponentName className) {
				// This is called when the connection with the service has been
				// unexpectedly disconnected -- that is, its process crashed.
				// Because it is running in our same process, we should never
				// see this happen.
				sugestioService = null;
				Toast.makeText(mContext,
						R.string.sugestio_service_disconnected,
						Toast.LENGTH_SHORT).show();
			}
		};

		doBindSugestioService();

		findMeAction = (Button) findViewById(R.id.findMeAction);
		findMeAction.setOnClickListener(findListener);
		previousYelpFind = (Button) findViewById(R.id.PreviousYelpFind);
		previousYelpFind.setOnClickListener(previousListener);
		nextYelpFind = (Button) findViewById(R.id.NextYelpFind);
		nextYelpFind.setOnClickListener(nextListener);

		recommendAction = (Button) findViewById(R.id.recommedAction);
		recommendAction.setOnClickListener(recommendListener);

		mapView = (MapView) findViewById(R.id.mapView);

		mapView.setStreetView(true);

		final MapController mc = mapView.getController();

		String coordinates[] = { "37.779534", "-122.393371" };

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider -> use
		// default
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);

		// Initialize the location fields
		if (location != null) {
			System.out.println("Provider " + provider + " has been selected.");

			currentLat = Double.toString(location.getLatitude());
			currentLng = Double.toString(location.getLongitude());
			p = new GeoPoint((int) (location.getLatitude() * 1E6),
					(int) (location.getLongitude() * 1E6));

		} else {
			Double lt = Double.parseDouble(coordinates[0]);
			Double lg = Double.parseDouble(coordinates[1]);
			currentLat = Double.toString(lt);
			currentLng = Double.toString(lg);
			p = new GeoPoint((int) (lt * 1E6), (int) (lg * 1E6));
		}

		mapOverlays = mapView.getOverlays();

		// MapOverlay mapOverlay = new MapOverlay();
		// MyLocationOverlay me = new MyLocationOverlay(this, mapView);
		// mapOverlays.add(me);

		mapView.setOnTouchListener(l);

		mc.setCenter(p);

		mc.animateTo(p);

		mc.setZoom(16);

		sugestioInterface = new Intent(mContext, SugestioService.class);

		startService(sugestioInterface);

	}

	
	public OnTouchListener l = new OnTouchListener() {

		public boolean onTouch(View v, MotionEvent event) {

			return false;
		}

	};
	 
	public void onStart() {
		super.onStart();
		SharedPreferences latestView = getSharedPreferences(RECOMMEND_PREFS, 0);

		myId = latestView.getString("userID", myId);
	}

	private OnClickListener previousListener = new OnClickListener() {

		public void onClick(View v) {
			if (yelpOffset > 0) {
				yelpOffset--;
			}
			yelpRetrieve("", yelpOffset);
		}
	};

	private OnClickListener nextListener = new OnClickListener() {

		public void onClick(View v) {
			yelpOffset++;
			yelpRetrieve("", yelpOffset);
		}
	};

	private void yelpRetrieve(String term, int offset) {

		YelpClient yc = new YelpClient();
		Drawable yelpMarker = getResources().getDrawable(
				R.drawable.pin_orange_active);
		

		// yelpMarker.setBounds(0, 0, yelpMarker.getIntrinsicWidth(),
		// yelpMarker.getIntrinsicHeight());
		yelpMapOverlay = new overlayedItems(yelpMarker, mapView);
		if (!mapOverlays.isEmpty())
			mapOverlays.remove(0);
		mapOverlays.add(yelpMapOverlay);
		mapView.postInvalidate();
		int i;
		places = yc.findNerby(term, offset, currentLat, currentLng);
		if (places != null) {
			for (Business biz : places.getBusinesses()) {

				i = places.getBusinesses().indexOf(biz);
				OverlayItem overlayitem = new OverlayItem(
						new GeoPoint((int) (biz.getLocation().getCoordinate()
								.getLatitude() * 1E6), (int) (biz.getLocation()
								.getCoordinate().getLongitude() * 1E6)),
						biz.getName() + " " + i, biz.getId());

				yelpMapOverlay.addOverlay(overlayitem);
				
				yelpMapOverlay.setFocus(overlayitem);
			}
			mapView.postInvalidate();
			
		}

	}

	private OnClickListener findListener = new OnClickListener() {

		public void onClick(View v) {
			yelpRetrieve("", 0);
		}

	};

	private OnClickListener recommendListener = new OnClickListener() {

		public void onClick(View v) {
			Drawable recMarker = getResources().getDrawable(
					R.drawable.pin_blue_active);

			// recMarker.setBounds(0, 0, recMarker.getIntrinsicWidth(),
			// recMarker.getIntrinsicHeight());

			if (!mapOverlays.isEmpty()) {

				while (mapOverlays.iterator().hasNext()) {
					mapOverlays.remove(mapOverlays.iterator().next());
				}
			}
			recMapOverlay = new overlayedItems(recMarker, mapView);

			mapOverlays.add(recMapOverlay);

			List<Recommendation> recommendationsReceived;
			recommendationsReceived = sugestioService.getRecommendations(myId,
					"");

			if (recommendationsReceived != null) {
				if (recommendationsReceived.size() > 0) {
					for (Recommendation rec : recommendationsReceived) {

						int com = rec.getItem().getLocation_latlong()
								.indexOf(",");
						int len = rec.getItem().getLocation_latlong().length();
						String lat = rec.getItem().getLocation_latlong()
								.substring(0, com);
						Double llat = Double.parseDouble(lat);
						String lng = rec.getItem().getLocation_latlong()
								.substring(com + 1, len - 1);
						Double llng = Double.parseDouble(lng);
						OverlayItem overlayitem = new OverlayItem(new GeoPoint(
								(int) (llat * 1E6), (int) (llng * 1E6)), rec
								.getItem().getTitle(), rec.getItem()
								.getDescription_short());

						recMapOverlay.addOverlay(overlayitem);

					}
					
				} else {
					Toast.makeText(mContext, "0 Recommendations Found",
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(mContext, "No Recommendations Found",
						Toast.LENGTH_SHORT).show();
			}
			mapView.postInvalidate();
		}
		
	};

	@Override
	protected boolean isRouteDisplayed() {

		return false;

	}

	class overlayedItems extends
			com.google.android.maps.ItemizedOverlay<OverlayItem> {
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private Drawable defaultMarker;
		private Item sugestioItem = null;
		private Consumption sugestioConsumption = null;

		public overlayedItems(Drawable defaultMarker, MapView mapview) {
			super(boundCenter(defaultMarker));
			mContext = mapview.getContext();
		}

		public overlayedItems(Drawable defaultMarker, Context context) {
			super(boundCenter(defaultMarker));
			this.defaultMarker = defaultMarker;

			mContext = context;
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);

		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			overlay.setMarker(this.defaultMarker);
			populate();
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		@Override
		protected boolean onTap(int index) {
			Business biz;
			Business selectedBiz;
			OverlayItem item = mOverlays.get(index);
			biz = places.getBusinesses().get(index);

			Dialog dialog = new Dialog(mContext);

			YelpClient yc = new YelpClient();
			int i;
			i = places.getBusinesses().indexOf(biz);
			selectedBiz = yc.getBusiness(biz.getId());

			sugestioItem = new Item();
			sugestioItem.setId(biz.getId());
			sugestioItem.setLocation_latlong(selectedBiz.getLocation()
					.getCoordinate().getLatitude()
					+ ", "
					+ selectedBiz.getLocation().getCoordinate().getLongitude());
			sugestioItem.setDescription_short(selectedBiz.getName());
			sugestioItem.setPermalink(selectedBiz.getUrl());
			sugestioItem.setTitle(selectedBiz.getName());

			String[] tokens;

			int j = 0;
			while (selectedBiz.getCategories().size() > j) {

				tokens = selectedBiz.getCategories().get(j).toString()
						.split("[\\[\\],]");

				for (int x = 1; x < tokens.length; x++) {
					
					sugestioItem.addCategory(tokens[x]);
				}
				j++;
			}

			dialog.setContentView(R.layout.location_detail);
			dialog.setTitle("Location Detail");

			TextView tagPrompt = (TextView) dialog
					.findViewById(R.id.taglist_text);

			RatingBar rb = ((RatingBar) dialog.findViewById(R.id.ratingbar));

			rb.setOnRatingBarChangeListener(ratingsChanged);

			TextView text = (TextView) dialog.findViewById(R.id.name);
			text.setText(selectedBiz.getName());
			TextView text5 = (TextView) dialog.findViewById(R.id.snippet);
			text5.setText(selectedBiz.getSnippetText());
			TextView text2 = (TextView) dialog.findViewById(R.id.address);
			text2.setText(selectedBiz.getLocation().getAddress().toString());
			TextView text3 = (TextView) dialog.findViewById(R.id.categories);
			text3.setText(selectedBiz.getCategories().toString());
			TextView text4 = (TextView) dialog.findViewById(R.id.rating);
			text4.setText(selectedBiz.getRatingImgUrl());

			Button okButton = (Button) dialog.findViewById(R.id.OKButton);
			okButton.setOnClickListener(oKListener);
			Button cancelButton = (Button) dialog
					.findViewById(R.id.CancelButton);
			cancelButton.setOnClickListener(cancelListener);

			dialog.show();
			return true;
		}

		OnRatingBarChangeListener ratingsChanged = new OnRatingBarChangeListener() {

			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {

				final int numStars = ratingBar.getNumStars();
				System.out.println("rating" + "5:1:" + Float.toString(rating));
				sugestioConsumption = new Consumption();
				sugestioConsumption.setItemid(sugestioItem.getId());
				sugestioConsumption.setType("RATING");
				sugestioConsumption.setUserid(myId);
				sugestioConsumption.setDetail(Double.toString(rating));
				// rated = true;

			}

		};

		OnClickListener oKListener = new OnClickListener() {

			public void onClick(View v) {
				if (sugestioItem != null) {
					sugestioService.addItem(sugestioItem);
					if (true) {
						sugestioService.addConsumption(sugestioConsumption);
					}
				}
			}

		};
		OnClickListener cancelListener = new OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}

		};
	}


}
