/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tct.gallery3d.filtershow.filters;

import android.graphics.Bitmap;

import com.tct.gallery3d.R;
import com.tct.gallery3d.filtershow.tools.AutoEditHelper;

public class ImageFilterContrast extends SimpleImageFilter {
    public static final String SERIALIZATION_NAME = "CONTRAST";
    private static final float AUTO_EDIT_CONTRAST_OFFSET = 10.0f;

    public ImageFilterContrast() {
        mName = "Contrast";
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation =
                (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Contrast");
        representation.setSerializationName(SERIALIZATION_NAME);

        representation.setFilterClass(ImageFilterContrast.class);
        representation.setTextId(R.string.contrast);
        representation.setMinimum(-100);
        representation.setMaximum(100);
        representation.setDefaultValue(0);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, float strength);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float value = getParameters().getValue();
        if (AutoEditHelper.AUTO_EDIT_ON) {
            value = value + AUTO_EDIT_CONTRAST_OFFSET;
        }
        nativeApplyFilter(bitmap, w, h, value);
        return bitmap;
    }
}
