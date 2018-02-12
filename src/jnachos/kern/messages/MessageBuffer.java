package jnachos.kern.messages;
/**
 * Message buffer, is used as the data structure to keep necessary information for message passing
 * An instance of this class is used every time two new processes want to start communication with each other.
 * Every process maintains a list of instance of this class which it uses for communication.
 * A list of instances of this class is maintained by the kernel.
 */
import java.util.*;

import jnachos.kern.JNachos;
import jnachos.kern.NachosProcess;


public class MessageBuffer {


	private NachosProcess sender;
	private NachosProcess receiver;
	private int bufferID;
	private boolean isActive;
	private List<String> messages;

	public MessageBuffer(NachosProcess sender,NachosProcess receiver){
		this.sender = sender;
		this.receiver = receiver;
		this.bufferID = JNachos.getMessageBuffer().find();
		this.messages = new ArrayList<String>();
		isActive = true;
	}
	
	public MessageBuffer(NachosProcess sender,NachosProcess receiver,int bufferID) {
		this.sender = sender;
		this.receiver = receiver;
		this.bufferID = bufferID;
		this.messages = new ArrayList<String>();
		isActive = true;
	}
	
	public NachosProcess getSender() {
		return this.sender;
	}
	
	public NachosProcess getReceiver() {
		return this.receiver;
	}
	
	public int getBufferID() {
		return this.bufferID;
	}
	
	public List<String> getMessages(){
		return messages;
	}
	
	public boolean isActive() {
		return this.isActive;
	}
	
	public void setActiveFlag(boolean flag) {
		this.isActive = flag;
	}
}
