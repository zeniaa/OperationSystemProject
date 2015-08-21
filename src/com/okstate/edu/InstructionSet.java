package com.okstate.edu;


/**
 * 
 * @author Zenia Arora
 * @Date 04-28-2015
 * @Description:
 * 		This routine identifies the instruction to be executed.
 * It takes the action as per the Opcode.
 */
public class InstructionSet {

	static int inputCounter = 0;
	static String op1Add = "";
	static String op2Add = "";
	static String op1Value = "";
	static String op2Value = "";
	public static int ioClock = 0;
	
	/**
	 * This method executes the given instruction as per the opcode.
	 * It first determines which type of instruction it is and then 
	 * pass it on to its corresponding method to execute.
	 * 
	 * @param opCode: Opcode of an instruction.
	 * @param instruction: Instruction to be executed.
	 * @param jobId : job for which the instruction to be executed.
	 * @return: true/false
	 */
	public static boolean executeInstruction(int opCode,String instruction, String jobId){
		//variables to be used for trace file
		op1Add = "    ";
		op2Add = "    ";
		op1Value = "        ";
		op2Value = "        ";
		boolean flag = true;
		PCB pcb = Memory.pcbs.get(jobId);
		StringBuffer value = new StringBuffer(CPU.getPsw());
		//Check for Segment address fault
		flag = checkSegmentAddressFault(opCode,instruction);
		if(flag){
			//Check if the instructions type is MM
			if(opCode >= 0 && opCode <= 12){
				long operand1 = 0;
				long operand2 = 0;
				
				int op1Address = getOp1Address(instruction);
				int op2Address = getOp2Address(instruction);
				operand1 = getOpValue(op1Address, jobId);
				operand2 = getOpValue(op2Address, jobId);
				if(ProcessManager.error){
					flag = false;
				}else{
					flag = executeMMSet(opCode, op1Address,op2Address, operand1, operand2, pcb);
					op1Add = CPU.decimalToHex(op1Address, 4);
					op2Add = CPU.decimalToHex(op2Address, 4);
					op1Value = Memory.readMemory(op1Address);
					op2Value = Memory.readMemory(op2Address);
				}
				
			}
			//Check if the instructions type is MB
			else if(opCode == 16){
				flag = executeMBSet(opCode, instruction, pcb);
			}
			//Check if the instructions type is MX
			else if(opCode >= 32 && opCode <= 34){
				
				int indexRegNo = Integer.parseInt(instruction.substring(6, 8),2);
				int op1Address = getOp2Address(instruction);
				if(indexRegNo == 0){
					//Index register value not provided.
					flag = false;
					value.replace(4, 8, "1001");
					CPU.setPsw(value.toString());
				}else{
					long operand2 = getOpValue(op1Address, jobId);
					if(ProcessManager.error){
						flag = false;
					}else{
						flag = executeMXSet(opCode,indexRegNo,operand2, pcb);
					}
				}
				op1Add = CPU.decimalToHex(op1Address, 4);
				op1Value = Memory.readMemory(op1Address);
			}
			//Check if the instructions type is MS
			else if(opCode >= 48 && opCode <= 51){
				int op1Addr = getOp1Address(instruction);
				String op1 = CPU.hexToBinary(Memory.readMemory(op1Addr),32);
				int shift = Integer.parseInt(instruction.substring(27, 32),2);
				flag = executeMSSet(opCode, op1Addr, op1, shift, pcb);
				
				op1Add = CPU.decimalToHex(op1Addr, 4);
				op1Value = Memory.readMemory(op1Addr);
			}
			//HLT instruction
			else if(opCode == 63){
				//HLT
				ProcessManager.hault = true;
				flag = false;
				value.replace(4, 8, "0111");
				CPU.setPsw(value.toString());
			}else{
				//Invalid Opcode
				ProcessManager.error = true;
				flag = false;
				value.replace(4, 8, "0110");
				CPU.setPsw(value.toString());
			}
		}else{
			//Segment address fault
			ProcessManager.error = true;
			value.replace(4, 8, "0100");
			CPU.setPsw(value.toString());
		}
		
		return flag;
	}
	
