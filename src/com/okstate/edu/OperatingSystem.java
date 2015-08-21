package com.okstate.edu;

/**
 *  @Name: Zenia Arora
 * 	@Course # : CS-5323
 * 	@Assignment : Design and implementation of a simplified multiprogrammed batch Operating System.
 * 	@Date : 04-28-2015
 * 
 * 	@Description:
 * 			OperatingSystem is a routine which starts up the simulator OS. First it creates the 
 *			virtual hardware i.e Memory & CPU. After creation it initializes these hardwares with
 *			default values. It reads the input module name from the command line argument.
 *			The functionality of individual method is explained in the comments above methods.
 *
 */

public class OperatingSystem {

	Spooler spool;
	CPUManager cpuManager;
	ProcessManager pm;
	public OperatingSystem(){
		spool = new Spooler();
		cpuManager = new CPUManager();
		pm = new ProcessManager();
	}
	
	public static void main(String[] args) {
		//Check if the module name is provided into command line parameter
		if(args.length == 1){
			String fileName = args[0];
			
			OperatingSystem os = new OperatingSystem();
			try{
				os.initializeMemoryAndCPU();
				os.beginOS(fileName);
			}catch(ErrorHandler e){
				e.throwExceptionMessage(e.getErrorCode());
				OutputSpooler.writeInputSpoolingEvents(new StringBuffer(e.terminationErrorMessage));
			}
		}
		//Error if no module name is provided.
		else if (args.length==0){
			System.out.println("Please provide name of input stream.");
		}
		// Error if more than one input parameters provided.
		else{
			System.out.println("Too many input parameters to the program.");
		}
	}
	
	/**
	 * Start the virtual operating system to spool and load form job stream.
	 * After loading it starts processing loaded jobs. If there are more jobs
	 * to spool, it spools and loads them once enough memory is available to load.
	 * @param fileName
	 * @throws ErrorHandler
	 */
	public void beginOS(String fileName) throws ErrorHandler{
		boolean eof = false;
		while(!eof || CPUManager.readyQueue.size()>0 || ProcessManager.waitingQueue.size()>0){
			if(!eof){
				eof = spool.startSpooling(fileName);
			}
			pm.startProcessing();
		}
		OutputSpooler.writeSystemLevelInfoOnTermination();
	}
	
	/**
	 * Initialize memory and CPU
	 */
	public void initializeMemoryAndCPU(){
		Memory.initialize();
		CPU.initializeCPU();
	}

}
