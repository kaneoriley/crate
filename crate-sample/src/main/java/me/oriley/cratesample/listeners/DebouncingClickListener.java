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

package me.oriley.cratesample.listeners;

import android.support.annotation.NonNull;
import android.view.View;

public abstract class DebouncingClickListener implements View.OnClickListener {

    private final long mDebounceDelay;

    private long mLastClickTime;

    public DebouncingClickListener(long debounceDelay) {
        mDebounceDelay = Math.max(0, debounceDelay);
    }

    @Override
    public final void onClick(@NonNull View view) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastClickTime > mDebounceDelay) {
            mLastClickTime = currentTime;
            //noinspection unchecked
            performClick(view);
        }
    }

    public abstract void performClick(@NonNull View view);
}
