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

import android.support.annotation.*;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import me.oriley.cratesample.fragments.BaseFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.oriley.cratesample.utils.FlagUtils.hasFlag;

@SuppressWarnings("unused")
public class FragmentHelper {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLAG_ANIM_NONE, FLAG_ANIM_ENTER, FLAG_ANIM_EXIT, FLAG_ANIM_POP_ENTER, FLAG_ANIM_POP_EXIT, FLAG_ANIM_ALL})
    public @interface AnimationType {}

    public static final int FLAG_ANIM_NONE        = 0x00000000;
    public static final int FLAG_ANIM_ENTER       = 0x00000001;
    public static final int FLAG_ANIM_EXIT        = 0x00000002;
    public static final int FLAG_ANIM_POP_ENTER   = 0x00000004;
    public static final int FLAG_ANIM_POP_EXIT    = 0x00000008;
    public static final int FLAG_ANIM_ALL         = 0x000000FF;

    @NonNull
    private final AppCompatActivity mActivity;

    @NonNull
    private final FragmentManager mFragmentManager;

    @IdRes
    private final int mFragmentContainerId;

    @AnimRes
    private final int mInAnimation;

    @AnimRes
    private final int mOutAnimation;

    @AnimRes
    private final int mInPopAnimation;

    @AnimRes
    private final int mOutPopAnimation;


    FragmentHelper(@NonNull AppCompatActivity activity,
                   @IdRes int fragmentContainerId,
                   @AnimRes int inAnimation,
                   @AnimRes int outAnimation,
                   @AnimRes int inPopAnimation,
                   @AnimRes int outPopAnimation) {
        mActivity = activity;
        mFragmentManager = activity.getSupportFragmentManager();
        mFragmentContainerId = fragmentContainerId;
        mInAnimation = inAnimation;
        mOutAnimation = outAnimation;
        mInPopAnimation = inPopAnimation;
        mOutPopAnimation = outPopAnimation;
    }


    @Nullable
    public final BaseFragment getCurrentFragment() {
        return (BaseFragment) mFragmentManager.findFragmentById(mFragmentContainerId);
    }

    public final boolean isCurrentFragment(@NonNull BaseFragment fragment) {
        return getCurrentFragment() == fragment;
    }

    public final void removeCurrentFragment() {
        removeCurrentFragment(FLAG_ANIM_NONE);
    }

    public final void removeCurrentFragment(@AnimationType int animationFlags) {
        BaseFragment fragment = getCurrentFragment();
        if (fragment != null) {
            removeFragment(fragment, animationFlags);
        }
    }

    public void removeFragment(@NonNull BaseFragment fragment, @AnimationType int animationFlags) {
        if (!mActivity.isFinishing() && !fragment.isDetached()) {
            mFragmentManager.beginTransaction()
                    .setCustomAnimations(hasFlag(animationFlags, FLAG_ANIM_ENTER) ? mInAnimation : 0,
                            hasFlag(animationFlags, FLAG_ANIM_EXIT) ? mOutAnimation : 0)
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    public final void addFragment(@NonNull Class<? extends BaseFragment> fragmentClass,
                                  @AnimationType int animationFlags,
                                  boolean addToBackStack) {
        BaseFragment fragment = findOrCreate(fragmentClass);
        showFragment(fragment, animationFlags, addToBackStack, false);
    }

    public final <T extends BaseFragment> void showFragment(@NonNull Class<T> fragmentClass) {
        T fragment = findOrCreate(fragmentClass);
        showFragment(fragment);
    }

    public final <T extends BaseFragment> void showFragment(@NonNull Class<T> fragmentClass,
                                                            @AnimationType int animationFlags,
                                                            boolean addToBackStack) {
        T fragment = findOrCreate(fragmentClass);
        showFragment(fragment, animationFlags, addToBackStack);
    }

    public final void showFragment(@NonNull BaseFragment fragment) {
        showFragment(fragment, FLAG_ANIM_NONE, true);
    }

    public final void showFragment(@NonNull BaseFragment fragment,
                                   @AnimationType int animationFlags,
                                   boolean addToBackStack) {
        showFragment(fragment, animationFlags, addToBackStack, true);
    }

    public final void showFragment(@NonNull BaseFragment fragment,
                                   @AnimationType int animationFlags,
                                   boolean addToBackStack,
                                   boolean replace) {
        if (!mActivity.isFinishing()) {
            String fragmentName = fragment.getClass().getSimpleName();
            FragmentTransaction ft = mFragmentManager.beginTransaction();

            int enterAnim = hasFlag(animationFlags, FLAG_ANIM_ENTER) ? mInAnimation : 0;
            int exitAnim = hasFlag(animationFlags, FLAG_ANIM_EXIT) ? mOutAnimation : 0;
            int enterPopAnim = hasFlag(animationFlags, FLAG_ANIM_POP_ENTER) ? mInPopAnimation : 0;
            int exitPopAnim = hasFlag(animationFlags, FLAG_ANIM_POP_EXIT) ? mOutPopAnimation : 0;

            ft.setCustomAnimations(enterAnim, exitAnim, enterPopAnim, exitPopAnim);
            if (addToBackStack) {
                ft.addToBackStack(fragmentName);
            }
            if (replace) {
                ft.replace(mFragmentContainerId, fragment, defaultTag(fragment.getClass()));
            } else {
                ft.add(mFragmentContainerId, fragment, defaultTag(fragment.getClass()));
            }
            ft.commitAllowingStateLoss();
        }
    }

    @NonNull
    public final <T extends BaseFragment> T findOrCreate(@NonNull Class<T> fragmentClass) {
        return findByTagOrCreate(fragmentClass);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final <T extends BaseFragment> T findByTag(@NonNull Class<? extends BaseFragment> fragmentClass) {
        return findByTag(defaultTag(fragmentClass));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final <T extends BaseFragment> T findByTag(@NonNull String tag) {
        return (T) mFragmentManager.findFragmentByTag(tag);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <T extends BaseFragment> T findByTagOrCreate(@NonNull Class<? extends BaseFragment> fragmentClass) {
        BaseFragment fragment = findByTag(fragmentClass);
        if (fragment == null) {
            fragment = (BaseFragment) BaseFragment.instantiate(mActivity, fragmentClass.getName());
        }
        return (T) fragment;
    }

    @NonNull
    protected static String defaultTag(@NonNull Class<? extends BaseFragment> fragmentClass) {
        return fragmentClass.getName();
    }
}
