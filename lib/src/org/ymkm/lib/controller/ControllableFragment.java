package org.ymkm.lib.controller;

import org.ymkm.lib.controller.core.ControllableFragmentInterface;

public interface ControllableFragment extends ControllableFragmentInterface {

	ControllableFragment sendToUi(Runnable runnable);

}
