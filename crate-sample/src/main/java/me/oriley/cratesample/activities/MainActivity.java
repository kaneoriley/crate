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

package me.oriley.cratesample.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import butterknife.Bind;
import me.oriley.cratesample.BuildConfig;
import me.oriley.cratesample.R;
import me.oriley.cratesample.fragments.*;

import static me.oriley.cratesample.activities.FragmentHelper.FLAG_ANIM_ALL;
import static me.oriley.cratesample.activities.FragmentHelper.FLAG_ANIM_NONE;

public class MainActivity extends BaseActivity implements OnNavigationItemSelectedListener {

    private static final String KEY_CURRENT_ITEM = "currentItem";

    @Bind(R.id.nav_view)
    NavigationView mNavigationView;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Nullable
    private Runnable mPendingRunnable;

    @Nullable
    private BaseFragment mPendingRemoveFragment;

    @IdRes
    private int mCurrentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                supportInvalidateOptionsMenu();
                if (mPendingRunnable != null) {
                    mPendingRunnable.run();
                    mPendingRunnable = null;
                }
                setIgnoreTouch(false);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // We don't want the drawer toggle animating
                super.onDrawerSlide(drawerView, 0f);
            }
        };

        //noinspection deprecation
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(R.id.nav_fonts);
        } else {
            mCurrentItem = savedInstanceState.getInt(KEY_CURRENT_ITEM, R.id.nav_fonts);
            mNavigationView.setCheckedItem(mCurrentItem);
        }
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
        outState.putInt(KEY_CURRENT_ITEM, mCurrentItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return onNavigationItemSelected(item.getItemId());
    }

    protected boolean onNavigationItemSelected(int itemId) {

        switch (itemId) {
            case R.id.nav_info:
                mPendingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        showInfoDialog();
                    }
                };
                mDrawerLayout.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_fonts:
                openDrawerFragment(FontListFragment.class, itemId);
                break;
            case R.id.nav_images:
                openDrawerFragment(BitmapListFragment.class, itemId);
                break;
            case R.id.nav_svgs:
                openDrawerFragment(SvgListFragment.class, itemId);
                break;
            case R.id.nav_video:
                openDrawerFragment(VideoListFragment.class, itemId);
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onFragmentAnimationComplete() {
        if (mPendingRemoveFragment != null) {
            removeFragment(mPendingRemoveFragment, FragmentHelper.FLAG_ANIM_NONE);
            mPendingRemoveFragment = null;
        }
    }

    protected void openDrawerFragment(@NonNull final Class<? extends BaseFragment> fragmentClass,
                                      @IdRes int menuId) {
        Fragment fragment = getCurrentFragment();
        if (fragment == null) {
            addFragment(fragmentClass, FLAG_ANIM_NONE, false);
            return;
        }

        if (fragmentClass.isInstance(fragment)) {
            closeDrawer();
            return;
        }

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mPendingRunnable = new Runnable() {
                @Override
                public void run() {
                    mPendingRemoveFragment = getCurrentFragment();
                    addFragment(fragmentClass, FLAG_ANIM_ALL, false);
                }
            };
            setIgnoreTouch(true);
            closeDrawer();
        } else {
            addFragment(fragmentClass, FLAG_ANIM_ALL, false);
        }

        mCurrentItem = menuId;
        mNavigationView.setCheckedItem(menuId);
    }

    private void closeDrawer() {
        mDrawerLayout.closeDrawers();
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
}
