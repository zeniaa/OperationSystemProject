package com.okstate.edu;

/**
 * @Name: Zenia Arora
 * @Date: 04-28-2015
 * @Description:
 * 		This routine is the implementation of Memory. It has methods to
 * 	read data from memory and write data to the memory address.
 */
import java.util.TreeMap;

public class Memory {

	private static TreeMap<Integer,String> memory;
	public static TreeMap<Integer,Integer> fbmv;
	public static TreeMap<String,PCB> pcbs;
	
	/**
	 * This method creates and initializes the memory.
	 */
	public static void initialize(){
		memory = new TreeMap<Integer, String>();
		fbmv = new TreeMap<Integer, Integer>();
		pcbs = new TreeMap<String, PCB>();
		
		for(int i=16, j=64; i<4096;i++){
			memory.put(j, null);
			fbmv.put(j, 0);
			j=j+4;
		}
	}
	
	/**
	 * This method writes the data into the memory at a given address
	 * @param address: address at which data is to be stored.
	 * @param instruction: data to be stored into memory
	 * @throws ErrorHandler
	 */
	public static boolean writeMemory(int address, String instruction){
		boolean flag = true;
		if(address <= 16380){
			memory.put(address, instruction);
		}else{
			//Memory address out of range.
			ErrorHandler.recordError(111);
			ProcessManager.error = false;
			flag = false;
		}
		return flag;
	}
	
	/**
	 * This method reads data from the memory from the give address.
	 * 
	 * @param address: Address from where data has to be read.
	 * @return: data at given address
	 * @throws ErrorHandler
	 */
	public static String readMemory(int address){
		String instruction = null;
		if(address <= 16380){
			instruction = memory.get(address);
		}else{
			//Memory address out of range.
			ErrorHandler.recordError(111);
			ProcessManager.error = true;
		}
		return instruction;
	}
	
	/**
	 * This method find starting address of next available segment for the
	 * size requested/needed.
	 * @param sizeRequest : Requested size of the segment.
	 * @return
	 */
	public static int getAvailableMemorySegmentStartingAddress(int sizeRequest){
		int words = getWords(sizeRequest);
		int sizeCount = 0;
		int startAddress = -1;
		for(int key : fbmv.keySet()){
			if(fbmv.get(key) == 0 && startAddress == -1){
				startAddress = key;
				sizeCount++;
			}else if(fbmv.get(key) == 0){
				sizeCount++;
			}
			if(sizeCount == words){
				break;
			}
			if(fbmv.get(key) == 1){
				startAddress = -1;
				sizeCount = 0;
			}
		}
		if(sizeCount != words){
			startAddress = -1;
		}else{
			int j = startAddress;
			for(int i = 0; i < words; i++){
				fbmv.put(j, 1);
				j=j+4;
			}
		}
		return startAddress;
	}
	
	/**
	 * This method releases the memory
	 * @param startAddress : address from where it should be released.
	 * @param size : size of the segment.
	 */
	public static void freeMemorySegement(int startAddress, int size){
		int j = startAddress;
		int words = getWords(size);
		for(int i = 0; i < words; i++){
			fbmv.put(j, 0);
			memory.put(j, null);
			j=j+4;
		}
	}
	
	public static int  getWords(int size){
		int words = 0;
		words = size / 4;
		if(size%4 != 0){
			words++;
		}
		return words;
	}
	
	/**
	 * Store the PCB on memory
	 * @param jobId
	 * @param pcb
	 * @return
	 */
	public static boolean storePCB(String jobId, PCB pcb){
		boolean flag = true;
		if(pcbs.size()<16){
			pcbs.put(jobId, pcb);
		}else{
			flag =false;
		}
		return flag;
	}
	
	/**
	 * Release PCB area for a job form memory.
	 */
	public static void removePCB(String jobId){
		PCB pcb = pcbs.get(jobId);
		for(int i=0;i<4;i++){
			releaseMemory(pcb.sor[i]);
		}
		pcbs.remove(jobId);
	}
	
	private static void releaseMemory(String sor){
		int startAddress = Integer.parseInt(sor.substring(0, 14),2);
		int length = Integer.parseInt(sor.substring(22),2);
		freeMemorySegement(startAddress,length);
	}
	
}
