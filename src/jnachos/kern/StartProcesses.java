package jnachos.kern;

import jnachos.filesystem.OpenFile;
import jnachos.machine.Machine;

public class StartProcesses implements VoidFunctionPtr{
	/**
	 * The body for processes to run
	 * 
	 * @param pArg is a String filename.
	 */
	@Override
	public void call(Object pArg) {
		String filename = (String)pArg;
		
		// The executable file to run
		OpenFile executable = JNachos.mFileSystem.open(filename);

		// If the file does not exist
		if (executable == null) {
			Debug.print('t', "Unable to open file" + filename);
			System.out.println("Unable to open file" + filename);
			return;
		}

		// Load the file into the memory space
		AddrSpace space = new AddrSpace(executable);
		JNachos.getCurrentProcess().setSpace(space);

		// set the initial register values
		space.initRegisters();

		// load page table register
		space.restoreState();

		// jump to the user program
		// machine->Run never returns;
		Machine.run();

		// the address space exits
		// by doing the syscall "exit"
		assert (false);

	}
	
}
