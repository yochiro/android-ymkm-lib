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

package org.ymkm.lib.providers;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * Defines a provider which maps {@code <CONTENT_URI>} Uri to cache files
 * 
 * <p>
 * Though {@code content://} scheme is defined in Android, built-in objects
 * accepting Uri as parameters such as {@link ImageView} or {@link MediaPlayer}
 * do not seem to accept it.<br>
 * This simple content provider enables the scheme in
 * {@link ImageView#setImageUri} or {@link MediaPlayer.setUri}.<br>
 * This requires the application to define this Provider in the Manifest file
 * </p>
 * <p>
 * The asset file Uri is prefixed by {@code CONTENT_URI} : e.g. for a cached
 * file my/img/img.png :<br>
 * {@code content://org.ymkm.lib.local.cache.provider/my/img/img.png}
 * </p>
 */
public class LocalCacheProvider extends ContentProvider {

	private static final String CLASS_NAME = "LocalCacheProvider";

	// This URI is the symbolic name for the provider class
	// Cache folder
	public static final Uri CONTENT_URI = Uri
			.parse("content://org.ymkm.lib.local.cache.provider");

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {

		Log.v(CLASS_NAME, "Called with uri: '" + uri);

		String fileLocation = getContext().getCacheDir() + File.separator
				+ uri.getPath().substring(1);

		// Force read-only mode
		ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(
				fileLocation), ParcelFileDescriptor.MODE_READ_ONLY);
		return pfd;
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s,
			String[] as) {
		return 0;
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		return null;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String s, String[] as1,
			String s1) {
		return null;
	}
}
