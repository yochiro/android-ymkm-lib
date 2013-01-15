/*******************************************************************************
 * Copyright 2013 Yoann Mikami <yoann@ymkm.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ymkm.lib.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;

public class BitmapUtils {
	
	public static BitmapFactory.Options getBitmapOptionsSampledToSize(Context context, Uri path, int targetW, int targetH, boolean fitToSmallestDimension) {
		return getBitmapOptionsSampledToSize(context, path, targetW, targetH, fitToSmallestDimension, null);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(Context context, Uri path, int targetW, int targetH, boolean fitToSmallestDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapDimensions(context, path, bitmapOptions);
		return getBitmapOptions(bitmapOptions.outWidth, bitmapOptions.outHeight, targetW, targetH, fitToSmallestDimension, bitmapOptions);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(Context context, int resId, int targetW, int targetH, boolean fitToSmallestDimension) {
		return getBitmapOptionsSampledToSize(context, resId, targetW, targetH, fitToSmallestDimension, null);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(Context context, int resId, int targetW, int targetH, boolean fitToSmallestDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapDimensions(context, resId, bitmapOptions);
		return getBitmapOptions(bitmapOptions.outWidth, bitmapOptions.outHeight, targetW, targetH, fitToSmallestDimension, bitmapOptions);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(String path, int targetW, int targetH, boolean fitToSmallestDimension) {
		return getBitmapOptionsSampledToSize(path, targetW, targetH, fitToSmallestDimension, null);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(String path, int targetW, int targetH, boolean fitToSmallestDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapDimensions(path, bitmapOptions);
		return getBitmapOptions(bitmapOptions.outWidth, bitmapOptions.outHeight, targetW, targetH, fitToSmallestDimension, bitmapOptions);
	}

	public static BitmapFactory.Options getBitmapOptionsSampledToSize(int origW, int origH, int targetW, int targetH, boolean fitToSmallestDimension) {
		return getBitmapOptions(origW, origH, targetW, targetH, fitToSmallestDimension, null);
	}
	
	public static BitmapFactory.Options getBitmapOptionsSampledToSize(int origW, int origH, int targetW, int targetH, boolean fitToSmallestDimension, BitmapFactory.Options bitmapOptions) {
		return getBitmapOptions(origW, origH, targetW, targetH, fitToSmallestDimension, bitmapOptions);
	}

	public static BitmapFactory.Options getBitmapDimensions(Context context, Uri path) {
		return getBitmapDimensions(context, path, null);
	}

	public static BitmapFactory.Options getBitmapDimensions(Context context, Uri path, BitmapFactory.Options bitmapOptions) {
		InputStream is = null;
		try {
			is = context.getContentResolver().openInputStream(path);
			if (null == bitmapOptions) {
				bitmapOptions = new BitmapFactory.Options();
			}
			bitmapOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, bitmapOptions);
			bitmapOptions.inJustDecodeBounds = false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bitmapOptions;
	}
	
	public static BitmapFactory.Options getBitmapDimensions(Context context, int resId) {
		return getBitmapDimensions(context, resId, null);
	}
	
	public static BitmapFactory.Options getBitmapDimensions(Context context, int resId, BitmapFactory.Options bitmapOptions) {
		if (null == bitmapOptions) {
			bitmapOptions = new BitmapFactory.Options();
		}
		bitmapOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(context.getResources(), resId, bitmapOptions);
		bitmapOptions.inJustDecodeBounds = false;
		return bitmapOptions;
	}
	
	public static BitmapFactory.Options getBitmapDimensions(String path) {
		return getBitmapDimensions(path, null);
	}
	
	public static BitmapFactory.Options getBitmapDimensions(String path, BitmapFactory.Options bitmapOptions) {
		if (null == bitmapOptions) {
			bitmapOptions = new BitmapFactory.Options();
		}
		bitmapOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bitmapOptions);
		bitmapOptions.inJustDecodeBounds = false;
		return bitmapOptions;
	}
	
	public static Bitmap createScaledBitmapToSize(Context context, Uri path, int targetW, int targetH, boolean fitToSmallerDimension) {
		return createScaledBitmapToSize(context, path, targetW, targetH, fitToSmallerDimension, null);

	}

	public static Bitmap createScaledBitmapToSize(Context context, Uri path, int targetW, int targetH, boolean fitToSmallerDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapOptionsSampledToSize(context, path, targetW, targetH, fitToSmallerDimension, bitmapOptions);
		InputStream is = null;
		try {
			is = context.getContentResolver().openInputStream(path);
			return BitmapUtils.createScaledBitmapToSize(BitmapFactory.decodeStream(is, null, bitmapOptions), targetW, targetH);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static Bitmap createScaledBitmapToSize(Context context, int resId, int targetW, int targetH, boolean fitToSmallerDimension) {
		return createScaledBitmapToSize(context, resId, targetW, targetH, fitToSmallerDimension, null);

	}

	public static Bitmap createScaledBitmapToSize(Context context, int resId, int targetW, int targetH, boolean fitToSmallerDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapOptionsSampledToSize(context, resId, targetW, targetH, fitToSmallerDimension, bitmapOptions);
		return BitmapUtils.createScaledBitmapToSize(BitmapFactory.decodeResource(context.getResources(), resId, bitmapOptions), targetW, targetH);
	}

	public static Bitmap createScaledBitmapToSize(String path, int targetW, int targetH, boolean fitToSmallerDimension) {
		return createScaledBitmapToSize(path, targetW, targetH, fitToSmallerDimension, null);

	}

	public static Bitmap createScaledBitmapToSize(String path, int targetW, int targetH, boolean fitToSmallerDimension, BitmapFactory.Options bitmapOptions) {
		bitmapOptions = BitmapUtils.getBitmapOptionsSampledToSize(path, targetW, targetH, fitToSmallerDimension, bitmapOptions);
		return BitmapUtils.createScaledBitmapToSize(BitmapFactory.decodeFile(path, bitmapOptions), targetW, targetH);
	}

	public static Bitmap createScaledBitmapToSize(Bitmap in, int targetW, int targetH) {
		return ThumbnailUtils.extractThumbnail(in, targetW, targetH);
	}
	
    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    // Rotates and/or mirrors the bitmap. If a new bitmap is created, the
    // original bitmap is recycled.
    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees != 0 || mirror) && b != null) {
            Matrix m = new Matrix();
            // Mirror first.
            // horizontal flip + rotation = -rotation + horizontal flip
            if (mirror) {
                m.postScale(-1, 1);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate(b.getWidth(), 0);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate(b.getHeight(), 0);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }
            if (degrees != 0) {
                // clockwise
                m.postRotate(degrees,
                        (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            }

            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }


	private static BitmapFactory.Options getBitmapOptions(int srcWidth, int srcHeight, int dstWidth, int dstHeight, boolean fitToSmallerDimension, BitmapFactory.Options bitmapOptions) {
		float scaleF = 0f;
		if (null == bitmapOptions) {
			bitmapOptions = new BitmapFactory.Options();
		}
		if (srcWidth > 0 && srcHeight > 0) {
			if (0 == dstWidth) {
				dstWidth = (int)((((float) srcWidth) / ((float) srcHeight))*dstHeight);
			}
			else if (0 == dstHeight) {
				dstHeight = (int)((((float) srcHeight) / ((float) srcWidth))*dstWidth);
			}
	
			if (srcWidth > dstWidth || srcHeight > dstHeight) {
				if (srcWidth > srcHeight && fitToSmallerDimension) {
					scaleF = ((float) srcWidth)
							/ ((float) dstWidth);
				} else {
					scaleF = ((float) srcHeight)
							/ ((float) dstHeight);
				}
				// Convert to log base 2 : log[b](x) =
				// log[e](x)/log[e](b)
				scaleF = (float) Math.floor((float) (Math.log(scaleF) / Math.log(2f)));
			}
		}
		int scale = (int) Math.pow(2, (int) scaleF);
		bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		bitmapOptions.inSampleSize = scale;
		return bitmapOptions;
	}
}
