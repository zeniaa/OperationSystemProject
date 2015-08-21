package com.okstate.edu;

/**
 *  @Name: Zenia Arora
 * 	@Date : 04-28-2015
 * 
 * 	@Description:
 * 			This routine is used to input spool jobs from the Input stream.
 * It reads the input stream line by line, validates each line. On validation 
 * success, it proceeds to the next line, else it reports the error and then move
 * to the next line.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class Spooler {
	
	public static int interJobId = 1;
	public static int filePointer = 0;
	public static TreeMap<String,Long[]> diskInput = new TreeMap<String, Long[]>();
	public static TreeMap<String,Long[]> diskOutput  = new TreeMap<String, Long[]>();
	
	public ArrayList<String> spoolingErrors = new ArrayList<String>();
	public ArrayList<String> spoolingWarning = new ArrayList<String>();
	boolean eof = false;
	
	public static int maxDegree;
	public static int quantum;
	boolean config = false;
	
	public static int spoolingEventTracker = 0;
	static String line = null;
	
	/**
	 * Spooling of the input stream begins here.
	 * @param fileName
	 * @return
	 * @throws ErrorHandler
	 */
	public boolean startSpooling(String fileName) throws ErrorHandler{
		
		boolean flag = true;
		
		try{
			File inputFile = new File(fileName);
			
			SystemInfo.streamName = inputFile.getName();
			
			//Check if the input stream exists
			if(inputFile.exists()){
				BufferedReader fileReader = new BufferedReader(new FileReader(inputFile));
				
				//move the file pointer to the location where it has been left in earlier batch reading. 
				for(int i=1; i<filePointer ; i++){
					line = fileReader.readLine();
				}
				
				boolean jobTag = false;
				boolean endTag = false;
				
				if(line == null || !(line.contains("//JOB") || line.contains("//CONFIG"))){
					line = fileReader.readLine();
					filePointer++;
				}
				
				//Keep reading the stream line by line.
				while(line!= null && flag && !eof){
					spoolingErrors.clear();
					//Check if the tag is CONFIG
					if(line.contains("//CONFIG")){
						String[] tuples = line.trim().split(" ");
						if(tuples.length == 3){
							if(!tuples[0].equals("//CONFIG")){
								//record error and go to next line
								spoolingErrors.add(ErrorHandler.recordError(124));
								line = fileReader.readLine();
								filePointer++;
								ErrorHandler.writeErrors(spoolingErrors);
								continue;
							}
							try{
								if(tuples[1].length() != 2){
									//Max degree should be 2 digit HEX
									spoolingErrors.add(ErrorHandler.recordError(127));
									line = fileReader.readLine();
									filePointer++;
									ErrorHandler.writeErrors(spoolingErrors);
									continue;
								}
								if(tuples[2].length() != 4){
									//Quantum value should be 4 digit HEX
									spoolingErrors.add(ErrorHandler.recordError(128));
									line = fileReader.readLine();
									filePointer++;
									ErrorHandler.writeErrors(spoolingErrors);
									continue;
								}
								maxDegree = Integer.parseInt(tuples[1],16);
								quantum = Integer.parseInt(tuples[2],16);
								config = true;
							}catch(NumberFormatException e){
								//CONFIG tag values not in HEX.
								spoolingErrors.add(ErrorHandler.recordError(125));
								line = fileReader.readLine();
								filePointer++;
								ErrorHandler.writeErrors(spoolingErrors);
								continue;
							}
							//Show warning when max degree is given greater than 16 in input stream.
							if(maxDegree>16){
								spoolingWarning.add("WARNING: Given Max degree is greater than 16. Max degree is set to 16.");
								ErrorHandler.writeErrors(spoolingWarning);
								spoolingWarning.clear();
								maxDegree = 16;
							}
							line = fileReader.readLine();
							filePointer++;
						}else{
							//Invalid CONFIG tag.
							spoolingErrors.add(ErrorHandler.recordError(124) + " : "+line);
							line = fileReader.readLine();
							filePointer++;
							ErrorHandler.writeErrors(spoolingErrors);
						}
					}
					//Check if tag is JOB and after the CONFIG file. 
					if(config){
						if(endTag && !line.contains("//JOB")){
							endTag = false;
							spoolingErrors.add(ErrorHandler.recordError(137));
							ErrorHandler.writeErrors(spoolingErrors);
						}
						if(Memory.pcbs.size() >= maxDegree){
							endTag = false;
							flag = false;
						}
						else if(line.contains("//JOB")){
							spoolingEventTracker++;
							jobTag = true;
							endTag = false;
							flag = this.validateJob(fileReader);
						}else{
							endTag = false;
							if(line != null && line.contains("//END")){
								endTag = true;
							}
							line = fileReader.readLine();
							filePointer++;
						}
					}else{
						//Missing CONFIG tag.
						spoolingErrors.add(ErrorHandler.recordError(126));
						line = fileReader.readLine();
						filePointer++;
						ErrorHandler.writeErrors(spoolingErrors);
					}
					
				}
				
				if(line == null){
					eof = true;
				}
				
				fileReader.close();
				
			}else{
				//Input stream not found.
				throw new ErrorHandler(101);
			}
			
		}catch(IOException e){
			throw new ErrorHandler(100);
		}
		return eof;
	}
	
	/**
	 * This method validates the //JOB tag from the input stream.
	 * @param fileReader
	 * @return
	 * @throws ErrorHandler
	 */
	public boolean validateJob(BufferedReader fileReader) throws ErrorHandler{
		boolean flag = true;
		try{
			spoolingErrors.clear();
			String[] tuples = line.trim().split(" ");
			if(tuples.length == 4){
				int inputSize = 0;
				int outputSize = 0;
				if(!tuples[0].equals("//JOB")){
					//Invalid JOB tag. 129
					spoolingErrors.add(ErrorHandler.recordError(129) + ": "+ line);
				}
				if(tuples[1].length()!=4){
					//JobId must be of 4 chars. 131
					spoolingErrors.add(ErrorHandler.recordError(131) + ": "+ line);
				}
				if(tuples[2].length() != 2){
					//Input spool request must be of 2 chars. 132
					spoolingErrors.add(ErrorHandler.recordError(132) + ": "+ line);
				}
				if(tuples[3].length() != 2){
					//Output spool request must be of 2 chars. 133
					spoolingErrors.add(ErrorHandler.recordError(133) + ": "+ line);
				}
				try{
					inputSize = Integer.parseInt(tuples[2],16);
					outputSize = Integer.parseInt(tuples[3],16);
				}catch(NumberFormatException e){
					// Input/Output request not in HEX. 134
					spoolingErrors.add(ErrorHandler.recordError(134) + ": "+ line);
				}
				
				if(spoolingErrors.size()>0){
					ErrorHandler.writeErrors(spoolingErrors);
					line = fileReader.readLine();
					filePointer++;
				}else{
					//System assigned ID to the job.
					String jobId = tuples[1] + "_" + interJobId;
					interJobId++;
					// I/O segments on Disk.
					Long[] input = new Long[inputSize];
					Long[] output = new Long[outputSize];
					for(int i=0;i<output.length;i++) 
						output[i] = 0l;
					//Store I/O segments on the Disk
					diskInput.put(jobId, input);
					diskOutput.put(jobId, output);
					
					line = fileReader.readLine();
					filePointer++;
					
					//Read load module and load it into memory.
					flag = Loader.loadJob(fileReader, jobId);
				}
			}else{
				//Invalid JOB tag.
				spoolingErrors.add(ErrorHandler.recordError(129) + line);
				ErrorHandler.writeErrors(spoolingErrors);
				line = fileReader.readLine();
				filePointer++;
			}
		}catch(IOException e){
			throw new ErrorHandler(100);
		}
		
		return flag;
	}
}
