package com.dory.recommend.service;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.yelp.v2.Business;
import com.yelp.v2.YelpSearchResult;

import com.google.android.maps.*;

public class YelpClient {

	// private final String YWSID = "vOBHuEoCt8uqXudvl4TBVQ";

	private String baseUri = "http://api.yelp.com/" + "business_review_search?";
	private Gson gson;
	private String account;
	private final double ONE_MILE = 0.005;
	private final double METER_CONVERSION = 1609.34401;
	private MapController mapController;
	private MapView mapView;

	private String CONSUMER_KEY = "6-_shIW5A7w6FZruV-vCiw";
	private String CONSUMER_SECRET = "v3rpFumNbn6LMN-TEuewrAXgUgU";
	private String TOKEN = "mbRIZhu3gMA-dvGQF9RWViTCZyfdIg5w";
	private String TOKEN_SECRET = "hKo4bUw4qIloZa5QN19QgeKe-DE";

	public enum EXCLUDE_CATEGORIES {
		cheesesteaks, chicken_wings, hotdogs, hotdog, pizza, desserts, donuts, icecream, candy, cheese, chocolate
	};

	private static final String ACTIVITY = "activities";
	private static final String FOOD = "food";

	/**
	 * User-agent string to use when making requests. Should be filled using
	 * {@link #prepareUserAgent(Context)} before making any other calls.
	 */

	/*
	 * private static String sUserAgent = null; private static final int
	 * HTTP_STATUS_OK = 200;
	 */

	public YelpClient() {

	}

	public YelpSearchResult findNerby(String category,int offset, String lat, String lng) {
		// Define your keys, tokens and secrets. These are available from the
		// Yelp website.

		int off = offset;
		// Execute a signed call to the Yelp service.
		OAuthService service = new ServiceBuilder().provider(YelpV2API.class)
				.apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build();
		Token accessToken = new Token(TOKEN, TOKEN_SECRET);
		OAuthRequest request = new OAuthRequest(Verb.GET,
				"http://api.yelp.com/v2/search");
		request.addQuerystringParameter("ll", lat + "," + lng);
		request.addQuerystringParameter("term", category);
		request.addQuerystringParameter("offset", Integer.toString(off * 20));
		service.signRequest(accessToken, request);
		Response response = request.send();
		String rawData = response.getBody();
		YelpSearchResult places = null;
		// Sample of how to turn that text into Java objects.
		try {
			places = new Gson().fromJson(rawData, YelpSearchResult.class);

			System.out.println("Your search found " + places.getTotal()
					+ " results.");
			System.out.println("Yelp returned " + places.getBusinesses().size()
					+ " businesses in this request.");
			System.out.println();
			/*
			 * for(Business biz : places.getBusinesses()) {
			 * System.out.println(biz.getName()); for(String address :
			 * biz.getLocation().getAddress()) { System.out.println("  " +
			 * address); }
			 * 
			 * System.out.print("  " + biz.getLocation().getCity());
			 * System.out.println(biz.getUrl()); System.out.println(); }
			 */

		} catch (Exception e) {
			System.out.println("Error, could not parse returned data!");
			System.out.println(rawData);
		}
		return places;

	}

	public Business getBusiness(String id) {

		// Execute a signed call to the Yelp service.
		OAuthService service = new ServiceBuilder().provider(YelpV2API.class)
				.apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build();
		Token accessToken = new Token(TOKEN, TOKEN_SECRET);
		OAuthRequest request = new OAuthRequest(Verb.GET,
				"http://api.yelp.com/v2/business/" + id);

		service.signRequest(accessToken, request);
		Response response = request.send();
		String rawData = response.getBody();
		Business business = null;
		// Sample of how to turn that text into Java objects.
		try {
			business = new Gson().fromJson(rawData, Business.class);

			System.out.println("Your search found " + business.getName()
					+ " results.");
			System.out.println("Yelp returned " + business.getReviewCount()
					+ " reviews in this request.");
			System.out.println();
			/*
			 * for(Business biz : places.getBusinesses()) {
			 * System.out.println(biz.getName()); for(String address :
			 * biz.getLocation().getAddress()) { System.out.println("  " +
			 * address); }
			 * 
			 * System.out.print("  " + biz.getLocation().getCity());
			 * System.out.println(biz.getUrl()); System.out.println(); }
			 */

		} catch (Exception e) {
			System.out.println("Error, could not parse returned data!");
			System.out.println(rawData);
		}
		return business;

	}

}
