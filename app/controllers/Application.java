package controllers;

import play.data.Form;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
    static final String HOST_ACCESS_TOKEN = "https://www.foursquare.com/oauth2/access_token";
	static final String AUTHORIZE_URL = "https://foursquare.com/oauth2/authenticate";
	static final String CLIENT_ID = "J0HLBSD2XZHV255CB5BDPWG4MM4SGXLFX4KWXD30KQKVBACG";
	static final String CLIENT_SECRET = "1IJY2XWOJUDITMHEN2BMHWKRR1PNTKXCFHNAMWSRXAPPTX2G";
	static final String REDIRECT_URI = "https://localhost:9443/accessToken";
	static final String FOURSQUARE_CHECKIN = "https://api.foursquare.com/v2/users/self/checkins";
	static final String GRANT_TYPE = "authorization_code";
	static models.User user;
	static WS.Response response;
	
    public static Result index() {
    	if (session("loggedIn") == null)
    		user = new models.User("", "Guest");
    	else
    		user = models.User.findByName(session("username"));
    	return ok(views.html.index.render(user, models.User.findAll()));
    }
    
    public static Promise<Result> displayUser(String name) {
    	models.User u = models.User.findByName(name);
    	final Promise<Result> resultPromise;
    	if (session("loggedIn") == null) {	// not logged in...
			resultPromise =	WS.url(FOURSQUARE_CHECKIN)
							.setQueryParameter("oauth_token", u.accesstoken)
							.setQueryParameter("v", "20140204")
							.get()
							.map(
									new Function<WS.Response, Result>() {
										public Result apply(WS.Response response) {
											JsonNode json = response.asJson();
											String shout = json.findPath("checkins")
															   .findPath("items")
															   .findPath("shout").asText();
											String location = json.findPath("checkins")
													   		  		.findPath("items")
													   		  		.findPath("venue")
													   		  		.findPath("name").asText();
											return ok("Most recent checkin:\nShout: " + shout + "\n" +
														"Location: " + location
													);
										}
									}
								);
    	}
    	else {
    		if (session("username").equals(name)) {
    			//logged in and viewing own profile
    			resultPromise =	WS.url(FOURSQUARE_CHECKIN)
						.setQueryParameter("oauth_token", u.accesstoken)
						.setQueryParameter("v", "20140204")
						.get()
						.map(
								new Function<WS.Response, Result>() {
									public Result apply(WS.Response response) {
										JsonNode json = response.asJson();
										json = json.findPath("checkins")
												   .findPath("items");
										String shout = json.findPath("shout").asText();
										String location = json.findPath("venue")
												   		  		.findPath("name").asText();
										String id = json.findPath("id").asText();
										String createdAt = json.findPath("createdAt").asText();
										String timeZoneOffset = json.findPath("timeZoneOffset").asText();
										return ok("Most recent checkin:\nShout: " + shout + "\n" +
													"Location: " + location +
													"\n\n================\nSPECIAL DETAILS\n================\n" +
													"\nID: " + id +
													"\nTime Zone Offset: " + timeZoneOffset + 
													"\nCreated At: " + createdAt
												);
									}
								}
							);
    		}
    		else {
    			// logged in but view another profile
    			resultPromise =	WS.url(FOURSQUARE_CHECKIN)
						.setQueryParameter("oauth_token", u.accesstoken)
						.setQueryParameter("v", "20140204")
						.get()
						.map(
								new Function<WS.Response, Result>() {
									public Result apply(WS.Response response) {
										JsonNode json = response.asJson();
										String shout = json.findPath("checkins")
														   .findPath("items")
														   .findPath("shout").asText();
										String location = json.findPath("checkins")
												   		  		.findPath("items")
												   		  		.findPath("venue")
												   		  		.findPath("name").asText();
										return ok("Most recent checkin:\nShout: " + shout + "\n" +
													"Location: " + location
												);
									}
								}
							);
    		}
    	}
        return resultPromise;
    }
    
    public static Result fouresquareOAuth() {
    	if (session("loggedIn") == null)
    		return redirect(controllers.routes.Application.signIn());
    	if (models.User.findByName(session("username")) != null) {
    		user = models.User.findByName(session("username"));
    		return redirect(controllers.routes.Application.index());
    	}
    	String fullRedirect = AUTHORIZE_URL +
    						 "?client_id=" + CLIENT_ID +
    						 "&response_type=code" +
    						 "&redirect_uri=" + REDIRECT_URI;
    	return redirect(fullRedirect);
    }
    
    public static Promise<Result> getAccessToken() {
    	final Promise<Result> resultPromise 
    				= WS.url(HOST_ACCESS_TOKEN).setQueryParameter("client_id", CLIENT_ID)
                		.setQueryParameter("client_secret", CLIENT_SECRET).setQueryParameter("grant_type", GRANT_TYPE)
                		.setQueryParameter("redirect_uri", REDIRECT_URI)
                		.setQueryParameter("code", request().getQueryString("code").toString())
                		.get()
                		.map(
								new Function<WS.Response, Result>() {
					   				public Result apply(WS.Response response) {
					   					String accessToken = response.asJson().findPath("access_token").asText();
					   					if (models.User.findByAccessToken(accessToken) == null
					   							|| models.User.findByName(session("name")) == null)
					   						models.User.create(accessToken, session("username"));
					   					return redirect(controllers.routes.Application.index());
					   				}
					   			}
                			);
        return resultPromise;
    }
    
    /*
     * Login and Authentication Functionality
     */
    
    public static class User {
    	public String username;
    	
    	public String validate() {
    		if (username == null || username.length() == 0)
    			return "Please enter a user name.";
    		return null;
    	}
    }

    public static Result authenticate() {
    	Form<User> userForm = Form.form(User.class).bindFromRequest();
    	if (userForm.hasErrors()) {
    		return badRequest(views.html.login.render(userForm));
    	}
    	else {
    		session().clear();
    		session("loggedIn", "yes");
    		session("username", userForm.get().username);
    		return redirect(controllers.routes.Application.fouresquareOAuth());
    	}
 	}
    
    public static Result signIn() {
    	if (session("loggedIn") != null)
    		return redirect(controllers.routes.Application.index());
    	return ok(views.html.login.render(Form.form(User.class)));
    }
    
    public static Result signOut() {
    	session().clear();
    	return redirect(controllers.routes.Application.signIn());
    }
}
