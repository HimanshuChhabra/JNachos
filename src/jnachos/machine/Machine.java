/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.machine;

import jnachos.kern.*;

/**
 *
 */
public class Machine {

	// Used for calculating the number of reads & writes
	public static int read1 = 0;
	public static int read2 = 0;
	public static int read4 = 0;

	public static int write1 = 0;
	public static int write2 = 0;
	public static int write4 = 0;

	// If we are using the TLBS
	public static boolean mUSE_TLB;
	public static boolean mSingleStep;

	// Set the page size equal to the disk sector size, for simplicity
	public static final int PageSize = 128;//16;//4;//128;//4
	public static final int NumPhysPages = 4096;//128;//16;//1024;//16
	public static final int MemorySize = (NumPhysPages * PageSize); //64
	public static final int TLBSize = 4; // if there is a TLB, make it small

	// Textual names of the exceptions that can be generated by user program
	// execution, for debugging.
	public static String[] exceptionNames = { "no exception", "syscall", "page fault/no TLB entry", "page read only",
			"bus error", "address error", "overflow", "illegal instruction" };

	// User program CPU state. The full set of MIPS Machine.mRegisters, plus a
	// few
	// more because we need to be able to start/stop a user program between
	// any two instructions (thus we need to keep track of things like load
	// delay slots, etc.)
	public static final int StackReg = 29; // User's stack pointer
	public static final int RetAddrReg = 31; // Holds return address for
												// procedure calls
	public static final int NumGPRegs = 32; // 32 general purpose
											// Machine.mRegisters on MIPS
	public static final int HiReg = 32; // Double register to hold multiply
										// result
	public static final int LoReg = 33;
	public static final int PCReg = 34; // Current program counter
	public static final int NextPCReg = 35; // Next program counter (for branch
											// delay)
	public static final int PrevPCReg = 36; // Previous program counter (for
											// debugging)
	public static final int LoadReg = 37; // The register target of a delayed
											// load.
	public static final int LoadValueReg = 38;// The value to be loaded by a
												// delayed load.
	public static final int BadVAddrReg = 39; // The failing virtual address on
												// an exception
	public static final int NumTotalRegs = 40;

	// The main memory RAM for the machine
	public static byte[] mMainMemory;

	// The registers in the CPU
	public static int mRegisters[];

	/**
	 * The hardware timer. This class can throw interrupts at scheduable
	 * intervals.
	 */
	private static Timer mTimer;

	/**
	 * Initialize the simulation of user program execution.
	 *
	 * @param debug
	 *            if true, drop into the debugger after each user instruction is
	 *            executed.
	 */
	public Machine(boolean debug, VoidFunctionPtr timerHandler, int seed, boolean randomYield) {
		setTimer(new Timer(timerHandler, seed, randomYield));

		// Create the CPU registers
		mRegisters = new int[NumTotalRegs];

		// Initialize the registers
		for (int i = 0; i < NumTotalRegs; i++) {
			mRegisters[i] = 0;
		}

		// Create the main memory
		mMainMemory = new byte[MemorySize];
		for (int i = 0; i < MemorySize; i++) {
			mMainMemory[i] = 0;
		}

		// If we are using the TLB
		if (mUSE_TLB) {
			// Create a new TLB
			MMU.mTlb = new TranslationEntry[TLBSize];
			for (int i = 0; i < TLBSize; i++) {
				// Set the valid bits to false
				MMU.mTlb[i].valid = false;

				// Initialze the page table
				MMU.mPageTable = null;
			}
		} else {
			// Set both to null
			MMU.mTlb = null;
			MMU.mPageTable = null;
		}

		mSingleStep = debug;
	}

	/**
	 * killMachine
	 * 
	 * 
	 */

	public static void killMachine() {

	}

	/**
	 * Simulate the execution of a user-level program on Nachos. Called by the
	 * kernel when the program starts up; never returns.
	 *
	 * This routine is re-entrant, in that it can be called multiple times
	 * concurrently -- one for each thread executing user code.
	 */
	public static void run() {
		// Storage for decoded instruction
		Instruction instr = new Instruction();

		if (Debug.isEnabled('m')) {
			System.out.println(
					"Starting process " + JNachos.getCurrentProcess().getName() + " at time " + Statistics.totalTicks);
		}

		// Set to user mode while executing user instructions
		Interrupt.setStatus(Interrupt.UserMode); 

		// Continuously execute user code
		while (true) {
			// Execute one instruction
			/// Machine.dumpState();
			MipsSim.oneInstruction(instr);

			// Update the time
			Interrupt.oneTick();

			if (mSingleStep)// && (runUntilTime <= Statistics.totalTicks))
			{
				debugger();
			}
		}
	}

