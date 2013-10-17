package org.nsdev.apps.linktester;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.DisplayMetrics;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.MarkerOptionsChooser;

import java.lang.ref.WeakReference;

/**
 * Created by neal on 2013-09-19.
 */
public class PortalMarkerOptionsChooser extends MarkerOptionsChooser {

    private final WeakReference<Context> contextRef;
    private final Paint clusterPaintMedium;
    private final Paint clusterPaintSmall;
    private final Paint clusterPaintLarge;
    private final Paint clusterPaintGreyBox;

    public PortalMarkerOptionsChooser(Activity context) {
        this.contextRef = new WeakReference<Context>(context);

        Resources res = context.getResources();

        clusterPaintMedium = new Paint();
        clusterPaintMedium.setColor(Color.WHITE);
        clusterPaintMedium.setAlpha(255);
        clusterPaintMedium.setAntiAlias(true);
        clusterPaintMedium.setDither(false);
        clusterPaintMedium.setShadowLayer(6,0,0,Color.BLACK);
        clusterPaintMedium.setTextAlign(Paint.Align.CENTER);
        clusterPaintMedium.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC));
        clusterPaintMedium.setTextSize(res.getDimension(R.dimen.cluster_text_size_medium));

        clusterPaintSmall = new Paint(clusterPaintMedium);
        clusterPaintSmall.setTextSize(res.getDimension(R.dimen.cluster_text_size_small));

        clusterPaintLarge = new Paint(clusterPaintMedium);
        clusterPaintLarge.setTextSize(res.getDimension(R.dimen.cluster_text_size_large));

        clusterPaintGreyBox = new Paint();
        clusterPaintGreyBox.setColor(Color.argb(255, 10, 28, 77));
        clusterPaintGreyBox.setDither(false);
        clusterPaintGreyBox.setStrokeWidth(0);
    }

    @Override
    public void choose(MarkerOptions markerOptions, ClusterPoint clusterPoint) {
        Context context = contextRef.get();
        if (context != null) {
            Resources res = context.getResources();
            boolean isCluster = clusterPoint.size() > 1;
            BitmapDescriptor icon;
            String title = null;
            String snippet = null;

            if (isCluster) {
                icon = BitmapDescriptorFactory.fromBitmap(getClusterBitmap(res, R.drawable.ic_portal, clusterPoint.size()));
            } else {
                PortalKey key = (PortalKey)clusterPoint.getPointAtOffset(0).getTag();
                title = key.getPortalTitle();
                snippet = key.getPortalAddress();
                if (key.getKeyCount() > 1) {
                    icon = BitmapDescriptorFactory.fromBitmap(getClusterBitmap(res, R.drawable.ic_portal, key.getKeyCount()));
                }
                else
                {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_portal);
                }
            }
            markerOptions.icon(icon);
            markerOptions.title(title);
            markerOptions.snippet(snippet);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.infoWindowAnchor(0.5f,0f);
        }
    }

    @SuppressLint("NewApi")
    private Bitmap getClusterBitmap(Resources res, int resourceId, int clusterSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = true;
        }
        Bitmap bitmap = BitmapFactory.decodeResource(res, resourceId, options);
        if (!bitmap.isMutable()) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Canvas canvas = new Canvas(bitmap);

        Paint paint;
        float originY;
        if (clusterSize < 100) {
            paint = clusterPaintLarge;
            originY = bitmap.getHeight() * 0.64f;
        } else if (clusterSize < 1000) {
            paint = clusterPaintMedium;
            originY = bitmap.getHeight() * 0.6f;
        } else {
            paint = clusterPaintSmall;
            originY = bitmap.getHeight() * 0.56f;
        }

        canvas.drawText(String.valueOf(clusterSize), bitmap.getWidth() * 0.5f, originY, paint);

        return bitmap;
    }
}
