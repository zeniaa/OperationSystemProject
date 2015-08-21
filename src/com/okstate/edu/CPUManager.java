package com.okstate.edu;

/**
 * @name: Zenia Arora
 * @Date: 04-28-2015
 * 
 * @Routine : CPU Manager
 *	1. Description:
 *			This routine executes the program loaded into the memory.
 *	This routine reads PSW for PC value. Depending on PC value loads next 
 *	instruction into IR, decodes it and then executes it. This routine also
 *	sets the initial values of SOR registers. It generates trace file if 
 *	trace flag is set.
 *			
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CPUManager {

	public static ArrayList<String> readyQueue = new ArrayList<String>();
	public static ArrayList<String> waitingQueue = new ArrayList<String>();
	public final int infinityConst = 100000;
	static int elapsedTime = 0;
	static int eventQuantum = 0;
	
	/**
	 * This method runs into continuous loop and executes loaded program
	 * It loads next instruction to be executed into IR. The it decodes it
	 * and based on the operation executes it. After execution of each instruction,
	 * PC is incremented to address of next instruction. If the trace flag is ON
	 * it generates trace file as well.
	 */
	public boolean executeProgram(String jobId){
		PCB pcb = Memory.pcbs.get(jobId);
		pcb.cpuShots++;
		boolean flag = true;
		
		int quantumTime = Spooler.quantum;
		if(pcb.cpuShots>4 && pcb.cpuShots<=12){
			quantumTime = 2 * Spooler.quantum;
		}else if(pcb.cpuShots>12){
			quantumTime = 4 * Spooler.quantum;
		}
		
		//Record snapshot of system after every 2000 vts
		if(eventQuantum == 0 || eventQuantum <= CPU.systemClock){
			eventQuantum = CPU.systemClock + 2000;
			OutputSpooler.recordSystemEvent(pcb);
		}
		
		//Restore current jobs pcb values into CPU registers.
		restoreCPU(pcb);
		
		//Loop until time quantum is expired or till we come across any error/interrupt.
		while(flag){
			
			//Time slice check for one job
			if(quantumTime < elapsedTime){
				ProcessManager.contextSwitch = true;
				flag = false;
				elapsedTime = 0;
				break;
			}
			
			//Infinite loop check
			if(pcb.infinityCheck > infinityConst){
				StringBuffer value = new StringBuffer(CPU.getPsw());
				value.replace(4, 8, "1000");
				CPU.setPsw(value.toString());
				ProcessManager.error = true;
				flag = false;
				SystemInfo.infiniteLoopJobs.append(jobId+" | ");
				SystemInfo.timeOfInfiniteLoops += pcb.executionTime;
			}
			else{
				this.setIR(jobId);
				//Decode instruction
				
				String binaryOpCode = CPU.getInstructionReg().substring(0, 6);
				int opCode = Integer.parseInt(binaryOpCode,2);
				String hexPSW = CPU.decimalToHex(Long.parseLong(CPU.getPsw(), 2),8);
				//Execute instruction
				flag = InstructionSet.executeInstruction(opCode,CPU.getInstructionReg(), jobId);
				
				if(pcb.traceFlag){
					//Gather data to be written into trace file
					String hexIR = CPU.decimalToHex(Long.parseLong(CPU.getInstructionReg(), 2),8);
					StringBuffer trace = new StringBuffer(hexPSW+ " | " 
							+ hexIR + " || "
							+ InstructionSet.op1Add + " | "
							+ InstructionSet.op1Value + " || "
							+ InstructionSet.op2Add + " | "
							+ InstructionSet.op2Value + " | ");
					generateTraceFile(trace.toString().toUpperCase(), jobId);
				}
				if(opCode != 16){
					incrementPC();
				}
				pcb.infinityCheck++;
				
			}
		}
		
		//Store the current state of CPU into PCB before switch.
		storeCPUToPCB(pcb);
		
		return flag;
	}
	
	/**
	 * This method stores the values from the job PCB 
	 * to CPU registers.
	 * 
	 * @param pcb
	 */
	public void restoreCPU(PCB pcb){
		CPU.sor[0] = pcb.sor[0];
		CPU.sor[1] = pcb.sor[1];
		CPU.sor[2] = pcb.sor[2];
		CPU.sor[3] = pcb.sor[3];
		
		CPU.indexReg[0] = pcb.indexReg[0];
		CPU.indexReg[1] = pcb.indexReg[1];
		CPU.indexReg[2] = pcb.indexReg[2];
		
		CPU.setPsw(pcb.psw);
		CPU.setInstructionReg(pcb.instructionReg);
	}
	
	/**
	 * This method stores the CPU state into the
	 * Job's PCB.
	 * 
	 * @param pcb
	 */
	public void storeCPUToPCB(PCB pcb){
		pcb.sor[0] = CPU.sor[0];
		pcb.sor[1] = CPU.sor[1];
		pcb.sor[2] = CPU.sor[2];
		pcb.sor[3] = CPU.sor[3];
		
		pcb.indexReg[0] = CPU.indexReg[0];
		pcb.indexReg[1] = CPU.indexReg[1];
		pcb.indexReg[2] = CPU.indexReg[2];
		
		pcb.psw = CPU.getPsw();
		pcb.instructionReg = CPU.getInstructionReg();
	}
	
	/**
	 * This method set the next instruction to be executed into IR,
	 * based on the value in PC of PSW
	 * @throws ErrorHandler
	 */
	public void setIR(String jobId){
		String binaryPC = CPU.getPsw().substring(18);
		Integer addressOfNextInst = Integer.parseInt(binaryPC,2);
		String hexInstruction = Memory.readMemory(addressOfNextInst);
		//System.out.println(hexInstruction);
		String binInstruction = CPU.hexToBinary(hexInstruction, 32);
		CPU.setInstructionReg(binInstruction);
	}
	
	/**
	 * This method increments PC and sets it back into PSW.
	 * @throws ErrorHandler
	 */
	public void incrementPC(){
		String binaryPC = CPU.getPsw().substring(18);
		Integer addressOfNextInst = Integer.parseInt(binaryPC,2) + 4;
		
		StringBuffer value = new StringBuffer(CPU.getPsw());
		value.replace(18, 32, CPU.decimalToBinary(addressOfNextInst, 14));
		CPU.setPsw(value.toString());
		
	}
	
	/**
	 * Method to generate trace file.
	 * @param trace
	 * @throws ErrorHandler
	 */
	public static void generateTraceFile(String trace, String jobId){
		try{
			File traceFile = new File(SystemInfo.streamName + "_trace_file_" + jobId);
			if(traceFile.exists()){
				BufferedWriter fileWriter = new BufferedWriter(new FileWriter(traceFile,true));
				fileWriter.write(trace+"\n");
				fileWriter.flush();
				fileWriter.close();
			}else{ 
				BufferedWriter fileWriter = new BufferedWriter(new FileWriter(traceFile));
				traceFile.createNewFile();
				//Header line1
				fileWriter.write(String.format("%42s", "||   OPERAND 1     || ")
						+ String.format("%18s", "   OPERAND 2   ||\n")
						);
				
				//Underline
				StringBuffer underLine = new StringBuffer();
				for(int i=0;i<60;i++){
					underLine.append("-");
				}
				fileWriter.write(underLine.toString()+"\n");
				
				fileWriter.write(String.format("%11s", "PSW   | ")
						+ String.format("%12s", "IR    || ")
						
						+ String.format("%7s", "Addr.| ")
						+ String.format("%12s", "Value  || ")
						
						+ String.format("%7s", "Addr.| ")
						+ String.format("%10s", " Value  ||\n")
						);
				fileWriter.write(String.format("%11s", "HEX   | ")
						+ String.format("%12s", "HEX   || ")
						
						+ String.format("%7s", "HEX  | ")
						+ String.format("%12s", "HEX    || ")
						
						+ String.format("%7s", "HEX  | ")
						+ String.format("%10s", " HEX    ||\n")
						);
				//Underline
				underLine = new StringBuffer();
				for(int i=0;i<60;i++){
					underLine.append("-");
				}
				fileWriter.write(underLine.toString()+"\n");
				fileWriter.write(trace+"\n");
				fileWriter.flush();
				fileWriter.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		
		
	}
}
