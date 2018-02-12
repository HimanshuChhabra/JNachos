/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import java.util.*;

import javax.crypto.Mac;

import com.sun.swing.internal.plaf.metal.resources.metal_pt_BR;

import jnachos.filesystem.OpenFile;
import jnachos.kern.messages.MessageBuffer;
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

	public static final int SC_SendMessage = 13;

	public static final int SC_WaitMessage = 14;

	public static final int SC_SendAnswer = 15;

	public static final int SC_WaitAnswer = 16;

	/** Map of waiting processes */
	public static HashMap<Integer, List<Integer>> waitMap  = new HashMap<Integer, List<Integer>>();

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

			Set<Integer> bufferIdSet = JNachos.getCurrentProcess().getProcessMessageQueue().keySet();
			
			Iterator<Integer> iterator = bufferIdSet.iterator();
			while(iterator.hasNext()) {
				int buff = iterator.next();
				if(JNachos.getCurrentProcess().getProcessMessageQueue().containsKey(buff) && JNachos.getCurrentProcess().getProcessMessageQueue().get(buff).isActive()) {
					if(JNachos.getMessageBufferQueue().containsKey(buff) && JNachos.getMessageBufferQueue().get(buff).contains(0))
					JNachos.getMessageBufferQueue().get(buff).get(0).setActiveFlag(false);
					 
					if(JNachos.getMessageBufferQueue().containsKey(buff) && JNachos.getMessageBufferQueue().get(buff).contains(1))
						JNachos.getMessageBufferQueue().get(buff).get(1).setActiveFlag(false);
					
					cleanBuffer(buff);
				}
			}

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
			String fileName = parseInput(startingAddress);
			if(fileName.length() > 0){
				updateAddrSpace(fileName);
			}else{
				//Waking up processes waiting for this process
				checkWaitingProcess(-1);
				JNachos.getCurrentProcess().finish();
			}
			Interrupt.setLevel(oldLevelexec);
			break;

		case SC_SendMessage:{
			boolean oldlevel = Interrupt.setLevel(false);

			int  receiverPtr = Machine.readRegister(4);
			int  messagePtr = Machine.readRegister(5);
			int  bufferID = Machine.readRegister(6);
			String receiverName = parseInput(receiverPtr);
			String message = parseInput(messagePtr);

			NachosProcess receiverProcess = NachosProcess.getProcessByName(receiverName);
			if(receiverProcess != null) {

				if(JNachos.getCurrentProcess().getMessageCount() <= JNachos.getMessageLimit()) {

					if(bufferID  == -1) {
						MessageBuffer newBuffer = new MessageBuffer(JNachos.getCurrentProcess(), receiverProcess);
						if(newBuffer.getBufferID() == -1) {
							System.out.println("Message Buffer Exhausted, cannot proceed with communication. ");
							Machine.writeRegister(2, newBuffer.getBufferID());
							NachosProcess.updateCounters();
							Interrupt.setLevel(oldlevel);
							break;
						}
						System.out.println("Message Buffer : " +newBuffer.getBufferID()+" allocated to processes "+ JNachos.getCurrentProcess().getName()+" and "+receiverName);
						newBuffer.getMessages().add(message);
						List<MessageBuffer> messageBufferList = new ArrayList<MessageBuffer>();
						messageBufferList.add(newBuffer);

						JNachos.getMessageBufferQueue().put(newBuffer.getBufferID(), messageBufferList);
						receiverProcess.getProcessMessageQueue().put(newBuffer.getBufferID()	, newBuffer);

						//return value to user program
						Machine.writeRegister(2, newBuffer.getBufferID());

						System.out.println("Message sent to Process "+receiverName + " with message : " +message+ " by process " + JNachos.getCurrentProcess().getName());
					}else {
						//check for valid sender and receiver pair
						MessageBuffer mBuffer = validateCommunication(bufferID,receiverProcess);
						if(mBuffer != null) {
							mBuffer.getMessages().add(message);
							receiverProcess.getProcessMessageQueue().put(bufferID	, mBuffer);

							//return value to user program
							Machine.writeRegister(2, bufferID);
							System.out.println("Message sent to Process "+receiverName + " with message : " +message+ " by process " + JNachos.getCurrentProcess().getName());
						}else {
							//Pass some error message
							Machine.writeRegister(2, -1);
							System.out.println("Security error \nProcess "+ JNachos.getCurrentProcess().getName() + " attempted Communication via incorrect Buffer with ID: " +bufferID+ " to communicate with process: "+receiverName);
						}
					}
				}else {
					
					MessageBuffer mBuffer = validateCommunication(bufferID,receiverProcess);
					if(mBuffer != null) {
						mBuffer.getMessages().add(JNachos.DUMMY_MESSAGE);
						receiverProcess.getProcessMessageQueue().put(bufferID	, mBuffer);

						//return value to user program
						Machine.writeRegister(2, bufferID);
					}
					
					System.out.println("Maximum Messages Limit reached by process: " + JNachos.getCurrentProcess().getName());
					System.out.println("Dummy Response sent to the process: " + receiverName);
				}

			}else {
				//no need to send dummy
				cleanBuffer(bufferID);
				System.out.println("Dummy Response");
			}

			NachosProcess.updateCounters();
			Interrupt.setLevel(oldlevel);

			break;}

		case SC_WaitMessage:{
			boolean oldlevel = Interrupt.setLevel(false);
			int  senderPtr = Machine.readRegister(4);
			String sender  = parseInput(senderPtr);
			int  messagePtr = Machine.readRegister(5);
			int  bufferID = Machine.readRegister(6);

			while(true) {
				if(NachosProcess.getProcessByName(sender) != null) {
					MessageBuffer mbuffer = fetchProcessBuffer(bufferID,sender);
					if(mbuffer != null && !mbuffer.getMessages().isEmpty()) {
						String message = mbuffer.getMessages().remove(0);
						writeMessageInMem(message,messagePtr);
						Machine.writeRegister(2,mbuffer.getBufferID());
						System.out.println("Message Received by process "+ JNachos.getCurrentProcess().getName()+" from process "+ sender + ":" + message);
						break;
					}else {
						JNachos.getCurrentProcess().yield();
					}
				}else {
					MessageBuffer mbuffer = fetchProcessBuffer(bufferID,sender);
					if(mbuffer != null && !mbuffer.getMessages().isEmpty()) {
						String message = mbuffer.getMessages().remove(0);
						writeMessageInMem(message,messagePtr);
						Machine.writeRegister(2,mbuffer.getBufferID());
						System.out.println("Message Received by process "+ JNachos.getCurrentProcess().getName()+" from process "+ sender + ":" + message);
					}else {
						//send dummy message
						cleanBuffer(bufferID);
						writeMessageInMem(JNachos.DUMMY_MESSAGE,messagePtr);
						Machine.writeRegister(2,-1);
						System.out.println("Process: "+sender+" exited without sending any message to process "+JNachos.getCurrentProcess().getName());
						System.out.println("Dummy Message sent to process: "+JNachos.getCurrentProcess().getName());
					}
					break;
				}
			}

			NachosProcess.updateCounters();
			Interrupt.setLevel(oldlevel);

			break;}

		case SC_SendAnswer:{
			boolean oldlevel = Interrupt.setLevel(false);

			int  resultStatus = Machine.readRegister(4);
			int  answerPtr = Machine.readRegister(5);
			String answer  = parseInput(answerPtr);
			int  bufferID = Machine.readRegister(6);

			MessageBuffer mBuffer  = JNachos.getCurrentProcess().getProcessMessageQueue().get(bufferID);
			if(mBuffer != null) {
				NachosProcess receiver = mBuffer.getSender();
				if(receiver != null && NachosProcess.isProcessAlive(receiver.getPid())) {
					if(receiver.getProcessMessageQueue().get(bufferID) == null) {
						MessageBuffer newBuffer = new MessageBuffer(JNachos.getCurrentProcess(), receiver, bufferID);
						newBuffer.getMessages().add(answer);
						JNachos.getMessageBufferQueue().get(bufferID).add(newBuffer);
						receiver.getProcessMessageQueue().put(bufferID,newBuffer);
					}else {
						receiver.getProcessMessageQueue().get(bufferID).getMessages().add(answer);
					}
					Machine.writeRegister(2, JNachos.getCurrentProcess().getPid());
					System.out.println("Answer sent to Process "+receiver.getName() + " with message : " +answer + " by process " + JNachos.getCurrentProcess().getName());
				}else {
					cleanBuffer(bufferID);
					//no need to send dummy
					Machine.writeRegister(2, -1);
					System.out.println("Receiver either null or dead");
				}
			}

			NachosProcess.updateCounters();
			Interrupt.setLevel(oldlevel);
			break;}

		case SC_WaitAnswer:{
			boolean oldlevel = Interrupt.setLevel(false);  

			int  resultStatus = Machine.readRegister(4);
			int  answerPtr = Machine.readRegister(5);
			int  bufferID = Machine.readRegister(6);
			String sender = null;
			
			if(JNachos.getCurrentProcess().getProcessMessageQueue().get(bufferID) != null)
				sender = JNachos.getCurrentProcess().getProcessMessageQueue().get(bufferID).getSender().getName();
			else {
				sender = JNachos.getMessageBufferQueue().get(bufferID).get(0).getReceiver().getName();
			}

			while(true) {
				if(NachosProcess.getProcessByName(sender) != null) {
					MessageBuffer mbuffer = fetchProcessBuffer(bufferID,sender);
					if(mbuffer != null && !mbuffer.getMessages().isEmpty()) {
						String message = mbuffer.getMessages().remove(0);
						writeMessageInMem(message, answerPtr);
						Machine.writeRegister(2, JNachos.getCurrentProcess().getPid());
						System.out.println("Answer Received by process "+ JNachos.getCurrentProcess().getName()+" from process "+ sender + ":" + message);
						break;
					}else {
						JNachos.getCurrentProcess().yield();
					}
				}else {
					MessageBuffer mbuffer = fetchProcessBuffer(bufferID,sender);
					if(mbuffer != null && !mbuffer.getMessages().isEmpty()) {
						String message = mbuffer.getMessages().remove(0);
						writeMessageInMem(message, answerPtr);
						Machine.writeRegister(2, JNachos.getCurrentProcess().getPid());
						System.out.println("Answer Received by process "+ JNachos.getCurrentProcess().getName()+" from process "+ sender + ":" + message);
					}else {
						cleanBuffer(bufferID);
						writeMessageInMem(JNachos.DUMMY_MESSAGE, 0);
						Machine.writeRegister(2, 0);
						System.out.println("Dummy Response sent to: " + JNachos.getCurrentProcess().getName()+ " , Process "+ sender + " Exited with no response");
					}
					break;
				}
			}

			NachosProcess.updateCounters();
			Interrupt.setLevel(oldlevel);
			break;} 

		default:
			Interrupt.halt();
			break;
		}
	}

	private static void writeMessageInMem(String message, int ptr) {

		for(int i = ptr, j = 0 ; i < ptr + message.length() ; i++ , j++) {

			Machine.writeMem(i, 1, (int)message.charAt(j));
		}

	}

	private static MessageBuffer fetchProcessBuffer(int bufferID, String sender) {
		Map<Integer,MessageBuffer> messagesQueue = JNachos.getCurrentProcess().getProcessMessageQueue();
		if(!messagesQueue.isEmpty()) {
			if(messagesQueue.containsKey(bufferID)) {
				return	messagesQueue.get(bufferID);
			} 
			else if(sender != null){
				for(MessageBuffer entry : messagesQueue.values()) {
					if(entry.getSender().getName().equals(sender)) {
						return entry;
					}
				}
			}
		}
		return null;
	}

	private static MessageBuffer validateCommunication(int bufferID , NachosProcess receiverProcess) {
		MessageBuffer mBuffer = JNachos.getMessageBufferQueue().get(bufferID).get(0);
		if(mBuffer != null) {
			if(mBuffer.getReceiver().equals(receiverProcess)) {
				return mBuffer;
			}
		}
		return null;
	}

	/**
	 * Free out physical Pages before finishing the process
	 * Clear bits in the BitMap
	 * Clear Pages from Main Memory to avoid Information leak
	 */

	public static void freePhysicalPages() {
		Map<Integer,Integer> phytoProcess = JNachos.getPhyPageNumtoProcessId();
		int processID = JNachos.getCurrentProcess().getPid();
		//System.out.println("Clearing Pages for ProcessId: " + processID);
		for(Map.Entry<Integer, Integer> value: phytoProcess.entrySet()){
			if(value.getValue() == processID){
				AddrSpace.mFreeMap.clear(value.getKey());
				//System.out.print(value.getKey()+ " ");
				// Zero out all of main memory
				Arrays.fill(Machine.mMainMemory, value.getKey() * Machine.PageSize,
						(value.getKey() + 1) * Machine.PageSize, (byte) 0);

				JNachos.getPhyPageList().remove(value.getKey());
			}
		}
		//System.out.println("");
	}

	/**
	 * Fetches the filename from the main Memory
	 * @param startingAddress
	 * @return
	 */

	private static String parseInput(int startingAddress) {
		/*int i = startingAddress;

		String filename = "";

		while( Machine.mMainMemory[i] != '\0'){

			filename+=(char)Machine.mMainMemory[i];
			i++;
		}
		if(filename.equals(" ")) {
			filename = "dummy";
		}
		return filename;*/

		String filename = "";
		int val;

		while(true){
			val = Machine.readMem(startingAddress, 1);
			if((char)val !='\0') {
				filename+=(char)val+"";
				startingAddress++;
			}else {
				break;
			}
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

	private static void cleanBuffer(int bufferID) {
		if(bufferID >=0)
		JNachos.clearMessageBuffer(bufferID);
	}
}
