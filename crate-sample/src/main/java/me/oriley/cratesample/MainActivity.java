/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.oriley.cratesample;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Bind;
import me.oriley.crate.Crate;
import me.oriley.crate.FontAsset;
import me.oriley.crate.ImageAsset;
import me.oriley.crate.SvgAsset;
import me.oriley.cratesample.loaders.CrateBitmapLoader;
import me.oriley.cratesample.loaders.CrateFontLoader;
import me.oriley.cratesample.loaders.CrateSvgLoader;
import me.oriley.cratesample.widget.CrateBitmapView;
import me.oriley.cratesample.widget.CrateFontView;
import me.oriley.cratesample.widget.CrateSvgView;

public class MainActivity extends BaseActivity implements OnNavigationItemSelectedListener {

    private static final String KEY_LAYOUT_MANAGER_STATE = "layoutManagerState";
    private static final String KEY_CURRENT_ITEM = "currentItem";

    @Bind(R.id.nav_view)
    NavigationView mNavigationView;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Bind(R.id.view_pager)
    ViewPager mViewPager;

    private Crate mCrate;

    @Nullable
    private RecyclerView.LayoutManager mLayoutManager;

    @IdRes
    private int mCurrentItem;

    private int mBitmapColumnCount;

    private int mSvgColumnCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCrate = new Crate.Builder(this)
                .bitmapCacheMaxSize(20)
                .typefaceCacheMaxSize(200)
                .svgCacheMaxSize(2000)
                .build();

        setContentView(R.layout.activity_main);

        Resources res = getResources();
        mBitmapColumnCount = res.getInteger(R.integer.bitmap_column_count);
        mSvgColumnCount = res.getInteger(R.integer.svg_column_count);

        mRecyclerView.setHasFixedSize(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);

        mCurrentItem = R.id.nav_fonts;
        Parcelable layoutState = null;
        if (savedInstanceState != null) {
            mCurrentItem = savedInstanceState.getInt(KEY_CURRENT_ITEM, mCurrentItem);
            layoutState = savedInstanceState.getParcelable(KEY_LAYOUT_MANAGER_STATE);
        }

