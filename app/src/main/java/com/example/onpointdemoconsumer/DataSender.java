package com.example.onpointdemoconsumer;

import android.content.Context;
import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class DataSender {
    private String DatabaseURL;
    private String userId;
    private RequestQueue queue;
    private Context context;

    public DataSender(Context context, String URL, String userId){
        DatabaseURL=URL;
        this.userId=userId;
        this.context = context.getApplicationContext();
        queue = Volley.newRequestQueue(this.context);
    }
    public void uploadLocation(String Latitude, String Longitude){
        JSONObject LL = new JSONObject();
        try {
            LL.put("latitude", Latitude);
            LL.put("longitude", Longitude);
            LL.put("timestamp", System.currentTimeMillis()/1000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        final String mRequestBody = LL.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, DatabaseURL+"/locationhistories/update?userId="+userId, response -> Log.i("LOG_RESPONSE", response), error -> Log.e("LOG_RESPONSE", error.toString())) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null)
                    responseString = String.valueOf(response.statusCode);
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        queue.add(stringRequest);
    }
}
