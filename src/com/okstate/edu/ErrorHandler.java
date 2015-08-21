package com.okstate.edu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * 
 * @author: Zenia Arora
 * @Date: 04-28-2015
 * @Description:
 * 		This routine is to handle all the errors. It catches errors as well as
 * 	warnings encountered while the execution of program.
 *
 */

public class ErrorHandler extends Exception {

	private static final long serialVersionUID = 1L;
	private int errorCode = 0;
	public static String terminationErrorMessage = "NONE";
	public static ArrayList<String> warningMessages = new ArrayList<String>();
	public static int errorclock = 0;
	public static String termination = "Normal";
	public static String errorMsg = null;
	
	/*
	 * Variables for Error messages
	 */
	
	private final static String error100 = "ERROR(100): IO Error while loading module.";
	private final static String error101 = "ERROR(101): Job stream not found.";
	
	private final static String error102 = "ERROR(102): First line of load module does not exist.";
	private final static String error103 = "ERROR(103): Insufficient segment lengths in load module.";
	private final static String error104 = "ERROR(104): Segment lengths does not contain 4 hex digits.";
	private final static String error105 = "ERROR(105): Segment lengths are not in hex digits.";
	private final static String error106 = "ERROR(106): Segment length too big to accomodate into SOR";
	private final static String error107 = "ERROR(107): Invalid module format.";
	private final static String error108 = "ERROR(108): Field 1 is not 2 digit Hex number.";
	private final static String error109 = "ERROR(109): Field 1 is not Hex number.";
	private final static String error110 = "ERROR(110): Field 2 must be of same length as specified in Field 1.";
	private final static String error111 = "ERROR(111): Memory address out of range.";
	private final static String error112 = "ERROR(112): Incorrect Module format. Incorrect WIC line format.";
	private final static String error113 = "ERROR(113): Incorrect Module format. WIC given length and actual length mismatch.";
	private final static String error114 = "ERROR(114): Incorrect Module format. Program start address not in HEX.";
	private final static String error115 = "ERROR(115): Invalid input value.";
	private final static String error116 = "ERROR(116): Attempt to read beyond end of input.";
	private final static String error123 = "ERROR(123): Incorrect Module format. Invalid instruction.";
	
	private final static String error117 = "ERROR(117): Memory address fault.";
	private final static String error118 = "ERROR(118): Segment address fault.";
	private final static String error119 = "ERROR(119): Attempt to divide by zero.";
	private final static String error120 = "ERROR(120): Invalid Opcode.";
	private final static String error121 = "ERROR(121): Suspected infinite job.";
	private final static String error122 = "ERROR(122): Index register value not provided.";
	
	private final static String error124 = "ERROR(124): Invalid //CONFIG tag.";
	private final static String error125 = "ERROR(125): //CONFIG tag values not in HEX.";
	private final static String error126 = "ERROR(126): Missing //CONFIG tag.";
	private final static String error127 = "ERROR(127): Max degree should be 2 digit HEX.";
	private final static String error128 = "ERROR(128): Quantum value should be 4 digit HEX.";
	
	private final static String error129 = "ERROR(129): Invalid //JOB tag : ";
	private final static String error130 = "ERROR(130): Improper JOB tag structure. Skipping till next JOB tag.";
	private final static String error131 = "ERROR(131): JobId must be of 4 chars.";
	private final static String error132 = "ERROR(132): Input spool request must be of 2 chars.";
	private final static String error133 = "ERROR(133): Output spool request must be of 2 chars.";
	private final static String error134 = "ERROR(134): Input/Output request not in HEX.";
	
	private final static String error135 = "ERROR(135): Input request size and actual size missmatch.";
	private final static String error136 = "ERROR(136): Missing //END tag.";
	private final static String error137 = "ERROR(137): Missing //JOB tag.";
	
	private final static String error138 = "ERROR(138): Attempt to write beyond end of output segment";
	
	private final static String error139 = "ERROR(139): Error while executing MX instruction.";
	private final static String error140 = "ERROR(140): Error while executing MB instruction.";
	
	private final static String error141 = "ERROR(141): Program segment missing for this job.";
	private final static String error142 = "ERROR(142): Insufficient inputs provided to this job.";
	
