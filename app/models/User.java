package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity 
@Table(name="user")
public class User extends Model {

	private static final long serialVersionUID = 5986141107988665766L;

	@Id
	@Constraints.Required
    @Formats.NonEmpty
    public String accesstoken;
	
	@Id
    @Constraints.Required
    @Formats.NonEmpty
    public String name;
    
    public User (String accesstoken, String name) {
    	this.accesstoken = accesstoken;    	
    	this.name = name;
    }
	
	public static Finder<String,User> find = 
    		new Finder<String,User>(String.class, User.class);
	
	/**
     * Retrieve all users.
     */
    public static List<User> findAll() {
        return find.all();
    }
    
    /**
     * Retrieve a User by access token
     */
    public static User findByAccessToken(String accesstoken) {
        return find.where().eq("accesstoken", accesstoken).findUnique();
    }
    
    /**
     * Retrieve a User by name
     */
    public static User findByName(String name) {
        return find.where().eq("name", name).findUnique();
    }
    
    public static void create (String accesstoken, String name) {
    	new User(accesstoken, name).save();
    }
}
