package com.hpn789.plextozidoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Play extends AppCompatActivity {

    static final String tokenParameter = "X-Plex-Token=";
    private Intent intent;
    private String address = "";
    private String videoKey = "";
    private String libraryKey = "";
    private String token = "";
    private int duration = 0;
    private int viewOffset = 0;
    private String directPath = "";

    private TextView textView;
    private Button playButton;

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
        String url = address + "/library/sections/" + info.getKey() + "/search?type=" + info.getType().searchId + "&" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(libraryKey);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            directPath = plexPathToLocalPath(path);
                            videoKey = parser.getVideoKey();
                            duration = parser.getDuration();

                            // If the debug flag is on then update the text field
                            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("debug", false))
                            {
                                textView.setText(String.format(Locale.ENGLISH, "Intent: %s\n\nPath Substitution: %s\n\nView Offset: %d\n\nDuration: %d\n\nAddress: %s\n\nVideo Key: %s\n\nToken: %s\n\nLibrary Key: %s\n\nMedia Type: %s", intentToString(intent), directPath, viewOffset, duration, address, videoKey, token, info.getKey(), info.getType().name));
                                playButton.setEnabled(true);
                            }
                            // Else just play the movie
                            else
                            {
                                playButton.callOnClick();
                            }
                        }
                        else if(index + 1 < infos.size())
                        {
                            searchPath(infos, index + 1);
                        }
                        else
                        {
                            textView.setText(String.format(Locale.ENGLISH, "Not found\n\nIntent: %s", intentToString(intent)));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },
                error -> textView.setText("That didn't work"));

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchLibrary()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/sections/?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
                    String[] names = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("libraries", "").split(",");
                    PlexXmlParser parser = new PlexXmlParser(Arrays.asList(names));
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try {
                        List<PlexLibraryInfo> libraries = parser.parse(targetStream);
                        searchPath(libraries, 0);

                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }

                },
                error -> textView.setText("That didn't work!"));

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();

        intent = getIntent();

        String inputString = intent.getDataString();
        Log.d("plex", "" + inputString);
        int indexOfLibrary = inputString.indexOf("/library/");
        address = inputString.substring(0, indexOfLibrary);
        libraryKey = inputString.substring(indexOfLibrary, inputString.indexOf("?"));
        String tmp = inputString.substring(inputString.indexOf(tokenParameter) + tokenParameter.length());
        token = tmp.contains("&") ? tmp.substring(0, tmp.indexOf("&")) : tmp;

        viewOffset = intent.getIntExtra("viewOffset", 0);

        textView = findViewById(R.id.textView2);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(v -> {
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("useZidooPlayer", true))
            {
                startZidooPlayer(directPath, viewOffset);
            }
            else
            {
                startPlayer(directPath);
            }
        });

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
            return path.replace(replaced, PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("replaced_with", "")).replace("\\", "/");
        }
        return path;
    }

    protected void startZidooPlayer(String path, int viewOffset)
    {
        // see https://github.com/Andy2244/jellyfin-androidtv-zidoo/blob/Zidoo-Edition/app/src/main/java/org/jellyfin/androidtv/ui/playback/ExternalPlayerActivity.java
        // NOTE: This code requires the new ZIDOO API to work. 6.4.42+
        Intent newIntent = new Intent(Intent.ACTION_VIEW);

        String from = "Local";
        newIntent.putExtra("SourceFrom", from);
        newIntent.setDataAndType(Uri.parse(path), "video/mkv");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        newIntent.setPackage("com.android.gallery3d");
        newIntent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.MovieActivity");

        if(viewOffset > 0)
        {
            newIntent.putExtra("from_start", false);
            newIntent.putExtra("position", viewOffset);
        }
        else
        {
            newIntent.putExtra("from_start", true);
        }

        newIntent.putExtra("return_result", true);

        String username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbUsername", "");
        if(!username.equals(""))
        {
            newIntent.putExtra("smb_username", username);
            newIntent.putExtra("smb_password", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbPassword", ""));
        }

        startActivityForResult(newIntent, 98);
    }


    @Override
    protected void onStop() {
        super.onStop();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && requestCode == 98)
        {
            int position = data.getIntExtra("position", 0);
            if(position > 0)
            {
                RequestQueue queue = Volley.newRequestQueue(this);
                String url;
                if(duration > 0 && position > (duration * .9))
                {
                    // Mark it as watched
                    url = address + "/:/scrobble?key=" + videoKey + "&identifier=com.plexapp.plugins.library&" + tokenParameter + token;
                }
                else
                {
                    // Update progress
                    url = address + "/:/progress?key=" + videoKey + "&identifier=com.plexapp.plugins.library&time=" + position + "&state=stopped&" + tokenParameter + token;
                }

                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {
                            // Nothing to do
                        },
                        error -> Toast.makeText(getApplicationContext(), "That didn't work!", Toast.LENGTH_LONG).show()
                );

                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        }
    }

    public static String intentToString(Intent intent)
    {
        if (intent == null)
            return "";

        StringBuilder stringBuilder = new StringBuilder("action: ")
                .append(intent.getAction())
                .append(" data: ")
                .append(intent.getDataString())
                .append(" extras: ")
                ;
        for (String key : intent.getExtras().keySet())
            stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");

        return stringBuilder.toString();

    }
}