package com.lovejoy777.rroandlayersmanager.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.support.v7.graphics.Palette;

import com.bitsyko.libicons.IconPackHelper;

public class IconUtils {

    private static final Canvas sCanvas = new Canvas();
    private static final Rect sOldBounds = new Rect();

    public static Bitmap createIconBitmap(Drawable icon, Context context,
                                          IconPackHelper iconPack) {
        synchronized (sCanvas) {
            final int iconBitmapSize = icon.getMinimumWidth();

            Drawable iconMask = null;
            Drawable iconBack = null;
            Drawable iconPaletteBack = null;
            Drawable iconUpon = null;
            float scale = 1f;
            float angle = 0;
            float translationX = 0;
            float translationY = 0;
            int defaultSwatchColor = 0;
            int backTintColor = 0;
            IconPackHelper.SwatchType swatchType = IconPackHelper.SwatchType.None;
            float[] colorFilter = null;

            if (iconPack != null) {
                iconMask = iconPack.getIconMask();
                iconBack = iconPack.getIconBack();
                iconPaletteBack = iconPack.getIconPaletteBack();
                iconUpon = iconPack.getIconUpon();
                scale = iconPack.getIconScale();
                angle = iconPack.getIconAngle();
                translationX = iconPack.getTranslationX();
                translationY = iconPack.getTranslationY();
                swatchType = iconPack.getSwatchType();
                colorFilter = iconPack.getColorFilter();
            }

            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;

            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth - width) / 2;
            final int top = (textureHeight - height) / 2;

            if (swatchType != null && swatchType != IconPackHelper.SwatchType.None) {
                Palette palette = Palette.from(bitmap).generate();
                switch (swatchType) {
                    case Vibrant:
                        backTintColor = palette.getVibrantColor(defaultSwatchColor);
                        break;
                    case VibrantLight:
                        backTintColor = palette.getLightVibrantColor(defaultSwatchColor);
                        break;
                    case VibrantDark:
                        backTintColor = palette.getDarkVibrantColor(defaultSwatchColor);
                        break;
                    case Muted:
                        backTintColor = palette.getMutedColor(defaultSwatchColor);
                        break;
                    case MutedLight:
                        backTintColor = palette.getLightMutedColor(defaultSwatchColor);
                        break;
                    case MutedDark:
                        backTintColor = palette.getDarkMutedColor(defaultSwatchColor);
                        break;
                }
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(0, 0, width, height);
            canvas.save();
            final float halfWidth = width / 2f;
            final float halfHeight = height / 2f;
            canvas.rotate(angle, halfWidth, halfHeight);
            canvas.scale(scale, scale, halfWidth, halfHeight);
            canvas.translate(translationX, translationY);
            if (colorFilter != null) {
                Paint p = null;
                if (icon instanceof BitmapDrawable) {
                    p = ((BitmapDrawable) icon).getPaint();
                } else if (icon instanceof PaintDrawable) {
                    p = ((PaintDrawable) icon).getPaint();
                }
                if (p != null) {
                    p.setColorFilter(new ColorMatrixColorFilter(colorFilter));
                }
            }
            icon.draw(canvas);
            canvas.restore();
            if (iconMask != null) {
                iconMask.setBounds(icon.getBounds());
                ((BitmapDrawable) iconMask).getPaint().setXfermode(
                        new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                iconMask.draw(canvas);
            }
            Drawable back = null;
            if (swatchType != null && swatchType != IconPackHelper.SwatchType.None) {
                back = iconPaletteBack;
                defaultSwatchColor = iconPack.getDefaultSwatchColor();
            } else if (iconBack != null) {
                back = iconBack;
            }
            if (back != null) {
                canvas.setBitmap(null);
                Bitmap finalBitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                        Bitmap.Config.ARGB_8888);
                canvas.setBitmap(finalBitmap);
                back.setBounds(icon.getBounds());
                Paint paint = ((BitmapDrawable) back).getPaint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                if (backTintColor != 0) {
                    paint.setColorFilter(new PorterDuffColorFilter(backTintColor,
                            PorterDuff.Mode.MULTIPLY));
                }
                back.draw(canvas);
                canvas.drawBitmap(bitmap, null, icon.getBounds(), null);
                bitmap.recycle();
                bitmap = finalBitmap.copy(Bitmap.Config.ARGB_8888,true);
                finalBitmap.recycle();
            }
            if (iconUpon != null) {
                iconUpon.draw(canvas);
            }
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }
}