	/**
	 * Reads the specified register
	 * 
	 * @param (int)
	 *            num: The register to read.
	 * @return (int) The value held in that register
	 */
	public static int readRegister(int num) {
		assert ((num >= 0) && (num < NumTotalRegs));
		Debug.print('m', "ReadRegister " + num + ", value" + Machine.mRegisters[num]);
		return Machine.mRegisters[num];
	}

	/**
	 * Writes the specified register
	 * 
	 * @param (int)
	 *            num: The register to read.
	 * @param (int)
	 *            value: The value to load into the specified register
	 */
	public static void writeRegister(int num, int value) {
		assert ((num >= 0) && (num < NumTotalRegs));
		Debug.print('m', "WriteRegister " + num + ", value" + value);
		Machine.mRegisters[num] = value;
	}

	/**
	 *
	 */
	public static void delayedLoad(int pNextReg, int pNextValue) {
		Machine.mRegisters[Machine.mRegisters[LoadReg]] = Machine.mRegisters[LoadValueReg];
		Machine.mRegisters[LoadReg] = pNextReg;
		Machine.mRegisters[LoadValueReg] = pNextValue;
		Machine.mRegisters[0] = 0; // and always make sure R0 stays zero.
	}

	/**
	 * Read "size" (1, 2, or 4) bytes of virtual memory at "addr" into the
	 * location pointed to by "value".
	 *
	 * Returns FALSE if the translation step from virtual to physical memory
	 * failed.
	 *
	 * "addr" -- the virtual address to read from "size" -- the number of bytes
	 * to read (1, 2, or 4) "value" -- the place to write the result
	 */
	public static Integer readMem(int addr, int size) {
		Integer data = null;
		ExceptionType exception;
		int[] physicalAddress = new int[1];

		Debug.print('a', "Reading VA " + Integer.toHexString(addr) + ", size " + size);

		exception = MMU.translate(addr, physicalAddress, size, false);

		if (exception != ExceptionType.NoException) {
			raiseException(exception, addr);
			return null;
		}

		switch (size) {
		case 1:
			data = new Integer(mMainMemory[physicalAddress[0]]);
			read1++;
			/// new Integer(mMainMemory[addr]); //No descernable change
			break;

		case 2:
			data = new Integer(MipsSim.shortToHost(
					(mMainMemory[physicalAddress[0]] << 8) + (mMainMemory[physicalAddress[0] + 1] & 0xFF)));
			read2++;
			break;

		case 4:
			data = new Integer(MipsSim.wordToHost(
					(mMainMemory[physicalAddress[0]] << 24) + ((mMainMemory[physicalAddress[0] + 1] & 0xFF) << 16)
							+ ((mMainMemory[physicalAddress[0] + 2] & 0xFF) << 8)
							+ (mMainMemory[physicalAddress[0] + 3] & 0xFF)));
			read4++;

			Debug.print('a', mMainMemory[physicalAddress[0]] + "," + mMainMemory[physicalAddress[0] + 1] + ","
					+ mMainMemory[physicalAddress[0] + 2] + "," + mMainMemory[physicalAddress[0] + 3]);

			break;

		default:
			assert (false);
		}

		Debug.print('a', "\tvalue read = " + Integer.toHexString(data));

		return data;
	}

	/**
	 * Write "size" (1, 2, or 4) bytes of the contents of "value" into virtual
	 * memory at location "addr".
	 *
	 * Returns FALSE if the translation step from virtual to physical memory
	 * failed.
	 *
	 * @param addr
	 *            -- the virtual address to write to
	 * @param size
	 *            -- the number of bytes to be written (1, 2, or 4)
	 * @param value
	 *            -- the data to be written
	 */