	private final static String error143 = "ERROR(143): Invalid or missing //DATA tag.";
	
	private final static String errordefault = "ERROR(111): Some Illegal Activity.";
	
	
	public ErrorHandler(){
		super();
	}
	public ErrorHandler(int code){
		this.errorCode = code;
	}
	
	public int getErrorCode(){
		return errorCode;
	}
	
	/**
	 * This method returns the error message corresponding to the error code.
	 * @param code
	 */
	public void throwExceptionMessage(int code){
		CPU.systemClock+=15;
		errorclock+=15;
		termination = "Abnormal";
		switch(code){
			case 100:
				terminationErrorMessage = error100;
				break;
			case 101:
				terminationErrorMessage = error101;
				break;
			default:
				terminationErrorMessage = errordefault;
		}
	}
	
	/**
	 * This method is returns the error message corresponding to the given error code.
	 * 
	 * @param code: Error code
	 * @return
	 */
	public static String recordError(int code){
		CPU.systemClock+=15;
		errorclock+=15;
		termination = "Abnormal";
		switch(code){
			case 100:
				errorMsg = error100;
				break;
			case 101:
				errorMsg = error101;
				break;
			case 102:
				errorMsg = error102;
				break;
			case 103:
				errorMsg = error103;
				break;
			case 104:
				errorMsg = error104;
				break;
			case 105:
				errorMsg = error105;
				break;
			case 106:
				errorMsg = error106;
				break;
			case 107:
				errorMsg = error107;
				break;
			case 108:
				errorMsg = error108;
				break;
			case 109:
				errorMsg = error109;
				break;
			case 110:
				errorMsg = error110;
				break;
			case 111:
				errorMsg = error111;
				break;
			case 112:
				errorMsg = error112;
				break;
			case 113:
				errorMsg = error113;
				break;
			case 114:
				errorMsg = error114;
				break;
			case 115:
				errorMsg = error115;
				break;
			case 116:
				errorMsg = error116;
				break;
			case 117:
				errorMsg = error117;
				break;
			case 118:
				errorMsg = error118;
				break;
			case 119:
				errorMsg = error119;
				break;
			case 120:
				errorMsg = error120;
				break;
			case 121:
				errorMsg = error121;
				break;
			case 122:
				errorMsg = error122;
				break;
			case 123:
				errorMsg = error123;
				break;
			case 124:
				errorMsg = error124;
				break;
			case 125:
				errorMsg = error125;
				break;
			case 126:
				errorMsg = error126;
				break;
			case 127:
				errorMsg = error127;
				break;
			case 128:
				errorMsg = error128;
				break;
			case 129:
				errorMsg = error129;
				break;
			case 130:
				errorMsg = error130;
				break;
			case 131:
				errorMsg = error131;
				break;
			case 132:
				errorMsg = error132;
				break;
			case 133:
				errorMsg = error133;
				break;
			case 134:
				errorMsg = error134;
				break;
			case 135:
				errorMsg = error135;
				break;
			case 136:
				errorMsg = error136;
				break;
			case 137:
				errorMsg = error137;
				break;
			case 138:
				errorMsg = error138;
				break;
			case 139:
				errorMsg = error139;
				break;
			case 140:
				errorMsg = error140;
				break;
			case 141:
				errorMsg = error141;
				break;
			case 142:
				errorMsg = error142;
				break;
			case 143:
				errorMsg = error143;
				break;
		}
		return errorMsg;
	}
	
	/**
	 * This method writes errors into the execution_profile.
	 * @param errors
	 */
	public static void writeErrors(ArrayList<String> errors){
		File outputFile = new File("execution_profile_"+SystemInfo.streamName);
		try{
			BufferedWriter fileWriter = null;
			if(outputFile.exists()){
				fileWriter = new BufferedWriter(new FileWriter(outputFile,true));
			}else{
				fileWriter = new BufferedWriter(new FileWriter(outputFile));
				outputFile.createNewFile();
			}
			for(String str:errors){
				fileWriter.write(str+"\n");
			}
			fileWriter.flush();
			fileWriter.close();
		}catch(Exception e){
			ErrorHandler.recordError(100);
		}
	}
}
