package org.ymkm.lib.controller.support;

import org.ymkm.lib.controller.core.ControllableFragmentInterface;

public interface ControllableFragment extends ControllableFragmentInterface {

	ControllableFragment sendToUi(Runnable runnable);
}