	public static boolean writeMem(int addr, int size, int value) {
		ExceptionType exception;
		int[] physicalAddress = new int[1];

		Debug.print('a', "Writing VA " + Integer.toHexString(addr) + ", size " + size + ", value " + value);

		exception = MMU.translate(addr, physicalAddress, size, true);

		if (exception != ExceptionType.NoException) {
			Machine.raiseException(exception, addr);
			return false;
		}

		switch (size) {
		case 1:
			// int res = MipsSim.shortToMachine(value);
			mMainMemory[physicalAddress[0]] = (byte) (value);
			write1++;
			break;

		case 2:
			int res1 = MipsSim.shortToMachine(value);// & 0xffff );

			mMainMemory[physicalAddress[0]] = (byte) (res1 >>> 8);
			mMainMemory[physicalAddress[0] + 1] = (byte) (res1);
			write2++;
			break;

		case 4:

			int res2 = MipsSim.wordToMachine(value);
			mMainMemory[physicalAddress[0]] = (byte) (res2 >>> 24);
			mMainMemory[physicalAddress[0] + 1] = (byte) (res2 >>> 16);
			mMainMemory[physicalAddress[0] + 2] = (byte) (res2 >>> 8);
			mMainMemory[physicalAddress[0] + 3] = (byte) (res2);

			Debug.print('d',
					"Wrote: " + value + "\t" + mMainMemory[physicalAddress[0]] + ","
							+ mMainMemory[physicalAddress[0] + 1] + "," + mMainMemory[physicalAddress[0] + 2] + ","
							+ mMainMemory[physicalAddress[0] + 3]);
			write4++;
			break;

		default:
			assert (false);
		}

		return true;
	}

	// Translate an address, and check for
	// alignment. Set the use and dirty bits in
	// the translation entry appropriately,
	// and return an exception code if the
	// translation couldn't be completed.

	/**
	 *
	 *
	 */
	public static void raiseException(ExceptionType which, int badVAddr) {
		Debug.print('m', "Exception: " + which);
		Machine.mRegisters[BadVAddrReg] = badVAddr;
		delayedLoad(0, 0); // finish anything in progress
		Interrupt.setStatus(Interrupt.SystemMode);
		ExceptionHandler.handleException(which); // interrupts are enabled at
													// this point
		Interrupt.setStatus(Interrupt.UserMode);

	}

	// Trap to the Nachos kernel, because of a
	// system call or other exception.

	/**
	 *
	 *
	 */
	public static void dumpState() {
		int i;

		System.out.println("Machine Machine.mRegisters:\n");
		for (i = 0; i < NumGPRegs; i++) {
			switch (i) {
			case StackReg:
				System.out.println("\tSP(" + i + ")=" + Machine.mRegisters[i]);
				// ((i % 4) == 3) ? "\n" : "");
				break;

			case RetAddrReg:
				System.out.println("\tRA(" + i + ")=" + Machine.mRegisters[i]);
				// ((i % 4) == 3) ? "\n" : "");
				break;

			default:
				System.out.println("\t" + i + "  =" + Machine.mRegisters[i]);
				// ((i % 4) == 3) ? "\n" : "");
				break;
			}
		}

		System.out.println("\tHi: " + Machine.mRegisters[HiReg]);
		System.out.println("\tLo: " + Machine.mRegisters[LoReg]);
		System.out.println("\tPC: " + Machine.mRegisters[PCReg]);
		System.out.println("\tNextPC: " + Machine.mRegisters[NextPCReg]);
		System.out.println("\tPrevPC: " + Machine.mRegisters[PrevPCReg]);
		System.out.println("\tLoad: " + Machine.mRegisters[LoadReg]);
		System.out.println("\tLoadV: " + Machine.mRegisters[LoadValueReg]);

	}

	/**
	 * ---------------------------------------------------------------------------------
	 *
	 *
	 * ---------------------------------------------------------------------------------
	 */
	public static void debugger() {
		/*
		 * char[] buf = new char[80]; int num;
		 * 
		 * Interrupt.dumpState(); dumpState();
		 * 
		 * System.out.println(""+ stats.totalTicks);
		 * 
		 * fgets(buf, 80, stdin); if (sscanf(buf, "%d", &num) == 1) {
		 * runUntilTime = num; } else { runUntilTime = 0;
		 * 
		 * switch (*buf) { case '\n': break;
		 * 
		 * case 'c': mSingleStep = FALSE; break;
		 * 
		 * case '?': printf("Machine commands:\n");
		 * printf("    <return>  execute one instruction\n");
		 * printf("    <number>  run until the given timer tick\n");
		 * printf("    c         run until completion\n");
		 * printf("    ?         print help message\n"); break; } }
		 */
	}

	/**
	 * @param mTimer
	 *            the mTimer to set
	 */
	public static void setTimer(Timer pTimer) {
		mTimer = pTimer;
	}

	/**
	 * @return the mTimer
	 */
	public static Timer getTimer() {
		return mTimer;
	}

}
