package org.nsdev.apps.linktester.inventory;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by neal on 2013-10-13.
 */
public interface InventoryServiceAsync {
    @POST("/rpc/playerUndecorated/getInventory")
    @Headers({"content-type: text/plain;charset=UTF-8", "accept-encoding: gzip"
    })
    public void getInventory(@Header("X-XsrfToken") String token, @Body Object parameters, Callback<Response> inventoryResponse);

    @GET("/rpc/playerUndecorated/getInventory?json=")
    public void getInventoryRaw(Callback<Response> response);

    @GET("/_ah/login?continue=http://localhost/")
    public void authenticat(@Query("auth") String auth, Callback<Response> response);
}
