package jnachos.kern;

import jnachos.machine.Machine;
import jnachos.machine.Statistics;
import jnachos.machine.TranslationEntry;

public class PageFaultHandler {
	
	public static void handlePageFault(int badVAddr){
		//update pagefault counter
		Statistics.numPageFaults ++;
		
		//fetch the virtual page number
		int vpn = (int) badVAddr / Machine.PageSize;
		
		int newPhyPage = AddrSpace.mFreeMap.find();
		TranslationEntry pageEntry = null;
		//check if there is any free Physical Page in Main Memory
		
		if(newPhyPage!=-1){
		//fetch the page entry of the current Nachos Process
		pageEntry = JNachos.getCurrentProcess().getSpace().getPageTableEntry(vpn);
		
		//read from Swap Space and write it to the newPhyPage
		readFromSwapSpace(pageEntry.swapLoc, newPhyPage);
		
		//update the page entry
		updatePageTable(pageEntry, newPhyPage);
		
		//Update FIFO, Add the used Physical Page
		
		JNachos.getPhyPageList().add(newPhyPage);
		
		//update mapping
		JNachos.setPhyPageNumToPorcessID(newPhyPage, JNachos.getCurrentProcess().getPid());
		
		
		}else{ // No Free Physical Page
			
			//Evict a physical Page
			int evictedPage = JNachos.getPhyPageList().removeFirst();
			
			/*if(evictedPage == 0){
				System.out.println("");
			System.out.print(evictedPage+" ");
			}else{
				System.out.print(evictedPage+" ");
			}*/
			
			//fetch pageEntry
			
			pageEntry = getPageEntryByPhyPage(evictedPage);
			
			if(pageEntry != null){
				writeToSwapSpace(pageEntry.swapLoc, evictedPage);
				pageEntry.physicalPage = -1;
				pageEntry.valid = false;
				pageEntry.use = false;
				readFromSwapSpace(JNachos.getCurrentProcess().getSpace().getPageTableEntry(vpn).swapLoc,evictedPage);
				updatePageTable(JNachos.getCurrentProcess().getSpace().getPageTableEntry(vpn),evictedPage);
				
				// Updated from Map
				JNachos.setPhyPageNumToPorcessID(evictedPage, JNachos.getCurrentProcess().getPid());
				//Add to the  list
				JNachos.getPhyPageList().add(evictedPage);
			}
			else{
				Debug.print('t', "PageEntry Not Found in the Page Table");
				System.out.println("PageEntry Not Found in the Page Table");
				
			}
			
		}
		
	}
	
	private static void updatePageTable(TranslationEntry pageTableEntry,int newPhyPage) {
		pageTableEntry.physicalPage = newPhyPage;
		pageTableEntry.valid = true;
		pageTableEntry.use = true;
	}

	private static void readFromSwapSpace(int swapLoc, int phyPage) {
		byte buffer[] = new byte[Machine.PageSize];
		
		// read the data from the swap file into the buffer
		JNachos.getSwapFile().readAt(buffer, Machine.PageSize, swapLoc*Machine.PageSize);
		
		//Copy the contents of buffer into the main Memory
		System.arraycopy(buffer, 0, Machine.mMainMemory, phyPage*Machine.PageSize, Machine.PageSize);
	}

	private static void writeToSwapSpace(int swapLoc, int phyPage) {
		
		byte buffer[] = new byte[Machine.PageSize];
		
		System.arraycopy(Machine.mMainMemory, phyPage*Machine.PageSize,buffer,0,Machine.PageSize);
		
		JNachos.getSwapFile().writeAt(buffer, Machine.PageSize, swapLoc*Machine.PageSize);
		
	}

	private  static TranslationEntry getPageEntryByPhyPage(int evictedPage){
		
		//Fetch the process whose Page was evicted
		NachosProcess eProcess = NachosProcess.getProcessByID(JNachos.getProcessIdByPhyPageNumber(evictedPage));
		
		//fetch pageEntry of Page one
		TranslationEntry pageTable[] = eProcess.getSpace().getPageTableByProcess();
		
		for(int i=0; i< pageTable.length ; i++){
			
			if(pageTable[i].physicalPage == evictedPage){
				return pageTable[i];
			}
		}
		return null;
	}

}
