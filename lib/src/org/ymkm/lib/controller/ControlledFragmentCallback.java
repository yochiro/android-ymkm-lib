package org.ymkm.lib.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.ymkm.lib.controller.core.ControllableFragmentCallbackAbstract;
import org.ymkm.lib.controller.core.ControllableFragmentException;
import org.ymkm.lib.controller.core.ControllableFragmentInterface;

public abstract class ControlledFragmentCallback extends ControllableFragmentCallbackAbstract {

	/**
	 * Creates a new {@link ControlledFragmentCallback} instance, initialized with given parameters
	 * 
	 * @param clazz
	 *            Class of the callback instance to create
	 * @param fragment
	 *            the {@linkplain ControllableFragment} fragment the new callback gets assigned to * @return
	 * @return {@link ControlledFragmentCallback} if instantiation and initialization succeeded
	 * @throws ControllableFragmentException
	 *             if instantiation fails
	 */
	public static ControlledFragmentCallback createCallback(Class<? extends ControlledFragmentCallback> clazz,
			ControllableFragment fragment) throws ControllableFragmentException {
		ControlledFragmentCallback callback = null;
		try {
			Constructor<? extends ControlledFragmentCallback> cs = clazz.getConstructor(fragment.getClass());
			callback = cs.newInstance(fragment);
			callback.init(fragment);
		}
		catch (InstantiationException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
		catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
		return callback;
	}

	@Override
	protected final void doInit(ControllableFragmentInterface controllableFragment) {
		super.doInit(controllableFragment);
	}

	@Override
	public final ControlledFragmentCallback sendToUi(Runnable runnable) {
		return (ControlledFragmentCallback) super.sendToUi(runnable);
	}

	protected void doInit(ControllableFragment controllableFragment) {
		doInit((ControllableFragmentInterface) controllableFragment);
	}
}
