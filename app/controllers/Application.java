package controllers;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.WSRequestHolder;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
    static final String HOST_ACCESS_TOKEN = "https://www.foursquare.com/oauth2/access_token";
	static final String AUTHORIZE_URL = "https://foursquare.com/oauth2/authenticate";
	static final String CLIENT_ID = "J0HLBSD2XZHV255CB5BDPWG4MM4SGXLFX4KWXD30KQKVBACG";
	static final String CLIENT_SECRET = "1IJY2XWOJUDITMHEN2BMHWKRR1PNTKXCFHNAMWSRXAPPTX2G";
	static final String REDIRECT_URI = "https://localhost:9443/checkinInfo";
	static final String FOURSQUARE_CHECKIN = "https://api.foursquare.com/v2/users/self/checkins";
	static final String GRANT_TYPE = "authorization_code";
	
    public static Result index() {
    	if (session("loggedIn") == null)
    		return ok(views.html.login.render());
    	String fullRedirect = AUTHORIZE_URL +
    						 "?client_id=" + CLIENT_ID +
    						 "&response_type=code" +
    						 "&redirect_uri=" + REDIRECT_URI;
    	return ok(index.render(fullRedirect));
    }
    
    public static Promise<Result> code() {
    	WSRequestHolder authenticate = WS.url(HOST_ACCESS_TOKEN).setQueryParameter("client_id", CLIENT_ID)
                .setQueryParameter("client_secret", CLIENT_SECRET).setQueryParameter("grant_type", GRANT_TYPE)
                .setQueryParameter("redirect_uri", REDIRECT_URI)
                .setQueryParameter("code", request().getQueryString("code").toString());
        final Promise<Result> resultPromise = authenticate.get().flatMap(new Function<WS.Response, Promise<Result>>() {
            public Promise<Result> apply(WS.Response response) {
                JsonNode json = response.asJson();
                return WS.url(FOURSQUARE_CHECKIN)
                		 .setQueryParameter("oauth_token", json.findPath("access_token").asText())
                		 .setQueryParameter("v", "20140204")
                		 .get().map(new Function<WS.Response, Result>() {
                            public Result apply(WS.Response response) {
                                return ok(response.asJson());
                            }
                        });
            }
        });

        return resultPromise;
    }

    public static Result authenticate() {
 		session().clear();
 		session("loggedIn", "yes");
 		return redirect(controllers.routes.Application.index());
 	}
}
