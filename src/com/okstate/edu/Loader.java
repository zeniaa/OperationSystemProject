package com.okstate.edu;

/**
 * @name: Zenia Arora
 * @Date: 04-28-2015
 * 
 * @Routine : Loader
 *	1. Description:
 *			This routine loads the load module into Memory. It performs validation
 *			before loading the data into the Memory.
 *			
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class Loader {

	public static ArrayList<String> loadingErrors = new ArrayList<String>();
	public static boolean isError = false;
	
	/**
	 * This method loads a single job into the memory after all the 
	 * validations associated with it are correct. If validation fails then
	 * that JOB is discarded.
	 *  
	 * @param fileReader
	 * @param jobId
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean loadJob(BufferedReader fileReader, String jobId) throws ErrorHandler{
		boolean flag = true;
		
		isError = false;
		String[] tuples = Spooler.line.trim().split(" ");
		if(tuples.length == 4){
			for(int i=0; i< 4;i++){
				if(tuples[i].length()!=4){
					//Throw error if it does not contain 4 digit hex.
					isError = captureError(jobId,104);
					return flag;
				}
				//Check if the lengths are in hex
				try{
					int decimal = Integer.parseInt(tuples[i],16);
					//Load CPU register.
					if(decimal>511){
						isError = captureError(jobId,106);
						return flag;
					}
				}catch(NumberFormatException e){
					//Throw error if segment length is not in HEX.
					isError = captureError(jobId,105);
					return flag;
				}
			}
			if(!isError){
				int exeSegSize = Integer.parseInt(tuples[0],16);
				int inputSegSize = Integer.parseInt(tuples[1],16);
				int outputSegSize = Integer.parseInt(tuples[2],16);
				int waSegSize = Integer.parseInt(tuples[3],16);
				
				int exeSegStartAddress = Memory.getAvailableMemorySegmentStartingAddress(exeSegSize);
				int inputSegStartAddress = Memory.getAvailableMemorySegmentStartingAddress(inputSegSize);
				int outputSegStartAddress = Memory.getAvailableMemorySegmentStartingAddress(outputSegSize);
				int waSegStartAddress = Memory.getAvailableMemorySegmentStartingAddress(waSegSize);
				
				if(exeSegStartAddress == -1 
						|| inputSegStartAddress == -1 
						|| outputSegStartAddress == -1
						|| waSegStartAddress == -1){
					if (exeSegStartAddress != -1) Memory.freeMemorySegement(exeSegStartAddress, exeSegSize);
					if (inputSegStartAddress != -1) Memory.freeMemorySegement(inputSegStartAddress, inputSegSize);
					if (outputSegStartAddress != -1) Memory.freeMemorySegement(outputSegStartAddress, outputSegSize);
					if (waSegStartAddress != -1) Memory.freeMemorySegement(waSegStartAddress, waSegSize);
					flag = false;
					return flag;
				}else{
					//Initialize PCB for jobID
					PCB pcb = new PCB();
					pcb.jobId = jobId;
					setSORForJobinPCB(pcb,0,exeSegStartAddress,exeSegSize);
					setSORForJobinPCB(pcb,1,inputSegStartAddress,inputSegSize);
					setSORForJobinPCB(pcb,2,outputSegStartAddress,outputSegSize);
					setSORForJobinPCB(pcb,3,waSegStartAddress,waSegSize);
					
					//Release memory if there is any error found during job parsing.
					if(!parseLoadModule(fileReader, jobId, pcb)){
						if (exeSegStartAddress != -1) Memory.freeMemorySegement(exeSegStartAddress, exeSegSize);
						if (inputSegStartAddress != -1) Memory.freeMemorySegement(inputSegStartAddress, inputSegSize);
						if (outputSegStartAddress != -1) Memory.freeMemorySegement(outputSegStartAddress, outputSegSize);
						if (waSegStartAddress != -1) Memory.freeMemorySegement(waSegStartAddress, waSegSize);
						return flag;
					}else{
						pcb.arrivalTime = CPU.systemClock;
						flag = Memory.storePCB(jobId, pcb);
						CPUManager.readyQueue.add(jobId);
						if(Spooler.spoolingEventTracker > 5){
							Spooler.spoolingEventTracker = 0;
							StringBuffer event = new StringBuffer();
							String[] jobids = jobId.split("_");
							event.append("User Job id:" + jobids[0]+", ");
							event.append("System Job id:" + jobids[1]+", ");
							event.append("Loaded at: " + CPU.systemClock +"(DEC) VTS");
							OutputSpooler.writeInputSpoolingEvents(event);
						}
						gatherSystemInfo(exeSegSize,inputSegSize,outputSegSize,waSegSize);
					}
				}
			}
		}else{
			//Invalid JOB structure. This job will be discarded.
			isError = captureError(jobId,130);
		}
		
		return flag;
	}

	/**
	 * This method sets the SOR registers for the job
	 * @param pcb
	 * @param segNo
	 * @param startAddress
	 * @param length
	 */
	public static void setSORForJobinPCB(PCB pcb, int segNo, int startAddress, int length){
		
		String binarySegLength = CPU.decimalToBinary(length, 9);
		String binaryBaseAddress = CPU.decimalToBinary(startAddress, 14);
		
		StringBuffer value = new StringBuffer(pcb.sor[segNo]);
		value.replace(0, 14, binaryBaseAddress);
		value.replace(23, 32, binarySegLength);
		pcb.sor[segNo] = value.toString();
		
	}
	
	/**
	 * This method captures the error while validating the job
	 * and then writes it into the execution_profile.
	 * @param jobId
	 * @param errorCode
	 * @return
	 */
	public static boolean captureError(String jobId, int errorCode){
		loadingErrors.add("Job Id-> ("+jobId + ") : " + ErrorHandler.recordError(errorCode));
		ErrorHandler.writeErrors(loadingErrors);
		loadingErrors.clear();
		return true;
	}
	
	/**
	 * This method reads the load module and validates it.
	 * If there is any validation failure, the whole module is
	 * discarded and appropriate error is recorded.
	 * 
	 * @param fileReader
	 * @param jobId
	 * @param pcb
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean parseLoadModule(BufferedReader fileReader, String jobId, PCB pcb) throws ErrorHandler{
		ArrayList<String> programSeg = new ArrayList<String>();
		ArrayList<String> waSeg = new ArrayList<String>();
		ArrayList<String> warning = new ArrayList<String>();
		
		try{
			Spooler.line = fileReader.readLine();
			Spooler.filePointer++;
			if(Spooler.line != null){
				while(Spooler.line != null && !Spooler.line.contains("WIC")){
					String[] data = Spooler.line.trim().split(" ");
					
					if(data.length != 2){
						//Invalid module format
						isError = captureError(jobId,107);
						return false;
					}
					
					if(data[0].length() != 2){
						//Field 1 is not 2 digit Hex number.
						isError = captureError(jobId,108);
						return false;
					}
					
					int decField1 = 0;
					try{
						decField1 = Integer.parseInt(data[0],16);
					}catch(NumberFormatException e){
						//Field 1 is not a HEX number.
						isError = captureError(jobId,109);
						return false;
					}
					
					//Field 2 must be of same length as specified in Field 1
					if(data[1].length() != decField1){
						isError = captureError(jobId,110);
						return false;
					}
					
					//Read instruction line and then split it into the group of 8 chars
					String[] instrutions = data[1].trim().split("(?<=\\G.{8})");
					for(String instruction : instrutions){
						try{
							if(instruction.length()!=8){
								isError = captureError(jobId,123);
								return false;
							}
							Long.parseLong(instruction, 16);
							programSeg.add(instruction);
							
						}catch(NumberFormatException e){
							//Invalid instruction.
							isError = captureError(jobId,123);
							return false;
						}
						
					}
					Spooler.line = fileReader.readLine();
					Spooler.filePointer++;
				}
			
				if(programSeg.size()==0){
					isError = captureError(jobId,141);
					return false;
				}
				
				if(Spooler.line != null){

					String[] wicLine = Spooler.line.trim().split(" ");
					if(wicLine.length != 2 || !wicLine[0].equals("WIC") || wicLine[1].length() != 4){
						//Incorrect Module format. Incorrect WIC line format. 
						captureError(jobId,112);
						return false;
					}
					
					int wicCount = 0;
					try{
						wicCount = Integer.parseInt(wicLine[1],16);
					}catch(NumberFormatException e){
						captureError(jobId,108);
						return false;
					}
					
					//Loads the data in Work area
					Spooler.line = fileReader.readLine();
					Spooler.filePointer++;
					while(Spooler.line != null && wicCount > 0){
						String[] wicConstant = Spooler.line.trim().split(" ");
						if(wicConstant.length !=2 || wicConstant[0].length() != 4 || wicConstant[1].length() != 8){
							captureError(jobId,107);
							return false;
						}
						
						try{
							Integer.parseInt(wicConstant[0],16);
							Long.parseLong(wicConstant[1],16);
						}catch(NumberFormatException e){
							captureError(jobId,112);
							return false;
						}
						
						waSeg.add(wicConstant[1]);
						 
						Spooler.line = fileReader.readLine();
						Spooler.filePointer++;
						wicCount--;
					}
					
					if(wicCount != 0){
						//Invalid load module. WIC given length and actual length mismatch.
						captureError(jobId,113);
						return false;
					}
					
					//Read start address and trace flag
					if(Spooler.line != null){
						String[] stAddrAndTrace = Spooler.line.trim().split(" ");
						if(stAddrAndTrace.length !=2 || stAddrAndTrace[0].length() != 4 || stAddrAndTrace[1].length() != 1){
							captureError(jobId,107);
							return false;
						}
						
						try{
							int offSet = Integer.parseInt(stAddrAndTrace[0],16);
							int pcDecimal = Integer.parseInt(pcb.sor[0].substring(0,14),2) + offSet;
							String pcBinary = CPU.decimalToBinary(pcDecimal, 14);
							
							StringBuffer value = new StringBuffer(pcb.psw);
							value.replace(18, 32, pcBinary);
							pcb.psw = value.toString();
						}catch(NumberFormatException e){
							captureError(jobId,114);
							return false;
						}
						
						if(stAddrAndTrace[1].equals("1")){
							pcb.traceFlag=true;
						}else if (stAddrAndTrace[1].equals("0")){
							pcb.traceFlag=false;
						}else{
							pcb.traceFlag=false;
							warning.add("Job Id-> ("+jobId + ") WARNING: Invalid trace switch. Tracefile will not be generated.");
							ErrorHandler.writeErrors(warning);
						}
						
					}else{
						captureError(jobId,112);
						return false;
					}
					
					
					//Reads input values.
					Spooler.line = fileReader.readLine();
					Spooler.filePointer++;
					
					if(Spooler.line != null && Spooler.line.trim().equals("//")){
						Spooler.line = fileReader.readLine();
						Spooler.filePointer++;
						captureError(jobId,143);
						return false;	
					}
					
					Long[] inputs = Spooler.diskInput.get(jobId);
					int inputSize = inputs.length;
					int i=0;
					if(Spooler.line != null && Spooler.line.trim().equals("//DATA")){
						Spooler.line = fileReader.readLine();
						Spooler.filePointer++;
						while(Spooler.line != null && !Spooler.line.contains("//")){
							if(Spooler.line.trim().length() != 8){
								captureError(jobId,115);
								return false;
							}
							
							try{
								Long.parseLong(Spooler.line.trim(),16);
							}catch(NumberFormatException e){
								captureError(jobId,115);
								return false;
							}
							
							if(i<inputSize){
								inputs[i] = Long.parseLong(Spooler.line.trim(),16);
							}else{
								captureError(jobId,135);
								return false;
							}
							
							Spooler.line = fileReader.readLine();
							Spooler.filePointer++;
							i++;
						}
					}
					
					if(Spooler.line != null && Spooler.line.trim().equals("//END")){
						MemoryManager.storeDataInMemory(programSeg, pcb.sor[0], jobId);
						MemoryManager.storeDataInMemory(waSeg, pcb.sor[3], jobId);
						Spooler.line = fileReader.readLine();
						Spooler.filePointer++;
						return true;
					}else{
						Spooler.line = fileReader.readLine();
						Spooler.filePointer++;
						captureError(jobId,136);
						return false;
					}
				}else{
					isError = captureError(jobId,112);
					return false;
				}
			}else{
				//Invalid Load Module for a given job.
				isError = captureError(jobId,107);
				return false;
			}
		}catch(IOException e){
			throw new ErrorHandler(100);
		}
	}
	
	/**
	 * This method gathers the system level information for each job.
	 * @param execSize
	 * @param inputSize
	 * @param outputSize
	 * @param waSize
	 */
	public static void gatherSystemInfo(int execSize, int inputSize, int outputSize, int waSize){
		//Gather executable segment Size
		SystemInfo.maxCodeSeg = Math.max(SystemInfo.maxCodeSeg,execSize);
		if(SystemInfo.minCodeSeg==0){
			SystemInfo.minCodeSeg = execSize;
		}else{
			SystemInfo.minCodeSeg = Math.min(SystemInfo.minCodeSeg, execSize);
		}
		SystemInfo.totalCodeSeg+=execSize;
		
		//Gather input segment Size
		SystemInfo.maxInputSeg = Math.max(SystemInfo.maxInputSeg,inputSize);
		if(SystemInfo.minInputSeg==0){
			SystemInfo.minInputSeg = inputSize;
		}else{
			SystemInfo.minInputSeg = Math.min(SystemInfo.minInputSeg, inputSize);
		}
		SystemInfo.totalInputSeg+=inputSize;
		
		//Gather Output segment Size
		SystemInfo.maxOutputSeg = Math.max(SystemInfo.maxOutputSeg,outputSize);
		if(SystemInfo.minOutputSeg==0){
			SystemInfo.minOutputSeg = outputSize;
		}else{
			SystemInfo.minOutputSeg = Math.min(SystemInfo.minOutputSeg, outputSize);
		}
		SystemInfo.totalOutputSeg+=outputSize;
		
		//Gather Work Area segment Size
		SystemInfo.maxWaSeg = Math.max(SystemInfo.maxWaSeg,waSize);
		if(SystemInfo.minWaSeg==0){
			SystemInfo.minWaSeg = waSize;
		}else{
			SystemInfo.minWaSeg = Math.min(SystemInfo.minWaSeg, waSize);
		}
		SystemInfo.totalWaSeg+=waSize;

		//Gather Total Size of the Job
		int totalSizeOfJob = execSize + inputSize + outputSize + waSize;
		SystemInfo.maxMemory = Math.max(SystemInfo.maxMemory,totalSizeOfJob);
		if(SystemInfo.minMemory==0){
			SystemInfo.minMemory = totalSizeOfJob;
		}else{
			SystemInfo.minMemory = Math.min(SystemInfo.minMemory, totalSizeOfJob);
		}
		SystemInfo.totalMemory+=totalSizeOfJob;
		
		
	}
}
