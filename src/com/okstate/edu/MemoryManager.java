package com.okstate.edu;

import java.util.ArrayList;

public class MemoryManager {

	/**
	 * This method stores data into the memory for a given segment and job.
	 * @param segment
	 * @param SOR
	 * @param jobId
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean storeDataInMemory(ArrayList<String> segment, String SOR, String jobId) throws ErrorHandler{
		boolean flag = true;
		String binaryBaseAddress = SOR.substring(0, 14);
		int baseAddress = Integer.parseInt(binaryBaseAddress,2);
		int displacement = 0;
		for(String data : segment){
			if(!Memory.writeMemory(baseAddress+displacement, data)){
				flag = false;
				break;
			}
			displacement = displacement + 4;
		}
		return flag;
	}
	
}
