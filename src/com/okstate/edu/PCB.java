package com.okstate.edu;
/**
 *  @Name: Zenia Arora
 * 	@Date : 04-28-2015
 * 
 * 	@Description:
 * 			This is Process Control Block structure.
 *
 */
public class PCB {
	public String jobId;
	public boolean traceFlag;

	int arrivalTime;
	int completionTime;
	int expectedReadyTime;
	
	int infinityCheck;
	int executionTime;
	
	int totalIoTime;
	int errorHandlingTime;
	int cpuShots;
	int noOfIO;
	
	//SOR registers
	public String[] sor;
	
	//Index registers
	public String[] indexReg;

	//PSW register
	public String psw;
	
	//Instruction register 
	public String instructionReg;
	
	public int diskInputIndex = 0;
	public int diskOutputIndex = 0;
	
	/**
	 * Initialize PCB when it is created.
	 */
	public PCB(){
		sor = new String[4];
		for(int i=0; i<4;i++){
			sor[i] = "00000000000000000000000000000000";
		}
		indexReg = new String[3];
		for(int i=0; i<3;i++){
			indexReg[i] = "00000000000000000000000000000000";
		}
		psw = "00000000000000000000000000000000";
		instructionReg = "00000000000000000000000000000000";
	}
}
