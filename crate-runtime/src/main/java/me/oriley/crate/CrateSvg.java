/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.oriley.crate;

import android.graphics.drawable.PictureDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.InputStream;

@SuppressWarnings("unused")
abstract class CrateSvg {

    private static final String TAG = CrateSvg.class.getSimpleName();
    private static final String CAVEROCK_SVG_PARSER = "com.caverock.androidsvg.SVGParser";

    interface Parser {
        @Nullable
        PictureDrawable parseSvg(@Nullable InputStream stream) throws SvgParseException;
    }

    @NonNull
    static Parser getParser() {
        try {
            Class.forName(CAVEROCK_SVG_PARSER);
            return new CrateSvgParserCaverock();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving SVG parser. Using dummy.", e);
            return new CrateSvgParserDummy();
        }
    }

    public static final class SvgParseException extends Exception {
        public SvgParseException(String msg) {
            super(msg);
        }

        public SvgParseException(String msg, Throwable cause) {
            super(msg, cause);
        }

        public SvgParseException(Throwable cause) {
            super(cause);
        }
    }
}
