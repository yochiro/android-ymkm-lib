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

import android.location.Location;
import android.util.Pair;

/**
 * Utility functions related to geolocation
 * 
 * @author yoann@ymkm.org
 */
public final class GeoUtils {
	
	/**
	 * Returns the latitude/longitude of a geopoint located at a given time-distance from specified Location
	 * 
	 * <p>
	 * Given a {@link Location}, this method returns the latitude
	 * and longitude whose distance is {@code secsAhead} away from it.<br>
	 * This uses the Location speed and bearing to calculate the distance.<br>
	 * If speed is not available or is 0, returned coordinates are the same as the input.
	 * </p>
	 * 
	 * @param loc Location from which to return the preemptive coordinates
	 * @param secsAhead how many seconds should the location to return be away from loc
	 * @return the found coordinates Pair&lt;lat,lng&gt;, which can be the same as the input loc if speed is 0
	 */
	public static Pair<Double, Double> getPreeemptiveCoordsFromLoc(Location loc, int secsAhead) {

		double lat = loc.getLatitude();
		double lng = loc.getLongitude();

		lat = Math.toRadians(loc.getLatitude());
		lng = Math.toRadians(loc.getLongitude());
		double bearing = Math.toRadians(loc.getBearing());
		float R = 6371.0f; // Earth radius in kms
		float d = (loc.getSpeed()*secsAhead*60)/1000.0f; // in (meters/sec * sec / 1000) => kms
		float angDist = d/R; // Angular distance

		double lat2 = Math.asin( Math.sin(lat)*Math.cos(angDist) + 
                Math.cos(lat)*Math.sin(angDist)*Math.cos(bearing) );
		double lng2 = lng + Math.atan2(Math.sin(bearing)*Math.sin(angDist)*Math.cos(lat), 
                Math.cos(angDist)-Math.sin(lat)*Math.sin(lat2));

		lat = Math.toDegrees(lat2);
		lng = Math.toDegrees(lng2);

		return new Pair<Double, Double>(lat, lng);
	}
}
