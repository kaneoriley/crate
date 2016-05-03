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

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import butterknife.ButterKnife;
import me.oriley.cratesample.R;

@SuppressWarnings("unused")
public abstract class BaseActivity extends AppCompatActivity {

    @NonNull
    protected FragmentHelper mFragmentHelper;

    private boolean mIgnoreTouch;

    @CallSuper
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentHelper = new FragmentHelper(this, R.id.fragment, R.anim.fade_in, R.anim.fade_out, R.anim.fade_in,
                R.anim.fade_out);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        ButterKnife.bind(this);
    }

    public void onFragmentAnimationComplete() {
        // Override if necessary
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //noinspection ConstantConditions
        mFragmentHelper = null;
    }

    @CallSuper
    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        return mIgnoreTouch || super.dispatchTouchEvent(ev);
    }

    public final void setIgnoreTouch(boolean ignoreTouch) {
        mIgnoreTouch = ignoreTouch;
    }

}