	/**
	 * Check if the address displacement is within the segment length.
	 * 
	 * @param opCode
	 * @param instruction
	 * @return true/false.
	 */
	public static boolean checkSegmentAddressFault(int opCode, String instruction){
		boolean flag = true;
		//Check displacement for MM type instructions
		if(opCode >=0 && opCode <= 12){
			int segment1 = Integer.parseInt(instruction.substring(6, 8),2);
			int displacement1 = Integer.parseInt(instruction.substring(10, 19), 2);
			int segLength = Integer.parseInt(CPU.sor[segment1].substring(23, 32),2);
			if(displacement1 > segLength){
				flag = false;
			}
			int segment2 = Integer.parseInt(instruction.substring(19, 21),2);
			int displacement2 = Integer.parseInt(instruction.substring(23, 32), 2);
			segLength = Integer.parseInt(CPU.sor[segment2].substring(23, 32),2);
			if(displacement2 > segLength){
				flag = false;
			}
		}
		//Check displacement for MB and MX type instructions
		else if(opCode == 16 || (opCode >= 32 && opCode <=34)){
			int segment2 = Integer.parseInt(instruction.substring(19, 21),2);
			int displacement2 = Integer.parseInt(instruction.substring(23, 32), 2);
			int segLength = Integer.parseInt(CPU.sor[segment2].substring(23, 32),2);
			if(displacement2 > segLength){
				flag = false;
			}
		}
		//Check if the instructions type is MS
		else if(opCode >= 48 && opCode <= 51){
			int segment1 = Integer.parseInt(instruction.substring(6, 8),2);
			int displacement1 = Integer.parseInt(instruction.substring(10, 19), 2);
			int segLength = Integer.parseInt(CPU.sor[segment1].substring(23, 32),2);
			if(displacement1 > segLength){
				flag = false;
			}
		}
		return flag;
	}
	
	/**
	 * Retrieves the address of first operand.
	 */
	public static int getOp1Address(String instruction){
		
		//Fetch address and value of operand 1.
		int segment = Integer.parseInt(instruction.substring(6, 8),2);
		int indexNo = Integer.parseInt(instruction.substring(8, 10),2);
		
		int baseAddress = Integer.parseInt(CPU.sor[segment].substring(0, 14),2);
		int indexValue = 0;
		if(indexNo != 0){
			indexValue = Integer.parseInt(CPU.indexReg[indexNo-1], 2);
		}
		
		int displacement = Integer.parseInt(instruction.substring(10, 19), 2);
		
		int opAddress = baseAddress+indexValue+displacement;
		return opAddress;
	}
	
	/**
	 * Retrieves the address of second operand.
	 */
	public static int getOp2Address(String instruction){
		
		//Fetch address and value of operand 2.
		int segment = Integer.parseInt(instruction.substring(19, 21),2);
		int indexNo = Integer.parseInt(instruction.substring(21, 23),2);
		
		int baseAddress = Integer.parseInt(CPU.sor[segment].substring(0, 14),2);
		int indexValue = 0;
		if(indexNo != 0){
			indexValue = Integer.parseInt(CPU.indexReg[indexNo-1], 2);
		}
		int displacement = Integer.parseInt(instruction.substring(23, 32), 2);
		
		int opAddress = baseAddress+indexValue+displacement;
		return opAddress;
	}
	
	/**
	 * Retrieves the value for and operand at given address.
	 */
	public static long getOpValue(int opAddress,String jobId){
		long value = 0;
		int remainder = opAddress % 4;
		int wordAddress = opAddress - remainder;
		String word = Memory.readMemory(wordAddress);
		//String[] bytes = word.split("(?<=\\G.{2})");
		value = (int)Long.parseLong(word!=null?word:"0",16);
		return value;
	}
	
	/**
	 * Writes the value of operand at a given address.
	 */
	public static boolean setOpValue(int opAddress, String result){
		return Memory.writeMemory(opAddress, result);
	}
	
