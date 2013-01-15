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

import java.util.Collection;

public class CollectionUtils {

	public interface Predicate<T> {
		boolean apply(T type);
	}

	public static <T> T search(Collection<T> in, Predicate<T> predicate) {
		T out = null;
		if (null != in && in.size() > 0) {
			for (T el : in) {
				if (predicate.apply(el)) {
					out = el;
					break;
				}
			}
		}
		return out;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> filter(Collection<T> in, Predicate<T> predicate) {
		Collection<T> out;
		try {
			out = in.getClass().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		if (null != in && in.size() > 0) {
			for (T el : in) {
				if (predicate.apply(el)) {
					out.add(el);
				}
			}
		}
		return out;
	}
}
