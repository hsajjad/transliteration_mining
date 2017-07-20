// take two inputs, one is the training data and other is the seed data.
// Helmut's method

import java.io.*;
import java.util.*;

public class MultigramGraphSupervised_BigramFully {
	private static int LOGADD_PRECISION = 6;
	
	private static int MAX_ITERATION = 10;
	private static int SEMI_MAX_ITERATION = 15; // one less then the semi without test
	
	private Double thisLogLikelihood = 0.0;
	private Double prevLogLikelihood = 0.0;
	private Double LAMBDA = Math.log10(0.5);
	private static Double INI = -2000.0;

	private int totalEdges=0;
	
	private char src[];
	private char tar[];
	private Integer srcCharTotal = 0, tarCharTotal = 0;
	private Double alphaTotal;
	private String srcTarLast;
	public HashMap nd = new HashMap();
	public HashMap eg = new HashMap();
	public HashMap srcChar = new HashMap();
	public HashMap tarChar = new HashMap();
	public HashMap srcWord = new HashMap();
	public HashMap tarWord = new HashMap();
	public HashMap gemmaOld = new HashMap();
	public HashMap gemmaInput = new HashMap();
	public HashMap gemmaSeed = new HashMap();
	public HashMap translation = new HashMap();
	public HashMap translationTest = new HashMap();
	public HashMap translationNew = new HashMap();
	public HashMap translationInput = new HashMap();
	public HashMap translationSeed = new HashMap();
	public HashMap interpolation = new HashMap();
	public HashMap<Integer, ArrayList> graphDepth = new HashMap<Integer, ArrayList>();
	
	private HashMap testSrcChar = new HashMap(); // contain test src character and a word pair containing it 
	private HashMap testTarChar = new HashMap(); 

	private double calcBigramTranslatWordProb(String source, HashMap bigramTranslatProb )
	{
		Double prob = 1.0;
		
		String curr = "", prev = "<s>";
		
		for(int i = 0; i < source.length(); i++){
			curr = Character.toString(source.charAt(i));
			String pair = prev+"<>"+curr;
			
			prob = prob * (Double) bigramTranslatProb.get(pair);
			
			if(i+1 == source.length()){
				pair = curr+"<>"+"<-s>";
				prob = prob * (Double) bigramTranslatProb.get(pair);
			}
				
			prev = curr;
		}	
			
		return Math.log10(prob);
	}
	
