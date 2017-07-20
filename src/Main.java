import java.util.*;

public class Main {
	public HashMap gemmaNew = new HashMap();
	public HashMap graph = new HashMap(); // will save all graphs
	ArrayList input = new ArrayList();
	ArrayList seed = new ArrayList();
	ArrayList test = new ArrayList();
	ArrayList lmCorpusSrc = new ArrayList();
	ArrayList lmCorpusTar = new ArrayList();
	
	public HashMap multigram = new HashMap();
	

	public static void main(String[] args) {

		Main obj = new Main();
//		obj.callDecoder();
		
		ArrayList input = new ArrayList();
		
		//obj.operationLM(args[0]);
		//obj.callNoise();
		
		String parameters = obj.argumentList(args, input);
		String [] paramStrings = parameters.split(" ");
				
		if(Integer.parseInt(paramStrings[0]) == 0){
			// aligner
		}
		else if(Integer.parseInt(paramStrings[0]) == 1)
		{
			//unsupervised
			// 1 + inputFile + " " + testFile + " " + threshold + " " + smoothingParameter + " " 
			//+ outputTiRules + " " + unsuperIter;
			obj.operationInput(paramStrings[1]);
			obj.operationTest(paramStrings[2]);
			
			Double threshold = Double.parseDouble(paramStrings[3]);
			Double smoothingP = Double.parseDouble(paramStrings[4]);
			int tiRules = Integer.parseInt(paramStrings[5]);
			int iteration = Integer.parseInt(paramStrings[6]);
			int charAlign = Integer.parseInt(paramStrings[7]);
			obj.operationLMSrc(paramStrings[8]);
			obj.operationLMTar(paramStrings[9]);
			int wbsmoothing = Integer.parseInt(paramStrings[10]);
			int ngramOrder = Integer.parseInt(paramStrings[11]);
			int nontiOrder = Integer.parseInt(paramStrings[12]);
			
			
			obj.callUnsupervised(threshold, smoothingP, tiRules, iteration, charAlign, wbsmoothing,
					ngramOrder, nontiOrder); // threshold, smoothingP
			
		}
		else if(Integer.parseInt(paramStrings[0]) == 2)
		{
			// semisupervised
			obj.operationInput(paramStrings[1]);
			obj.operationTest(paramStrings[2]);
			
			Double threshold = Double.parseDouble(paramStrings[3]);
			Double smoothingP = Double.parseDouble(paramStrings[4]);
			int tiRules = Integer.parseInt(paramStrings[5]);
			int unIteration = Integer.parseInt(paramStrings[6]);
			int semiIteration = Integer.parseInt(paramStrings[7]);
			obj.operationSeed(paramStrings[8]);
			int charAlign = Integer.parseInt(paramStrings[9]);
			
			obj.operationLMSrc(paramStrings[10]);
			obj.operationLMTar(paramStrings[11]);
			
			int wbsmoothing = Integer.parseInt(paramStrings[12]);
			int ngramOrder = Integer.parseInt(paramStrings[13]);
			int nontiOrder = Integer.parseInt(paramStrings[14]);
			obj.callSemisupervised(threshold, smoothingP, tiRules, unIteration, semiIteration, charAlign,wbsmoothing,
					ngramOrder, nontiOrder);
		}
		else if(Integer.parseInt(paramStrings[0]) == 3)
		{
			//supervised
			// 1 + inputFile + " " + testFile + " " + threshold + " " + smoothingParameter + " " 
			//+ outputTiRules + " " + unsuperIter;
			obj.operationInput(paramStrings[1]);
			obj.operationTest(paramStrings[2]);
			
			Double threshold = Double.parseDouble(paramStrings[3]);
			Double smoothingP = Double.parseDouble(paramStrings[4]);
			int tiRules = Integer.parseInt(paramStrings[5]);
			int iteration = Integer.parseInt(paramStrings[6]);
			int charAlign = Integer.parseInt(paramStrings[7]);
			obj.operationLMSrc(paramStrings[8]);
			obj.operationLMTar(paramStrings[9]);
			int wbsmoothing = Integer.parseInt(paramStrings[10]);
			int ngramOrder = Integer.parseInt(paramStrings[11]);
			int nontiOrder = Integer.parseInt(paramStrings[12]);
			
			
			obj.callSupervised(threshold, smoothingP, tiRules, iteration, charAlign, wbsmoothing,
					ngramOrder, nontiOrder); // threshold, smoothingP
			
		}
		
	}
	
/*	public void callDecoder()
	{
		multigram.put("a-A", -1.0);
		multigram.put("a-B", -1.4);
		multigram.put(" -A", -1.5);
		multigram.put("a- ", -1.6);
		multigram.put("b-B", -1.1);
		multigram.put("b-A", -1.6);
		
		
		ArrayList inp = new ArrayList();
		inp.add("ab" + "\t" + "AA");
		inp.add("ba\tBB");
		
		decode dc = new decode();
		dc.decoderMain(inp, multigram);
	}
	
*/	
	public void callNoise()
	{
		NoiseModel ob = new NoiseModel();
		ob.trainTar(lmCorpusTar);
		//HashMap<Integer,Double> translationTest = new HashMap<Integer,Double> ();
		//ob.test(lmCorpus, translationTest, 2);
		int temp = 0;
	}
	
