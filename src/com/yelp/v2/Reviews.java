package com.yelp.v2;

import java.util.List;

public class Reviews {
	private String  id;
	private int rating;
	private String ratingImgUrl;
	private String ratingImgUrlSmall;
	private int timeCreated; //in sec since 1970
	private User user;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public int getRating() {
		return rating;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	public String getRatingImgUrl() {
		return ratingImgUrl;
	}
	public void setRatingImgUrl(String ratingImgUrl) {
		this.ratingImgUrl = ratingImgUrl;
	}
	public String getRatingImgUrlSmall() {
		return ratingImgUrlSmall;
	}
	public void setRatingImgUrlSmall(String ratingImgUrlSmall) {
		this.ratingImgUrlSmall = ratingImgUrlSmall;
	}
	public int getTimeCreated() {
		return timeCreated;
	}
	public void setTimeCreated(int timeCreated) {
		this.timeCreated = timeCreated;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
}