	private void bigramTranslationProbability(HashMap translationUnigram, HashMap translationBigram, HashMap bigramTranslatProb){
		
		Iterator it = translationBigram.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String [] pair = key.split("<>");
			
			Double prob = (Double) translationBigram.get(key) / (Double) translationUnigram.get(pair[0]);
			
			bigramTranslatProb.put(key, prob);
		}		
		
	}
	
	private void bigramTranslationCounts(String source, HashMap translationUnigramSrc, HashMap translationBigramSrc){
		
		String curr = "", prev = "<s>";
		for(int i = 0; i < source.length(); i++){
			curr = Character.toString(source.charAt(i));
			String pair = prev+"<>"+curr;
			
			if(translationBigramSrc.containsKey(pair)){
				translationBigramSrc.put(pair, (Double) translationBigramSrc.get(pair) + 1);
			}
			else
			{ 		translationBigramSrc.put(pair, 1.0);     }
			
			if(i+1 == source.length()){
				pair = curr+"<>"+"<-s>";
				
				if(translationBigramSrc.containsKey(pair)){
					translationBigramSrc.put(pair, (Double) translationBigramSrc.get(pair) + 1);
				}
				else
				{  		translationBigramSrc.put(pair, 1.0);   	}
			}
			// unigram count
			if(translationUnigramSrc.containsKey(curr)){
				translationUnigramSrc.put(curr, (Double) translationUnigramSrc.get(curr) + 1);
			}
			else
			{ 		translationUnigramSrc.put(curr,  1.0);  }
			
			prev = curr;
		} // end of for
		
		if(translationUnigramSrc.containsKey("<-s>")){
			translationUnigramSrc.put("<-s>", (Double) translationUnigramSrc.get("<-s>") + 1);
		}
		else { 		translationUnigramSrc.put("<-s>", 1.0);    }
		
		if(translationUnigramSrc.containsKey("<s>")){
			translationUnigramSrc.put("<s>", (Double) translationUnigramSrc.get("<s>") + 1);
		}
		else
		{ 		translationUnigramSrc.put("<s>",  1.0);  }
		
}
	
	
	private void testTokenize (String src_tar){
		
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();
		
		for (int i = 0; i <= source.length() - 1; i++) {
			
			if(testSrcChar.containsKey(source.charAt(i))){
				// do nothing
			}
			else
			{
				testSrcChar.put( source.charAt(i), src_tar);
			}
		}

		for (int i = 0; i <= target.length() - 1; i++) {
			if(testTarChar.containsKey(target.charAt(i))){
				// do nothing
			}
			else
			{
				testTarChar.put(target.charAt(i), src_tar);
			}
		}
		
	}
	
	private void charExistInTrain(ArrayList nWP){
		
		HashMap newWordPairs = new HashMap();
		
		Iterator it11 = testSrcChar.keySet().iterator();
		while (it11.hasNext()) {
			Character key = (Character) it11.next();
			String wordpair =  (String) testSrcChar.get(key);

			if(srcChar.containsKey(key)){
				// okay
			}
			else{
				// add word pair in the training data
				if(newWordPairs.containsKey(wordpair)){
					// do nothing
				}
				else{
					newWordPairs.put(wordpair, key);	
				}	
			}
		}
		
		Iterator it12 = testTarChar.keySet().iterator();
		while (it12.hasNext()) {
			Character key = (Character) it12.next();
			String wordpair =  (String) testTarChar.get(key);

			if(tarChar.containsKey(key)){
				// okay
			}
			else{
				// add word pair in the training data
				if(newWordPairs.containsKey(wordpair)){
					// do nothing
				}
				else{
					newWordPairs.put(wordpair, key);	
				}
				
			}
		}
		
		Iterator itWP = newWordPairs.keySet().iterator();
		while(itWP.hasNext())
		{
			String wp = (String) itWP.next();
			nWP.add(wp);
		}
	
	}
	
	public void tokenize(String src_tar) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		srcWord.put(source, 0.0);
		tarWord.put(target, 0.0);

		src = new char[source.length() + 1];
		tar = new char[target.length() + 1];
		src[0] = ' ';
		tar[0] = ' ';

		for (int i = 0; i <= source.length() - 1; i++) {
			src[i + 1] = source.charAt(i);
			// srcCharTotal++;

			srcChar.put(src[i + 1], 0.0);
		}
		// src[source.length() + 1] = '9';

		for (int i = 0; i <= target.length() - 1; i++) {
			tar[i + 1] = target.charAt(i);
			// tarCharTotal++;

			tarChar.put(tar[i + 1], 0.0);
		}
		 //tar[target.length() + 1] = '9';
	}

	public void translatCharLogProb() {
		// change srcChar, tarChar values to probabilities by dividing them
		// with the total number of characters on src and tar side respectively

		Iterator it1 = srcWord.keySet().iterator();
		while (it1.hasNext()) {
			String strTemp = (String) it1.next();

			for (int i = 0; i <= strTemp.length() - 1; i++) {

				srcCharTotal++;

				if (srcChar.containsKey(strTemp.charAt(i))) {
					Double aa = (Double) srcChar.get(strTemp.charAt(i));
					srcChar.put(strTemp.charAt(i), aa + 1);
				} else {
					srcChar.put(strTemp.charAt(i), 1.0);
				}
			}

		}

		Iterator it2 = tarWord.keySet().iterator();
		while (it2.hasNext()) {
			String strTemp = (String) it2.next();

			for (int i = 0; i <= strTemp.length() - 1; i++) {

				tarCharTotal++;

				if (tarChar.containsKey(strTemp.charAt(i))) {
					Double aa = (Double) tarChar.get(strTemp.charAt(i));
					tarChar.put(strTemp.charAt(i), aa + 1);
				} else {
					tarChar.put(strTemp.charAt(i), 1.0);
				}
			}

		}

		Iterator it = srcChar.keySet().iterator();
		while (it.hasNext()) {
			Character key = (Character) it.next();
			Double aa = (Double) srcChar.get(key) / srcCharTotal;

			srcChar.put(key, Math.log10(aa));
		}

		Iterator it11 = tarChar.keySet().iterator();
		while (it11.hasNext()) {
			Character key = (Character) it11.next();
			Double aa = (Double) tarChar.get(key) / tarCharTotal;

			tarChar.put(key, Math.log10(aa));
		}

	}

	public Double iniTransitionProb() {
		// calculate total number of unique source and target characters.
		Double temp = 0.0;
		Double transitionProb;
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 1; j++) {
				temp = temp
						+ (Math.pow(srcChar.size(), i) * Math.pow(tarChar
								.size(), j));
			}
		}
		transitionProb = 1 / temp;
		// System.out.print(Math.log10(transitionProb));
		return Math.log10(transitionProb);

	}

	public Double addLogCount(Double logA, Double logB) {
		if (logA == logB) {
			return (logA + Math.log10(2.0));
		}

		if (logA > logB) {
			if (logA - logB > LOGADD_PRECISION) {
				return (logA);
			} else {
				return (logA + Math.log10(1 + Math.pow(10, logB - logA)));
			}
		}
		// so, logB > logA
		if (logB - logA > LOGADD_PRECISION) {
			return (logB);
		}

		return (logB + Math.log10(1 + Math.pow(10, logA - logB)));
	}

	public void calcAlpha(HashMap gDepth) {

		Object[] key1 = gDepth.keySet().toArray();
		Arrays.sort(key1);

		for (int j = 0; j < key1.length; j++) {
			Iterator it3 = ((ArrayList) gDepth.get(key1[j])).iterator();
			Double temp;

			while (it3.hasNext()) { // nodes inside a particular key of gdepth
				temp = INI;
				Mnode a = (Mnode) it3.next();
				ArrayList<Medge> eit = (ArrayList<Medge>) a.getIncoming();

				if (eit.size() < 1) {
					a.setAlpha(0.0);
				} else {

					for (int i = 0; i < eit.size(); i++) {

						// System.out.println(eit.get(i).getTransition()
						// +" "+Math.pow(10, (Double)
						// eg.get(eit.get(i).getTransition())));
						// if (i == 0) {
						// temp = (eit.get(i).getStartNode().getAlpha() +
						// (Double) eg
						// .get(eit.get(i).getTransition()));
						// } else {
						Double tt = (eit.get(i).getStartNode().getAlpha() + (Double) eg
								.get(eit.get(i).getTransition()));
						temp = addLogCount(temp, tt);
						// }

						// System.out.println("this is "
						// + eit.get(i).getStartNode().getName());
						// System.out.println("this is " + a.getName());
						// System.out.println("loop value" + temp);
					}
					a.setAlpha(temp);
					alphaTotal = temp;
					// System.out.println(temp);
				}
			}
		}
	}

	public void calcBeta(HashMap gDepth) {
		Double temp = INI;
		Object[] key1 = gDepth.keySet().toArray();
		Arrays.sort(key1);
		int left = 0;

		for (int right = key1.length - 1; left < right; left++, right--) {
			Object tmp = key1[left];
			key1[left] = key1[right];
			key1[right] = tmp;
		}

		for (int j = 0; j < key1.length; j++) {
			Iterator it3 = ((ArrayList) gDepth.get(key1[j])).iterator();

			while (it3.hasNext()) { // nodes inside a particular key of gdepth
				temp = INI;
				Mnode a = (Mnode) it3.next();

				ArrayList<Medge> eit = (ArrayList<Medge>) a.getOutgoing();
				if (eit.size() < 1) {
					a.setBeta(0.0);
				} else {
					for (int i = 0; i < eit.size(); i++) {

						// if (i == 0) {
						// temp = (eit.get(i).getEndNode().getBeta() + (Double)
						// eg
						// .get(eit.get(i).getTransition()));
						// } else {
						Double tt = (eit.get(i).getEndNode().getBeta() + (Double) eg
								.get(eit.get(i).getTransition()));
						temp = addLogCount(temp, tt);
						// }
						// System.out.println("this is "
						// + eit.get(i).getEndNode().getName());
						// System.out.println("this is " + a.getName());
						// System.out.println("loop value" + temp);
					}
					a.setBeta(temp);
					// System.out.println("Beta" + temp);
				}
			}
		}
	}

	public void calcGemma(HashMap gDepth, HashMap gemmaNew, Double alphaTotal,
			int num_of_input) {

		Object[] key1 = gDepth.keySet().toArray();
		Arrays.sort(key1);

		if ((Double) translationNew.get(num_of_input) == 0.0)
		// its translation not transliteration
		{
			// dont do anything
			// System.out.println("Translation hai");
		} else {
			for (int j = 0; j < key1.length; j++) {

				Iterator it3 = ((ArrayList) gDepth.get(key1[j])).iterator();

				while (it3.hasNext()) { // nodes inside a particular key of
					// gdepth
					Mnode a = (Mnode) it3.next();
					ArrayList<Medge> eit = (ArrayList<Medge>) a.getIncoming();
					if (eit.size() < 1) {
						// do nothing
					} else {
						for (int i = 0; i < eit.size(); i++) {

							Double temp = INI;
							temp = (eit.get(i).getStartNode().getAlpha()
									+ eit.get(i).getEndNode().getBeta() + (Double) eg
									.get(eit.get(i).getTransition()))
									- alphaTotal;

							// System.out.print(eit.get(i).getStartNode().getName()
							// + " + ");
							// System.out.print(eit.get(i).getEndNode().getName()
							// + " + ");
							// System.out.println(eit.get(i).getTransition());
							//
							// System.out.print(eit.get(i).getStartNode().getAlpha()
							// + " + ");
							// System.out.print(eit.get(i).getEndNode().getBeta()
							// + " + ");
							// System.out.print(eg.get(eit.get(i).getTransition()));
							// System.out.println(" = " + temp);

							// if
							// (gemma.containsKey((eit.get(i).getTransition())))
							// {

							// updating gemma
							// System.out.print("before " + temp);
							temp = gemmaTransliterationWeight(num_of_input,
									temp);

							// if (((Double) gemmaNew.get(eit.get(i)
							// .getTransition())) == 0.0) {
							// gemmaNew.put(eit.get(i).getTransition(), temp);
							// System.out.println(" after " + temp);
							// } else {
							Double tt = (Double) gemmaNew.get(eit.get(i)
									.getTransition());

							temp = addLogCount(temp, tt);
							gemmaNew.put(eit.get(i).getTransition(), temp);

							// if (num_of_input > 1300){
							// System.out.println(" after " + temp);
							// }

						}
					}
				}
			}
		}
	}

	public Double gemmaTransliterationWeight(int key, Double temp) {
		Double transliteration;

		Double translatProb = (Double) translationNew.get(key);

		// if (translatProb == 0.0) {
		// transliteration = 0.0;
		// temp = 0.0;
		// } else {
		transliteration = Math.log10(1 - Math.pow(10, translatProb));
		temp = temp + transliteration;
		// }
		// Double transliteration = Math.log10(1 - Math.pow(10, (Double)
		// translationNew.get(key)));
		// System.out.println(" translit " + transliteration + " translat " +
		// translatProb + " temp " + temp);

		return temp;
	}

	public void printGemma(HashMap gemmaNew, String tiRules) {

		String str = "";
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(tiRules));
			Iterator it = gemmaNew.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				str = Math.pow(10, (Double) gemmaNew.get(key)) + " " + key;
				out.write(str);
				out.write('\n');
			}
			out.flush();
			out.close();

		} catch (IOException e) {
		}

	}

	public void printHash() {
		Object[] key = nd.keySet().toArray();
		Arrays.sort(key);

		for (int j = 0; j < key.length; j++) {
			Mnode a = (Mnode) nd.get(key[j]);
			System.out.print("alpha of " + a.getName());
			System.out.print(" is " + a.getAlpha());
			System.out.println("   Beta is " + a.getBeta());
			ArrayList<Medge> eit = (ArrayList<Medge>) a.getIncoming();
			for (int i = 0; i < eit.size(); i++) {
				// System.out.print(" " + eit.get(i).getStartNode().getName());
				// System.out.print(" " + eit.get(i).getTransition());
				// System.out.print(" " + eit.get(i).getTransitionProb());
				// System.out.print(" " + eit.get(i).getEndNode().getName());
			}
			// System.out.println("--");
		}

		System.out.println(eg.size());
		System.out.println(nd.size());
	}

	public int getSrcTransition(int i, Mnode st) {
		if (i == ((Mnode) nd.get(st.getName())).getSrcPos()) {

			return 0;
		} else {
			return i;
		}

	}

	public int getTarTransition(int j, Mnode st) {
		if (j == ((Mnode) nd.get(st.getName())).getTarPos()) {
			return 0;
		} else {
			return j;
		}

	}

	public void makeNode(int i, int j, Mnode st) {
		Double transitionProb = INI;
		Mnode n = new Mnode("" + i + "-" + j, i, j);
		int srcTrans, tarTrans;
		
		if (nd.containsKey(n.getName())) {
			n = (Mnode) (nd.get(n.getName()));

			srcTrans = getSrcTransition(i, st);
			tarTrans = getTarTransition(j, st);

			Medge e = new Medge("" + src[srcTrans] + "-" + tar[tarTrans] + "",
					transitionProb, st, n);
			// eg.put(e.getTransition(), e);
			eg.put(e.getTransition(), INI);
			((Mnode) nd.get(st.getName())).setOutgoing(e);
			((Mnode) nd.get(n.getName())).setIncoming(e);

			n = (Mnode) nd.get(n.getName());

			return;
		} else {
			nd.put(n.getName(), n);

			if (st != null) { // we are not at the start of the graph

				srcTrans = getSrcTransition(i, st);
				tarTrans = getTarTransition(j, st);

				Medge e = new Medge("" + src[srcTrans] + "-" + tar[tarTrans]
						+ "", transitionProb, st, n);
				eg.put(e.getTransition(), INI);
				st.setOutgoing(e);
				n.setIncoming(e);
				nd.put(n.getName(), n);
				nd.put(st.getName(), st);
			}
		}
		int srcLen = src.length - 1;
		int tarLen = tar.length - 1;

		if (j < tarLen) {
			makeNode(i, j + 1, n);
		}
		if (i < srcLen) {
			makeNode(i + 1, j, n);
		}
		if (i < srcLen && j < tarLen) {
			makeNode(i + 1, j + 1, n);
		}

	}


	public void makeNodeTest(int i, int j, Mnode st) {
		Double transitionProb = INI;
		Mnode n = new Mnode("" + i + "-" + j, i, j);
		int srcTrans, tarTrans;

		if (nd.containsKey(n.getName())) {
			n = (Mnode) (nd.get(n.getName()));

			srcTrans = getSrcTransition(i, st);
			tarTrans = getTarTransition(j, st);

			Medge e = new Medge("" + src[srcTrans] + "-" + tar[tarTrans] + "",
					transitionProb, st, n);
			// eg.put(e.getTransition(), e);
			eg.put(e.getTransition(), INI);
			((Mnode) nd.get(st.getName())).setOutgoing(e);
			((Mnode) nd.get(n.getName())).setIncoming(e);

			n = (Mnode) nd.get(n.getName());

			return;
		} else {
			nd.put(n.getName(), n);

			if (st != null) { // we are not at the start of the graph

				srcTrans = getSrcTransition(i, st);
				tarTrans = getTarTransition(j, st);

				Medge e = new Medge("" + src[srcTrans] + "-" + tar[tarTrans]
						+ "", transitionProb, st, n);
				eg.put(e.getTransition(), INI);
				st.setOutgoing(e);
				n.setIncoming(e);
				nd.put(n.getName(), n);
				nd.put(st.getName(), st);
			}
		}
		int srcLen = src.length - 1;
		int tarLen = tar.length - 1;

		if (j < tarLen) {
			makeNode(i, j + 1, n);
		}
		if (i < srcLen) {
			makeNode(i + 1, j, n);
		}
		if (i < srcLen && j < tarLen) {
			makeNode(i + 1, j + 1, n);
		}

	}

	
	
	public void calcGraphDepth(HashMap graphDepth, HashMap nd) {
		Iterator it = nd.values().iterator();

		while (it.hasNext()) {
			Mnode node = ((Mnode) it.next());
			String temp = node.getName();
			String[] a = temp.split("-");
			Integer k = Integer.parseInt(a[0]) + Integer.parseInt(a[1]);

			if (graphDepth.containsKey(k)) {
				ArrayList al1 = (ArrayList) graphDepth.get(k);
				al1.add(node);
				graphDepth.put(k, al1);
			} else {
				ArrayList al = new ArrayList();
				al.add(node);
				graphDepth.put(k, al);
			}
			// System.out.println(node.getName());

		}
	}

	// public void saveGraph(HashMap graph, int inputNum) {
	// // graph.put(inputNum, nd);
	// graph.put(inputNum, graphDepth);
	// }

	public void maximizationInput(HashMap gemmaInput, HashMap translationInp,
			Double inputSizeLog) {
		Double total = INI, translatTotal = INI;

		Iterator it2 = translationInp.keySet().iterator();
		while (it2.hasNext()) {
			int k = (Integer) it2.next();

			translatTotal = addLogCount(translatTotal, (Double) translationInp
					.get(k));
		}

		LAMBDA = translatTotal - inputSizeLog; // update lambda

		System.out.println(" LAMBDA = " + Math.pow(10, LAMBDA));

		Iterator it = gemmaInput.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			total = addLogCount(total, (Double) gemmaInput.get(key));
		}

		Iterator it1 = gemmaInput.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			Double newVal = (Double) gemmaInput.get(key) - total;
			gemmaInput.put(key, newVal);
		}
	}

	public Double maximizationSeed(HashMap gemmaSeed, Double inputSizeLog) {

		Double total = INI;
		Double totalProb = 0.0;

		Iterator it = gemmaSeed.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			total = addLogCount(total, (Double) gemmaSeed.get(key));
		}
		
		//System.out.println("maximizationseed" + Math.pow(10, total));
		
		Iterator it1 = gemmaSeed.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			Double newVal = (Double) gemmaSeed.get(key) - total;
			gemmaSeed.put(key, newVal);

		//	System.out.println(Math.pow(10, newVal) + " " + key);

		}
		return total;
	}

	public void maximization(HashMap gemmaNew, Double inputSizeLog) {
		Double total = INI, translatTotal = INI;
		int flag = 0;

		Iterator it2 = translationNew.keySet().iterator();
		while (it2.hasNext()) {
			int k = (Integer) it2.next();

			if (flag < Math.pow(10, inputSizeLog)) { // not to add seed elements
				translatTotal = addLogCount(translatTotal,
						(Double) translationNew.get(k));
			} else {
				break;
			}
			flag++;

		}

		LAMBDA = translatTotal - inputSizeLog; // update lambda

		System.out.println(" LAMBDA = " + Math.pow(10, LAMBDA));

		Iterator it = gemmaNew.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			total = addLogCount(total, (Double) gemmaNew.get(key));

		}

		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();

			Double newVal = (Double) gemmaNew.get(key) - total;
			gemmaNew.put(key, newVal);
		}
	}

	public void calcViterbi(HashMap gDepth) {
		int flag = 0;
		Double temp;
		String tempStr = "";

		Object[] key = gDepth.keySet().toArray();
		Arrays.sort(key);

		for (int j = 0; j < key.length; j++) {
			Iterator it3 = ((ArrayList) gDepth.get(key[j])).iterator();

			while (it3.hasNext()) { // nodes inside a particular key of gdepth
				temp = 0.0;
				tempStr = "";

				Mnode a = (Mnode) it3.next();
				ArrayList<Medge> eit = (ArrayList<Medge>) a.getIncoming();

				if (eit.size() < 1) {
					a.setViterbi(0.0);

				} else {
					for (int i = 0; i < eit.size(); i++) {
						Double kk = 0.0;
						String kkStr = "";

						if (i == 0) {
							temp = eit.get(i).getStartNode().getViterbi()
									+ (Double) eg.get(eit.get(i)
											.getTransition());

							tempStr = eit.get(i).getStartNode().getViterbiStr()
									+ "NNN" + eit.get(i).getTransition();
						} else {
							kk = eit.get(i).getStartNode().getViterbi()
									+ (Double) eg.get(eit.get(i)
											.getTransition());
							kkStr = eit.get(i).getStartNode().getViterbiStr()
									+ "NNN" + eit.get(i).getTransition();

							if (temp < kk) {
								temp = kk;
								tempStr = kkStr;
							}
						}
					}

					a.setViterbiStr(tempStr);
					a.setViterbi(temp);
				}
			}
		}
	}

	public int multigramsInViterbi(HashMap gDepth, HashMap multigrams, HashMap bigramMultigrams, HashMap unigramMultigrams, int totalTokens) {
		Object[] key = gDepth.keySet().toArray();
		Arrays.sort(key);

		Output obj = new Output();
		Mnode temper = (Mnode) ((ArrayList) gDepth.get(key[key.length - 1]))
				.get(0);

		String vitStr = temper.getViterbiStr();
		String[] vitStrArr = vitStr.split("NNN");
		
		String first = "", curr = "";
		
		// for every seed pair
		
		if(unigramMultigrams.containsKey("<s>"))
		{
			unigramMultigrams.put("<s>", (Double) unigramMultigrams.get("<s>") + 1.0);
			totalTokens++;
		}
		else
		{ 	unigramMultigrams.put("<s>", 1.0); 	totalTokens++;   }
		
		if(unigramMultigrams.containsKey("<-s>"))
		{
			unigramMultigrams.put("<-s>", (Double) unigramMultigrams.get("<-s>") + 1.0);
			totalTokens++;
		}
		else
		{ 	unigramMultigrams.put("<-s>", 1.0); 	totalTokens++;    }
		
		
		for (int i =1; i<vitStrArr.length; i++){ // value at 0 is always null
			
			totalTokens++;
			multigrams.put(vitStrArr[i], INI); // all multigrams except start and end boundary
			
			if(unigramMultigrams.containsKey(vitStrArr[i]))
			{
				unigramMultigrams.put(vitStrArr[i], (Double) unigramMultigrams.get(vitStrArr[i]) + 1.0);
			}
			else
			{
				unigramMultigrams.put(vitStrArr[i], 1.0);
			}
		
			// for bigram alpha
			if(i == 1){ // first element
				first = "<s>";
				curr = vitStrArr[i];
				String pair = first + "NNN" + curr;
				if(bigramMultigrams.containsKey(pair))
				{
					bigramMultigrams.put(pair, (Double) bigramMultigrams.get(pair) + 1.0);
				}
				else
				{
					bigramMultigrams.put(pair, 1.0);
				}
				
			}
			else
			{
				first = curr;
				curr = vitStrArr[i];
				String pair = first + "NNN" + curr;
				if(bigramMultigrams.containsKey(pair))
				{
					bigramMultigrams.put(pair, (Double) bigramMultigrams.get(pair) + 1.0);
				}
				else
				{
					bigramMultigrams.put(pair, 1.0);
				}
				
			}
			
			if(i == vitStrArr.length-1)
			{
				String pair = curr + "NNN" + "<-s>";
				if(bigramMultigrams.containsKey(pair))
				{
					bigramMultigrams.put(pair, (Double) bigramMultigrams.get(pair) + 1.0);
				}
				else
				{
					bigramMultigrams.put(pair, 1.0);
				}
			}	
		}
		return totalTokens;
	}

	public void multigramsInViterbi(HashMap gDepth, HashMap multigrams) {
		Object[] key = gDepth.keySet().toArray();
		Arrays.sort(key);

		Output obj = new Output();
		Mnode temper = (Mnode) ((ArrayList) gDepth.get(key[key.length - 1]))
				.get(0);

		String vitStr = temper.getViterbiStr();
		String[] vitStrArr = vitStr.split("NNN");

		for (String i : vitStrArr) {
			multigrams.put(i, INI);
		}

	}

	public void printViterbiAlignments(HashMap gDepth) {
		Object[] key = gDepth.keySet().toArray();
		Arrays.sort(key);

		Output obj = new Output();
		Mnode temper = (Mnode) ((ArrayList) gDepth.get(key[key.length - 1]))
				.get(0);

		String vitStr = temper.getViterbiStr();
		Double vitProb = temper.getViterbi();
		if (vitProb == 0.0) {
			// vitProb = -200000.0;
		}

		// extracting word pairs from alignment
		String srcStr = "", tarStr = "";
		String[] vitStrArr = vitStr.split("NNN");

		for (String i : vitStrArr) {
			if (i.length() == 2) {
				char[] a = i.toCharArray();
				if (a[0] == '-') {
					tarStr = tarStr + a[1];
				} else {
					srcStr = srcStr + a[0];
				}
			} else if (i.length() == 3) {
				String[] t = i.split("-");
				srcStr = srcStr + t[0];
				tarStr = tarStr + t[1];
			}
		}
		double normalizeViterbi = vitProb / (srcStr.length() + tarStr.length());
		// print word pairs and prob
		// String str = srcStr + "\t" + tarStr + "\t" + vitProb;

		String str = srcStr + "\t" + tarStr + "\t" + normalizeViterbi;

		// System.out.println(str);
		obj.printOutput("" + str, Boolean.TRUE, "viterbi");
	}

	public void logLikelihood(Double alphaTotal) {
		// System.out.println(thisLogLikelihood);
		if (thisLogLikelihood == 0.0) {
			thisLogLikelihood = alphaTotal;
		} else {
			thisLogLikelihood = addLogCount(thisLogLikelihood, alphaTotal);
		}

	}

	public HashMap evidenceTrim(HashMap map) {
		// sort hash map on values
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		double j = 0.0;
		int sizeOfHash = map.size();
		double trim = sizeOfHash * 0.05;
		System.out.println("trim" + trim);

		HashMap result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (j > trim) {
				result.put(entry.getKey(), entry.getValue());
			} else {
				result.put(entry.getKey(), 0.0);
			}

			j++;
		}
		return result;
	}

	public Double translationLogProbBigram(String src_tar) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		Double srcProb = 0.0;
		Double tarProb = 0.0;
		for (int i = 0; i <= source.length() - 1; i++) {

			if (i == 0) {
				srcProb = (Double) srcChar.get(source.charAt(i));
			} else {
				// srcProb = addLogCount(srcProb, (Double)
				// srcChar.get(source.charAt(i)));
				srcProb = srcProb + (Double) srcChar.get(source.charAt(i));
			}
		}
		for (int i = 0; i <= target.length() - 1; i++) {

			if (i == 0) {
				tarProb = (Double) tarChar.get(target.charAt(i));
			} else {
				// tarProb = addLogCount(tarProb, (Double)
				// tarChar.get(target.charAt(i)));
				tarProb = tarProb + (Double) tarChar.get(target.charAt(i));
			}
		}
		// System.out.println(src_tar + " " + (srcProb+ tarProb));
		// return addLogCount(tarProb, srcProb);
		return (tarProb + srcProb);
	}

	public Double translationLogProb(String src_tar) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		Double srcProb = 0.0;
		Double tarProb = 0.0;
		for (int i = 0; i <= source.length() - 1; i++) {

			if (i == 0) {
				srcProb = (Double) srcChar.get(source.charAt(i));
			} else {
				// srcProb = addLogCount(srcProb, (Double)
				// srcChar.get(source.charAt(i)));
				srcProb = srcProb + (Double) srcChar.get(source.charAt(i));
			}
		}
		for (int i = 0; i <= target.length() - 1; i++) {

			if (i == 0) {
				tarProb = (Double) tarChar.get(target.charAt(i));
			} else {
				// tarProb = addLogCount(tarProb, (Double)
				// tarChar.get(target.charAt(i)));
				tarProb = tarProb + (Double) tarChar.get(target.charAt(i));
			}
		}
		// System.out.println(src_tar + " " + (srcProb+ tarProb));
		// return addLogCount(tarProb, srcProb);
		return (tarProb + srcProb);
	}

	// public void interpolation(int key, Double alphaTotal, Double LAMBDA) {
	// Double oneMinusLambda = Math.log10(1 - Math.pow(10, LAMBDA));
	// System.out.println("\nAlpha " + alphaTotal + "oneMinus " +
	// oneMinusLambda);
	// Double translitScore = alphaTotal + oneMinusLambda;
	// Double translatScore = (Double) translation.get(key) + LAMBDA;
	// System.out.println("\ntranslat " + translatScore + "translit " +
	// translitScore);
	// Double inter = addLogCount(translitScore, translatScore);
	//
	// interpolation.put(key, inter);
	//
	// }

	public void translatProbNorm(int key, Double alphaTotal, Double LAMBDA) {
		// p(ta|e,f)
		Double temp1, temp2, temp3;

		if (LAMBDA == INI) {
			// System.out.println("here" + LAMBDA);
			translationNew.put(key, INI);

		} else {

			Double oneMinusLambda = Math.log10(1 - Math.pow(10, LAMBDA));
			temp1 = oneMinusLambda + alphaTotal;
			temp2 = LAMBDA + (Double) translation.get(key);
			temp3 = temp1 - temp2;

			// System.out.println("LAMDA = "+Math.pow(10,LAMBDA));

			Double prob = 1 / (1 + Math.pow(10, temp3));
			Double logProb = Math.log10(prob);
			// System.out.println("alpha = " + Math.pow(10, alphaTotal));

			// System.out.println("translation = "
			// + Math.pow(10, (Double) translation.get(key)));

			// Double prob = (Double) translation.get(key) + LAMBDA
			// - (Double) interpolation.get(key);
			if (logProb == 0.0) {
				// System.out.println(" t1 "+ Math.pow(10, temp1) + " t2 " +
				// Math.pow(10, temp2) + " t3 " +
				// Math.pow(10, temp3) + " translat " + Math.pow(10,
				// (Double)translation.get(key) ));
				// System.out.println(" lambda "+ oneMinusLambda + " aplha " +
				// alphaTotal + " sum " + (oneMinusLambda + alphaTotal));
				// System.out.println(" P(ta|e,f) IS ONE ");
			}
			// System.out.println(key);
			translationNew.put(key, logProb);
		}
	}

	public void setTransitionProb(HashMap eg, Double tp) {
		Iterator it3 = eg.keySet().iterator();
		while (it3.hasNext()) {
			eg.put(it3.next(), tp); // initalize transition prob
		}
	}

	public void putEGInGemma(HashMap eg, HashMap gemmaNew) {
		Iterator it3 = eg.keySet().iterator();
		while (it3.hasNext()) {
			String temp = (String) it3.next();
			if (!(gemmaNew.containsKey(temp))) { // if edge does not already
				// exist
				gemmaNew.put(temp, INI);
			}
			totalEdges++;
		}
	}

	public int calcLikeLihoodDiff(int iteration) {

		if (iteration == 0) {
			prevLogLikelihood = thisLogLikelihood;
			thisLogLikelihood = 0.0;
			return 0;
		} else {
			System.out.println("Likelihood Diff = "
					+ Math.abs(thisLogLikelihood - prevLogLikelihood));

			// likelihood = likelihood + "\n" + Math.abs(likelihood -
			// prevLikelihood);

			if (Math.abs(thisLogLikelihood - prevLogLikelihood) < 0.00001) {
				return MAX_ITERATION;
			} else {
				prevLogLikelihood = thisLogLikelihood;
				thisLogLikelihood = 0.0;
				return 0;
			}
		}
	}

	public void printTranslationScore(ArrayList input, HashMap translationNew,
			Double value, Double alphaWB) {

		// String name = "translatScore" + value;
		String name = "translatScore" + alphaWB;
		System.out.println(name);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(name));

			Iterator it10 = translationNew.keySet().iterator();
			while (it10.hasNext()) {
				int key = (Integer) it10.next();
				String str1 = (String) input.get(key);

				if (value == 0.0) {
					String str = (String) input.get(key) + "\t"
							+ (Math.pow(10, (Double) translationNew.get(key)));

					out.write(str);
					out.write('\n');
				} else {
					// print word pair with score
					if ((Math.pow(10, (Double) translationNew.get(key))) < value) {
						out.write(str1);
						out.write('\n');
					}
				}

			}
			out.close();
			out.flush();
		} catch (IOException e) {
		}

		// if ((Math.pow(10, (Double) obj.translationNew.get(key))) < 0.05) {
		// out05.write(str1);
		// out05.write('\n');
		// }

	}

	public void semiSupervisedInput(ArrayList input, HashMap gemmaInput,
			HashMap translationNew) {

		int inputNum;
		gemmaOld = new HashMap();
		gemmaOld.putAll(gemmaInput);

		Iterator it1 = gemmaInput.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaInput.put(key, INI);
		}

		Iterator itInput = input.iterator();
		inputNum = 0;
		while (itInput.hasNext()) {
			tokenize((String) itInput.next());
			makeNode(0, 0, null); // set transition probability here
			calcGraphDepth(graphDepth, nd);

			Iterator it2 = eg.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				eg.put(key, gemmaOld.get(key));
			}

			calcAlpha(graphDepth);
			translatProbNorm(inputNum, alphaTotal, LAMBDA);
			calcBeta(graphDepth);
			calcGemma(graphDepth, gemmaInput, alphaTotal, inputNum);

			nd = new HashMap();
			graphDepth = new HashMap();
			eg = new HashMap();
			inputNum++;
		} // itInput ends

		for (int i = 0; i < input.size(); i++) {
			translationInput.put(i, translationNew.get(i));
		}
		maximizationInput(gemmaInput, translationInput, Math
				.log10(input.size()));
	}

	public String semiSupervisedSeed(ArrayList seed, HashMap gemmaSeed,
			HashMap translationNew, int inputSize) {

		HashMap multigrams = new HashMap();

		int inputNum;
		gemmaOld = new HashMap();
		gemmaOld.putAll(gemmaSeed);

		Iterator it1 = gemmaSeed.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaSeed.put(key, INI);
		}

		inputNum = inputSize;
		Iterator itInput = seed.iterator();

		while (itInput.hasNext()) {
			tokenize((String) itInput.next());
			makeNode(0, 0, null); // set transition probability here
			calcGraphDepth(graphDepth, nd);

			Iterator it2 = eg.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				eg.put(key, gemmaOld.get(key));
			}

			calcAlpha(graphDepth);
			translatProbNorm(inputNum, alphaTotal, -2000.0);
			calcBeta(graphDepth);

			calcGemma(graphDepth, gemmaSeed, alphaTotal, inputNum);
			logLikelihood(alphaTotal);
			calcViterbi(graphDepth);
			multigramsInViterbi(graphDepth, multigrams);
			
			nd = new HashMap();
			graphDepth = new HashMap();
			eg = new HashMap();
			inputNum++;
		} // itInput ends

		// for (int i = inputSize; i < (inputSize + seed.size()); i++) {
		// //System.out.println(translationNew.get(i));
		// translationSeed.put(i, translationNew.get(i));
		// }

		Double totalFreq = maximizationSeed(gemmaSeed, Math.log10(seed.size()));
		return multigrams.size()+" "+totalFreq;
	}
		
	public String semiSupervisedSeedBigram(ArrayList seed, HashMap gemmaSeed,
			HashMap translationNew, int inputSize, HashMap alphaB, HashMap bigramMultigrams, 
			HashMap unigramMultigrams) {

		HashMap multigrams = new HashMap();
		HashMap startBigram = new HashMap();
		HashMap endBigram = new HashMap();
		
		double boundaryLogProb = 0.0;
		
		int inputNum;
		int totalMultigramTokensInViterbi = 0;
		
		gemmaOld = new HashMap();
		gemmaOld.putAll(gemmaSeed);

		Iterator it1 = gemmaSeed.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaSeed.put(key, INI);
		}

		inputNum = inputSize;
		Iterator itInput = seed.iterator();

		while (itInput.hasNext()) {
			tokenize((String) itInput.next());
			makeNode(0, 0, null);
			calcGraphDepth(graphDepth, nd);

			Iterator it2 = eg.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				eg.put(key, gemmaOld.get(key));
			}
			
			calcAlpha(graphDepth);
			translatProbNorm(inputNum, alphaTotal, -2000.0);
			calcBeta(graphDepth);

			calcGemma(graphDepth, gemmaSeed, alphaTotal, inputNum);
			calcViterbi(graphDepth);
			
			totalMultigramTokensInViterbi = multigramsInViterbi(graphDepth, multigrams, bigramMultigrams, unigramMultigrams, totalMultigramTokensInViterbi);
		//	boundaryMarker(graphDepth, startBigram, endBigram); // contains all start end including with null. above contains only from viterbi
			
			nd = new HashMap();
			graphDepth = new HashMap();
			eg = new HashMap();
			inputNum++;
		} // itInput ends

	//	printHash(bigramMultigrams, "bigramMultigrams");
	//	printHash(unigramMultigrams, "unigramMultigrams");
	//	printHash(startBigram, "startBigram");
	//	printHash(endBigram, "endBigram");
		
	//	bigramMultigrams.putAll(startBigram);
	//	bigramMultigrams.putAll(endBigram);
		
		// calculate alpha(b) which is the no. of transliteration unit pair types where b is the first element
		alphaOfB(bigramMultigrams, alphaB);
		
		Double totalFreq = maximizationSeed(gemmaSeed, Math.log10(seed.size()));
		
		boundaryLogProb = logProbOfBoundary((Double) unigramMultigrams.get("<s>"), totalMultigramTokensInViterbi);
		//gemmaSeed.put("<s>", boundaryLogProb);
		//gemmaSeed.put("<-s>", boundaryLogProb);
		
		return multigrams.size()+" "+totalFreq +" " + boundaryLogProb;
	}
	
	private double logProbOfBoundary(Double count, int all)
	{
		double prob = count / (double) all;
		return Math.log10(prob);
	}
	
	private void boundaryMarker(HashMap gDepth, HashMap startBigram, HashMap endBigram)
	{ // function to count boundry bigrams
		
		Object[] key1 = gDepth.keySet().toArray();
		Arrays.sort(key1);

		
			Iterator it3 = ((ArrayList) gDepth.get(key1[0])).iterator(); // first node of the graph
			Double temp;

			ArrayList<Medge> eit0, eit1;
			String [] start = new String [3];
			String [] end = new String [3];
			
			while (it3.hasNext()) { // nodes inside a particular key of gdepth
				temp = INI;
				Mnode a = (Mnode) it3.next();
				eit0 = (ArrayList<Medge>) a.getOutgoing();
				
				for (int i = 0; i < eit0.size(); i++) {
					String edge = eit0.get(i).getTransition();
					String bigram = "<s>NNN"+edge;
					
					if(startBigram.containsKey(bigram)){
						startBigram.put(bigram, (Integer) startBigram.get(bigram) + 1);
					}
					else
					{
						startBigram.put(bigram, 1);
					}
				}
			}
			
			Iterator itEnd = ((ArrayList) gDepth.get(key1[key1.length-1])).iterator(); // first node of the graph
			
			while (itEnd.hasNext()) { // nodes inside a particular key of gdepth
				temp = INI;
				Mnode a = (Mnode) itEnd.next();
				eit1 = (ArrayList<Medge>) a.getIncoming();
				
				for (int i = 0; i < eit1.size(); i++) {
					String edge = eit1.get(i).getTransition();
					String bigram = edge+"NNN<-s>";
					
					if(endBigram.containsKey(bigram)){
						endBigram.put(bigram, (Integer) endBigram.get(bigram) + 1);
					}
					else
					{
						endBigram.put(bigram, 1);
					}
				}
			}	
	}
	
	
	
	public void alphaOfB(HashMap bigramMultigrams, HashMap alphaB)
	{
		Iterator it = bigramMultigrams.keySet().iterator();
		while (it.hasNext()) {
			String [] key = ((String) it.next()).split("NNN");
			if(alphaB.containsKey(key[0]))
			{
				alphaB.put(key[0], (Double) alphaB.get(key[0]) + 1.0);
			}
			else
			{
				alphaB.put(key[0], 1.0);
			}
		}
	}

	public void smoothedMultigramEstimates(HashMap gemmaInput,
			HashMap gemmaSeed, HashMap gemmaNew, Double numOfMultigrams, Double totalFreq) {

		Double inputTemp, seedTemp, N = INI;
		int flagInput, flagSeed;

			Iterator it2 = gemmaSeed.keySet().iterator();
		while (it2.hasNext()) {
			String key = (String) it2.next();
			Double temp = totalFreq + (Double) gemmaSeed.get(key); // convert prob
			// into
			// frequencies
			gemmaSeed.put(key, temp); // now it has frequencies in it
		}

		// calculate N after converting seed values to frequency
		System.out.println("total" + Math.pow(10, totalFreq));
		
		Double denominator = addLogCount(totalFreq, numOfMultigrams);

		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			flagSeed = 0;
			flagInput = 0;

			String key = (String) it1.next();
			if (gemmaSeed.containsKey(key)) {
				flagSeed = 1;
				seedTemp = (Double) gemmaSeed.get(key); // these are frequencies
			} else {
				seedTemp = INI;
			}

			if (gemmaInput.containsKey(key)) {
				flagInput = 1;
				inputTemp = numOfMultigrams + (Double) gemmaInput.get(key);
			} else {
				inputTemp = INI;
			}

			Double valTemp = addLogCount(seedTemp, inputTemp);
			Double newVal = valTemp - denominator;

			if (flagInput == 1) {
				gemmaInput.put(key, newVal);
			}
			if (flagSeed == 1) {
				gemmaSeed.put(key, newVal);
			}

		}
		// for all multigrams in gemmaNew

	}
	
	public Double translationLogProbTest(String src_tar) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		Double srcProb = 0.0;
		Double tarProb = 0.0;
		for (int i = 0; i <= source.length() - 1; i++) {
			char sr = source.charAt(i);
			Double tempSrcProb;
			
			if(!srcChar.containsKey(sr))
			{
				tempSrcProb = Math.log10(1.0/srcCharTotal);
			}
			else
			{
				tempSrcProb = (Double) srcChar.get(source.charAt(i));
			}
			
			if (i == 0) {
				srcProb = tempSrcProb;
			} else {
				srcProb = srcProb + tempSrcProb;
			}
		}
		for (int i = 0; i <= target.length() - 1; i++) {
			char tr = target.charAt(i);
			Double tempTarProb;
			if(!tarChar.containsKey(tr))
			{
				tempTarProb = Math.log10(1.0/tarCharTotal);
			}
			else
			{
				tempTarProb = (Double) tarChar.get(target.charAt(i));
			}

			if (i == 0) {
				tarProb = tempTarProb;
			} else {
				tarProb = tarProb + tempTarProb;
			}
		}
		return (tarProb + srcProb);
	}

	public void putEGInGemmaValue(HashMap eg, HashMap gemmaNew) {
		Iterator it3 = eg.keySet().iterator();
		while (it3.hasNext()) {
			String temp = (String) it3.next();
			if (!(gemmaNew.containsKey(temp))) { // if edge does not already
				// exist
				gemmaNew.put(temp, (Double) eg.get(temp));
			}
		}
	}
	
	public void main(ArrayList test, Double threshold, int tiRules, int semiIteration, ArrayList seed, HashMap gemmaNew,
			HashMap graph, int charAlign, ArrayList lmCorpusSrc, ArrayList lmCorpusTar, int nontiOrder) {
		
		int totalEdgesInTrain = 0;
		String likelihood = "";
		String lambdaStr = "";
		Double prevLikelihood = 0.0;
		int inputNum;
		
		HashMap translationBigramSrc = new HashMap();
		HashMap translationUnigramSrc = new HashMap();
		HashMap bigramTranslatProbSrc = new HashMap();
		
		HashMap translationBigramTar = new HashMap();
		HashMap translationUnigramTar = new HashMap();
		HashMap bigramTranslatProbTar = new HashMap();
		
		HashMap bigramTranslatWordProb = new HashMap();
		HashMap bigramTranslatTrainWordProb = new HashMap();
		
		ArrayList inputSeed = new ArrayList();
		inputSeed.addAll(seed);
		
		MultigramGraphSupervised_BigramFully obj = new MultigramGraphSupervised_BigramFully();
		
		//****************************************
		// first see which characters are missing in train and add those pairs from test
		/*Iterator itT = inputSeed.iterator();
		while (itT.hasNext()) {
			obj.tokenize((String) itT.next());
		}
		Iterator itTestToken = test.iterator();
		while(itTestToken.hasNext()){
			obj.testTokenize((String) itTestToken.next());
		}
		ArrayList newWP = new ArrayList();
		obj.charExistInTrain(newWP);
		
		input.addAll(newWP); // now add newWP in the input
		
		inputSeed = new ArrayList();
		inputSeed.addAll(input);
		inputSeed.addAll(seed);
		
		srcChar  = new HashMap();
		tarChar = new HashMap();*/
		//****************************************
		
		Iterator itToken = inputSeed.iterator();
		while (itToken.hasNext()) {
			String src_tar = (String) itToken.next(); 
			obj.tokenize(src_tar);
			
			// for bigram translation probability
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();
			
			bigramTranslationCounts(source, translationUnigramSrc, translationBigramSrc);
			bigramTranslationCounts(target, translationUnigramTar, translationBigramTar);
		}
		
		bigramTranslationProbability(translationUnigramSrc, translationBigramSrc, bigramTranslatProbSrc);
		bigramTranslationProbability(translationUnigramTar, translationBigramTar, bigramTranslatProbTar);
		
		Double tp = obj.iniTransitionProb();
		obj.translatCharLogProb(); // this calculates probability of each character

		inputNum = 0;
		Iterator it11 = inputSeed.iterator();
		while (it11.hasNext()) {
			String src_tar = (String) it11.next();
			Double prob = obj.translationLogProb(src_tar); // this calculates the translation probability
			obj.translation.put(inputNum, prob);
			
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();
			
			Double bigramLogProbSrc = calcBigramTranslatWordProb(source, bigramTranslatProbSrc);
			Double bigramLogProbTar = calcBigramTranslatWordProb(target, bigramTranslatProbTar);
			bigramTranslatTrainWordProb.put(inputNum, bigramLogProbSrc + bigramLogProbTar);
		
			inputNum++;
		}
		
		/*		
		// down here there can be a possibility that character is not in the srcChar or tagChar
		inputNum = 0;
		Iterator itTes = test.iterator();
		while (itTes.hasNext()) {
			String src_tar = (String) itTes.next();
			Double prob = obj.translationLogProbTest(src_tar); // this calculates the translation probability
			obj.translationTest.put(inputNum, prob);
			
			//StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			//String source = st.nextToken();
			//String target = st.nextToken();
			
			//Double bigramLogProbSrc = calcBigramTranslatWordProb(source, bigramTranslatProbSrc);
			//Double bigramLogProbTar = calcBigramTranslatWordProb(target, bigramTranslatProbTar);
			//bigramTranslatWordProb.put(inputNum, bigramLogProbSrc + bigramLogProbTar);
			inputNum++;
		}
		
*/
		if(lmCorpusSrc.size() == 0)
		 {
			 System.out.println("No external LM for source. Copying labelled file to LM ..");
			 Iterator itTes2 = seed.iterator();
				while (itTes2.hasNext()) {
					String src_tar = (String) itTes2.next();
					StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
					lmCorpusSrc.add(st.nextToken());
				}
		 }
		
		if(lmCorpusTar.size() == 0)
		 {
			 System.out.println("No external LM for target. Copying labelled file to LM ..");
			 Iterator itTes1 = seed.iterator();
				while (itTes1.hasNext()) {
					String src_tar = (String) itTes1.next();
					StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
					String temp = st.nextToken();
					lmCorpusTar.add(st.nextToken());
				}
		 }
		
		HashMap <Integer, Double> translationTest1 = new HashMap <Integer, Double> ();
		NoiseModel o = new NoiseModel();
		o.trainSrc(lmCorpusSrc);
		o.trainTar(lmCorpusTar);
			
		if(nontiOrder == 1)
		{
			o.test(test, translationTest1, 1);
		}
		if(nontiOrder == 2)
		{
			o.test(test, translationTest1, 2);
		}
		if(nontiOrder == 3)
		{
			o.test(test, translationTest1, 1);
		}

		//obj.translation = new HashMap();
		//o.test(inputSeed, obj.translation, 2);
		//printingHashNum(translationTest1, "trigram");
	
		
		for (int i = 0; i < 1; i++) { // EM loop
			System.out.println("EM iteration: " + i);

			Iterator itInput = inputSeed.iterator();
			inputNum = 0;
			
			while (itInput.hasNext()) {
				obj.tokenize((String) itInput.next());
				obj.makeNode(0, 0, null); // set transition probability here
				obj.calcGraphDepth(obj.graphDepth, obj.nd);

				if (i == 0) {
					obj.putEGInGemma(obj.eg, gemmaNew); // also calculate totalEdges (tokens)
					obj.setTransitionProb(obj.eg, tp);
					obj.putEGInGemmaValue(obj.eg, gemmaSeed);
					}

				obj.nd = new HashMap();
				obj.graphDepth = new HashMap();
				obj.eg = new HashMap();
				inputNum++;
			} // itInput ends
			
			if(i == 0){
			totalEdgesInTrain = obj.totalEdges;
			obj.totalEdges = 0;
			}

		}// EM
		
		gemmaNew = new HashMap();
		gemmaNew.putAll(gemmaSeed);

		HashMap alphaB = new HashMap();
		HashMap bigramMultigram = new HashMap();
		HashMap unigramMultigram = new HashMap();
		Double totalFreq = 0.0;
		Double boundaryLogProb = 0.0;
		
		this.thisLogLikelihood = 0.0;
		
		for (int i = 0; i < semiIteration; i++) { // EM loop
			System.out.println("Second EM: " + i);

			//obj.semiSupervisedInput(input, gemmaInput, obj.translationNew);
			
			// change it to calculate the following hash tables only for last iteration
			alphaB = new HashMap(); // no. of transliteration unit pair types where b is the first element
			bigramMultigram = new HashMap(); // frequency of transliteration unit pair (b,c) in the aligned seed corpus
			unigramMultigram = new HashMap();// freq of transliteration pair b in the aligned seed corpus
			
			String temp = "";
			if(i+1 < semiIteration){ // last iteration do bigram stuff as well
				temp = obj.semiSupervisedSeed(seed, gemmaSeed,obj.translationNew, 0);
			}
			else
			{
				temp = obj.semiSupervisedSeedBigram(seed, gemmaSeed,
						obj.translationNew, 0, alphaB, bigramMultigram, unigramMultigram);	
			}
			
			String [] tempArr = temp.split(" ");
			int numOfMultigrams = Integer.parseInt(tempArr[0]);
			totalFreq = Double.parseDouble(tempArr[1]);
			
			if(i+1 == semiIteration){
				boundaryLogProb = Double.parseDouble(tempArr[2]);	
			}			
			//	obj.smoothedMultigramEstimates(gemmaInput, gemmaSeed, gemmaNew,	Math.log10(alphaWB), totalFreq);
			if(semiIteration -i < 2){}
			else{
				if(obj.calcLikeLihoodDiff(i) > 0)
				{
					System.out.println("STOP");
					i = semiIteration - 2;
				}
			}
		}
		
		gemmaSeed.put("<s>", boundaryLogProb);
		gemmaSeed.put("<-s>", boundaryLogProb);
		
		HashMap aGivenb = new HashMap();
		HashMap backoffEstimateB = new HashMap();
		
		obj.bigramSmoothedEstimates(gemmaInput, gemmaSeed, alphaB, bigramMultigram, unigramMultigram, totalFreq, aGivenb);
		obj.unigramBackoffEstimates(gemmaSeed, alphaB, totalFreq, backoffEstimateB, aGivenb);
		//printHash(aGivenb, "aGivenb");
		
		gemmaNew = new HashMap();
	//	gemmaNew.putAll(gemmaInput);
		gemmaNew.putAll(gemmaSeed);
		
		//semiTest_Bigram1 testObj = new semiTest_Bigram1();
		//testObj.main(input, gemmaNew, graph, alphaWB, test, obj.translationTest, totalEdgesInTrain, tiRules, charAlign, aGivenb);
		//testObj.main(seed, gemmaNew, graph, -500.0, test, translationTest1, totalEdgesInTrain,
				//tiRules, charAlign, aGivenb, backoffEstimateB, obj.LAMBDA, seed, bigramTranslatTrainWordProb);
		
		semiTest_Bigram1_supervised testObj = new semiTest_Bigram1_supervised();
		testObj.main(seed, gemmaNew, graph, -500.0, test, translationTest1, totalEdgesInTrain,
				tiRules, charAlign, aGivenb, backoffEstimateB, obj.LAMBDA, seed, bigramTranslatTrainWordProb, totalFreq);

	}
	
	public double translatProbNormReturn(int key, Double alphaTotal, Double LAMBDA) {
		// p(ta|e,f)
		Double temp1, temp2, temp3;
			Double oneMinusLambda = Math.log10(1 - Math.pow(10, LAMBDA));
			temp1 = oneMinusLambda + alphaTotal;
			temp2 = LAMBDA + (Double) translation.get(key);
			temp3 = temp1 - temp2;

			Double prob = 1 / (1 + Math.pow(10, temp3));
			//Double logProb = Math.log10(prob);
			return prob;
	}

	private void predictSeed(HashMap gemmaNew, ArrayList seed, HashMap translationNew, int inputSize)
	{
		int i =0;
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter("seedProb"));
			
		gemmaOld = new HashMap();
		gemmaOld.putAll(gemmaNew);
				
		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaNew.put(key, INI);
		}

		int inputNum = inputSize;
		Iterator itInput = seed.iterator();

		while (itInput.hasNext()) {
			tokenize((String) itInput.next());
			makeNode(0, 0, null); // set transition probability here
			calcGraphDepth(graphDepth, nd);

			Iterator it2 = eg.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				eg.put(key, gemmaOld.get(key));
			}

			calcAlpha(graphDepth);
			//translatProbNorm(inputNum, alphaTotal, LAMBDA);
			double prob = translatProbNormReturn(inputNum, alphaTotal, LAMBDA); // return probability of a word pair
			
			String outStr = seed.get(i) + "\t" + prob;
			out.write(outStr);
			out.write('\n');
			out.flush();
			
			nd = new HashMap();
			graphDepth = new HashMap();
			eg = new HashMap();
			inputNum++;
			i++;
		} // itInput ends
			out.flush();
			out.close();

		} catch (IOException e) {
		}
   }

	private void unigramBackoffEstimates(HashMap gemmaSeed, HashMap alphaB, double totalFreq, HashMap backoffEstimateB, HashMap aGivenb)
	{
		// alphaB / freqB + alphaB
		// later when needed, we multipy it with p(a) which is gemmaNew
		
		Iterator it = aGivenb.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			
			String [] pair = key.split("NNN"); // first one is b and second one is a. first,second format
			
			double numerator = Math.log10((Double)  alphaB.get(pair[0]));
			double freqB = totalFreq +  (Double) gemmaSeed.get(pair[0]);
			Double denomerator = addLogCount(numerator, freqB);
			
			backoffEstimateB.put(pair[0], numerator - denomerator);
		}
	}
	
	
	private void bigramSmoothedEstimates(HashMap gemmaInput, HashMap gemmaSeed, HashMap alphaB,
			HashMap bigramMultigram, HashMap unigramMultigram, Double totalFreq, HashMap aGivenb){
		
		HashMap gemmaCombine = new HashMap(); // p(a)
    	//	gemmaCombine.putAll(gemmaInput);
		gemmaCombine.putAll(gemmaSeed);
		
		Iterator it2 = bigramMultigram.keySet().iterator(); // for all multigrams
		while (it2.hasNext()) {
			String key = (String) it2.next();
			
			String [] tiUnits = key.split("NNN");
			Double currAlphaB = Math.log10((Double) alphaB.get(tiUnits[0]));
			
			Double bigramSeedFreq = Math.log10((Double) bigramMultigram.get(key));
			//System.out.println(key);
			
			Double freqB = totalFreq + (Double) gemmaSeed.get(tiUnits[0]); // convert prob
			// into frequencies
			Double probA = (Double) gemmaCombine.get(tiUnits[1]);
			
			Double temp1 = probA + currAlphaB;
			Double numerator = addLogCount(temp1, bigramSeedFreq);
			Double denomenator = addLogCount(freqB, currAlphaB);
			Double logProb = numerator - denomenator;
			aGivenb.put(key, logProb);
			
		}
		
	}
	private void printingHash(HashMap hash, String name){
		String str = "";
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(name));
			Iterator it = hash.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				str = Math.pow(10, (Double) hash.get(key)) + " " + key;
				out.write(str);
				out.write('\n');
			}
			out.flush();
			out.close();

		} catch (IOException e) {
		}	
	}

	private void printHash(HashMap hash, String name){
		String str = "";
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(name));
			Iterator it = hash.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				str = key + " " + hash.get(key);
				out.write(str);
				out.write('\n');
			}
			out.flush();
			out.close();

		} catch (IOException e) {
		}	
	}
	private void printingHashNum(HashMap hash, String name){
		String str = "";
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(name));
			Iterator it = hash.keySet().iterator();
			while (it.hasNext()) {
				int key = (Integer) it.next();
				str = Math.pow(10, (Double) hash.get(key)) + " " + key;
				out.write(str);
				out.write('\n');
			}
			out.flush();
			out.close();

		} catch (IOException e) {
		}	
	}
	}