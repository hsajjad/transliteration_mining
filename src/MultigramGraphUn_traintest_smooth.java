import java.io.*;
import java.util.*;

public class MultigramGraphUn_traintest_smooth {
	private static int LOGADD_PRECISION = 10;
	//private static int MAX_ITERATION = 5;
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
	public HashMap translation = new HashMap();
	public HashMap translationNew = new HashMap();
	public HashMap translationTest = new HashMap();
	public HashMap interpolation = new HashMap();
	public HashMap<Integer, ArrayList> graphDepth = new HashMap<Integer, ArrayList>();

	private HashMap testSrcChar = new HashMap(); // contain test src character and a word pair containing it 
	private HashMap testTarChar = new HashMap(); 

	
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
		// tar[target.length() + 1] = '9';
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
						+ (Math.pow(srcChar.size(), i) * Math.pow(
								tarChar.size(), j));
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
							// System.out.println(temp + " " + tt);
							temp = addLogCount(temp, tt);
							gemmaNew.put(eit.get(i).getTransition(), temp);
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

	public double maximization(HashMap gemmaNew, Double inputSizeLog) {
		Double total = INI, translatTotal = INI;
		int flag = 0, flag1 = 0;

		Iterator it2 = translationNew.keySet().iterator();
		while (it2.hasNext()) {
			int k = (Integer) it2.next();
			// System.out.println(translationNew.get(k));
			// if (flag1 == 0) {
			// translatTotal = (Double) translationNew.get(k);
			// flag1 = 1;
			// } else {
			translatTotal = addLogCount(translatTotal,
					(Double) translationNew.get(k));
			// }
		}
		// System.out.println(translatTotal+" "+ inputSizeLog);
		LAMBDA = translatTotal - inputSizeLog; // update lambda

		// System.out.println(" LAMBDA = " + Math.pow(10, LAMBDA));

		Iterator it = gemmaNew.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();

			// if (flag == 0) {
			// total = (Double) gemmaNew.get(key);
			// flag = 1;
			// } else {
			total = addLogCount(total, (Double) gemmaNew.get(key));
			// }
		}

		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();

