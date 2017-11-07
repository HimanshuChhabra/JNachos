/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import java.util.*;

import jnachos.filesystem.OpenFile;
import jnachos.machine.*;

/** The class handles System calls made from user programs. */
public class SystemCallHandler {
	/** The System call index for halting. */
	public static final int SC_Halt = 0;

	/** The System call index for exiting a program. */
	public static final int SC_Exit = 1;

	/** The System call index for executing program. */
	public static final int SC_Exec = 2;

	/** The System call index for joining with a process. */
	public static final int SC_Join = 3;

	/** The System call index for creating a file. */
	public static final int SC_Create = 4;

	/** The System call index for opening a file. */
	public static final int SC_Open = 5;

	/** The System call index for reading a file. */
	public static final int SC_Read = 6;

	/** The System call index for writting a file. */
	public static final int SC_Write = 7;

	/** The System call index for closing a file. */
	public static final int SC_Close = 8;

	/** The System call index for forking a forking a new process. */
	public static final int SC_Fork = 9;

	/** The System call index for yielding a program. */
	public static final int SC_Yield = 10;
	
	/** Map of waiting processes */
	public static HashMap<Integer, List<Integer>> waitMap  = new HashMap<Integer, List<Integer>>();
	//private static HashMap<String,>
	
	/**
	 * Entry point into the Nachos kernel. Called when a user program is
	 * executing, and either does a syscall, or generates an addressing or
	 * arithmetic exception.
	 * 
	 * For system calls, the following is the calling convention:
	 * 
	 * system call code -- r2 arg1 -- r4 arg2 -- r5 arg3 -- r6 arg4 -- r7
	 * 
	 * The result of the system call, if any, must be put back into r2.
	 * 
	 * And don't forget to increment the pc before returning. (Or else you'll
	 * loop making the same system call forever!
	 * 
	 * @pWhich is the kind of exception. The list of possible exceptions are in
	 *         Machine.java
	 **/
	public  static void handleSystemCall(int pWhichSysCall) {

		Debug.print('a', "!!!!" + Machine.read1 + "," + Machine.read2 + "," + Machine.read4 + "," + Machine.write1 + ","
				+ Machine.write2 + "," + Machine.write4);

		switch (pWhichSysCall) {
		// If halt is received shut down
		case SC_Halt:
			Debug.print('a', "Shutdown, initiated by user program.");
			Interrupt.halt();
			break;

		case SC_Exit:
			// Read in any arguments from the 4th register
			int arg = Machine.readRegister(4);

			System.out.println("SysCall:"+SC_Exit+" PID:" +JNachos.getCurrentProcess().getPid() + " exiting with code " + arg);
			// Check if there is any process waiting for the current process to finish
			checkWaitingProcess(arg);

			JNachos.getCurrentProcess().finish();
			
			break;
		
		case SC_Fork:
			// Capture the current state of the interrupts
			boolean oldLevel = Interrupt.setLevel(false);
			
			System.out.println("SysCall:"+SC_Fork+" PID:" +JNachos.getCurrentProcess().getPid());
			//Create new Child Process.
			int childProcessId = JNachos.getCurrentProcess().fork();
			// putting back the result to R2 for user program to read
			Machine.writeRegister(2, childProcessId);
			
			Interrupt.setLevel(oldLevel);
			break;
			
		case SC_Join:
			// Capture the current state of the interrupts
			boolean oldLeveljoin = Interrupt.setLevel(false);
			System.out.println("SysCall:"+SC_Join+" PID:" +JNachos.getCurrentProcess().getPid());
			// reading the value passed as argument to join
			int pidWaitProcess = Machine.readRegister(4);
			//Update process counters to next instruction
			NachosProcess.updateCounters();
			if(pidWaitProcess != 0 && pidWaitProcess != JNachos.getCurrentProcess().getPid()){
				
				maintainProcess(pidWaitProcess);	
				int returnValue = JNachos.getCurrentProcess().join(pidWaitProcess);
				Machine.writeRegister(2, returnValue);

			}
			Interrupt.setLevel(oldLeveljoin);
			break;
			
		case SC_Exec:
			boolean oldLevelexec = Interrupt.setLevel(false);
			System.out.println("SysCall:"+SC_Exec+" PID:" +JNachos.getCurrentProcess().getPid());
			int startingAddress = Machine.readRegister(4);
			String fileName = fetchFileName(startingAddress);
			if(fileName.length() > 0){
				updateAddrSpace(fileName);
			}else{
				//Waking up processes waiting for this process
				checkWaitingProcess(-1);
				JNachos.getCurrentProcess().finish();
			}
			Interrupt.setLevel(oldLevelexec);
			break;

		default:
			Interrupt.halt();
			break;
		}
	}
	
