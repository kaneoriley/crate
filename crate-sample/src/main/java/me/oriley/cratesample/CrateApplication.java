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

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import me.oriley.crate.Crate;

public class CrateApplication extends Application {

    @NonNull
    private static CrateApplication sInstance;

    private Crate mCrate;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = this;
    }

    @NonNull
    public static Crate getCrate() {
        if (sInstance.mCrate == null) {
            sInstance.mCrate = new Crate(sInstance);
        }
        return sInstance.mCrate;
    }
}
