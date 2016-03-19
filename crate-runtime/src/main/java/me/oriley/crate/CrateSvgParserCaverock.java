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
import android.support.annotation.Nullable;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.caverock.androidsvg.SVGParser;
import me.oriley.crate.CrateSvg.SvgParseException;

import java.io.IOException;
import java.io.InputStream;

public final class CrateSvgParserCaverock implements CrateSvg.Parser {

    @Override
    @Nullable
    public PictureDrawable parseSvg(@Nullable InputStream stream) throws SvgParseException {
        if (stream == null) {
            return null;
        }

        PictureDrawable drawable = null;
        try {
            //noinspection TryFinallyCanBeTryWithResources
            try {
                SVG svg = new ExposedParser().parse(stream);
                if (svg != null) {
                    drawable = new PictureDrawable(svg.renderToPicture());
                }
            } finally {
                stream.close();
            }
        } catch (IOException | SVGParseException e) {
            throw new SvgParseException(e.getCause());
        }
        return drawable;
    }

    // Subclassing to expose parse method
    private static final class ExposedParser extends SVGParser {
        @Override
        public SVG parse(InputStream is) throws SVGParseException {
            return super.parse(is);
        }
    }
}
