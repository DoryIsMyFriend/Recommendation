/**
 * The MIT License
 * 
 * Copyright (c) 2010 Sugestio
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sugestio.client;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.StatusLine;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import com.dory.recommend.R;
import com.dory.recommend.service.TwoLeggedOAuth;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import com.sugestio.client.model.Consumption;
import com.sugestio.client.model.Item;
import com.sugestio.client.model.Recommendation;
import com.sugestio.client.model.User;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class SugestioClient {

	private String baseUri = "http://api.sugestio.com";
	private Gson gson;
	private String account;

	/**
	 * User-agent string to use when making requests. Should be filled using
	 * {@link #prepareUserAgent(Context)} before making any other calls.
	 */
	private static String sUserAgent = null;
	private static final String TAG = "Sugestio Recommendations";
	/**
	 * {@link StatusLine} HTTP status code when no server error has occurred.
	 */
	private static final int HTTP_STATUS_OK = 200;

	public enum ResourceType {
		USER, ITEM, CONSUMPTION, RECOMMENDATION, SIMILAR, ANALYTICS
	}

	public enum PartitionType {
		CATEGORY, SEGMENT
	}

	/*
	 * public enum Verb { GET, POST, PUT, DELETE }
	 */
	/**
	 * Creates a new instance of the SugestioClient with the given access
	 * credentials.
	 * 
	 * @param account
	 *            your account key
	 */
	public SugestioClient(String account) {
		this.account = account;
		this.gson = new Gson();
	}

	/**
	 * Initiates an orderly shutdown of the client. Any pending web service
	 * requests are executed, but new requests will be rejected.
	 * 
	 * @throws Exception
	 */
	public void shutdown() {
		this.gson = null;
	}

	public List<Recommendation> getRecommendations(String userId)
			throws Exception {
		return getRecommendations(userId, null);
	}

	/**
	 * Get personal recommendations for the given user.
	 * 
	 * @param userid
	 *            the user
	 * @param parameters
	 *            query parameters
	 * @return recommendations
	 * @throws Exception
	 */
	public List<Recommendation> getRecommendations(String userid,
			Map<String, String> parameters) throws Exception {

		JsonElement response = doGet("/users/" + userid
				+ "/recommendations.json", parameters, false);

		if (response != null) {
			Type listType = new TypeToken<List<Recommendation>>() {
			}.getType();

			return gson.fromJson(response.getAsJsonArray(), listType);
		} else {
			return new ArrayList<Recommendation>();
		}

	}

	/**
	 * Get similar item recommendations for the given item.
	 * 
	 * @param itemid
	 *            the item
	 * @param parameters
	 *            query parameters
	 * @return similar item recommendations
	 * @throws Exception
	 */
	public List<Recommendation> getSimilarItems(String itemid,
			Map<String, String> parameters) throws Exception {

		JsonElement response = doGet("/items/" + itemid + "/similar.json",
				parameters, false);

		if (response != null) {
			Type listType = new TypeToken<List<Recommendation>>() {
			}.getType();
			return gson.fromJson(response.getAsJsonArray(), listType);
		} else {
			return new ArrayList<Recommendation>();
		}
	}

	/**
	 * Submit a consumption.
	 * 
	 * @param consumption
	 *            the consumption
	 * @return result
	 */
	public SugestioResult addConsumption(Consumption consumption) {
		return this.doPost("/consumptions", consumption);
	}

	/**
	 * Submit item meta data.
	 * 
	 * @param item
	 *            the item
	 * @return result
	 */
	public SugestioResult addItem(Item item) {
		return this.doPost("/items", item);
	}

	/**
	 * Submit user meta data.
	 * 
	 * @param user
	 *            the user
	 * @return result
	 */
	public SugestioResult addUser(User user) {
		return this.doPost("/users", user);
	}

	public static void prepareUserAgent(Context context) {
		try {
			// Read package name and version number from manifest
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(),
					0);
			sUserAgent = String.format(
					context.getString(R.string.template_user_agent),
					info.packageName, info.versionName);

		} catch (NameNotFoundException e) {
			Log.e(TAG, "Couldn't find package information in PackageManager", e);
		}
	}

	/**
	 * Thrown when there were problems contacting the remote API server, either
	 * because of a network error, or the server returned a bad status code.
	 */
	public static class ApiException extends Exception {
		public ApiException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		public ApiException(String detailMessage) {
			super(detailMessage);
		}
	}

	/**
	 * Performs a GET request and returns the response body as a JsonElement.
	 * 
	 * @param resource
	 *            the resource to get
	 * @param parameters
	 *            query string parameters
	 * @param raise404
	 *            if true, a HTTP response of 404 will raise an exception, if
	 *            false, the method will return null
	 * @return JsonElement
	 * @throws Exception
	 */
	private JsonElement doGet(String resource, Map<String, String> parameters,
			boolean raise404) throws Exception {

		String SUGESTIO_API_KEY = "improveyelp";
		String SUGESTIO_API_SECRET = "vq2bsRX8ja";

		OAuthService service = new ServiceBuilder()
				.provider(TwoLeggedOAuth.class).apiKey(SUGESTIO_API_KEY)
				.apiSecret(SUGESTIO_API_SECRET).build();

		// for 3-legged you would need to request the authorization token
		// for a two-legged OAuth server the token is empty
		Token token = new Token("", "");

		HttpClient httpClient = new DefaultHttpClient();
		SchemeRegistry sr = httpClient.getConnectionManager()
				.getSchemeRegistry();
		List<String> l = sr.getSchemeNames();

		String uri = getUri(resource);

		if (parameters != null && parameters.size() > 0) {

			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			for (String key : parameters.keySet()) {
				queryParams
						.add(new BasicNameValuePair(key, parameters.get(key)));
			}

			uri += "?" + URLEncodedUtils.format(queryParams, "UTF-8");
		}

		String encodedTitle = Uri.encode("Sugestion Recommendations");

		// Query the API for content
		String url = (String.format(uri, encodedTitle, false));
		try {

			OAuthRequest request = new OAuthRequest(Verb.GET, uri);
			request.addHeader("User-Agent", sUserAgent);

			service.signRequest(token, request);
			Response response = request.send();
			Log.i(TAG, "!!! Response: " + response.getBody());

			// HttpGet request = new HttpGet(uri);
			// request.setHeader("User-Agent", sUserAgent);

			// HttpResponse httpResponse = httpClient.execute(request);

			// StatusLine status = httpResponse.getStatusLine();

			// if (response.getCode() != HTTP_STATUS_OK) {
			// throw new ApiException("Invalid response from server: " +
			// response.getCode());
			// }
			String body = response.getBody();
			// String body = EntityUtils.toString(httpResponse.getEntity());
			// int code = httpResponse.getStatusLine().getStatusCode();
			int code = response.getCode();
			if (code == 200) {
				httpClient.getConnectionManager().shutdown();
				JsonParser parser = new JsonParser();
				return parser.parse(body);
			} else if (code == 404 && !raise404) {
				httpClient.getConnectionManager().shutdown();
				return null;
			} else {
				String message = "Response code " + code + ". ";
				message += body;
				throw new Exception(message);
			}

		} catch (Exception e) {
			httpClient.getConnectionManager().shutdown();
			throw e;
		}

	}

	/**
	 * Performs a POST request to the given resource. Encodes given JSON object
	 * as form data.
	 * 
	 * @param resource
	 * @param object
	 *            the object to submit
	 * @return result
	 */
	private SugestioResult doPost(String resource, Object object) {

		String SUGESTIO_API_KEY = "improveyelp";
		String SUGESTIO_API_SECRET = "vq2bsRX8ja";

		OAuthService service = new ServiceBuilder()
				.provider(TwoLeggedOAuth.class).apiKey(SUGESTIO_API_KEY)
				.apiSecret(SUGESTIO_API_SECRET).build();

		// for 3-legged you would need to request the authorization token
		// for a two-legged OAuth server the token is empty
		Token token = new Token("", "");

		// HttpClient httpClient = new DefaultHttpClient();
		//JsonObject jsonObject = gson.toJsonTree(object).getAsJsonObject();
		//List<NameValuePair> parameters = jsonToNameValuePairs(jsonObject);
		SugestioResult result = new SugestioResult();

		try {
			String uri = getUri(resource);
			OAuthRequest request = new OAuthRequest(Verb.POST, uri);

			request.addHeader("Content-type", "application/json");
			String payload = gson.toJson(object);
			request.addPayload(payload);

			service.signRequest(token, request);

			Log.i(TAG,
					"!!! Request: " + request.getUrl() + "; "
							+ request.getBodyContents() + "; "
							+ request.getBodyParams());
			Log.i(TAG, "!!! Request headers: "
					+ request.getHeaders().values().toString());
	
			Response response = request.send();
			Log.i(TAG, "!!! Response: " + response.getBody());

			// result.setCode(httpResponse.getStatusLine().getStatusCode());
			result.setCode(response.getCode());
			result.setOk(result.getCode() == 202);
			result.setMessage(response.getBody().toString());

			// httpClient.getConnectionManager().shutdown();

		} catch (Exception e) {

			// httpClient.getConnectionManager().shutdown();
			result.setOk(false);
			result.setCode(-1);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	/**
	 * Builds the full request URI from the base URI, account and resource.
	 * 
	 * @param resource
	 * @return
	 */
	private String getUri(String resource) {
		return baseUri + "/sites/" + account + resource;
	}

	/**
	 * Converts a JSON object into NameValuePairs suitable which can be attached
	 * as query parameters when performing a GET or form data when performing a
	 * POST.
	 * 
	 * @param json
	 *            the JSON object
	 * @return a list of NameValuePairs
	 */
	private List<NameValuePair> jsonToNameValuePairs(JsonObject json) {

		List<NameValuePair> pairs = new ArrayList<NameValuePair>();

		for (Entry<String, JsonElement> entry : json.entrySet()) {

			if (entry.getValue().isJsonPrimitive()) {
				pairs.add(new BasicNameValuePair(entry.getKey(), entry
						.getValue().getAsString()));
			} else if (entry.getValue().isJsonArray()) {
				JsonArray array = entry.getValue().getAsJsonArray();
				for (int i = 0; i < array.size(); i++) {
					pairs.add(new BasicNameValuePair(entry.getKey() + "[]",
							array.get(i).getAsString()));
				}
			}
		}

		return pairs;
	}
}