	/**
	 *  Executes the MM type instruction.
	 * @param opCode
	 * @param op1a: Address of 1st operand.
	 * @param op2a: Address of 2nd operand.
	 * @param op1: Value of operand 1.
	 * @param op2: Value of operand 2.
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean executeMMSet(int opCode, int op1a, int op2a, long op1, long op2, PCB pcb){
		boolean flag = true;
		String hexResult = null;
		CPU.systemClock+=4;
		pcb.executionTime+=4;
		CPUManager.elapsedTime+=4;
		StringBuffer value = new StringBuffer(CPU.getPsw());
		switch(opCode){
			case 0://NOP
				flag = true;
				break;
			case 1://ADD
				op1 = op1 + op2;
				hexResult = CPU.decimalToHex(op1, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 2://AND
				op1 = op1 & op2;
				hexResult = CPU.decimalToHex(op1, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 3://Compare (C)
				long result = op1 - op2;
				value = new StringBuffer(CPU.getPsw());
				if(result > Integer.MAX_VALUE || result < Integer.MIN_VALUE ){
					value.replace(0, 4, "0001");
					ErrorHandler.warningMessages.add("Overflow. Potential loss of bits.");
				}else if(result == 0){
					value.replace(0, 4, "1000");
				}else if(result < 0){
					value.replace(0, 4, "0100");
				}else if(result > 0){
					value.replace(0, 4, "0010");
				}
				CPU.setPsw(value.toString());
				flag = true;
				break;
			case 4:// Logical Compare (CL)
				value = new StringBuffer(CPU.getPsw());
				if(op1 == op2){
					value.replace(0, 4, "1000");
				}else if(op1 < op2){
					value.replace(0, 4, "0100");
				}else if(op1 > op2){
					value.replace(0, 4, "0010");
				}
				CPU.setPsw(value.toString());
				flag = true;
				break;
			case 5://DIV
				if(op2 == 0){
					value.replace(4, 8, "0101");
					CPU.setPsw(value.toString());
					ProcessManager.error = true;
					flag = false;
				}else{
					op1 = op1 / op2;
					hexResult = CPU.decimalToHex(op1, 8);
					flag = setOpValue(op1a, hexResult);
				}
				break;
			case 6://MLT
				op1 = op1 * op2;
				hexResult = CPU.decimalToHex(op1, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 7://MOD
				if(op2 == 0){
					value.replace(4, 8, "0101");
					CPU.setPsw(value.toString());
					ProcessManager.error = true;
					flag = false;
				}else{
					op1 = op1 % op2;
					hexResult = CPU.decimalToHex(op1, 8);
					flag = setOpValue(op1a, hexResult);
				}
				break;
			case 8://MOV
				op2 = (int) op2;
				hexResult = CPU.decimalToHex(op2, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 9://OR
				op1 = op1 | op2;
				hexResult = CPU.decimalToHex(op1, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 10://RD
				try{
					Long[] inputs = Spooler.diskInput.get(pcb.jobId);
					if(inputs[pcb.diskInputIndex] == null){
						ErrorHandler.recordError(142);
						ProcessManager.error = true;
					}else{
						String input = CPU.decimalToHex(inputs[pcb.diskInputIndex], 8);
						flag = setOpValue(op2a, input);
						pcb.diskInputIndex++;
						pcb.expectedReadyTime = CPU.systemClock + 10;
						pcb.executionTime+=10;
						pcb.noOfIO++;
						ioClock+=10;
						ProcessManager.IO = true;
					}
				}catch(IndexOutOfBoundsException e){
					ErrorHandler.recordError(116);
					ProcessManager.error = true;
				}
				flag = false;
				break;
			case 11://SUB
				op1 = op1 - op2;
				hexResult = CPU.decimalToHex(op1, 8);
				flag = setOpValue(op1a, hexResult);
				break;
			case 12://WR
				try{
					Long[] outputs = Spooler.diskOutput.get(pcb.jobId);
					outputs[pcb.diskOutputIndex] = op2;
					
					pcb.diskOutputIndex++;
					pcb.expectedReadyTime = CPU.systemClock + 10;
					pcb.executionTime+=10;
					pcb.noOfIO++;
					ioClock+=10;
					ProcessManager.IO = true;
				}catch(IndexOutOfBoundsException e){
					ErrorHandler.recordError(138);
					ProcessManager.error = true;
				}
				flag = false;
				break;
		}
		
		return flag;
	}
	
	/**
	 * Executes instruction of type MB.
	 * @param opCode
	 * @param instruction
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean executeMBSet(int opCode, String instruction, PCB pcb){
		boolean flag = true;
		boolean check = false;
		CPU.systemClock+=2;
		CPUManager.elapsedTime+=2;
		pcb.executionTime+=2;
		try{
			switch(opCode){
			case 16://BC
				String mask = instruction.substring(6, 10);
				String cc = CPU.getPsw().substring(0, 4);
				int opAddress = getOp2Address(instruction);
				String binAddress = CPU.decimalToBinary(opAddress, 14);
				StringBuffer value = new StringBuffer(CPU.getPsw());
				if(mask.equals("1111")){
					value.replace(18, 32, binAddress);
					CPU.setPsw(value.toString());
					check = true;
				}else{
					//In case of branching set the PC value to new location.
					for(int i=0;i<4;i++){
						if(mask.charAt(i) == '1' && cc.charAt(i) == '1'){
							value.replace(18, 32, binAddress);
							CPU.setPsw(value.toString());
							check = true;
							break;
						}
					}
				}
				//If there is no branching, increment the value of PC by 4
				if(!check){
					String binaryPC = CPU.getPsw().substring(18);
					Integer addressOfNextInst = Integer.parseInt(binaryPC,2) + 4;
					value = new StringBuffer(CPU.getPsw());
					value.replace(18, 32, CPU.decimalToBinary(addressOfNextInst, 14));
					CPU.setPsw(value.toString());
				}
				
				op1Add = CPU.decimalToHex(opAddress, 4);
				op1Value = Memory.readMemory(opAddress);
				
				flag = true;
				break;
			}
		}catch(Exception e){
			ErrorHandler.recordError(140);
			flag = false;
			ProcessManager.error = false;
		}
		
		return flag;
	}
	
	/**
	 * Execute the MX type instructions
	 * @param opCode
	 * @param indexRegNo
	 * @param op1
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean executeMXSet(int opCode, int indexRegNo, long op1, PCB pcb){
		boolean flag = true;
		int result = 0;
		CPU.systemClock+=3;
		CPUManager.elapsedTime+=3;
		pcb.executionTime+=3;
		try{
			switch(opCode){
			case 32://LD
				CPU.indexReg[indexRegNo-1] = CPU.decimalToBinary(op1, 32);
				flag = true;
				break;
			case 33://XADD
				result = Integer.parseInt(CPU.indexReg[indexRegNo-1],2) + Integer.parseInt(CPU.decimalToBinary(op1, 32),2);
				CPU.indexReg[indexRegNo-1] = CPU.decimalToBinary(result, 32);
				flag = true;
				break;
			case 34://XSUB
				result = Integer.parseInt(CPU.indexReg[indexRegNo-1],2) - Integer.parseInt(CPU.decimalToBinary(op1, 32),2);
				CPU.indexReg[indexRegNo-1] = CPU.decimalToBinary(result, 32);
				flag = true;
				break;
		}
		}catch(Exception e){
			ErrorHandler.recordError(139);
			flag = false;
			ProcessManager.error = false;
		}
		return flag;
	}
	
	/**
	 * Executes the MS type instruction
	 * @param opCode
	 * @param op1a: address of operand 1
	 * @param op1: value of operand 1
	 * @param shift: shift by bits
	 * @return
	 * @throws ErrorHandler
	 */
	public static boolean executeMSSet(int opCode, int op1a, String op1, int shift, PCB pcb){
		boolean flag = true;
		CPU.systemClock+=3;
		CPUManager.elapsedTime+=3;
		pcb.executionTime+=3;
		long result = 0l;
		StringBuffer value = new StringBuffer();
		switch(opCode){
			case 48://SLC
				value.append(op1.substring(shift));
				value.append(op1.substring(0,shift));
				flag = Memory.writeMemory(op1a,CPU.binaryToHex(value.toString(), 8));
				break;
			case 49://SL
				result = Long.parseLong(op1, 2) << shift;
				flag = Memory.writeMemory(op1a,CPU.decimalToHex(result, 8));
				break;
			case 50://SRC
				value.append(op1.substring(op1.length() - shift));
				value.append(op1.substring(0,op1.length() - shift));
				flag = Memory.writeMemory(op1a,CPU.binaryToHex(value.toString(), 8));
				break;
			case 51://SR
				result = Long.parseLong(op1, 2) >> shift;
				flag = Memory.writeMemory(op1a,CPU.decimalToHex(result, 8));
				break;
		}
		return flag;
	}
	
	
	
}
