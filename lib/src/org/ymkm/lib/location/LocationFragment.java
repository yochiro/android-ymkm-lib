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

import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import org.ymkm.lib.controller.ControlledFragment;
import org.ymkm.lib.controller.core.ControlledFragmentCallback;

/**
 * Honeycomb and newer version compatible fragment that handles the localization
 * logic.
 * 
 * <p>
 * It tries to follow the geolocalization best practices as described on the
 * official Android developer blog :
 * http://android-developers.blogspot.jp/2011/06/deep-dive-into-location.html
 * </p>
 * <p>
 * It is made into a fragment to ease its integration into an application, as it
 * can sync in with its lifecycle.
 * </p>
 * <p>
 * It enhances the default Location manager, adding things such as back-off in
 * case that GPS (for which getting the first fix can take time) is needed.<br>
 * Most of the process is ran in a separate thread, to avoid cluttering the main
 * thread.
 * </p>
 * <p>
 * Optimal status : this fragment uses the term "Optimal" to define a state
 * where the localization accuracy requested is obtained (e.g. network if only
 * coarse is needed, or GPS is fine location is needed).<br>
 * If a back-off mechanism is used, the application may receive several location
 * updates before "optimality" is attained, if ever.<br>
 * Optimality change can be monitored using the callback provided by
 * {@link LocationUpdatedListener}.
 * </p>
 * <p>
 * Basic Usage :<br>
 * In the Activity that will contain the fragment :
 * 
 * <pre>
 * <code>
 *  ...
 * 	protected void onCreate(Bundle savedInstanceState) {
 * 		super.onCreate(savedInstanceState);
 * 		if (null == savedInstanceState) {
 * 			locConfig = new LocationConfig() {
 * 				...
 * 			};
 * 			FragmentManager fm = getSupportFragmentManager();
 * 			fm.beginTransaction().add(LocationFragment.createInstance(locConfig), "Location").commit();
 * 		}
 * </code>
 * </pre>
 * 
 * </p>
 * <p>
 * {@link LocationConfig} is a configuration class that needs to be provided to
 * the fragment at creation through the static constructor
 * {@code LocationFragment#createInstance(LocationConfig)}.
 * </p>
 * <p>
 * The configuration contains values such as location update refresh rate,
 * accuracy needed, location provider {@link Criteria} etc...
 * </p>
 * Then in the {@code onResume}, the fragment may be started the following way :
 * 
 * <pre>
 * <code>
 * 	protected void onResume() {
 * 		super.onResume();
 * 		((LocationFragment)getSupportFragmentManager().findFragmentByTag("Location")).setLocationUpdatedListener(
 * 				new LocationUpdatedListener() {
 * 					...
 * 				});
 * 	}
 * </code>
 * </pre>
 * <p>
 * The {@link LocationUpdatedListener} is a callback interface that will track
 * all evens related to locations, using the configuration values previously set
 * using {@link LocationConfig}.
 * </p>
 * <p>
 * For more details, refer to the Javadoc of each interface.<br>
 * </p>
 * 
 * @author mikami-yoann
 * 
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LocationFragment extends ControlledFragment {

	public final static int STATUS_OTHER = -3;
	public final static int STATUS_RESTARTING = -2;
	public final static int STATUS_STOPPED = -1;
	public final static int STATUS_OUT_OF_SERVICE = LocationProvider.OUT_OF_SERVICE; // 0x0
	public final static int STATUS_TEMPORARILY_UNAVAILABLE = LocationProvider.TEMPORARILY_UNAVAILABLE; // 0x1
	public final static int STATUS_AVAILABLE = LocationProvider.AVAILABLE; // 0x2
	public final static int STATUS_PROVIDER_DISABLED = 0x100;
	public final static int STATUS_PROVIDER_ENABLED = 0x200;
	public final static int STATUS_FIX_LOST = 0x400;

	private final static String TAG = "LocationFragment";

	/**
	 * Returns a new {@link LocationFragment}, setting it with specified
	 * {@link LocationConfig}.
	 * 
	 * @param config
	 *            the configuration to use
	 * @return a new LocationFragment
	 */
	@TargetApi(11)
	public static LocationFragment createInstance(LocationConfig config) {
		LocationFragment frag = new LocationFragment();
		Bundle b = new Bundle();
		b.putParcelable("config", config);
		b.putBoolean("newThread", true);
		frag.setArguments(b);
		return frag;
	}

	/**
	 * Initializes the fragment
	 * 
	 * {@code savedInstanceState} contains the {@link LocationConfig}
	 * configuration instance. (which is a {@code Parcelable} object)
	 */
	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		this.mConfig = (LocationConfig) b.getParcelable("config");
		if (null != savedInstanceState) {
			this.mConfig = (LocationConfig) savedInstanceState.getParcelable("config");
		}
		assert (null != this.mConfig);
		this.mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		this.mSingleUpdatePI = PendingIntent.getBroadcast(getActivity(), 1, new Intent(SINGLE_LOCATION_UPDATE_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT);
		this.mLocatorStatus = STATUS_LOCATION_UNKNOWN;
		setRetainInstance(true);
	}

	/**
	 * Retrieves the activity looper, to run callbacks on it
	 */
	@TargetApi(11)
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mUIHandler = new Handler(activity.getMainLooper());
	}

	/**
	 * Initializes the running thread
	 */
	@TargetApi(11)
	@Override
	public void onStart() {
		super.onStart();
	}

	/**
	 * Stops the localization process
	 */
	@TargetApi(11)
	@Override
	public void onPause() {
		super.onPause();
		if (getActivity().isChangingConfigurations()) {
			return;
		}
		if (!mConfig.keepRunningWhenPaused()) {
			mLocUpdatedListener = null;
			if (null != mUIHandler) {
				mUIHandler.removeCallbacksAndMessages(null);
				mUIHandler = null;
			}
			// Needs to be ran synchronously
			(new ShutdownRunnable(null, STATUS_STOPPED, true)).run();
		}
	}

	/**
	 * Starts the localization process
	 */
	@TargetApi(11)
	@Override
	public void onResume() {
		super.onResume();
		if (mLocatorStatus == STATUS_LOCATION_UNKNOWN) {
			startupLocalization(null);
		}
	}

	/**
	 * Stops the running thread
	 */
	@TargetApi(11)
	@Override
	public void onStop() {
		super.onStop();
		if (getActivity().isChangingConfigurations()) {
			return;
		}
		if (mConfig.keepRunningWhenPaused()) {
			mLocUpdatedListener = null;
			if (null != mUIHandler) {
				mUIHandler.removeCallbacksAndMessages(null);
				mUIHandler = null;
			}
			// Needs to be ran synchronously
			(new ShutdownRunnable(null, STATUS_STOPPED, true)).run();
		}
	}

	@TargetApi(11)
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@TargetApi(11)
	@Override
	public void onDetach() {
		super.onDetach();
	}

	@TargetApi(11)
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("config", mConfig);
	}

	@Override
	protected String doGetFragmentName() {
		return TAG + " looper";
	}

	@Override
	protected Class<? extends ControlledFragmentCallback> doGetCallbackClass() {
		return null;
	}

	/**
	 * Notifies this location fragment instance that configuration was updated
	 * from outside
	 * 
	 * This will restart the localization process, so the current fix will be
	 * lost.<br>
	 * Therefore, only use this if the configuration change was significant
	 * enough that it requires a restart (e.g. GPS enabled/disabled by user in
	 * the application...)
	 */
	public synchronized void notifyConfigurationUpdated() {
		restartLocalization(null, STATUS_RESTARTING);
	}

	/**
	 * Sets the callback that will catch event updates
	 * 
	 * @param locListener
	 *            the listener to callback
	 */
	public LocationFragment setLocationUpdatedListener(LocationUpdatedListener locListener) {
		this.mLocUpdatedListener = locListener;
		return this;
	}

	/**
	 * Returns the most accurate and timely previously detected location. Where
	 * the last result is beyond the specified maximum distance or latency a
	 * one-off location update is returned via the
	 * {@link LocationUpdatedListener} specified in
	 * {@link setChangedLocationListener}.
	 * 
	 * @param minDistance
	 *            Minimum distance (in m) before we require a location update.
	 * @param minTime
	 *            Minimum time (in ms) required between location updates.
	 * @return The most accurate and / or timely previously detected location.
	 */
	public Location getLastBestLocation(int minDistance, long minTime) {
		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long now = System.currentTimeMillis();
		long bestTime = Long.MAX_VALUE;

		Log.d(TAG, "getLastBestLocation - minDistance = " + minDistance + " - minTime = " + minTime);

		// Iterate through all the providers on the system, keeping
		// note of the most accurate result within the acceptable time limit.
		// If no result is found within maxTime, return the newest Location.
		List<String> matchingProviders = mLocationManager.getAllProviders();
		if (matchingProviders.size() > 0) {
			for (String provider : matchingProviders) {
				Location location = mLocationManager.getLastKnownLocation(provider);
				if (location != null) {
					float accuracy = location.getAccuracy();
					// Time difference bw location and current time
					long time = now - location.getTime();

					if ((time < minTime && accuracy < bestAccuracy)) {
						bestResult = location;
						bestAccuracy = accuracy;
						bestTime = time;
					} else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
						bestResult = location;
						bestTime = time;
					}
				}
			}
		}

		// If the best result is beyond the allowed time limit, or the accuracy
		// of the
		// best result is wider than the acceptable maximum distance, returns
		// null.
		if (bestTime < minTime || bestAccuracy > minDistance) {
			bestResult = null;
		}

		Log.d(TAG, "getLastBestLocation - bestResult = " + bestResult);

		return bestResult;
	}

	@TargetApi(11)
	public LocationFragment() {
	}

	/**
	 * This {@link BroadcastReceiver} listens for a single location update
	 * before unregistering itself. The oneshot location update is returned via
	 * the {@link LocationUpdatedListener} specified in
	 * {@link setChangedLocationListener}.
	 */
	private BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "SingleUpdateReceiver::onReceive - Status = " + mLocatorStatus);

			String key = LocationManager.KEY_LOCATION_CHANGED;
			Location location = (Location) intent.getExtras().get(key);

			if (location != null) {
				if (mConfig.getMaxWaitTimeForOptimalFix() > 0) {
					getHandler().removeCallbacks(_timeoutRunnable);
				}
				mLocatorStatus = STATUS_LOCATION_REQUEST_ENDED;

				getActivity().unregisterReceiver(singleUpdateReceiver);
				if (android.os.Build.VERSION.SDK_INT < 9) {
					mLocationManager.removeUpdates(mSingleUpdatePI);
				}

				startupLocalization(location);
			}
		}
	};

	/**
	 * {@link BroadcastReceiver} for the optimal location provider.
	 */
	private LocationListener mOptimalLocationUpdateListener = new LocationListener() {

		@Override
		public void onLocationChanged(final Location location) {
			if (null != mLocUpdatedListener && null != mUIHandler) {
				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						if (null != mLocUpdatedListener) {
							Log.d(TAG, "-> LocationUpdatedListener.onLocationUpdated");
							mLocUpdatedListener.onLocationUpdated(location, true);
						}
					}
				});
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// Ignore
		}

		@Override
		public void onProviderEnabled(String provider) {
			// Ignore
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Ignore
		}
	};

	/**
	 * {@link BroadcastReceiver} for the backoff (coarse) location provider.
	 */
	private LocationListener mBackOffLocationUpdateListener = new LocationListener() {

		@Override
		public void onLocationChanged(final Location location) {
			if (null != mLocUpdatedListener && null != mUIHandler) {
				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						if (null != mLocUpdatedListener) {
							Log.d(TAG, "-> LocationUpdatedListener.onLocationUpdated");
							mLocUpdatedListener.onLocationUpdated(location, false);
						}
					}
				});
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// Ignore
		}

		@Override
		public void onProviderEnabled(String provider) {
			// Ignore
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Ignore
		}
	};

	/**
	 * {@link BroadcastReceiver} to track location provider status changes
	 */
	private LocationListener mStatusChangeLocationListener = new LocationListener() {

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onLocationChanged(Location location) {
			if (STATUS_LOCATION_OPTIMAL != mLocatorStatus) {
				Log.d(TAG, "Status change listener / onLocationChanged - Status = " + mLocatorStatus);
				mLocatorStatus = STATUS_LOCATION_OPTIMAL;
				if (null != mLocUpdatedListener && null != mUIHandler) {
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (null != mLocUpdatedListener) {
								Log.d(TAG, "-> LocationUpdatedListener.onIsOptimal");
								mLocUpdatedListener.onOptimalStatusChanged(true);
							}
						}
					});
				}
				if (mNeedsBackOff) {
					mLocationManager.removeUpdates(mBackOffLocationUpdateListener);
				}
				if (mConfig.getMaxWaitTimeForOptimalFix() > 0) {
					getHandler().removeCallbacks(_timeoutRunnable);
				}
				// For the case of GPS
				String bestProvider = mLocationManager.getBestProvider(mOptimalCriteria, true);
				if (LocationManager.GPS_PROVIDER.equals(bestProvider) && !mHasGpsFix) {
					mHasGpsFix = true;
					IntentFilter filter = new IntentFilter();
					filter.addAction("android.location.GPS_FIX_CHANGE");
					getActivity().registerReceiver(_gpsFix, filter, null, getHandler());
				}
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d(TAG, "Status change listener / onStatusChanged - New status = " + status);
			restartLocalization(provider, status);
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.d(TAG, "Status change listener / onProviderDisabled");
			restartLocalization(provider, STATUS_PROVIDER_DISABLED);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "Status change listener / onProviderEnabled");
			restartLocalization(provider, STATUS_PROVIDER_ENABLED);
		}
	};

	/**
	 * If the best Location Provider (usually GPS) is not available when we
	 * request location updates, this listener will be notified if / when it
	 * becomes available. It calls requestLocationUpdates to re-register the
	 * location listeners using the better Location Provider.
	 */
	private LocationListener mBestInactiveLocationProviderListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location l) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "BestInactiveLocationProviderListener - Switching to provider " + provider
					+ " as the new optimal provider");

			if (STATUS_LOCATION_OPTIMAL == mLocatorStatus) {
				if (null != mLocUpdatedListener && null != mUIHandler) {
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (null != mLocUpdatedListener) {
								Log.d(TAG, "-> LocationUpdatedListener.onIsOptimal");
								mLocUpdatedListener.onOptimalStatusChanged(false);
							}
						}
					});
				}
			}
			// Re-register the location listeners using the better Location
			// provider.
			restartLocalization(provider, STATUS_PROVIDER_ENABLED);
		}
	};

	@SuppressLint("NewApi")
	private synchronized void requestSingleUpdate() {
		if (STATUS_LOCATION_REQUESTING == mLocatorStatus) {
			return;
		}
		IntentFilter locIntentFilter = new IntentFilter(SINGLE_LOCATION_UPDATE_ACTION);
		getActivity().registerReceiver(singleUpdateReceiver, locIntentFilter, null, getHandler());
		Criteria c = new Criteria();
		c.setAccuracy(LocationConfig.getNetworkStatus(getActivity())
				? Criteria.ACCURACY_COARSE
				: Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		// Use Android specific client if available
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			mLocationManager.requestSingleUpdate(c, mSingleUpdatePI);
		} else {
			// Fallback to a normal broadcastreceiver pattern : will unregister
			// itself on receive
			mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(c, true), 0L, 0.0f,
					mSingleUpdatePI);
		}

		// Set timeout at this point to enable a timeout on the first fix.
		// May happen if e.g. no network
		if (mConfig.getMaxWaitTimeForOptimalFix() > 0) {
			getHandler().postDelayed(_timeoutRunnable, mConfig.getMaxWaitTimeForOptimalFix());
		}
		this.mLocatorStatus = STATUS_LOCATION_REQUESTING;
	}

	private synchronized void cancelSingleUpdate() {
		if (STATUS_LOCATION_REQUESTING == mLocatorStatus) {
			getActivity().unregisterReceiver(singleUpdateReceiver);
			mLocationManager.removeUpdates(mSingleUpdatePI);
			mLocatorStatus = STATUS_LOCATION_UNKNOWN;
		}
	}

	private void startupLocalization(final Location loc) {
		getHandler().post(new StartupRunnable(loc));
	}

	private void restartLocalization(String provider, int reason) {
		getHandler().post(new ShutdownRunnable(null, reason, false));
		getHandler().post(new StartupRunnable(null));
	}

	@SuppressWarnings("unused")
	private void shutdownLocalization(String provider, int reason) {
		getHandler().post(new ShutdownRunnable(provider, reason, true));
	}

	private final Runnable _timeoutRunnable = new Runnable() {

		@Override
		public void run() {
			if (null != mLocUpdatedListener && null != mUIHandler) {
				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "-> LocationUpdatedListener.onTimeout");
						mLocUpdatedListener.onTimeout();
					}
				});
			}
			getHandler().post(new ShutdownRunnable(null, STATUS_OTHER, false));
		}
	};

	private final class ShutdownRunnable implements Runnable {

		private String mProvider;
		private int mReason;
		private boolean mReset;

		public ShutdownRunnable(String provider, int reason, boolean reset) {
			mProvider = provider;
			mReason = reason;
			mReset = reset;
		}

		@TargetApi(11)
		@Override
		public void run() {
			Log.d(TAG, "ShutdownRunnable - Provider = " + (null != mProvider ? mProvider : "N/A") + ", Status = "
					+ mLocatorStatus + " - reason = " + mReason);

			if (mHasGpsFix) {
				mHasGpsFix = false;
				getActivity().unregisterReceiver(_gpsFix);
			}

			getHandler().removeCallbacks(_timeoutRunnable);
			if (STATUS_LOCATION_READY <= mLocatorStatus) {
				mLocationManager.removeUpdates(mOptimalLocationUpdateListener);
				if (mNeedsBackOff) {
					mLocationManager.removeUpdates(mBackOffLocationUpdateListener);
				}
				mLocationManager.removeUpdates(mStatusChangeLocationListener);
				mLocationManager.removeUpdates(mBestInactiveLocationProviderListener);
				if (mReset && null != mLocUpdatedListener && null != mUIHandler && STATUS_STOPPED != mReason) {
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (null != mLocUpdatedListener) {
								Log.d(TAG, "-> LocationUpdatedListener.onLocationLost");
								mLocUpdatedListener.onLocationLost(mProvider, mReason);
							}
						}
					});
				}
			} else if (STATUS_LOCATION_REQUESTING == mLocatorStatus) {
				cancelSingleUpdate();
			} else if (STATUS_LOCATION_UNAVAILABLE == mLocatorStatus) {
				mLocationManager.removeUpdates(mBestInactiveLocationProviderListener);
			}
			if (isRemoving()) {
				if (null != mPassiveUpdatePI) {
					mLocationManager.removeUpdates(mPassiveUpdatePI);
					mPassiveUpdatePI = null;
				}
			}
			if (mReset) {
				mLocatorStatus = STATUS_LOCATION_UNKNOWN;
			} else if (STATUS_LOCATION_READY < mLocatorStatus) {
				mLocatorStatus = STATUS_LOCATION_READY;
				if (null != mLocUpdatedListener && null != mUIHandler && STATUS_STOPPED != mReason) {
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (null != mLocUpdatedListener) {
								Log.d(TAG, "-> LocationUpdatedListener.onIsOptimal");
								mLocUpdatedListener.onOptimalStatusChanged(false);
							}
						}
					});
				}
			}
		}
	};

	private final class StartupRunnable implements Runnable {

		private Location mLoc = null;

		public StartupRunnable(Location loc) {
			mLoc = loc;
		}

		@SuppressLint("NewApi")
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void run() {
			Log.d(TAG, "StartupRunnable - Status = " + mLocatorStatus);

			getHandler().removeCallbacks(_timeoutRunnable);

			mNeedsBackOff = mConfig.needsBackOff();
			mApproxCriteria = getCoarseCriteria(new Criteria());
			mOptimalCriteria = getOptimalCriteria(new Criteria());

			if (!checkProviderAvailability()) {
				String bestProvider = mLocationManager.getBestProvider(mOptimalCriteria, false);
				if (null != mLocUpdatedListener && null != mUIHandler) {
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (null != mLocUpdatedListener) {
								Log.d(TAG, "-> LocationUpdatedListener.onLocationUnavailable");
								mLocUpdatedListener.onLocationUnavailable();
							}
						}
					});
				}
				mLocationManager.requestLocationUpdates(bestProvider, 0, 0, mBestInactiveLocationProviderListener,
						getHandler().getLooper());
				mLocatorStatus = STATUS_LOCATION_UNAVAILABLE;
				return;
			}

			switch (mLocatorStatus) {
				case STATUS_LOCATION_REQUESTING :
					cancelSingleUpdate();
					getHandler().post(new StartupRunnable(mLoc));
					return;
				case STATUS_LOCATION_UNAVAILABLE :
					mLocatorStatus = STATUS_LOCATION_UNKNOWN;
					mLocationManager.removeUpdates(mBestInactiveLocationProviderListener);
					// Keep going
				case STATUS_LOCATION_UNKNOWN :
					if ((null == (mLoc = getLastBestLocation(mConfig.getMaxDistanceSinceLastBestLocation(),
							mConfig.getMaxTimeSinceLastBestLocation())))) {
						Log.d(TAG, "StartupRunnable - No last best location found, requesting coarse single update...");
						requestSingleUpdate();
						if (null != mLocUpdatedListener && null != mUIHandler) {
							mUIHandler.post(new Runnable() {
								@Override
								public void run() {
									if (null != mLocUpdatedListener) {
										Log.d(TAG, "-> LocationUpdatedListener.onLocationRequesting");
										mLocUpdatedListener.onLocationRequesting();
									}
								}
							});
						}
						return;
					}
					Log.d(TAG, "StartupRunnable - Last best location found!");
					// If last best location okay, then go to next status
					// directly
					// below
				case STATUS_LOCATION_REQUEST_ENDED :
					Log.d(TAG, "StartupRunnable - New Status : " + mLocatorStatus + "=>" + STATUS_LOCATION_READY);
					mLocatorStatus = STATUS_LOCATION_READY;
					if (null != mLocUpdatedListener && null != mUIHandler) {
						final Location loc = mLoc;
						mUIHandler.post(new Runnable() {
							@Override
							public void run() {
								if (null != mLocUpdatedListener) {
									Log.d(TAG, "-> LocationUpdatedListener.onLocationReady");
									mLocUpdatedListener.onLocationReady(loc);
								}
							}
						});
					}
					// Keep going, as we switched to READY status
				case STATUS_LOCATION_READY :
					if (mNeedsBackOff) {
						Log.d(TAG, "StartupRunnable - High accuracy requested : register coarse provider fallback");
						mLocationManager.requestLocationUpdates(
								mLocationManager.getBestProvider(mApproxCriteria, true), mConfig.getTimePrecision(),
								mConfig.getDistancePrecision(), mBackOffLocationUpdateListener, getHandler()
										.getLooper());
						// Set timeout at this point if backoff set.
						if (mConfig.getMaxWaitTimeForOptimalFix() > 0) {
							getHandler().postDelayed(_timeoutRunnable, mConfig.getMaxWaitTimeForOptimalFix());
						}
					} else {
						Log.d(TAG, "StartupRunnable - New Status : " + mLocatorStatus + "=>" + STATUS_LOCATION_OPTIMAL);
						mLocatorStatus = STATUS_LOCATION_OPTIMAL;
						if (null != mLocUpdatedListener && null != mUIHandler) {
							mUIHandler.post(new Runnable() {
								@Override
								public void run() {
									if (null != mLocUpdatedListener) {
										Log.d(TAG, "-> LocationUpdatedListener.onIsOptimal");
										mLocUpdatedListener.onOptimalStatusChanged(true);
									}
								}
							});
						}
					}
					// Keep going
				case STATUS_LOCATION_OPTIMAL :
					Log.d(TAG, "StartupRunnable - register status change listener");
					String bestProvider = mLocationManager.getBestProvider(mOptimalCriteria, true);
					mLocationManager.requestLocationUpdates(bestProvider, 0, 0, mStatusChangeLocationListener,
							getHandler().getLooper());
					Log.d(TAG, "StartupRunnable - register optimal provider listener");
					mLocationManager.requestLocationUpdates(bestProvider, mConfig.getTimePrecision(),
							mConfig.getDistancePrecision(), mOptimalLocationUpdateListener, getHandler().getLooper());

					/* Passive location */
					if (null != mPassiveUpdatePI) {
						mLocationManager.removeUpdates(mPassiveUpdatePI);
						mPassiveUpdatePI = null;
					}
					Class<?> passiveReceiver = mConfig.getPassiveLocationUpdateReceiver();
					if (null != passiveReceiver) {
						Log.d(TAG, "StartupRunnable - register passive provider listener");
						Intent passiveIntent = new Intent(getActivity(), passiveReceiver);
						mPassiveUpdatePI = PendingIntent.getBroadcast(getActivity(), 2, passiveIntent,
								PendingIntent.FLAG_UPDATE_CURRENT);
						mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
								mConfig.getTimePrecision(), mConfig.getDistancePrecision(), mPassiveUpdatePI);
						// TODO register passive location update
						// broadcastreceiver
					}

					// Listen for a better provider becoming available.
					bestProvider = mLocationManager.getBestProvider(mOptimalCriteria, false);
					String bestAvailableProvider = mLocationManager.getBestProvider(mOptimalCriteria, true);
					if (bestProvider != null && !bestProvider.equals(bestAvailableProvider)) {
						mLocationManager.requestLocationUpdates(bestProvider, 0, 0,
								mBestInactiveLocationProviderListener, getHandler().getLooper());
					}
					break;
			}
		}
	};

	private final BroadcastReceiver _gpsFix = new BroadcastReceiver() {
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isEnabled = intent.getBooleanExtra("enabled", true);
			if (!isEnabled) {
				Log.d(TAG, "_gpsFix BroadcastReceiver::onReceive - STATUS_FIX_LOST");
				getActivity().unregisterReceiver(_gpsFix);
				mHasGpsFix = false;
				restartLocalization(LocationManager.GPS_PROVIDER, STATUS_FIX_LOST);
			}
		}
	};

	private boolean checkProviderAvailability() {
		if (null == mLocationManager.getBestProvider(mOptimalCriteria, true)) {
			Log.d(TAG, "StartupRunnabe - No providers currently enabled : wait until one becomes active!");
			return false;
		}
		return true;
	}

	private Criteria getCoarseCriteria(Criteria criteria) {
		mConfig.setBackOffCriteria(criteria);
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		return criteria;
	}

	/**
	 * If accuracy isnt set by the callback, a default value will be set here
	 * depending on {@code needsBackOff} value : if it returns true, the
	 * Criteria will be set with accuracy ACCURACY_FINE.<br>
	 * Otherwise, accuracy will be ACCURACY_COARSE.
	 */
	private Criteria getOptimalCriteria(Criteria criteria) {
		mConfig.setOptimalCriteria(criteria);
		int acc = criteria.getAccuracy();
		if (mNeedsBackOff && Criteria.ACCURACY_FINE != acc) {
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
		}
		return criteria;
	}

	private final static String SINGLE_LOCATION_UPDATE_ACTION = "org.ymkm.lib.location.SINGLE_LOCATION_UPDATED";

	private LocationManager mLocationManager;

	private final static int STATUS_LOCATION_UNAVAILABLE = -1;
	private final static int STATUS_LOCATION_UNKNOWN = 0;
	private final static int STATUS_LOCATION_REQUESTING = 1;
	private final static int STATUS_LOCATION_REQUEST_ENDED = 2;
	private final static int STATUS_LOCATION_READY = 4;
	private final static int STATUS_LOCATION_OPTIMAL = 8;

	private PendingIntent mSingleUpdatePI;
	private PendingIntent mPassiveUpdatePI;
	private Criteria mOptimalCriteria;
	private Criteria mApproxCriteria;
	private boolean mNeedsBackOff;
	private LocationUpdatedListener mLocUpdatedListener;
	private LocationConfig mConfig;
	private int mLocatorStatus;
	private Handler mUIHandler;
	private boolean mHasGpsFix = false;
}
