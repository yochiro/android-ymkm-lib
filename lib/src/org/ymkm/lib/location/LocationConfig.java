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

package org.ymkm.lib.location;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

import org.ymkm.lib.location.support.LocationFragment;

/**
 * Configuration class for {@link LocatorFragment}.
 * 
 * <p>
 * It implements Parcelable, to make it storable inside {@link Bundle}
 * instances, though this abstract class itself is stateless.
 * </p>
 * <ul>
 * <li>
 * {@link getMaxTimeSinceLastBestLocation()}, {@link
 * getMaxDistanceSinceLastBestLocation()} return in milliseconds (resp. meters)
 * the maximum time (resp. distance) that a cached location should be allowed to
 * be used as a first fix.<br>
 * Each location provider stores the last fix they were triggered with that can
 * be retrieved programmatically. If the Location is within the time/distance
 * range returned by these two methods, then it will be used directly as a first
 * fix, thus triggering {@code onLocationReady} immediately. Otherwise a single
 * request location update is performed.</li>
 * <li>
 * {@link getTimePrecision()}, {@link getDistancePrecision()} return in
 * milliseconds (resp. meters) the frequency at which the location updates
 * should occur, as a minimal threshold for resp. time and distance.<br>
 * These values get fed to the optimal location provider.</li>
 * <li>
 * {@link getMaxWaitTimeForOptimalFix()} allows to set a time limit to get the optimal fix.
 * Especially useful for GPS, which can take up to a minute to get a fix, or can run indefinitely
 * if not in an area where the GPS signal is available. If 0, then no time limit it set, and it
 * is up to the programmer to define the behavior to adopt when the fix takes too long.</li>
 * <li>
 * {@link needsBackOff()} may return true if a backoff is needed. This is mostly
 * the case when GPS provider is used. However, back-off is not mandatory even
 * when dealing with GPS : {@link needsBackOff()} may return false while setting
 * the provider accuracy to {@code Criteria.ACCURACY_FINE} in {@link
 * setOptimalCriteria(Criteria)} to allow the GPS to run without back-off, but
 * this is *highly* disrecommended.</li>
 * <li>
 * {@link setBackOffCriteria(Criteria criteria)} is called only when {@link
 * needsBackOff()} returns true.<br>
 * This allows the implementor to set the criteria it needs for the backoff
 * location provider.<br>
 * The provider accuracy should probably be set to
 * {@code Criteria.ACCURACY_COARSE}, though it is set automatically by the
 * {@link LocationFragment}.</li>
 * <li>
 * {@link setOptimalCriteria(Criteria criteria)} is called to set the criteria
 * the implementor needs for the optimal location provider. If it sets the
 * accuracy, it will be used as is; however, if left blank,
 * {@link LocationFragment} will use either the coarse of the fine accuracy
 * depending on whether {@link needsBackOff()} returns true or false.</li>
 * <li>{@link keepRunningWhenPaused()} returns a boolean that specifies whether
 * the location service should keep running even when the fragment is paused.
 * If that is the case, the service shutdown will be delayed to the onStop method.</li>
 * </ul>
 * 
 * @author yoann@ymkm.org
 * 
 */
public abstract class LocationConfig implements Parcelable {

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Helper to get the system GPS status
	 * 
	 * @param context the context, needed to perform query
	 * @return true if GPS is enabled, false otherwise
	 */
	public static boolean getGpsStatus(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(
				contentResolver, LocationManager.GPS_PROVIDER);
		return gpsStatus;
	}

	/**
	 * Helper to get the system network status
	 * 
	 * @param context the context, needed to perform query
	 * @return true if network is enabled, false otherwise
	 */
	public static boolean getNetworkStatus(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		boolean netStatus = Settings.Secure.isLocationProviderEnabled(
				contentResolver, LocationManager.NETWORK_PROVIDER);
	    ConnectivityManager connectivityManager 
        						= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return netStatus && activeNetworkInfo != null;
	}