			Double newVal = (Double) gemmaNew.get(key) - total;
			gemmaNew.put(key, newVal);
			// gemmaNew.put(key, INI); // set gemma to zero and update eg for
			// next
			// iteration
			// eg.put(key, newVal);
			// System.out.println(key + " " + Math.pow(10, newVal));
		}
		return total;
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
		String[] vitStrArr = vitStr.split(" ");
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

	public Double translationLogProb(String src_tar) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		Double srcProb = 0.0;
		Double tarProb = 0.0;
		for (int i = 0; i <= source.length() - 1; i++) {
			double srcValue= 0.0;
			
			if(srcChar.containsKey(source.charAt(i))){
				srcValue = (Double) srcChar.get(source.charAt(i)); 
			}
			if (i == 0) {
				srcProb = srcValue;
			} else {
				// srcProb = addLogCount(srcProb, (Double)
				// srcChar.get(source.charAt(i)));
				srcProb = srcProb + srcValue;
			}
		}
		for (int i = 0; i <= target.length() - 1; i++) {
			double tarValue = 0.0;
			if(tarChar.containsKey(target.charAt(i))){
				tarValue = (Double) tarChar.get(target.charAt(i)); 
			}
			
			if (i == 0) {
				tarProb = tarValue;
			} else {
				// tarProb = addLogCount(tarProb, (Double)
				// tarChar.get(target.charAt(i)));
				tarProb = tarProb + tarValue;
			}
		}

		// System.out.println(src_tar + " " + (srcProb+ tarProb));
		// return addLogCount(tarProb, srcProb);
		return (tarProb + srcProb);
	}

	public void translatProbNorm(int key, Double alphaTotal, Double LAMBDA,
			ArrayList input) {
		// p(ta|e,f)
		Double oneMinusLambda = Math.log10(1 - Math.pow(10, LAMBDA));
		Double temp1 = oneMinusLambda + alphaTotal;
		Double temp2 = LAMBDA + (Double) translation.get(key);
		Double temp3 = temp1 - temp2;

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
			// System.out.println(" P(ta|e,f) IS ONE " + input.get(key));
		}
		// System.out.println(" PROB "+ logProb + " translat " +
		// translation.get(key) );
		translationNew.put(key, logProb);

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
				return 1;
			} else {
				prevLogLikelihood = thisLogLikelihood;
				thisLogLikelihood = 0.0;
				return 0;
			}
		}
	}

	public void printTranslationScore(ArrayList input, HashMap translationNew,
			Double value) {

		String name = "translatScore" + value;
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
//input, test, threshold, smoothingP, tiRules, iteration, gemmaNew, graph
	
	public void main(ArrayList input, ArrayList test, Double threshold,
			int tiRules, int EMiteration, HashMap gemmaNew, HashMap graph, int charAlign,
			ArrayList lmCorpusSrc, ArrayList lmCorpusTar, int nontiOrder) {
		
		// check if tiRules is null
		
		int totalEdgesInTrain = 0;
		String likelihood = "";
		String lambdaStr = "";
		Double prevLikelihood = 0.0;
		int inputNum;

		MultigramGraphUn_traintest_smooth obj = new MultigramGraphUn_traintest_smooth();

		Iterator itToken = input.iterator();
		while (itToken.hasNext()) {
			String srcTar =(String) itToken.next(); 
			obj.tokenize(srcTar);
		}
		Double tp = obj.iniTransitionProb();
		
		obj.srcChar  = new HashMap();
		obj.tarChar = new HashMap();
		
		Iterator tplus = input.iterator();
		while (tplus.hasNext()) {
			String key = (String) tplus.next();
			obj.tokenize(key);
		}
	
		obj.translatCharLogProb();
		
		inputNum = 0;
		Iterator it11 = input.iterator();
		while (it11.hasNext()) {
			Double prob = obj.translationLogProb((String) it11.next());
			obj.translation.put(inputNum, prob);
			inputNum++;
		}
		
		inputNum = 0;
		Iterator itTes = test.iterator();
		while (itTes.hasNext()) {
			Double prob = obj.translationLogProb((String) itTes.next()); // this calculates the translation probability
			obj.translationTest.put(inputNum, prob);
			inputNum++;
		}
		

		//*************************smoothed translation model********************//		
		if(lmCorpusSrc.size() == 0)
		 {
			 System.out.println("No external LM for source. Copying unlabelled file to LM ..");
			 Iterator itTes2 = input.iterator();
				while (itTes2.hasNext()) {
					String src_tar = (String) itTes2.next();
					StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
					lmCorpusSrc.add(st.nextToken());
				}
		 }
		
		if(lmCorpusTar.size() == 0)
		 {
			 System.out.println("No external LM for target. Copying unlabelled file to LM ..");
			 Iterator itTes1 = input.iterator();
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
			
		
		//obj.translation = new HashMap();
		if(nontiOrder == 1)
		{
			o.test(test, translationTest1, 1);
		//	o.test(input, obj.translation, 1);	
		}
		if(nontiOrder == 2)
		{
			o.test(test, translationTest1, 2);
		//	o.test(input, obj.translation, 2);	
		}
		if(nontiOrder == 3)
		{
			o.test(test, translationTest1, 1);
		//	o.test(input, obj.translation, 2);	
		}
		//printingHashNum(translationTest1, "trigram");
		
		//*************************smoothed translation model ends********************//
		double totalFreq = 0.0;
		//for (int i = 0; i < MAX_ITERATION; i++) { // EM loop
		for (int i = 0; i < EMiteration; i++) { // EM loop
			System.out.println("EM iteration: " + i);
			Iterator itInput = input.iterator();
			inputNum = 0;

			if (i > 0) {
				gemmaOld.putAll(gemmaNew);

				Iterator it1 = gemmaNew.keySet().iterator();
				while (it1.hasNext()) {
					String key = (String) it1.next();
					gemmaNew.put(key, INI);
				}
			}

			while (itInput.hasNext()) {
				obj.tokenize((String) itInput.next());
				obj.makeNode(0, 0, null); // set transition probability here
				obj.calcGraphDepth(obj.graphDepth, obj.nd);

				if (i == 0) {
					obj.putEGInGemma(obj.eg, gemmaNew);
					obj.setTransitionProb(obj.eg, tp);
				} else {
					// obj.eg.putAll(gemmaOld);
					Iterator it2 = obj.eg.keySet().iterator();
					while (it2.hasNext()) {
						String key = (String) it2.next();
						obj.eg.put(key, gemmaOld.get(key));
					}
				}

				obj.calcAlpha(obj.graphDepth);
				obj.translatProbNorm(inputNum, obj.alphaTotal, obj.LAMBDA,
						input);
				obj.calcBeta(obj.graphDepth);
				obj.calcGemma(obj.graphDepth, gemmaNew, obj.alphaTotal,
						inputNum);
				obj.logLikelihood(obj.alphaTotal);

				obj.nd = new HashMap();
				obj.graphDepth = new HashMap();
				obj.eg = new HashMap();
				inputNum++;
			} // itInput ends
			
			if(i == 0){
				totalEdgesInTrain = obj.totalEdges;
				obj.totalEdges = 0;
				}

			
			totalFreq = obj.maximization(gemmaNew, Math.log10(input.size()));

			if (obj.calcLikeLihoodDiff(i) > 0) {
				System.out.println("STOP");
				i = EMiteration;
			}
		}// EM
		
		semiTest_smooth testObj = new semiTest_smooth();
		testObj.main(input, gemmaNew, graph, -500.0, test, translationTest1,
				totalEdgesInTrain, tiRules, charAlign, obj.LAMBDA, threshold, totalFreq);
		
	}
}