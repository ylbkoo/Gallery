/* ========================================================================== */
/* Modifications on Features list / Changes Request / Problems Report         */
/* -------------------------------------------------------------------------- */
/* date      | author         | key              | comment (what, where, why) */
/* ----------|----------------|------------------|--------------------------- */
/* 08/30/2013| jiawei.li      | FR-487461        | [Ergo] Video player        */
/*-----------|----------------|------------------|--------------------------- */
/* ========================================================================== */

package com.android.gallery3d.ext;

import android.net.Uri;
import android.util.Log;

import java.util.Locale;

/**
 * Util class for Movie functions. *
 */
public class MovieUtils {

    private static final String TAG = "MovieUtils";
    private static final boolean LOG = true;

    private MovieUtils() {}

    /**
     * Whether current video(Uri) is RTSP streaming or not.
     * @param uri
     * @param mimeType
     * @return
     */
    public static boolean isRtspStreaming(Uri uri, String mimeType) {
        boolean rtsp = false;
        if (uri != null) {
            if ("rtsp".equalsIgnoreCase(uri.getScheme())) {
                rtsp = true;
            }
        }
        if (LOG) {
            Log.d(TAG, "isRtspStreaming(" + uri + ", " + mimeType + ") return " + rtsp);
        }
        return rtsp;
    }

    /**
     * Whether current video(Uri) is HTTP streaming or not.
     * @param uri
     * @param mimeType
     * @return
     */
    public static boolean isHttpStreaming(Uri uri, String mimeType) {
        boolean http = false;
        if (uri != null) {
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                http = true;
            } else if ("https".equalsIgnoreCase(uri.getScheme())) {
                http = true;
            }
        }
        if (LOG) {
            Log.d(TAG, "isHttpStreaming(" + uri + ", " + mimeType + ") return " + http);
        }
        return http;
    }

    /**
     * Whether current video(Uri) is live streaming or not.
     * @param uri
     * @param mimeType
     * @return
     */
    public static boolean isSdpStreaming(Uri uri, String mimeType) {
        boolean sdp = false;
        if (uri != null) {
            if ("application/sdp".equals(mimeType)) {
                sdp = true;
            } else if (uri.toString().toLowerCase(Locale.ENGLISH).endsWith(".sdp")) {
                sdp = true;
            }
        }
        if (LOG) {
            Log.d(TAG, "isSdpStreaming(" + uri + ", " + mimeType + ") return " + sdp);
        }
        return sdp;
    }

    /**
     * Whether current video(Uri) is local file or not.
     * @param uri
     * @param mimeType
     * @return
     */
    public static boolean isLocalFile(Uri uri, String mimeType) {
        boolean local = (!isSdpStreaming(uri, mimeType)
                && !isRtspStreaming(uri, mimeType)
                && !isHttpStreaming(uri, mimeType));
        if (LOG) {
            Log.d(TAG, "isLocalFile(" + uri + ", " + mimeType + ") return " + local);
        }
        return local;
    }
}
