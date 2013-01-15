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

import android.location.Location;

/**
 * Callback interface defines the various events sent by location providers
 * and/or [@link LocationManager}. <br>
 * Caution : All these methods run in the <strong>UI thread</strong> !! This
 * allows the application to update the UI in response to these events, but it
 * also means that no blocking operation should occur inside!
 * <ul>
 * <li>
 * {@link LocationUpdatedListener#onLocationReady(Location)} is called when the
 * very first fix was acquired.<br>
 * The fix may come from either the optimal provider, or the back-off provider
 * if the latter is enabled.<br>
 * </li>
 * <li>
 * {@link LocationUpdatedListener#onLocationUpdated(Location, boolean)} is
 * called for each location update after the first fix.<br>
 * the second boolean parameter indicates whether the given location is optimal
 * based on the requested accuracy {@link LocationConfig#needsBackOff()}.</li>
 * <li>
 * By monitoring the boolean status passed to
 * {@link LocationUpdatedListener#onOptimalStatusChanged(boolean)}, the
 * application can know whether the location updates come from the wanted source
 * or not.<br>
 * Alternatively, the optimal status is passed on each call to
 * {@code onLocationUpdated}.</li>
 * <li>
 * {@link LocationUpdatedListener#onLocationUnavailable()} will be called if no
 * fix can be gotten from any provider (e.g. none are available, or if any kind
 * of timeout occur).</li>
 * <li>
 * {@link LocationUpdatedListener#onLocationRequesting()} will be called before
 * getting first fix.<br>
 * The application may choose to display some kind of feedback to the user, such
 * as an indefinite progress bar at this stage, until {@code onLocationReady} or
 * {@code onLocationUnvailable} gets called.</li>
 * <li>
 * {@link LocationUpdatedListener#onLocationLost(String, int)} will be called if
 * the location provider fix cannot be retrieved anymore.<br>
 * This will be called only once no more provider can replace the lost one.<br>
 * For instance, optimality may be lost ({@code onOptimalStateChanged}) without
 * this method being called if e.g. GPS switched to network. However, if network
 * is also lost and no other provider can take the relay, then this method will
 * eventually be called.</li>
 * <li>
 * {@link LocationUpdatedListener#onTimeout()} will be called only if
 * {@link LocationConfig#getMaxWaitTimeForOptimalFix()} is set to return a non
 * zero positive value.<br/>
 * If no fix could be obtained after the number of milliseconds specified by
 * this method, a timeout will be triggered, and the location service be
 * shutdown. The application can check whether the fix took too long to obtain,
 * separate to other types of unavailable statuses (e.g. provider is off...).</li>
 * </ul>
 * 
 * @author yoann@ymkm.org
 */
public interface LocationUpdatedListener {

	/**
	 * Called after getting the first geolocation fix
	 * 
	 * @param loc
	 *            the location found by the provider
	 */
	public void onLocationReady(Location loc);

	/**
	 * Called whenever the optimality status changes
	 * 
	 * @param isOptimal
	 *            true if location is optimal, false otherwise
	 */
	public void onOptimalStatusChanged(boolean isOptimal);

	/**
	 * Called when provider lost track of the geolocation
	 * 
	 * @param provider
	 *            if not null, provider whom we lost contact with
	 * @param status
	 *            disconnection reason. LocationProvider statuses, available
	 *            through {@link LocationFragment.STATUS_AVAILABLE},
	 *            {@link LocationFragment.STATUS_OUT_OF_SERVICE} and
	 *            {@link LocationFragment.STATUS_TEMPORARILY_UNAVAILABLE}, or
	 *            any other of {@link LocationFragment.STATUS_XXX} if due to
	 *            application status change (pause,stop...).
	 */
	public void onLocationLost(String provider, int status);

	/**
	 * Called if fix could not be retrieved due to a timeout.
	 * 
	 * Timeout occurs only if {@link LocationConfig} sets one through
	 * {@link LocationConfig#doGetMaxWaitTimeForOptimalFix}.
	 * 
	 * The location service will be shutdown at this point, and it is up to the
	 * implementor to reenable it if needed.
	 */
	public void onTimeout();

	/**
	 * Called for each location update after the first fix
	 * 
	 * @param loc
	 *            the updated location
	 * @param isOptimal
	 *            if this update occurred on an optimal provider
	 */
	public void onLocationUpdated(Location loc, boolean isOptimal);

	/**
	 * When no provider is available for getting location
	 */
	public void onLocationUnavailable();

	/**
	 * When waiting for first location fix
	 */
	public void onLocationRequesting();
}
