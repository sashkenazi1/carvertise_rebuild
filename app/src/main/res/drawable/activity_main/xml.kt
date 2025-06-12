<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:background="#FFFFFF"
android:padding="16dp">

<!-- כותרת -->
<TextView
android:id="@+id/appTitle"
android:text="Carvertise.ai 🚗"
android:textSize="24sp"
android:textStyle="bold"
android:textColor="#4CAF50"
android:layout_width="wrap_content"
android:layout_height="wrap_content"
android:layout_centerHorizontal="true"
android:layout_alignParentTop="true"
android:layout_marginBottom="8dp" />

<!-- אזור תמונה -->
<FrameLayout
android:id="@+id/imageFrame"
android:layout_below="@id/appTitle"
android:layout_above="@+id/locationText"
android:layout_width="match_parent"
android:layout_height="0dp"
android:layout_marginTop="8dp"
android:layout_marginBottom="8dp"
android:layout_alignParentStart="true"
android:layout_alignParentEnd="true"
android:layout_weight="1">

<ImageView
android:id="@+id/adImage"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:scaleType="fitXY"
android:contentDescription="פרסומת מוצגת"
android:background="@drawable/image_border"
android:clipToOutline="true" />

<TextView
android:id="@+id/locationBubble"
android:text="Lat: 0.0\nLon: 0.0"
android:textSize="14sp"
android:textColor="#FFFFFF"
android:background="@drawable/bubble_background"
android:padding="8dp"
android:layout_gravity="top|end"
android:layout_margin="12dp"
android:elevation="4dp"
android:layout_width="wrap_content"
android:layout_height="wrap_content" />
</FrameLayout>

<!-- טקסט מיקום -->
<TextView
android:id="@+id/locationText"
android:text="Latitude: 0.0\nLongitude: 0.0"
android:textSize="18sp"
android:textColor="#333333"
android:layout_width="wrap_content"
android:layout_height="wrap_content"
android:layout_above="@+id/buttonLayout"
android:layout_centerHorizontal="true"
android:layout_marginBottom="12dp"
android:textAlignment="center"
android:gravity="center" />

<!-- כפתורים -->
<LinearLayout
android:id="@+id/buttonLayout"
android:layout_alignParentBottom="true"
android:layout_width="match_parent"
android:layout_height="wrap_content"
android:orientation="horizontal"
android:gravity="center"
android:layout_marginBottom="16dp">

<Button
android:id="@+id/openMapButton"
android:text="הצג במפה"
android:layout_width="wrap_content"
android:layout_height="wrap_content"
android:backgroundTint="#4CAF50"
android:textColor="#FFFFFF" />
</LinearLayout>

<!-- ספינר טעינה -->
<ProgressBar
android:id="@+id/loadingSpinner"
android:layout_centerInParent="true"
android:layout_width="wrap_content"
android:layout_height="wrap_content"
android:visibility="gone"
style="?android:attr/progressBarStyleSmall" />
</RelativeLayout>
