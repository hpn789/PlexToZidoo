package com.hpn789.plextozidoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Play extends AppCompatActivity {

    static final String library = "/library/";
    static final String tokenParameter = "X-Plex-Token=";
    private String address = "";
    private String key = "";
    private String token = "";

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.finish();
    }

    private void searchPath(List<PlexLibraryInfo> infos, int index)
    {
        PlexLibraryInfo info = infos.get(index);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url =address+"/library/sections/"+info.getKey()+"/search?type="+info.getType().searchId+"&"+tokenParameter+token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        PlexLibraryXmlParser parser = new PlexLibraryXmlParser(key);
                        InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                        try {
                            String path = parser.parse(targetStream);
                            if(!path.isEmpty())
                            {
                                path=plexPathToLocalPath(path);
                                if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("useZidooPlayer", true))
                                {
                                    startZidooPlayer(path, info.getViewOffset());
                                }
                                else
                                {
                                    startPlayer(path);
                                }
                            }
                            else if(index+1<infos.size())
                            {
                                searchPath(infos, index+1);
                            }
                            else
                            {
                                textView.setText("Not found");
                            }

                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchLibrary()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url =address+"/library/sections/?"+tokenParameter+token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        String[] names = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("libraries", "").split(",");
                        PlexXmlParser parser = new PlexXmlParser(Arrays.asList(names));
                        InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                        try {
                            List<PlexLibraryInfo> libraries = parser.parse(targetStream);
                            searchPath(libraries, 0);

                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();

        String dataString = intent.getDataString();
        Log.d("plex", ""+dataString);
        int indexOfLibrary = dataString.indexOf("/library/");
        address = dataString.substring(0, indexOfLibrary);
        key = dataString.substring(indexOfLibrary, dataString.indexOf("?"));
        String tmp = dataString.substring(dataString.indexOf(tokenParameter)+tokenParameter.length());
        token = tmp.contains("&") ? tmp.substring(0, tmp.indexOf("&")) : tmp;

        textView = (TextView) findViewById(R.id.textView2);

        searchLibrary();

    }

    protected void startPlayer(String path)
    {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.parse(path), "video/*" );
        startActivity(newIntent);
    }

    protected String plexPathToLocalPath(String path)
    {
        String replaced = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("path_to_replace", "");
        if(!replaced.equals(""))
        {
            return path.replace(replaced,PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("replaced_with", ""));
        }
        return path;
    }

    protected void startZidooPlayer(String path, int viewOffset)
    {

        //see https://github.com/Andy2244/jellyfin-androidtv-zidoo/blob/Zidoo-Edition/app/src/main/java/org/jellyfin/androidtv/ui/playback/ExternalPlayerActivity.java
        Intent newIntent = new Intent();

        String from = "Local";
        newIntent.putExtra("SourceFrom", from);
        newIntent.setDataAndType(Uri.parse(path), "video/mkv");


        newIntent.putExtra("MEDIA_BROWSER_USE_RT_MEDIA_PLAYER", true);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.setPackage("com.android.gallery3d");
        newIntent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.ZDMCActivity");

        String mode = "zdmc";
        newIntent.putExtra("play_mode", mode);
        String net_work = "local";
        net_work = "smb";
        newIntent.putExtra("net_mode", net_work);

        String username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbUsername", "");
        if(!username.equals(""))
        {
            newIntent.putExtra("smb_username",username);
            newIntent.putExtra("smb_password", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbPassword", ""));
        }

        startActivity(newIntent);

    }


    @Override
    protected void onStop() {
        super.onStop();

    }
}