	public void callSupervised(Double threshold, Double smoothingP, int tiRules, int iteration, int charAlign,
			int wbsmoothing, int ngramOrder, int nontiOrder)
	{
		//MultigramGraphUn_traintest MGObj = new MultigramGraphUn_traintest();
		//MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign);
		
		System.out.print("supervised  ");
			System.out.print("Smoothed ");
			if(ngramOrder == 1)
			{ // input is seed here
				System.out.print("Unigram ");
				MultigramGraphSupervised_UnigramFully MGObj = new MultigramGraphSupervised_UnigramFully();
				MGObj.main(test, threshold, tiRules, iteration, input, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 2)
			{
				System.out.print("Bigram ");
				MultigramGraphSupervised_BigramFully MGObj = new MultigramGraphSupervised_BigramFully();
				MGObj.main(test, threshold, tiRules, iteration, input, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 3)
			{
				System.out.print("Trigram ");
				MultigramGraphSupervised_TrigramFully MGObj = new MultigramGraphSupervised_TrigramFully();
				MGObj.main(test, threshold, tiRules, iteration, input, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
	}

	
	public void callUnsupervised(Double threshold, Double smoothingP, int tiRules, int iteration, int charAlign,
			int wbsmoothing, int ngramOrder, int nontiOrder)
	{
		//MultigramGraphUn_traintest MGObj = new MultigramGraphUn_traintest();
		//MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign);
		
		if(wbsmoothing == 0)
		{
				wbsmoothing = compareTrainTest(wbsmoothing);// this is to turn on smoothing if train test are different
		}
		
		System.out.print("Unsupervised  ");
		if(wbsmoothing == 1){ // require smoothing
			System.out.print("Smoothed ");
			if(ngramOrder == 1)
			{
				System.out.print("Unigram ");
				MultigramGraphUn_traintest_smooth MGObj = new MultigramGraphUn_traintest_smooth();
				MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 2)
			{
				System.out.print("Bigram ");
				MultigramGraphUn_traintestBigram_smooth MGObj = new MultigramGraphUn_traintestBigram_smooth();
				MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 3)
			{
				System.out.print("Trigram ");
				MultigramGraphUn_traintestTrigram_smooth MGObj = new MultigramGraphUn_traintestTrigram_smooth();
				MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}

		}
		else if (wbsmoothing == 0)
		{
			System.out.print("without WB smoothing ");
			if(ngramOrder == 1)
			{
				System.out.print("Unigram ");
				MultigramGraphUn_traintest MGObj = new MultigramGraphUn_traintest();
				MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign, nontiOrder);
			}
			if(ngramOrder == 2)
			{
				System.out.print("Bigram ");
				MultigramGraphUn_traintestBigram MGObj = new MultigramGraphUn_traintestBigram();
				MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph, charAlign, nontiOrder);
			}
			if(ngramOrder == 3)
			{
				System.out.print("Trigram ");
				MultigramGraphUn_traintestTrigram MGObj = new MultigramGraphUn_traintestTrigram();
				MGObj.main(input, test, threshold, smoothingP, tiRules, iteration, gemmaNew, graph, charAlign, nontiOrder);
			}
		}

	}
	
	private int compareTrainTest(int wbsmoothing)
	{ // if train and test are different then turn on smooting
		if(input.size() == test.size())
		{
			for(int i =0; i<input.size(); i++)
			{
				if(! ((String) input.get(i)).equals((String) test.get(i)))
				{
					i = input.size();
					wbsmoothing = 1; // not same turn on smoothing
				}
				else{}
			}
		}
		else
		{ wbsmoothing = 1; // if not same, turn on smoothing
		}			

		return wbsmoothing;
	}
	
	public void callSemisupervised(Double threshold, Double smoothingP, int tiRules,
			int unIteration, int semiIteration, int charAlign, int wbsmoothing, int ngramOrder, int nontiOrder)
	{			
		if(wbsmoothing == 0)
		{
				wbsmoothing = compareTrainTest(wbsmoothing);// this is to turn on smoothing if train test are different
		}
		
		System.out.print("Semisupervised  ");
		if(wbsmoothing == 1){ // require smoothing
			
			System.out.print("Smoothed ");
			if(ngramOrder == 1)
			{
				System.out.print("Unigram ");
				MultigramGraphSemi_traintest MGObj = new MultigramGraphSemi_traintest();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
						semiIteration, seed, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 2)
			{
				System.out.print("Bigram ");
				MultigramGraphSemi_traintest_Bigram MGObj = new MultigramGraphSemi_traintest_Bigram();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
						semiIteration, seed, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}
			if(ngramOrder == 3)
			{
				System.out.print("Trigram ");
				MultigramGraphSemi_Trigram MGObj = new MultigramGraphSemi_Trigram();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
						semiIteration, seed, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar, nontiOrder);
			}

		}
		else if (wbsmoothing == 0)
		{
			System.out.print("without WB smoothing ");
			if(ngramOrder == 1)
			{
				MultigramGraphSemi_Unigram_Unsmooth MGObj = new MultigramGraphSemi_Unigram_Unsmooth();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
					semiIteration, seed, gemmaNew, graph, charAlign, nontiOrder);
				
				//for(double i = 0.0; i<=300.0; i=i+50.0)
				//{
				//	System.out.print("Unigram ");
				//	MultigramGraphSemi_Unigram_Unsmooth_alpha MGObj = new MultigramGraphSemi_Unigram_Unsmooth_alpha();
				//	MGObj.main(input, test, threshold, i, tiRules, unIteration,
				//		semiIteration, seed, gemmaNew, graph, charAlign, nontiOrder);
				//}
				
				//for(double i = 400.0; i<=2000.0; i=i+100.0)
				//{
				//	System.out.print("Unigram ");
				//	MultigramGraphSemi_Unigram_Unsmooth_alpha MGObj = new MultigramGraphSemi_Unigram_Unsmooth_alpha();
				//	MGObj.main(input, test, threshold, i, tiRules, unIteration,
				//		semiIteration, seed, gemmaNew, graph, charAlign, nontiOrder);
				//}
				
			}
			if(ngramOrder == 2)
			{
				System.out.print("Bigram ");
				MultigramGraphSemi_Bigram_Unsmooth MGObj = new MultigramGraphSemi_Bigram_Unsmooth();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
						semiIteration, seed, gemmaNew, graph, charAlign, nontiOrder);
			}
			if(ngramOrder == 3)
			{
				System.out.print("Trigram ");
				MultigramGraphSemi_Trigram_Unsmooth MGObj = new MultigramGraphSemi_Trigram_Unsmooth();
				MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
						semiIteration, seed, gemmaNew, graph, charAlign, nontiOrder);
			}
		}
	}
		
	
  /*public void callSemisupervised(Double threshold, Double smoothingP, int tiRules,
		int unIteration, int semiIteration, int charAlign){
		
	//	MultigramGraphSemi_traintest MGObj = new MultigramGraphSemi_traintest();		
//MultigramGraphSemi_traintest_Bigram MGObj = new MultigramGraphSemi_traintest_Bigram();
		//MultigramGraphSemi_Trigram MGObj = new MultigramGraphSemi_Trigram();
		//MultigramGraphSupervised MGObj = new MultigramGraphSupervised();
			
		//MultigramGraphSupervised_Bigram MGObj = new MultigramGraphSupervised_Bigram();
			
	//MultigramGraphSupervised_Trigram MGObj = new MultigramGraphSupervised_Trigram();
			MultigramGraphSupervised_TrigramFully MGObj = new MultigramGraphSupervised_TrigramFully();
			
	//		MultigramGraphSupervised_UnigramFully MGObj = new MultigramGraphSupervised_UnigramFully();
	//		MultigramGraphSupervised_BigramFully MGObj = new MultigramGraphSupervised_BigramFully();
		MGObj.main(input, test, threshold, smoothingP, tiRules, unIteration,
				semiIteration, seed, gemmaNew, graph, charAlign, lmCorpusSrc, lmCorpusTar);
	
	}*/
	
	public void callAligner(Double threshold, Double smoothingP, String tiRules, int iteration, int charAlign){
			
			MultigramGraph MGObj = new MultigramGraph();
		//	MGObj.main(input, test, threshold, tiRules, iteration, gemmaNew, graph);
		
	}
	
	public void operationLMSrc(String File) {
		
		if(File.equals("NO"))
		{ }
		else
		{
			input inp = new input();
			inp.getUserInput(lmCorpusSrc, File);
			System.out.println("LM corpus Source " + lmCorpusSrc.size());
		}
	}
	
	public void operationLMTar(String File) {
		if(File.equals("NO"))
		{ }
		else
		{
			input inp = new input();
			inp.getUserInput(lmCorpusTar, File);
			System.out.println("LM corpus Target " + lmCorpusTar.size());			
		}

	}
	public void operationInput(String File) {
		input inp = new input();
		inp.getUserInput(input, File);
		System.out.println("Input  " + input.size());
	}
	
	public void operationSeed(String File) {
	input inp = new input();
	inp.getUserInput(seed, File);
	System.out.println("Seed  " + seed.size());
}

	public void operationTest(String File) {
	input inp = new input();
	inp.getUserInput(test, File);
	System.out.println("Test  " + test.size());
}

	
	private String argumentList(String[] args, ArrayList input){
		int i = 0, error = 0;
		String currArg = "";
		String inputFile = "";
		String seedFile = "";
		String testFile = "";
		String lmFileSrc = "", lmFileTar = "";
		int outputTiRules = 0;
		Double threshold = 0.0, smoothingParameter = 1000.0;
		int wbsmoothing = 0; // bydefault no smoothing if train and test are equal
		int aligner = 0, semisupervised = 0, unsupervised = 0;
		int charAlignment = 0;
		int unsuperIter = 0, semisuperIter = 0, supervised = 0;
		int defaultUnsupervisedIterations = 50;
		int defaultSemisupervisedIterations = 50;
		int ngram = 1; // bydefault unigram
		int nontingram=1;
		
		 while (i < args.length && args[i].startsWith("-")) {
	            currArg = args[i];
	            i++;
		 // now check for currentArg
		 
		 if (currArg.equals("-train")) {
             if (i < args.length){
                 inputFile = args[i];
                 i++;
             }
             else
             {
                 System.err.println("-train requires a filename");
                 printReadme();
                 System.exit(1);
             }
         }
		 
		 else if (currArg.equals("-seed")) {
             if (i < args.length){
                 seedFile = args[i];
                 i++;
             }
             else
             {
                 System.err.println("-seed requires a filename");
                 printReadme();
                 System.exit(1);
             }
         }
		 
		 if (currArg.equals("-test")) {
             if (i < args.length){
                 testFile = args[i];
                 i++;
             }
             else{
            	 	System.err.println("-train requires a filename");
            	 	printReadme();
            	 	System.exit(1);
             }
         }
		 if (currArg.equals("-lmSrc")) {
             if (i < args.length){
                 lmFileSrc = args[i];
                 i++;
             }
             else{
            	 	System.err.println("-lm source requires a filename");
            	 	printReadme();
            	 	System.exit(1);
             }
         }
		 if (currArg.equals("-lmTar")) {
             if (i < args.length){
                 lmFileTar = args[i];
                 i++;
             }
             else{
            	 	System.err.println("-lm target requires a filename");
            	 	printReadme();
            	 	System.exit(1);
             }
         }
		 else if (currArg.equals("-aligner")){
				aligner = 1; // only run aligner
				//i++;
		 }
		 else if(currArg.equals("-unsupervised")){
			 unsupervised = 1;  // run unsupervised mining
		 }
		 else if(currArg.equals("-semisupervised")){
			 semisupervised = 1; // run semi-supervised mining
	     }
		 else if(currArg.equals("-supervised")){
			 supervised = 1; // run semi-supervised mining
	     }
		 else if(currArg.equals("-unsupervisediteration")){ // for aligner, unsupervised & for step1 of semisupervised
			 if (i < args.length){
				 unsuperIter = Integer.parseInt(args[i]);
				 i++;
			 }
			 
		 }
		 else if(currArg.equals("-semisupervisediteration")){ // step 2 of semisupervised
			 if (i < args.length){
				 semisuperIter = Integer.parseInt(args[i]);
				 i++;
			 }
		 }
		 
		 // handling of output
		 else if(currArg.equals("-t")){ // filtering threshold. default is no threshold.
			 // Default prints complete list with score
			 if (i < args.length){
				 threshold = Double.parseDouble(args[i]);
				 i++;
             }
             else
             {
                 System.err.println("-t requires a value of threshold range between 0 to 1 where 0 means transliterations and 1 means non-transliterations");
                 printReadme();
                 System.exit(1);
             }
		 }
		 
		 else if (currArg.equals("-s")){ // smoothing parameter. Default is calculated on seed
			 if (i < args.length){
				 smoothingParameter = Double.parseDouble(args[i]);
				 i++;
             }
		      else
             {
                 System.err.println("-s requires a double value for smoothing parameter");
                 printReadme();
                 System.exit(1);
             }
			 
		 }
		 
		 else if (currArg.equals("-wbsmoothing")){ // use witten bell smooting. by default this is off for same train and test
	 		 wbsmoothing = 1;
		 }
		 else if (currArg.equals("-transliterationorder")){ // by default unigram
			 if (i < args.length){
				 ngram = Integer.parseInt(args[i]);
				 i++;
			 }
			 else
             {
                 System.err.println("-transliterationorder requires value 1 or 2 or 3");
                 printReadme();
                 System.exit(1);
             } 
		 }
		 else if (currArg.equals("-nontransliterationorder")){ // by default unigram
			 if (i < args.length){
				 nontingram = Integer.parseInt(args[i]);
				 i++;
			 }
			 else
             {
                 System.err.println("-nontransliterationorder requires value 1 or 2 or 3");
                 printReadme();
                 System.exit(1);
             } 
		 }
		 else if (currArg.equals("-a")) // needs list aligned at character level with viterbi probabilities,
			 // default is no alignment at character level
 		 {
			 charAlignment = 1;
		 }
		 
		 else if (currArg.equals("-tirules")) // output transliteration rules with log probability in this file
 		 {
			outputTiRules = 1; 
         }
		 	 
	} // end of while
 
		 if(inputFile ==  ""){
			 System.err.println("Requires an input filename in format -input fileName");
			 printReadme();
		 }
		 if(aligner == 1 ) {
			 System.out.println("Character alignment"); // run aligner
		 }
		
		 else if(unsupervised == 1)
		 {
			 // run unsupervised
			 System.out.println("Unsupervised mining");
			 if (unsuperIter == 0){ // use default number of iterations
				 unsuperIter = defaultUnsupervisedIterations;
			 }
		 }
		 else if(supervised == 1)
		 {
			 // run unsupervised
			 System.out.println("Supervised mining");
			 if (unsuperIter == 0){ // use default number of iterations
				 unsuperIter = defaultUnsupervisedIterations;
			 }
		 }
		 else if(semisupervised == 1 )
		 {
			 if(seedFile == ""){
				 System.err.println("Requires a seed file in format -seed fileName");
				 printReadme();
			 }
			 
			 System.out.println("Semi-supervised mining");  // run semoisupervised
			 if (unsuperIter == 0){ // use default number of iterations
				 unsuperIter = defaultUnsupervisedIterations;
			 }
			 if (semisuperIter == 0){ // use default number of iterations
				 semisuperIter = defaultSemisupervisedIterations;
			 }
		 }
		 else
		 {
			 System.err.println("Enter an application to run: aligner" +
			 		", unsupervised mining or semi-supervised mining or supervised mining");
			 printReadme();
			 System.exit(1);
		 }
		 
		 if(testFile == ""){
			 testFile = inputFile;
		 }
		 else
		 {
			 if(!testFile.equals(inputFile)) // if name of test file is not equal to input file then turn on smoothing by default
			 {		
				 wbsmoothing = 1; // for different test file use wb smooting by default
			 }
			 
		 }
		 if(lmFileSrc == "")
		 {
			 lmFileSrc = "NO";
		 }
		 if(lmFileTar == "")
		 {
			 lmFileTar = "NO";
		 }


	String parameters = "";
	// for aligner class
	// for unsupervised class
	// inputFile threshold smoothingParameter transliterationRules iterations
	if(aligner == 1)
	{
		parameters = 0 + inputFile;
	}
	else if(unsupervised == 1)
	{
		parameters = 1 + " " + inputFile + " " + testFile + " " + threshold + " " + smoothingParameter + " " +
				outputTiRules + " " + unsuperIter + " " + charAlignment + " " + lmFileSrc + " " + lmFileTar + " " + wbsmoothing + " " + ngram
				+ " " + nontingram;
	}
	else if (semisupervised == 1)
	{
		parameters = 2 + " " + inputFile + " " + testFile + " " + threshold + " " + smoothingParameter 
				+ " " + outputTiRules + " " + unsuperIter + " " + semisuperIter + " " +  seedFile + " " + charAlignment
				+ " " + lmFileSrc + " " + lmFileTar+ " " + wbsmoothing + " " + ngram + " " + nontingram;
	}
	else if(supervised == 1)
	{
		parameters = 3 + " " + inputFile + " " + testFile + " " + threshold + " " + smoothingParameter + " " +
				outputTiRules + " " + unsuperIter + " " + charAlignment + " " + lmFileSrc + " " + lmFileTar + " " + wbsmoothing + " " + ngram
				+ " " + nontingram;
	}
	// save it in a string and return to main class
	
	System.out.println(parameters);
	return parameters;
}
	
		public static double roundToDecimals(double d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (((double)temp)/Math.pow(10,c));
		}
	
	public void operation(String File) {

		Main objMain = new Main();
		
		input inp = new input();
		inp.getUserInput(input, File);
		System.out.println("Input  " + input.size());

	}
	
	private void printReadme(){
		
		System.out.println("Usage: " +
				"\n java -Xmx5g -Dfile.encoding=UTF-8 -jar multigrams.jar [-aligner | -unsupervised | semisupervised] -train trainFile -test testFile" +
				"\n " + "-aligner" + "\n\t" +	
		    		"Character aligns the test data based on the model learnt from the training data."
		);
		
		System.out.println("-unsupervised" + "\n\t" + "Unsupervised transliteration mining \n" +
				"-semisupervised "+ "\n\t" + "Semi-supervised transliteration mining \n" + "\n" +
				"For semi-supervised system, an additional option is to include seed data. -seedFile \n" +
				"Every line of input files contain a word pair where words are separted by tab \n" +
				"-train" + "\n\t" + "-train requires a name/path of the training file\n" +
				"" + 
				"-seed" + "\n\t" + "-seed requires a name/path of the seed file\n" +
				"-test" + "\n\t" + "-test requires a name/path of the test file\n" + "\n" +
				"Additional options:" + "\n" + "\n" + 
				"" +
				"-unsupervisediteration N" + "\n\t" + "Specify the number of iterations N for the aligner/unsupervised system/unsupervised part of the semi-supervised system." +
				" Default is 25 iterations.\n" +
				"" +
				"-semisupervisediteration M" + "\n\t" + "Specify the number of iterations M for the semi-supervised part of the semi-supervised system." +
				" Default is 15 iterations.\n" +
				"" +
				"-t A" + "\n\t" + "Threshold on the posterior probability of non-transliterations. Default is no threshold and " +
				"it outputs the complete test data with their posterior probability of non-transliterations.\n" +
				"" +
				"-s B" + "\n\t" + "Value of smoothing parameter in Witten-Bell smoothing. Default value is the number of multigrams in the Viterbi of the seed data\n" +
				"" +
				"-tirules" + "\n\t" + "Output transliteration rules learned from the training data with their log probabilities in a file\n" +
				"" +
				"-a" + "\n\t" + "Output the test data aligned at character level with their viterbi probabilities\n"
						
		
		);	

	}

}




