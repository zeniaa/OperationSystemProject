package com.okstate.edu;
/**
 *  @Name: Zenia Arora
 * 	@Date : 04-28-2015
 * 
 * 	@Description:
 * 			This routine stores the information need at the system level.
 *
 */

public class SystemInfo {
		
	public static String streamName;
	
	public static int maxCPUTime;
	public static int minCPUTime;
	public static int totalCPUTime;
	
	public static int maxTAT;
	public static int minTAT;
	public static int totalTAT;
	
	public static int maxCodeSeg;
	public static int minCodeSeg;
	public static int totalCodeSeg;
	
	public static int maxInputSeg;
	public static int minInputSeg;
	public static int totalInputSeg;
	
	public static int maxOutputSeg;
	public static int minOutputSeg;
	public static int totalOutputSeg;
	
	public static int maxWaSeg;
	public static int minWaSeg;
	public static int totalWaSeg;
	
	public static int maxMemory;
	public static int minMemory;
	public static int totalMemory;
	
	public static int maxCPUShots;
	public static int minCPUShots;
	public static int totalCPUShots;
	
	public static int maxIO;
	public static int minIO;
	public static int totalIO;
	
	public static int noOfNormalJobs;
	public static int noOfAbnormalJobs;
	
	public static int timeOfAdnormalJobs;
	public static int timeOfInfiniteLoops;
	
	public static StringBuffer infiniteLoopJobs = new StringBuffer();
}
