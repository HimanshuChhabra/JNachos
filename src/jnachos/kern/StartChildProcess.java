package jnachos.kern;

import jnachos.machine.Machine;

public class StartChildProcess implements VoidFunctionPtr {

	/**
	 * The body for processes to run
	 * 
	 * @param pArg is a String filename.
	 */
	@Override
	public void call(Object pArg) {

		JNachos.getCurrentProcess().restoreUserState();
		JNachos.getCurrentProcess().getSpace().restoreState();

		Machine.run();

		assert (false);

	}

}
