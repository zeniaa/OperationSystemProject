package com.okstate.edu;

/**
 * @author : Zenia Arora
 * @Date : 28-Apr-2015
 * 
 * @Routine : ProcessManager
 *	Description:
 *			This routine takes care of the round robin 
 *			implementation of the process management.
 *			
 */

import java.util.ArrayList;

public class ProcessManager {
	public static boolean contextSwitch = false;
	public static ArrayList<String> waitingQueue = new ArrayList<String>();
	public static boolean hault = false;
	public static boolean IO = false;
	public static boolean error = false;
	
	public CPUManager cpuManager;
	public ProcessManager(){
		cpuManager = new CPUManager();
	}
	
	/**
	 * This function loops infinitely till a job is terminated
	 * either normally or abnormally. This function is responsible
	 * for moving jobs into blocked queue from ready and vice-versa
	 * whenever an interrupt occurs.  
	 */
	public void startProcessing(){
		while(true){
			//Check is ready queue contains any job
			if(CPUManager.readyQueue.size()>0){
				String jobId = CPUManager.readyQueue.get(0);
				CPUManager.readyQueue.remove(0);
				PCB pcb = Memory.pcbs.get(jobId);
				boolean flag = cpuManager.executeProgram(jobId);
				if(!flag){
					//If stopped because of IO interrupt.
					if(IO){
						waitingQueue.add(jobId);
						IO=false;
					}
					//If stopped because of either error or halt instruction.
					else if(hault || error){
						pcb.completionTime = CPU.systemClock;
						if(error){
							checkForInterrupt(pcb);
							SystemInfo.noOfAbnormalJobs++;
							SystemInfo.timeOfAdnormalJobs+= pcb.executionTime;
						}else{
							SystemInfo.noOfNormalJobs++;
						}
						//Write result of job execution to execution_profile file.
						OutputSpooler.generateOutputFile(jobId);
						
						//Gather system level information
						gatherSystemInfoAfterTermination(jobId);
						
						//Release memory for the job
						Memory.removePCB(jobId);
						
						//Reset system level flags.
						ErrorHandler.errorMsg = null;
						hault = false;
						error = false;
						break;
					}
					//If there is a context switch.
					else if(contextSwitch){
						CPUManager.readyQueue.add(jobId);
						contextSwitch = false;
					}
				}
			}
			// If any job waiting and nothing in ready queue, then CPU is idle.
			else if(CPUManager.readyQueue.size() == 0 && waitingQueue.size()>0 ){
				CPU.systemClock++;
				CPU.idleTime++;
			}else{
				break;
			}
			
			//Check if any job is eligible to move to ready queue from waiting queue.
			if(waitingQueue.size() > 0){
				ArrayList<String> tempJobId = new ArrayList<String>();
				for(String jobid: waitingQueue){
					PCB tempPcb = Memory.pcbs.get(jobid);
					if(tempPcb.expectedReadyTime <= CPU.systemClock){
						CPUManager.readyQueue.add(tempPcb.jobId);
						tempJobId.add(tempPcb.jobId);
					}
				}
				for(String jobid: tempJobId){
					waitingQueue.remove(jobid);
				}
			}
			
		}
	}
	
	/**
	 * Check for interrupt. if yes, then log corresponding error.
	 */
	public static void checkForInterrupt(PCB pcb){
		String icCodeBinary = pcb.psw.substring(4, 8);
		int icCode = Integer.parseInt(icCodeBinary, 2);
		if(icCode == 4){
			ErrorHandler.recordError(118);
		}
		else if(icCode == 5){
			ErrorHandler.recordError(119);
		}
		else if(icCode == 6){
			ErrorHandler.recordError(120);
		}
		else if(icCode == 8){
			ErrorHandler.recordError(121);
		}
		else if(icCode == 9){
			ErrorHandler.recordError(122);
		}
	}
	
	/**
	 * This method gathers the system wide information 
	 * of the OS when all jobs are executed.
	 */
	public void gatherSystemInfoAfterTermination(String jobId){
		PCB pcb = Memory.pcbs.get(jobId);
		
		//Gather CPU Time
		SystemInfo.maxCPUTime = Math.max(SystemInfo.maxCPUTime, pcb.executionTime);
		if(SystemInfo.minCPUTime==0){
			SystemInfo.minCPUTime = pcb.executionTime;
		}else{
			SystemInfo.minCPUTime = Math.min(SystemInfo.minCPUTime, pcb.executionTime);
		}
		SystemInfo.totalCPUTime+=pcb.executionTime;
		
		//Gather JOB TAT
		SystemInfo.maxTAT = Math.max(SystemInfo.maxTAT, (pcb.completionTime - pcb.arrivalTime));
		if(SystemInfo.minTAT==0){
			SystemInfo.minTAT = (pcb.completionTime - pcb.arrivalTime);
		}else{
			SystemInfo.minTAT = Math.min(SystemInfo.minTAT, (pcb.completionTime - pcb.arrivalTime));
		}
		SystemInfo.totalTAT += (pcb.completionTime - pcb.arrivalTime);
		
		//Gather CPU Shots
		SystemInfo.maxCPUShots = Math.max(SystemInfo.maxCPUShots, pcb.cpuShots);
		if(SystemInfo.minCPUShots==0){
			SystemInfo.minCPUShots = pcb.cpuShots;
		}else{
			SystemInfo.minCPUShots = Math.min(SystemInfo.minCPUShots, pcb.cpuShots);
		}
		SystemInfo.totalCPUShots += pcb.cpuShots;
		
		//Gather IO requests
		SystemInfo.maxIO = Math.max(SystemInfo.maxIO, pcb.noOfIO);
		if(SystemInfo.minIO==0){
			SystemInfo.minIO = pcb.noOfIO;
		}else{
			SystemInfo.minIO = Math.min(SystemInfo.minIO, pcb.noOfIO);
		}
		SystemInfo.totalIO += pcb.noOfIO;
	}
}
