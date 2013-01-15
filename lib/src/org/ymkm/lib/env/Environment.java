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

package org.ymkm.lib.env;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

/**
 * Environment allows to define a list of target environment, such as dev,
 * staging and prod.
 * <p>
 * Each environment often yields different configuration values regarding remote
 * server URL, settings...<br>
 * This class helps maintain arbitrary sets of config values per environment,
 * and retrieves the specified value depending on current environment.
 * </p>
 * <p>
 * A new Environment instance can only be created through the static method
 * {@link Environment#get}.<br>
 * <p>
 * The application must define two values, which relates to the two parameters
 * passed to {@link Environment#get} :
 * <ul>
 * <li>{@code envKey} : references a metadata key defined in
 * {@code AndroidManifest.xml}.
 * 
 * <pre>
 *      &lt;!-- dev, stg, prod --&gt;
 *         &lt;meta-data
 *             android:name="deploy"
 *             android:value="stg" /&gt;
 * </pre>
 * 
 * For the above example, envKey is defined to be {@code deploy} and its current
 * value is set to {@code stg}.<br>
 * When building the application, target environment can be set accordingly by
 * modifying the value of {@code deploy}. <br>
 * The list of possible values it can take must be defined as a String array
 * resource.</li>
 * <li>{@code envStringArrayId} : references a string array ID defined in
 * res/values/arrays.xml :
 * 
 * <pre>
 *    ...
 *    &lt;string-array name="envs"&gt;
 *      &lt;item&gt;dev&lt;/item&gt;
 *      &lt;item&gt;stg&lt;/item&gt;
 *      &lt;item&gt;prod&lt;/item&gt;
 *    &lt;/string-array&gt;
 * </pre>
 * 
 * The above sample defines three environments, namely {@code dev}, {@code stg}
 * and {@code prod}. <br>
 * The string array ID {@code envStringArrayId} defines will thus be
 * {@code R.array.envs}.
 * 
 * </li>
 * </ul>
 * </p>
 * <p>
 * All environment-specific variables may be set similarily as string arrays,
 * with values defined in the same order as the {@code envStringArrayId} (ie.
 * following the example above, all string arrays must have 3 items defining
 * dev, stg and prod values resp. in this order).<br>
 * Example :
 * 
 * <pre>
 *    ...
 *    &lt;string-array name="server_url"&gt;
 *      &lt;item&gt;http://dev.env.com&lt;/item&gt;
 *      &lt;item&gt;http://stg-server.com&lt;/item&gt;
 *      &lt;item&gt;https://my-app.com&lt;/item&gt;
 *    &lt;/string-array&gt;
 * </pre>
 * 
 * The above configuration value can be retrieved using
 * {@link Environment#getEnvParam(int resId)}.
 * 
 * <pre>
 * Environment e = Environment.get(getContext(), &quot;deploy&quot;, R.array.envs);
 * String serverUrl = e.getEnvParam(R.array.server_url);
 * </pre>
 * 
 * Manifest's defined environment can be retrieved using
 * {@link Environment#getEnv()}.
 * </p>
 * 
 * @author yoann@ymkm.org
 */
public final class Environment {

	/**
	 * Returns an {@code Environment} instance
	 * <p>
	 * Environment needs a {@link Context} to retrieve the {@code envKey}
	 * metadata that must be defined in the {@code AndroidManifest.xml} file.
	 * </p>
	 * <p>
	 * Moreover, a String array must be defined with the set of possible
	 * environment values an environment can take. The StringArray ID thus must
	 * also be specified.
	 * </p>
	 * 
	 * @param ctxt
	 *            a {@link Context} to query the manifest's metadata from
	 * @param envKey
	 *            the metadata key to look up in the manifest file
	 * @param envStringArrayId
	 *            the StringArray Id defining the set of possible environments
	 * @return
	 */
	public static Environment get(Context ctxt, String envKey,
			int envStringArrayId) {
		return new Environment(ctxt, envKey, envStringArrayId);
	}

	/**
	 * Returns the environment value set in the Manifest
	 * 
	 * @return the environment value
	 */
	public String getEnv() {
		String value = null;
		try {
			ApplicationInfo ai;

			ai = mContext
					.get()
					.getPackageManager()
					.getApplicationInfo(mContext.get().getPackageName(),
							PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			value = (String) bundle.get(mEnvKey);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return value;
	}

	/**
	 * Returns the environment-specific String-array value using Manifest's
	 * defined environment as its index
	 * 
	 * @param resId
	 *            the StringArray ID for which to retrieve the configuration
	 *            value
	 * @return the configuration value for current environment value, or null if
	 *         not found
	 */
	public String getEnvParam(int resId) {
		String[] envs = mContext.get().getResources().getStringArray(resId);
		if (null != envs) {
			return getEnvParam(envs);
		}
		return null;
	}

	/**
	 * Returns the environment-specific String-array value using specified
	 * environment as its index
	 * 
	 * @param env
	 *            the environment to lookup
	 * @param resId
	 *            the StringArray ID for which to retrieve the configuration
	 *            value
	 * @return the configuration value for current environment value, or null if
	 *         not found
	 */
	public String getEnvParam(String env, int resId) {
		String[] envs = mContext.get().getResources().getStringArray(resId);
		if (null != envs) {
			return getEnvParam(env, envs);
		}
		return null;
	}


	private String getEnvParam(String[] values) {
		return getEnvParam(getEnv(), values);
	}

	private String getEnvParam(String env, String[] values) {
		String retVal = null;
		synchronized(this) {
			if (-1 == mEnvIdx) {
				String[] envs = mContext.get().getResources()
						.getStringArray(mEnvStringArrayId);
				for (int i = 0; i < envs.length; ++i) {
					String e = envs[i];
					if (env.equals(e)) {
						mEnvIdx = i;
						break;
					}
				}
			}
		}
		if (-1 != mEnvIdx && mEnvIdx < values.length) {
			retVal = values[mEnvIdx];
		}
		return retVal;
	}

	private int mEnvIdx = -1;
	private int mEnvStringArrayId = 0;
	private String mEnvKey = null;
	private WeakReference<Context> mContext = null;

	private Environment(Context ctxt, String envKey, int envStringArrayId) {
		mContext = new WeakReference<Context>(ctxt);
		this.mEnvKey = envKey;
		this.mEnvStringArrayId = envStringArrayId;
	}
}
