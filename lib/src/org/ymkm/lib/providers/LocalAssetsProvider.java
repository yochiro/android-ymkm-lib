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

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

/**
 * Defines a provider which maps {@code <CONTENT_URI>} Uri to local asset files
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
 * The asset file Uri is prefixed by {@code CONTENT_URI} : e.g. for a file in
 * assets/my/img/img.png :<br>
 * {@code content://org.ymkm.lib.local.assets.provider/my/img/img.png}
 * </p>
 * 
 * @author Yoann Mikami
 * 
 */
public class LocalAssetsProvider extends ContentProvider {

	private static final String CLASS_NAME = "LocalAssetsProvider";

	// The authority is the symbolic name for the provider class
	// Assets folder
	public static final Uri CONTENT_URI = Uri
			.parse("content://org.ymkm.lib.local.assets.provider");

	// AssetsManager to access files in assets folder
	private AssetManager mAssetManager;

	@Override
	public boolean onCreate() {
		mAssetManager = getContext().getAssets();
		return true;
	}

	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {

		String LOG_TAG = CLASS_NAME + " - openFile";

		Log.v(LOG_TAG, "Called with uri: '" + uri);

		String path = uri.getPath().substring(1);
		try {
			AssetFileDescriptor afd = mAssetManager.openFd(path);
			return afd;
		} catch (IOException e) {
			throw new FileNotFoundException("No asset found: " + uri);
		}
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