	/**
	 * Free out physical Pages before finishing the process
	 * Clear bits in the BitMap
	 * Clear Pages from Main Memory to avoid Information leak
	 */
	
	public static void freePhysicalPages() {
		Map<Integer,Integer> phytoProcess = JNachos.getPhyPageNumtoProcessId();
		int processID = JNachos.getCurrentProcess().getPid();
		System.out.println("Clearing Pages for ProcessId: " + processID);
		for(Map.Entry<Integer, Integer> value: phytoProcess.entrySet()){
			if(value.getValue() == processID){
				AddrSpace.mFreeMap.clear(value.getKey());
				System.out.print(value.getKey()+ " ");
				// Zero out all of main memory
				Arrays.fill(Machine.mMainMemory, value.getKey() * Machine.PageSize,
						(value.getKey() + 1) * Machine.PageSize, (byte) 0);
				
				JNachos.getPhyPageList().remove(value.getKey());
			}
		}
		System.out.println("");
	}

	/**
	 * Fetches the filename from the main Memory
	 * @param startingAddress
	 * @return
	 */
	
	private static String fetchFileName(int startingAddress) {
		int i = startingAddress;
		
		String filename = "";
		
		while( Machine.mMainMemory[i] != '\0'){
			
			filename+=(char)Machine.mMainMemory[i];
			i++;
		}
		
		return filename;
	}
/**
 * Maintains a Map of processes which will sleep for process
 * with @param pid to finish
 */
	private static void maintainProcess(int pid) {
		
			if(waitMap != null && waitMap.containsKey(pid)){
				waitMap.get(pid).add(JNachos.getCurrentProcess().getPid());
			}else{
				List<Integer> list = new ArrayList<>();
				list.add(JNachos.getCurrentProcess().getPid());
				waitMap.put(pid, list);
			}
		
	}
	/**
	 * Checks if there are any processes waiting for the current process to finish
	 * @param exitCode
	 * @return
	 */
	public static void checkWaitingProcess(int exitCode){
		int currentProcPid = JNachos.getCurrentProcess().getPid();
		if(waitMap.containsKey(currentProcPid)){
			
			List<Integer> waitingProcesses = waitMap.get(currentProcPid);
			
			for(Integer process : waitingProcesses){
				
				System.out.println("Process with PID: "+NachosProcess.getProcessByID(process).getPid()+" was waiting for PID: "+ currentProcPid+" to finish");
				Scheduler.readyToRun(NachosProcess.getProcessByID(process));
				NachosProcess.getProcessByID(process).getUserRegisters()[2] = exitCode;
			}
			waitMap.remove(currentProcPid);
			
		}
	}
	/**
	 * Reset the address space
	 * @param pAr is the user Program filename to run
	 * Not required to invoke Machine.run() again ,as we have already reset the PC.
	 */
	public synchronized static void updateAddrSpace(Object pArg) {
		String filename = (String)pArg;

		// The executable file to run
		OpenFile executable = JNachos.mFileSystem.open(filename);

		// If the file does not exist
		if (executable == null) {
			Debug.print('t', "Unable to open file" + filename + "Finishing the process");
			System.out.println("Unable to open file" + filename + "Finishing the process");
			// finish the current process and run the next process from ready list, else it will keep looping on Exec call
			JNachos.getCurrentProcess().finish();
		}
			
			// Load the file into the memory space
			AddrSpace space = new AddrSpace(executable);
			JNachos.getCurrentProcess().setSpace(space);

			// set the initial register values
			space.initRegisters();

			// load page table register
			space.restoreState();
			
		assert (false);

	}
}
