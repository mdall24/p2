package com.example.p2;
import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";

    private static final String KEY_TEAM_CODE = "teamCode";

    public void saveTeamCode(String teamCode){
        editor.putString(KEY_TEAM_CODE, teamCode);
        editor.apply();
    }

    public String getTeamCode(){
        return prefs.getString(KEY_TEAM_CODE, null);
    }
    public SessionManager(Context context){
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    public void saveLoginSession(String username){
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }
    public boolean isLoggedIn(){
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public String getUsername(){
        return prefs.getString(KEY_USERNAME, null);
    }
    public void logout(){
        editor.clear();
        editor.apply();
    }
    public boolean hasAskedPermission() {
        return prefs.getBoolean("askedPermission", false);
    }
    public void setAskedPermission(boolean asked) {
        editor.putBoolean("askedPermission", asked);
        editor.apply();
    }
}
