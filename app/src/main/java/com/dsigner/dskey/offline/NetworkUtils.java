package com.dsigner.dskey.offline;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtils {

    public static boolean hasInternet(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        NetworkCapabilities cap =
                cm.getNetworkCapabilities(cm.getActiveNetwork());

        return cap != null &&
                cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}