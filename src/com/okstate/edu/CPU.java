package com.okstate.edu;
/**
 * 
 * @Name: Zenia Arora
 * @Date: 04-28-2015
 * @Description:
 * 			This routine is the implementation of CPU. It contains
 *			various registers like SOR, Index Register, PSW and Instruction register.
 *			It also contains function to do binary, decimal and Hex operations.
 *
 */
public class CPU {

	//System Clock
	public static int systemClock = 0;
	
	//System Clock
	public static int idleTime = 0;
		
	//SOR registers
	public static String[] sor;
	
	//Index registers
	public static String[] indexReg;

	//PSW register
	private static String psw;
	
	//Instruction register 
	private static String instructionReg;

	/**
	 * This method initializes all the register with 0's.
	 */
	public static void initializeCPU(){
		
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
	

	public static String getPsw() {
		return psw;
	}

	public static void setPsw(String psw) {
		CPU.psw = psw;
	}

	public static String getInstructionReg() {
		return instructionReg;
	}

	public static void setInstructionReg(String instructionReg) {
		CPU.instructionReg = instructionReg;
	}
	
	/**
	 * This method converts hex number into a binary number
	 * @param hex: value to be converted
	 * @param numberOfBits: Total bits in the result
	 * @return: binary equivalent of input hex.
	 */
	public static String hexToBinary(String hex, int numberOfBits){
		long decimal = Long.parseLong(hex,16);
		String binaryString = Long.toBinaryString(decimal);
		String result = String.format("%"+numberOfBits+"s", binaryString).replace(" ", "0");
		if(result.length()>32){
			result = result.substring(result.length()-32, result.length());
		}
		return result;
	}
	
	/**
	 * This method converts decimal number into a binary number
	 * @param decimal: value to be converted
	 * @param numberOfBits: Total bits in the result
	 * @return: binary equivalent of input decimal.
	 */
	public static String decimalToBinary(long decimal,int numberOfBits){
		String binaryString = Long.toBinaryString(decimal);
		String result = String.format("%"+numberOfBits+"s", binaryString).replace(" ", "0");
		if(result.length()>32){
			result = result.substring(result.length()-32, result.length());
		}
		return result;
	}
	
	/**
	 * This method converts the decimal number to Hex number
	 * @param decimal: Value to be converted.
	 * @param numberOfChars: Total length of the Hex string.
	 * @return: Hex equivalent of input decimal
	 */
	public static String decimalToHex(long decimal, int numberOfChars){
		String value = "";
		if(decimal < 0){
			value = Integer.toHexString((int)decimal);
		}else{
			value = Long.toHexString(decimal);
		}
		String result = String.format("%"+numberOfChars+"s", value).replace(" ", "0");
		if(result.length()>32){
			result = result.substring(result.length()-32, result.length());
		}
		return result;
	}
	
	/**
	 * This method converts the binary number to Hex number
	 * @param decimal: Value to be converted.
	 * @param numberOfChars: Total length of the Hex string.
	 * @return: Hex equivalent of input decimal
	 */
	public static String binaryToHex(String binary, int numberOfChars){
		String result = "";
		if(binary != null && !binary.equals("")){
			if(binary.length() > 32){
				binary = binary.substring(binary.length()-32, binary.length());
			}
			int decimal = (int)Long.parseLong(binary, 2);
			String value ="";
			if(decimal<0){
				value = Integer.toHexString(decimal);
			}else{
				value = Long.toHexString(decimal);
			}
			result = String.format("%"+numberOfChars+"s", value).replace(" ", "0").toUpperCase();
		}
		return result;
	}
	/*
	 * Binary ARITHMATIC OPERATIONS
	 */
	public static String binaryAddition(String binary1,String binary2,int numberOfBits){
		int b1 = (short)Integer.parseInt(binary1, 2);
		int b2 = (short)Integer.parseInt(binary2, 2);
		int b3 = (short)(b1 + b2);
		String result = decimalToBinary(b3, numberOfBits);
		return result;
	}
	
}
