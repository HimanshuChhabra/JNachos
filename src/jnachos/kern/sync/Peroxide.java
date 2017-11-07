package jnachos.kern.sync;

import java.io.*;

import jnachos.kern.*;

public class Peroxide {

	/** Semaphore H */
	static Semaphore H = new Semaphore("SemH", 0);

	/**	*/
	static Semaphore O = new Semaphore("SemO", 0);

	/**	*/
	static Semaphore wait = new Semaphore("wait", 0);

	/**	*/
	static Semaphore mutex = new Semaphore("MUTEX", 1);

	/**	*/
	static Semaphore mutex1 = new Semaphore("MUTEX1", 1);

	/**	*/
	static long count = 0;

	static long count1 = 0;

	/**	*/
	static int Hcount, Ocount, nH, nO;

	/**	*/
	class HAtom implements VoidFunctionPtr {
		int mID;

		/**
		 *
		 */
		public HAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {
			mutex.P();
			if (count % 2 == 0) // first H atom
			{
				count++; // increment counter for the first H
				mutex.V(); // Critical section ended
				H.P(); // Waiting for the second H atom

			} else // second H atom
			{
				count++; // increment count for next first H
				mutex.V(); // Critical section ended
				O.V(); // wake up O atom

			}

			wait.P(); // wait for hydrogen peroxide message done

			System.out.println("H atom #" + mID + " used in making Hydrogen Peroxide.");
		}
	}

	/**	*/
	class OAtom implements VoidFunctionPtr {
		int mID;

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public OAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {

			mutex1.P();  //Critical Section Started
			
			if (count1 % 2 == 0) // first O atom
			{
				count1++; // increment counter for the first O atom
				mutex1.V(); // Critical section ended
				H.V(); // Wake up a H Atom
				wait.P(); // wait for another pair of O atom
			} else{
				count1++;
				O.P(); 
				
				makeHydrogenPeroxide(); // Display Message
				
				//wake up H atoms and they will return to resource pool
				wait.V(); 
				wait.V();
				wait.V();
				
				Hcount = Hcount - 2;
				Ocount = Ocount - 2;
				
				 
				System.out.println("Numbers Left: H Atoms: " + Hcount + ", O Atoms: " + Ocount);
				System.out.println("Numbers Used: H Atoms: " + (nH - Hcount) + ", O Atoms: " + (nO - Ocount));
				
				
				//end of critical section
				mutex1.V();
			}
			System.out.println("O atom #" + mID + " used in making Hydrogen Peroxide.");
		}
	}

	/**
	 * oAtom will call oReady. When this atom is used, do continuous "Yielding"
	 * - preserving resource
	 */
	public static void makeHydrogenPeroxide() {
		System.out.println("** Hydrogen Peroxide made!!! **");
	}

	/**
	 * oAtom will call oReady. When this atom is used, do continuous "Yielding"
	 * - preserving resource
	 */
	public Peroxide() {
		runHydrogenPeroxide();
	}

	/**
	 *
	 */
	public void runHydrogenPeroxide() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Number of H atoms ? ");
			nH = (new Integer(reader.readLine())).intValue();
			System.out.println("Number of O atoms ? ");
			nO = (new Integer(reader.readLine())).intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Hcount = nH;
		Ocount = nO;

		for (int i = 0; i < nH; i++) {
			HAtom atom = new HAtom(i);
			(new NachosProcess(new String("hAtom" + i))).fork(atom, null);
		}

		for (int j = 0; j < nO; j++) {
			OAtom atom = new OAtom(j);
			(new NachosProcess(new String("oAtom" + j))).fork(atom, null);
		}
	}
}