        mNavigationView.setCheckedItem(mCurrentItem);
        mLayoutManager = getLayoutManager(mCurrentItem);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(getAdapter(mCurrentItem));
            }
        });

        if (mLayoutManager != null) {
            mLayoutManager.onRestoreInstanceState(layoutState);
        }
        updateViews();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mLayoutManager != null) {
            outState.putParcelable(KEY_LAYOUT_MANAGER_STATE, mLayoutManager.onSaveInstanceState());
        }
        outState.putInt(KEY_CURRENT_ITEM, mCurrentItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mRecyclerView.setAdapter(null);
        mViewPager.setAdapter(null);

        mCrate.clear();
        mCrate = null;

        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return onNavigationItemSelected(item.getItemId());
    }

    private boolean onNavigationItemSelected(@IdRes int id) {
        if (id == R.id.nav_info) {
            showInfoDialog();
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        mCurrentItem = id;
        mLayoutManager = getLayoutManager(mCurrentItem);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                RecyclerView.Adapter adapter = getAdapter(mCurrentItem);
                mRecyclerView.setAdapter(adapter);
                if (adapter != null) {
                    mRecyclerView.scrollToPosition(Integer.MAX_VALUE / 2);
                }
            }
        });
        mViewPager.setAdapter(id == R.id.nav_video ? new VideoPagerAdapter(this) : null);

        updateViews();
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Nullable
    private RecyclerView.LayoutManager getLayoutManager(@IdRes int itemId) {
        switch (itemId) {
            case R.id.nav_fonts:
                return new LinearLayoutManager(this);
            case R.id.nav_images:
                return new GridLayoutManager(this, mBitmapColumnCount);
            case R.id.nav_svgs:
                return new GridLayoutManager(this, mSvgColumnCount);
            default:
                return null;
        }
    }

    @Nullable
    private RecyclerView.Adapter getAdapter(@IdRes int itemId) {
        switch (itemId) {
            case R.id.nav_fonts:
                return new FontRecyclerAdapter(mCrate, mRecyclerView.getMeasuredWidth());
            case R.id.nav_images:
                return new BitmapRecyclerAdapter(mCrate);
            case R.id.nav_svgs:
                return new SvgRecyclerAdapter(mCrate);
            default:
                return null;
        }
    }

    private void updateViews() {
        if (mCurrentItem == R.id.nav_video) {
            mRecyclerView.setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(String.format("%s %s", getString(R.string.app_name), BuildConfig.VERSION_NAME))
                .setMessage(R.string.info_dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private static final class FontViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        CrateFontView view;

        FontViewHolder(@NonNull View view) {
            super(view);
            this.view = (CrateFontView) view;
        }
    }

    private static final class FontRecyclerAdapter extends RecyclerView.Adapter<FontViewHolder> {

        @NonNull
        private final Crate mCrate;

        @NonNull
        private final CrateFontLoader mLoader;

        private final int mActualSize;

        FontRecyclerAdapter(@NonNull Crate crate, int parentSize) {
            mCrate = crate;
            mLoader = new CrateFontLoader(mCrate, parentSize);
            mActualSize = mCrate.assets.fonts.LIST.size();
        }

        @Override
        public FontViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.font_view_item, viewGroup, false);
            return new FontViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FontViewHolder holder, int position) {
            FontAsset fontAsset = mCrate.assets.fonts.LIST.get(position % mActualSize);
            mLoader.loadInto(holder.view, fontAsset);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    private static final class BitmapViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        CrateBitmapView view;

        BitmapViewHolder(@NonNull View view) {
            super(view);
            this.view = (CrateBitmapView) view;
        }
    }

    private static final class BitmapRecyclerAdapter extends RecyclerView.Adapter<BitmapViewHolder> {

        @NonNull
        protected final Crate mCrate;

        @NonNull
        private final CrateBitmapLoader mLoader;

        protected final int mActualSize;

        BitmapRecyclerAdapter(@NonNull Crate crate) {
            mCrate = crate;
            mLoader = new CrateBitmapLoader(crate);
            mActualSize = mCrate.assets.images.LIST.size();
        }

        @Override
        public BitmapViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.bitmap_view_item, viewGroup, false);
            return new BitmapViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BitmapViewHolder holder, int position) {
            ImageAsset imageAsset = mCrate.assets.images.LIST.get(position % mActualSize);
            mLoader.loadInto(holder.view, imageAsset);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    private static final class SvgViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        CrateSvgView view;

        SvgViewHolder(@NonNull View view) {
            super(view);
            this.view = (CrateSvgView) view;
        }
    }

    private static final class SvgRecyclerAdapter extends RecyclerView.Adapter<SvgViewHolder> {

        @NonNull
        protected final Crate mCrate;

        @NonNull
        private final CrateSvgLoader mLoader;

        protected final int mActualSize;

        SvgRecyclerAdapter(@NonNull Crate crate) {
            mCrate = crate;
            mLoader = new CrateSvgLoader(crate);
            mActualSize = mCrate.assets.svgs.LIST.size();
        }

        @Override
        public SvgViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.svg_view_item, viewGroup, false);
            return new SvgViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SvgViewHolder holder, int position) {
            SvgAsset svgAsset = mCrate.assets.svgs.LIST.get(position % mActualSize);
            mLoader.loadInto(holder.view, svgAsset);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    private static final class VideoPagerAdapter extends FragmentStatePagerAdapter {

        @NonNull
        private final VideoListFragment mFragment;

        public VideoPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity.getSupportFragmentManager());
            mFragment = new VideoListFragment();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragment;
        }

        @Override
        public int getCount() {
            return 1;
        }
    }
}
