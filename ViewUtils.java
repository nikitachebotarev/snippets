package ru.cnv.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ru.cnv.helper.BaseRecyclerAdapter;
import ru.cnv.helper.BaseViewpagerAdapter;
import ru.cnv.helper.PagerBinder;
import ru.cnv.helper.RecyclerBinder;
import ru.cnv.helper.TabBinder;

import static java.security.AccessController.getContext;

public class ViewUtils {

    public static void configureToggleButton(DrawerLayout drawerLayout, NavigationView navigationView, View toggle) {
        toggle.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });
    }

    public static void replace(AppCompatActivity activity, Fragment fragment, int containerId) {
        new Handler().postDelayed(() -> {
            activity.runOnUiThread(() -> {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(containerId, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }, 600);
    }

    public static void replaceWithArgument(AppCompatActivity activity, Fragment fragment, int containerId, String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        fragment.setArguments(bundle);
        ViewUtils.replace(activity, fragment, containerId);
    }

    public static void configureViewpager(ViewPager viewPager, ViewGroup tabsViewGroup, JsonArray jsonArray, TabBinder tabBinder, int initialPosition, PagerBinder... binders) {
        BaseViewpagerAdapter viewpagerAdapter = new BaseViewpagerAdapter();
        viewpagerAdapter.setJsonArray(jsonArray);
        viewpagerAdapter.setBinders(binders);
        viewPager.setAdapter(viewpagerAdapter);
        viewPager.addOnPageChangeListener(new TabBinder.OnPageChangeListener(tabsViewGroup, tabBinder));
        tabsViewGroup.removeAllViews();
        for (JsonElement jsonElement : jsonArray) {
            View view = LayoutInflater.from(viewPager.getContext()).inflate(tabBinder.getTabLayout(), tabsViewGroup, false);
            tabBinder.setTabTitle(((JsonObject) jsonElement), (ViewGroup) view);
            tabsViewGroup.addView(view);
            view.setOnClickListener(v -> {
                for (int i = 0; i < tabsViewGroup.getChildCount(); i++) {
                    if (tabsViewGroup.getChildAt(i).equals(v)) {
                        viewPager.setCurrentItem(i, true);
                    }
                }
            });
        }

        viewPager.setCurrentItem(initialPosition, false);
        for (int i = 0; i < tabsViewGroup.getChildCount(); i++) {
            tabBinder.onSelectionChanged(i == initialPosition, (ViewGroup) tabsViewGroup.getChildAt(i));
        }
    }

//    public static void configureExoPlayer(SimpleExoPlayerView simpleExoPlayerView, String url) {
//        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
//        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
//        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
//        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector);
//        simpleExoPlayerView.setPlayer(player);
//        simpleExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
//        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "yourApplicationName"));
//        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url));
//        player.prepare(videoSource);
//    }

    public static void configureRecyclerView(RecyclerView recyclerView, JsonArray jsonArray, RecyclerBinder... binders) {
        BaseRecyclerAdapter adapter = new BaseRecyclerAdapter();
        adapter.setJsonArray(jsonArray);
        adapter.setBinders(binders);
        recyclerView.setAdapter(adapter);
    }

    public static void shortToast(AppCompatActivity appCompatActivity, String text) {
        Toast.makeText(appCompatActivity, text, Toast.LENGTH_SHORT).show();
    }

    public static void longToast(AppCompatActivity appCompatActivity, String text) {
        Toast.makeText(appCompatActivity, text, Toast.LENGTH_LONG).show();
    }

    public static void shortSnack(String text, View view) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    public static void share(AppCompatActivity activity, String message) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        activity.startActivity(Intent.createChooser(intent, ""));
    }

    public static void browser(AppCompatActivity activity, String url) {
        if (!url.startsWith("https://")) {
            url = "https://" + url;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);
    }

    public static void image(Context context, View imageView, String image) {
        Glide.with(context).load(Uri.parse(image)).into((ImageView) imageView);
    }

    public static void text(View textView, String text) {
        ((TextView) textView).setText(text);
    }

    public static void list(AppCompatActivity activity, ViewGroup root, @LayoutRes int layout, JsonArray jsonArray, TabBinder binder) {
        root.removeAllViews();
        for (JsonElement item : jsonArray) {
            ViewGroup view = (ViewGroup) LayoutInflater.from(activity).inflate(layout, root, false);
            view.setOnClickListener(v -> {
                for (int i = 0; i < root.getChildCount(); i++) {
                    binder.onSelectionChanged(root.getChildAt(i) == v, ((ViewGroup) root.getChildAt(i)));
                }
            });
            binder.setTabTitle(item.getAsJsonObject(), view);
            root.addView(view);
        }
        if (root.getChildCount() != 0) {
            binder.onSelectionChanged(true, ((ViewGroup) root.getChildAt(0)));
        }
    }
}
