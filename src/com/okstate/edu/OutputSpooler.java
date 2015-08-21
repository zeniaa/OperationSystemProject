package com.okstate.edu;

/**
 *  @Name: Zenia Arora
 * 	@Date : 04-28-2015
 * 	@Description:
 * 			This routine is to spool output into the execution_profile.
 * 	This routine writes, errors, events, outputs and system level information
 * into the execution profile.
 *
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class OutputSpooler {

	/**
	 * This method writes the output of a job into 
	 * the execution profile.
	 * @param jobId
	 */
	public static void generateOutputFile(String jobId){
		PCB pcb = Memory.pcbs.get(jobId);
		File outPutFile = new File("execution_profile_"+SystemInfo.streamName);
		BufferedWriter fileWriter = null;
		try{
			if(!outPutFile.exists()){
				outPutFile.createNewFile();
				fileWriter = new BufferedWriter(new FileWriter(outPutFile));
			}else{
				fileWriter = new BufferedWriter(new FileWriter(outPutFile,true));
			}
		
			fileWriter.write("---------------------------------------------------\n");
			
			fileWriter.write("Job Id : "+jobId+"\n");
			fileWriter.write("Clock: "+CPU.systemClock+" (DEC)\n");
			fileWriter.write("Execution Time: " + pcb.executionTime + " (DEC)\n");
			fileWriter.write("TAT: " + (pcb.completionTime - pcb.arrivalTime) + " (DEC)\n");
			if(ProcessManager.error){
				fileWriter.write(ErrorHandler.errorMsg+"\n");
			}
			
			fileWriter.write("Memory Executable segment (HEX): \n");
			ArrayList<String> segment = getSegmentData(pcb, 0);
			for(String data:segment){
				fileWriter.write("\t"+data+"\n");
			}
			fileWriter.write(" Memory Input segment (HEX): \n");
			segment = getSegmentData(pcb, 1);
			for(String data:segment){
				fileWriter.write("\t"+data+"\n");
			}
			fileWriter.write("Memory Output segment (HEX): \n");
			segment = getSegmentData(pcb, 2);
			for(String data:segment){
				fileWriter.write("\t"+data+"\n");
			}
			fileWriter.write("Work Area segment (HEX): \n");
			segment = getSegmentData(pcb, 3);
			for(String data:segment){
				fileWriter.write("\t"+data+"\n");
			}
			
			fileWriter.write("Disk Output (DEC) : \n");
			Long[] output = Spooler.diskOutput.get(jobId);
			for(long out : output){
				fileWriter.write("\t" + out + "\n");
			}
			
			fileWriter.write("---------------------------------------------------\n");
			fileWriter.flush();
			fileWriter.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This method returns the contents of the segment, for a given job.
	 * @param pcb
	 * @param segNo
	 * @return
	 */
	public static ArrayList<String> getSegmentData(PCB pcb,int segNo){
		ArrayList<String> segmentData = new ArrayList<String>();
		int startAddress = Integer.parseInt(pcb.sor[segNo].substring(0, 14),2);
		int length = Integer.parseInt(pcb.sor[segNo].substring(23, 32),2);
		int endAddress = startAddress + length;
		for(int i=startAddress;i<endAddress;i=i+4){
			segmentData.add(Memory.readMemory(i));
		}
		return segmentData;
	}
	
	/**
	 * This method writes the events during the input spooling.
	 * @param data
	 */
	public static void writeInputSpoolingEvents(StringBuffer data){
		File outPutFile = new File("execution_profile_"+SystemInfo.streamName);
		BufferedWriter fileWriter = null;
		try{
			if(!outPutFile.exists()){
				outPutFile.createNewFile();
				fileWriter = new BufferedWriter(new FileWriter(outPutFile));
			}else{
				fileWriter = new BufferedWriter(new FileWriter(outPutFile,true));
			}
			fileWriter.write(data.toString()+"\n");
			fileWriter.flush();
			fileWriter.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This method records the system level events at
	 * regular intervals.
	 * @param pcb
	 */
	public static void recordSystemEvent(PCB pcb){
		File outPutFile = new File("execution_profile_"+SystemInfo.streamName);
		BufferedWriter fileWriter = null;
		try{
			if(!outPutFile.exists()){
				outPutFile.createNewFile();
				fileWriter = new BufferedWriter(new FileWriter(outPutFile));
			}else{
				fileWriter = new BufferedWriter(new FileWriter(outPutFile,true));
			}
			
			fileWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
			
			fileWriter.write("Running Job : "+pcb.jobId+"\n");
			fileWriter.write("Ready queue : \n");
			for(String job: CPUManager.readyQueue){
				fileWriter.write(job+" | ");
			}
			fileWriter.write("\n");
			fileWriter.write("Blocked queue : \n");
			for(String job: ProcessManager.waitingQueue){
				fileWriter.write(job+" | ");
			}
			fileWriter.write("\n");
			
			boolean startAddress = false;
			boolean endAddress = false;
			int beginAt = 0;
			int endsAt = 0;
			int lastAdd = 0;
			TreeMap<Integer,Integer> freeBlocks = new TreeMap<Integer,Integer>();
			for(int key : Memory.fbmv.keySet()){
				int bit = Memory.fbmv.get(key);
				if(bit == 0 && !startAddress){
					startAddress = true;
					endAddress = false;
					beginAt = key;
				}
				else if(bit == 1 && startAddress){
					startAddress = false;
					endAddress = true;
					endsAt = key;
					freeBlocks.put(beginAt, (endsAt-beginAt));
				}
				lastAdd = key+4;
			}
			if(!endAddress){
				freeBlocks.put(beginAt, (lastAdd-beginAt));
			}
			
			fileWriter.write("Free Blocks: \n");
			fileWriter.write("\tAddr \t|\tSize(bytes)\n");
			for(int key : freeBlocks.keySet()){
				fileWriter.write("\t"+ String.format("%"+5+"s", key) + "\t|\t"+freeBlocks.get(key)+"\n");
			}
			
			startAddress = false;
			endAddress = false;
			beginAt = 0;
			endsAt = 0;
			lastAdd = 0;
			TreeMap<Integer,Integer> usedBlocks = new TreeMap<Integer,Integer>();
			for(int key : Memory.fbmv.keySet()){
				int bit = Memory.fbmv.get(key);
				if(bit == 1 && !startAddress){
					startAddress = true;
					endAddress = false;
					beginAt = key;
				}
				else if(bit == 0 && startAddress){
					startAddress = false;
					endAddress = true;
					endsAt = key;
					usedBlocks.put(beginAt, (endsAt-beginAt));
				}
				lastAdd = key;
			}
			if(!endAddress){
				usedBlocks.put(beginAt, (lastAdd-beginAt));
			}
			
			fileWriter.write("Occupied Blocks: \n");
			fileWriter.write("\tAddr \t|\tSize(bytes)\n");
			for(int key : usedBlocks.keySet()){
				fileWriter.write("\t"+ String.format("%"+5+"s", key) + "\t|\t"+usedBlocks.get(key)+"\n");
			}
			
			fileWriter.write("Current degree of multiprogramming: " + Spooler.maxDegree +"\n");
			
			fileWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
			
			fileWriter.flush();
			fileWriter.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This method writes the system level information after 
	 * all the jobs in the input stream are processed.
	 */
	public static void writeSystemLevelInfoOnTermination(){
		File outPutFile = new File("execution_profile_"+SystemInfo.streamName);
		BufferedWriter fileWriter = null;
		try{
			if(!outPutFile.exists()){
				outPutFile.createNewFile();
				fileWriter = new BufferedWriter(new FileWriter(outPutFile));
			}else{
				fileWriter = new BufferedWriter(new FileWriter(outPutFile,true));
			}
			
			int totalJobs = SystemInfo.noOfAbnormalJobs + SystemInfo.noOfNormalJobs;
			
			fileWriter.write("Clock(vts) : "+CPU.systemClock+" (DEC)\n");
			fileWriter.write("Jobs Processed : "+totalJobs+"\n");
			
			fileWriter.write("CPU Time (vts): (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxCPUTime+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minCPUTime+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalCPUTime/totalJobs)+"\n");
			
			fileWriter.write("Turn-around Time (vts): (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxTAT+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minTAT+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalTAT/totalJobs)+"\n");
			
			fileWriter.write("Code Segment Size : (DEC) \n");
			fileWriter.write("\tMax :"+SystemInfo.maxCodeSeg+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minCodeSeg+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalCodeSeg/totalJobs)+"\n");
			
			fileWriter.write("Input Segment Size : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxInputSeg+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minInputSeg+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalInputSeg/totalJobs)+"\n");
			
			fileWriter.write("Output Segment Size : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxOutputSeg+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minOutputSeg+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalOutputSeg/totalJobs)+"\n");
			
			fileWriter.write("Work Area Segment Size : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxWaSeg+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minWaSeg+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalWaSeg/totalJobs)+"\n");
			
			fileWriter.write("Memory Used by a job : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxMemory+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minMemory+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalMemory/totalJobs)+"\n");
			
			fileWriter.write("CPU Shots : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxCPUShots+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minCPUShots+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalCPUShots/totalJobs)+"\n");
			
			fileWriter.write("IO Request : (DEC)\n");
			fileWriter.write("\tMax :"+SystemInfo.maxIO+"\n");
			fileWriter.write("\tMin :"+SystemInfo.minIO+"\n");
			fileWriter.write("\tAvg :"+(SystemInfo.totalIO/totalJobs)+"\n\n");
			
			fileWriter.write("CPU idle time (vts) :"+CPU.idleTime+" (DEC)\n");
		
			fileWriter.write("No. of jobs normally terminated: "+SystemInfo.noOfNormalJobs+" (DEC)\n");
			fileWriter.write("No. of jobs abnormally terminated: "+SystemInfo.noOfAbnormalJobs+" (DEC)\n");
			
			fileWriter.write("Time lost due to Abnormal jobs: "+SystemInfo.timeOfAdnormalJobs+" (DEC)\n");
			fileWriter.write("Time lost due to Infinite loop jobs: "+SystemInfo.timeOfInfiniteLoops+" (DEC)\n");
			
			fileWriter.write("Infinite loop jobs: "+SystemInfo.infiniteLoopJobs+"\n");
			
			fileWriter.flush();
			fileWriter.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
}
