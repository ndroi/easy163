package org.ndroi.easy163.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.TypedValue;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import org.ndroi.easy163.R;

public class UiUtils {
  public static Drawable createItemShapeDrawableMd2(Context context, ColorStateList fillColor) {
    MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(
        ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Google_Navigation, 0).build()
    );
    materialShapeDrawable.setFillColor(fillColor);
    int insetRight = dp2px(8);
    return new InsetDrawable(materialShapeDrawable, 0, 0, insetRight, 0);
  }

  public static int dp2px(int dp) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
  }
}