/*

Usage:
	java -Xmx5g -Dfile.encoding=UTF-8 -jar multigrams.jar [-aligner | -unsupervised | semisupervised] -train trainFile -test testFile 

    "-aligner"
    		Character aligns the test data based on the learning from the training data. 
	"-unsupervised" 
			Unsuprevised transliteration mining
	"-semisupervised"
			Semi-superivsed transliteration mining

	For semi-supervised system, additional option for seed data. "-seedFile"
	Every line of input files contain a word pair where words are separted by tab
 
	"-train"
	      -train requires a name/path of the training file

	"-seed"
		  -seed requires a name/path of the seed file

	"-test"
	      -train requires a name/path of the test file

	Additional options:
	
	"-unsupervisediteration N"
			Specify the number of iterations N for the aligner/unsupervised system/unsupervised part of the semi-supervised system. Default is 25 iterations.
	 
	"-semisupervisediteration M"
			Specify the number of iterations M for the semi-supervised part of the semi-supervised system. Default is 15 iterations.
	
	"-t A"  
			Threshold on the posterior probability of non-transliterations. Default is no threshold and it outputs the complete test data with their posterior
			 probability of non-transliterations. 

	"-s B"
			Value of smoothing parameter in Witten-Bell smoothing. Default value is the number of multigrams in the Viterbi of the seed data
		
	"-tirules" 
			Output transliteration rules learned from the training data with their log probabilities in a file

	"-a"
			Output the test data aligned at character level with their viterbi probabilities

	*/