	/**
	 * Sets the GPS status by launching the appropriate system settings
	 * 
	 * @param context
	 */
	public static void setGpsStatus(Context context) {
		Intent i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		context.startActivity(i);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Nothing to do, unless the subclass wants to do sth special
	}

	/**
	 * In milliseconds, the max time for which a previously saved location on a provider is considered good enough.
	 * 
	 * @return number of milliseconds
	 */
	public final long getMaxTimeSinceLastBestLocation() {
		return doGetMaxTimeSinceLastBestLocation();
	}

	/**
	 * In meters, the max distance for which a previously saved location on a provider is considered good enough.
	 * 
	 * @return number of meters
	 */
	public final int getMaxDistanceSinceLastBestLocation() {
		return doGetMaxDistanceSinceLastBestLocation();
	}

	/**
	 * In milliseconds, minimum time between two consecutive updates of the location provider
	 * 
	 * @return number of milliseconds
	 */
	public final long getTimePrecision() {
		return doGetTimePrecision();
	}

	/**
	 * In meters, minimum distance between two consecutive updates of the location provider
	 * 
	 * @return number of meters
	 */
	public final int getDistancePrecision() {
		return doGetDistancePrecision();
	}

	/**
	 * In milliseconds, the time to wait until we get a GPS Fix. 0 for no limit
	 * 
	 * If the number of ms specified has elapsed without getting the first fix,
	 * the onLocationUnavailable will be triggered.
	 * 
	 * Default is 0 (no limit).
	 * 
	 * @return the number of ms to wait until we trigger a LocationUnavailable event, or 0 for no time limit
	 */
	public final long getMaxWaitTimeForOptimalFix() {
		return doGetMaxWaitTimeForOptimalFix();
	}

	public final Class<?> getPassiveLocationUpdateReceiver() {
		return doGetPassiveLocationUpdateReceiver();
	}

	/**
	 * Called everytime a backoff criteria is needed
	 * <p>
	 * Only needed if {@code needsHighAccuracy} returns true.
	 * </p>
	 * <p>
	 * Setup may change depending on current application status
	 * </p>
	 * 
	 * @param criteria
	 */
	public final void setBackOffCriteria(Criteria criteria) {
		doSetBackOffCriteria(criteria);
	}

	/**
	 * Called to setup the optimal criteria the application needs
	 * 
	 * <p>
	 * Setup may change depending on current application status
	 * </p>
	 * 
	 * @param criteria
	 */
	public final void setOptimalCriteria(Criteria criteria) {
		doSetOptimalCriteria(criteria);
	}

	/**
	 * Returns true if back-off mechanism is needed (usually because GPS or
	 * similar high-accuracy positioning is needed)
	 * 
	 * Unless overridden, returns false.
	 * 
	 * @return true if back-off is needed, false otherwise
	 */
	public final boolean needsBackOff() {
		return doNeedsBackOff();
	}

	/**
	 * Should the GPS keep running even when the screen is turned off ?
	 * 
	 * Default if false.
	 * 
	 * @return true to keep it running when paused, false to shutdown GPS on pause
	 */
	public final boolean keepRunningWhenPaused() {
		return doKeepRunningWhenPaused();
	}
	
	protected abstract long doGetMaxTimeSinceLastBestLocation();

	protected abstract int doGetMaxDistanceSinceLastBestLocation();

	protected abstract long doGetTimePrecision();

	protected abstract int doGetDistancePrecision();

	protected long doGetMaxWaitTimeForOptimalFix() {
		return 0L;
	}
	
	protected Class<?> doGetPassiveLocationUpdateReceiver() {
		return null;
	}

	protected void doSetBackOffCriteria(Criteria criteria) {
	}

	protected boolean doNeedsBackOff() {
		return false;
	}

	protected void doSetOptimalCriteria(Criteria criteria) {
	}

	protected boolean doKeepRunningWhenPaused() {
		return false;
	}
}
