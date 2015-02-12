package net.aayush.skooterapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import net.aayush.skooterapp.AppController;
import net.aayush.skooterapp.BaseActivity;
import net.aayush.skooterapp.PeekActivity;
import net.aayush.skooterapp.PeekPostAdapter;
import net.aayush.skooterapp.R;
import net.aayush.skooterapp.ViewPostActivity;
import net.aayush.skooterapp.data.Post;
import net.aayush.skooterapp.data.Zone;
import net.aayush.skooterapp.data.ZoneDataHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Peek extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    protected static final String LOG_TAG = Peek.class.getSimpleName();
    protected Context mContext;
    protected ListView mListView;
    protected ArrayAdapter<Zone> zoneArrayAdapter;
    protected ArrayList<Zone> followingZones = new ArrayList<Zone>();
    private PeekPostAdapter mPostsAdapter;
    private ListView mListPosts;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<Post> mPostsList = new ArrayList<Post>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = container.getContext();
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_peek, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        downloadZones();

        //Get all the zones
        final ZoneDataHandler dataHandler = new ZoneDataHandler(mContext);
        List<Zone> zones = dataHandler.getAllZones();

        findZonesFollowedByUser(zones);

        mListView = (ListView) rootView.findViewById(R.id.list_zones);

        if (followingZones.size() > 0) {
            //Fetch the peek posts for the person
            getPeekData();

            TextView addZonesTextView = (TextView) rootView.findViewById(R.id.addZonesText);
            addZonesTextView.setVisibility(View.GONE);

            mPostsAdapter = new PeekPostAdapter(mContext, R.layout.list_view_peek_row, mPostsList);

            mListView.setAdapter(mPostsAdapter);

            mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), ViewPostActivity.class);
                    intent.putExtra(BaseActivity.SKOOTER_POST, mPostsList.get(position));
                    startActivity(intent);
                }
            });
        } else {
            mListView = (ListView) rootView.findViewById(R.id.list_zones);
            final ArrayAdapter<Zone> zoneArrayAdapter = new ArrayAdapter<Zone>(mContext, android.R.layout.simple_list_item_1, zones);
            View header = getLayoutInflater(savedInstanceState).inflate(R.layout.list_header_text_view, null);
            mListView.addHeaderView(header);
            mListView.setAdapter(zoneArrayAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dataHandler.followZoneById(position + 1);
                }
            });
        }

        return rootView;
    }

    @Override
    public void onRefresh() {
        getPeekData();
    }

    public void getPeekData() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("user_id", Integer.toString(BaseActivity.userId));

        String url = BaseActivity.substituteString(getResources().getString(R.string.peek), params);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                final String SKOOTS = "skoots";
                final String SKOOT_ID = "id";
                final String SKOOT_POST = "content";
                final String SKOOT_HANDLE = "channel";
                final String SKOOT_UPVOTES = "upvotes";
                final String SKOOT_DOWNVOTES = "downvotes";
                final String SKOOT_IF_USER_VOTED = "user_voted";
                final String SKOOT_USER_VOTE = "user_vote";
                final String SKOOT_USER_SCOOT = "user_skoot";
                final String SKOOT_CREATED_AT = "created_at";
                final String SKOOT_COMMENTS_COUNT = "comments_count";
                final String SKOOT_FAVORITE_COUNT = "favorites_count";
                final String SKOOT_USER_FAVORITED = "user_favorited";
                final String SKOOT_USER_COMMENTED = "user_commented";
                final String SKOOT_IMAGE_URL = "zone_image";
                final String SKOOT_IMAGE_PRESENT = "image_present";
                final String SKOOT_SMALL_IMAGE_URL = "small_image_url";
                final String SKOOT_LARGE_IMAGE_URL = "large_image_url";

                try {
                    JSONArray jsonArray = response.getJSONArray(SKOOTS);

                    mPostsList.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonPost = jsonArray.getJSONObject(i);
                        int id = jsonPost.getInt(SKOOT_ID);
                        String post = jsonPost.getString(SKOOT_POST);
                        String channel = "";
                        if (!jsonPost.isNull(SKOOT_HANDLE)) {
                            channel = "@" + jsonPost.getString(SKOOT_HANDLE);
                        }
                        int upvotes = jsonPost.getInt(SKOOT_UPVOTES);
                        int commentsCount = jsonPost.getInt(SKOOT_COMMENTS_COUNT);
                        int downvotes = jsonPost.getInt(SKOOT_DOWNVOTES);
                        boolean skoot_if_user_voted = jsonPost.getBoolean(SKOOT_IF_USER_VOTED);
                        boolean user_vote = jsonPost.getBoolean(SKOOT_USER_VOTE);
                        boolean user_skoot = jsonPost.getBoolean(SKOOT_USER_SCOOT);
                        boolean user_favorited = jsonPost.getBoolean(SKOOT_USER_FAVORITED);
                        boolean user_commented = jsonPost.getBoolean(SKOOT_USER_COMMENTED);
                        int favoriteCount = jsonPost.getInt(SKOOT_FAVORITE_COUNT);
                        String created_at = jsonPost.getString(SKOOT_CREATED_AT);
                        String image_url = jsonPost.getString(SKOOT_IMAGE_URL);
                        boolean isImagePresent = jsonPost.getBoolean(SKOOT_IMAGE_PRESENT);
                        String small_image_url = jsonPost.getString(SKOOT_SMALL_IMAGE_URL);
                        String large_image_url = jsonPost.getString(SKOOT_LARGE_IMAGE_URL);

                        Post postObject = new Post(id, channel, post, commentsCount, favoriteCount, upvotes, downvotes, skoot_if_user_voted, user_vote, user_skoot, user_favorited, user_commented, created_at, image_url, isImagePresent, small_image_url, large_image_url);
                        mPostsList.add(postObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Error processing Json Data");
                }
                if(mPostsAdapter != null) {
                    mPostsAdapter.notifyDataSetChanged();
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(LOG_TAG, "Error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();

                if (headers == null
                        || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<String, String>();
                }

                headers.put("user_id", Integer.toString(BaseActivity.userId));
                headers.put("access_token", BaseActivity.accessToken);

                return headers;
            }
        };

        AppController.getInstance().addToRequestQueue(jsonObjectRequest, "home_page");
    }
    private void downloadZones() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("user_id", Integer.toString(BaseActivity.userId));
        params.put("location_id", Integer.toString(BaseActivity.locationId));

        final String url = BaseActivity.substituteString(getResources().getString(R.string.zones), params);
        final ZoneDataHandler dataHandler = new ZoneDataHandler(mContext);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                final String ZONE_ID = "zone_id";
                final String NAME = "name";
                final String LATITUDE_MINIMUM = "lat_min";
                final String LATITUDE_MAXIMUM = "lat_max";
                final String LONGITUDE_MINIMUM = "long_min";
                final String LONGITUDE_MAXIMUM = "long_max";
                final String USER_FOLLOWS = "user_follows";

                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int zoneId = jsonObject.getInt(ZONE_ID);
                        String name = jsonObject.getString(NAME);
                        float latitudeMinimum = (float) jsonObject.getDouble(LATITUDE_MINIMUM);
                        float latitudeMaximum = (float) jsonObject.getDouble(LATITUDE_MAXIMUM);
                        float longitudeMinimum = (float) jsonObject.getDouble(LONGITUDE_MINIMUM);
                        float longitudeMaximum = (float) jsonObject.getDouble(LONGITUDE_MAXIMUM);
                        boolean userFollows = jsonObject.getBoolean(USER_FOLLOWS);

                        Zone zone = new Zone(zoneId, name, latitudeMinimum, latitudeMaximum, longitudeMinimum, longitudeMaximum, userFollows);

                        List<Zone> zones = dataHandler.getAllZones();

                        boolean flag = false;
                        for(Zone z: zones) {
                            if(z.getZoneId() == zone.getZoneId()) {
                                flag = true;
                                break;
                            }
                        }
                        if(!flag) {
                            //Add
                            dataHandler.addZone(zone);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(LOG_TAG, error.networkResponse);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();

                if (headers == null
                        || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<String, String>();
                }

                headers.put("user_id", Integer.toString(BaseActivity.userId));
                headers.put("access_token", BaseActivity.accessToken);

                return headers;
            }
        };

        AppController.getInstance().addToRequestQueue(jsonArrayRequest, "zones");
    }

    private void findZonesFollowedByUser(List<Zone> zones) {
        followingZones.clear();
        for (Zone zone : zones) {
            if (zone.getIsFollowing()) {
                followingZones.add(zone);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ZoneDataHandler dataHandler = new ZoneDataHandler(mContext);
        List<Zone> zones = dataHandler.getAllZones();
        findZonesFollowedByUser(zones);
        if (zoneArrayAdapter != null) {
            zoneArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.menu_peek, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_zone) {
            Intent intent = new Intent(mContext, PeekActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}