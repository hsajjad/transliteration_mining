// take two inputs, one is the training data and other is the seed data.
// Helmut's method

import java.io.*;
import java.util.*;

public class semiTest_Bigram1_smooth {
	private static int LOGADD_PRECISION = 6;
	private static int MAX_ITERATION = 2;
	private static int SEMI_MAX_ITERATION = 1;
	private Double thisLogLikelihood = 0.0;
	private Double prevLogLikelihood = 0.0;
	private Double LAMBDA = Math.log10(0.5);
	private static Double INI = -2000.0;

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
	public HashMap translationNew = new HashMap();
	public HashMap translationInput = new HashMap();
	public HashMap translationSeed = new HashMap();
	public HashMap interpolation = new HashMap();
	public HashMap<Integer, ArrayList> graphDepth = new HashMap<Integer, ArrayList>();

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

	public void calcAlpha(HashMap gDepth, HashMap aGivenb) {

		String prev = "<s>";
		String curr = "<s>";

		Object[] key1 = gDepth.keySet().toArray();
		Arrays.sort(key1);

		for (int j = 0; j < key1.length; j++) {
			Iterator it3 = ((ArrayList) gDepth.get(key1[j])).iterator();
			Double temp;

			prev = curr;

			while (it3.hasNext()) { // nodes inside a particular key of gdepth
				temp = INI;
				Mnode a = (Mnode) it3.next();
				ArrayList<Medge> eit = (ArrayList<Medge>) a.getIncoming();

				if (eit.size() < 1) {
					a.setAlpha(0.0);
				} else {

					for (int i = 0; i < eit.size(); i++) {
						curr = eit.get(i).getTransition();


						Double tt = (eit.get(i).getStartNode().getAlpha() + (Double) eg
								.get(eit.get(i).getTransition()));
						temp = addLogCount(temp, tt);
					}
					a.setAlpha(temp);
					alphaTotal = temp;
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
				str = key + "\t" + Math.pow(10, (Double) gemmaNew.get(key));
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


	public String calcViterbiStr(HashMap gDepth) {
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
					a.setViterbiStr("<s>");

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
		return tempStr;
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

			if (Math.abs(thisLogLikelihood - prevLogLikelihood) < 0.000000001) {
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
		//String name = "translatScore" + alphaWB;
		String name = "translatScore" + 0.0;
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

	public void printTranslationScoreUnsupervised(ArrayList input, HashMap translationNew,
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

	}

	private void bigramViterbiProb(HashMap gemmaOld, HashMap aGivenb, String viterbiStr, HashMap backoffEstimatesB){
		String [] viterbiArr = viterbiStr.split("NNN");

		double viterbiProb = 0.0;
		String prev = "";
		String curr = "";

		for(int i =1; i < viterbiArr.length; i++){
			if(i == 1)
			{
				prev = "<s>";
				curr = viterbiArr[i];
				if(aGivenb.containsKey(prev+"NNN"+curr)){
					viterbiProb = viterbiProb + (Double)  aGivenb.get(prev+"NNN"+curr);
				}
				else
				{
					if(backoffEstimatesB.containsKey(prev)){
						viterbiProb = viterbiProb + (Double) backoffEstimatesB.get(prev) + (Double)  gemmaOld.get(curr);	
					}
					else
					{
						viterbiProb = viterbiProb + (Double)  gemmaOld.get(curr);
					}

					//viterbiProb = viterbiProb + (Double)  gemmaOld.get(prev) + (Double) gemmaOld.get(curr);
				}

			}
			else
			{
				prev = curr;
				curr = viterbiArr[i];

				if(aGivenb.containsKey(prev+"NNN"+curr)){
					viterbiProb = viterbiProb + (Double)  aGivenb.get(prev+"NNN"+curr);
				}
				else
				{
					if(backoffEstimatesB.containsKey(prev)){
						viterbiProb = viterbiProb + (Double) backoffEstimatesB.get(prev) + (Double)  gemmaOld.get(curr);	
					}
					else
					{
						viterbiProb = viterbiProb + (Double)  gemmaOld.get(curr);
					}

				}
			}
		}
		alphaTotal = viterbiProb;
	}

	private double smoothedUnigram(HashMap gemmaNew, Double totalFreq, ArrayList input, ArrayList test){

		// c(q) + no.of.observed.tpes.multigrms*1/M   /   c(*) + no.of.observed.tpes.multigrms 
		// c(q) + no.of.observed.tpes.multigrms*p(q)   /   c(*) + no.of.observed.tpes.multigrms

		HashMap gemmaCount = new HashMap();

		Iterator it2 = gemmaNew.keySet().iterator();
		while (it2.hasNext()) {
			String key = (String) it2.next();
			Double count = 0.0;
			count = totalFreq + (Double) gemmaNew.get(key);
			gemmaCount.put(key, count);
		}

		Double observedTypes = Math.log10(gemmaNew.size());
		Double unknownProb = calculateM(input, test);

		HashMap gemmaO = new HashMap();
		gemmaO.putAll(gemmaNew);

		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaNew.put(key, INI);
		}

		Double denomenator = addLogCount(totalFreq, observedTypes);

		Iterator it3 = gemmaO.keySet().iterator();
		while (it3.hasNext()) {
			String key = (String) it3.next();
			Double count = (Double) gemmaCount.get(key);
			Double temp2 = observedTypes + (Double) gemmaO.get(key);
			Double numerator = addLogCount(temp2, count);
			gemmaNew.put(key, numerator - denomenator);
		}    

		Double temp1 = unknownProb + observedTypes;
		Double unknownSmoothProb = temp1 - denomenator;
		return unknownSmoothProb;
	}

	private double calculateM(ArrayList input, ArrayList test)
	{
		HashMap src = new HashMap();
		HashMap tar = new HashMap();

		Iterator arrlist = input.iterator();
		while(arrlist.hasNext())
		{
			String src_tar = (String) arrlist.next();
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();

			for (int i = 0; i <= source.length() - 1; i++) {
				src.put(source.charAt(i), 0);
			}
			for (int i = 0; i <= target.length() - 1; i++) {
				tar.put(target.charAt(i), 0);
			}

		}
		Iterator arrlist2 = test.iterator();
		while(arrlist2.hasNext())
		{
			String src_tar = (String) arrlist2.next();
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();

			for (int i = 0; i <= source.length() - 1; i++) {
				src.put(source.charAt(i), 0);
			}
			for (int i = 0; i <= target.length() - 1; i++) {
				tar.put(target.charAt(i), 0);
			}
		}

		Double unknownScore = (src.size() + 1) * (tar.size() + 1) * 1.0;
		return Math.log10(1.0/unknownScore);
	}

	public void main(ArrayList input, HashMap gemmaNew, HashMap graph, Double alphaWB, ArrayList test,
			HashMap translationTest, int totalEdgesInTrain, int tiRules, int charAlign, HashMap aGivenb,
			HashMap backoffEstimates, Double lambda, ArrayList seed, HashMap translationTrain, Double totalFreq) {

		// take transition probability and value of nodes from the trained system
		graph = new HashMap();
		ArrayList viterbiArray = new ArrayList();
		HashMap<Integer, Double> testEditDistance = new HashMap<Integer, Double>();
		HashMap<Integer, Double> seedEditDistance = new HashMap<Integer, Double>();

		lambda = Math.log10(0.5); // reesimating for test and it is different from train
		
		String likelihood = "";
		String lambdaStr = "";
		Double prevLikelihood = 0.0;
		int inputNum;

		double unigramSmoothedProb = smoothedUnigram(gemmaNew, totalFreq, input, test); // WB for unigram smoothing

		semiTest_Bigram1_smooth obj = new semiTest_Bigram1_smooth();
		obj.translation.putAll(translationTest);

		inputNum = 0;

		printHash(gemmaNew, "gemmaNew");

		gemmaOld.putAll(gemmaNew);

		Iterator it1 = gemmaNew.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			gemmaNew.put(key, INI);
		}

		// this loop try to optimze the value of lambda
		boolean flag = true;
		while(flag)
		{
			inputNum = 0;
			Iterator itInput = test.iterator();

			while (itInput.hasNext()) {
				String src_tar = (String) itInput.next();

				obj.tokenize(src_tar);
				obj.makeNode(0, 0, null); // set transition probability here
				obj.calcGraphDepth(obj.graphDepth, obj.nd);

				Iterator it2 = obj.eg.keySet().iterator();
				while (it2.hasNext()) {
					String key = (String) it2.next();
					if(gemmaOld.containsKey(key)){
						obj.eg.put(key, gemmaOld.get(key));	
					}
					else{
						//		obj.eg.put(key, Math.log10(1.0/totalEdgesInTrain));
						// update gemmaOld to print it later for tiRules
						//		gemmaOld.put(key, Math.log10(1.0/totalEdgesInTrain));
						obj.eg.put(key, unigramSmoothedProb);
						gemmaOld.put(key, unigramSmoothedProb);
					}
				}
				obj.eg.put("<s>", gemmaOld.get("<s>"));
				obj.eg.put("<-s>", gemmaOld.get("<-s>"));

				//				obj.calcAlpha(obj.graphDepth, aGivenb);

				String viterbiString = obj.calcViterbiStr(obj.graphDepth) + "NNN<-s>";
				// calculate bigram transliteration probability for the viterbi string and use that value as alphaTotal
				obj.bigramViterbiProb(gemmaOld, aGivenb, viterbiString, backoffEstimates);

				obj.translatProbNorm(inputNum, obj.alphaTotal, lambda);
				//	viterbiStrArray(src_tar, obj.graphDepth, viterbiArray);
				//				testEditDistance.put(inputNum, obj.calcEditDistance(src_tar, obj.alphaTotal));

				obj.nd = new HashMap();
				obj.graphDepth = new HashMap();
				obj.eg = new HashMap();
				inputNum++;
			} // itInput ends


			Double totalNoti = -2000.0;
			Iterator it2 = obj.translationNew.keySet().iterator();
			while (it2.hasNext()) {
				Integer key = (Integer) it2.next();
				totalNoti = addLogCount((Double) obj.translationNew.get(key), totalNoti);
			}
			double newLambda = Math.pow(10, (totalNoti - Math.log10(obj.translationNew.size())));
			//		System.out.println("no ti value " + Math.pow(10, (totalNoti - Math.log10(obj.translationNew.size()))));


			//		double newLambda = calculateLambda(lambda, obj.translationNew);
			double temp2 = newLambda - Math.pow(10, lambda); 
			//		
			if(temp2 < 0.000000001)
			{
				flag = false;
			}
			if(temp2 < 0) // means going back
			{
				flag = false;
			}
			else
			{
				System.out.println("Lambda = " + newLambda);
				lambda = Math.log10(newLambda);
			}
			//flag = false;
		} // flag loop ends

		/*	Output out = new Output();
			out.printOutput("", false, "viterbiScore");
			Iterator va = viterbiArray.iterator();
			while(va.hasNext())
			{
				String str = (String) va.next();
				out.printOutput(str, true, "viterbiScore");
			}*/

		// 888888888888888888888888888       on seed data
		double smallest = 10.0;
		double average = 0.0;

		inputNum = 0;
		Iterator itSeed = seed.iterator();
		while(itSeed.hasNext()){

			String src_tar = (String) itSeed.next();

			obj.tokenize(src_tar);
			obj.makeNode(0, 0, null); // set transition probability here
			obj.calcGraphDepth(obj.graphDepth, obj.nd);
			Iterator it2 = obj.eg.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				obj.eg.put(key, gemmaOld.get(key));
			}

			obj.eg.put("<s>", gemmaOld.get("<s>"));
			obj.eg.put("<-s>", gemmaOld.get("<-s>"));

			String viterbiString = obj.calcViterbiStr(obj.graphDepth) + "NNN<-s>";
			// calculate bigram transliteration probability for the viterbi string and use that value as alphaTotal
			obj.bigramViterbiProb(gemmaOld, aGivenb, viterbiString, backoffEstimates);

			//		double newValue = obj.calcEditDistanceSeed(src_tar, obj.alphaTotal);
			//		seedEditDistance.put(inputNum, newValue);

			obj.nd = new HashMap();
			obj.graphDepth = new HashMap();
			obj.eg = new HashMap();
			inputNum++;

			//		average = average + newValue;

			//		if(smallest > newValue){
			//			smallest = newValue;
			//		}

			obj.nd = new HashMap();
			obj.graphDepth = new HashMap();
			obj.eg = new HashMap();
			inputNum++;
		}		

		//	obj.predictSeed(gemmaOld, seed, input.size(), aGivenb, backoffEstimates, lambda,	translationTrain);

		if(tiRules == 1){ // if user provide a file to print transliteration rules with their probabilities
			obj.printGemma(gemmaOld, "transliterationUnits");
			System.out.println("Transliteration rules are output to file " + "transliterationUnits" );
		}

		if(charAlign == 1){
			// claculate viterbi probabilities and print viterbi score of alignment for each word pair. You should print words with probabilties as below
			System.out.println("Viterbi alignments with viterbi score are output to file " + "alignment" );
		}


		// obj.printGemma(gemmaNew);
		// for (Double i = 0.0; i <= 0.09; i = i + 0.01) {

		//System.out.println(i);
		if(alphaWB == 0.0){
			obj.printTranslationScoreUnsupervised(test, obj.translationNew, 0.0);
		}else{
			obj.printTranslationScore(test, obj.translationNew, 0.0, alphaWB);	
		}

	}

	private void viterbiStrArray(String src_tar, HashMap gDepth, ArrayList viterbiArray)
	{
		Object[] key = gDepth.keySet().toArray();
		Arrays.sort(key);

		Output obj = new Output();
		Mnode temper = (Mnode) ((ArrayList) gDepth.get(key[key.length - 1]))
		.get(0);
		Double vitProb = temper.getViterbi() / src_tar.length() -1;
		viterbiArray.add(src_tar + "\t" + Math.pow(10, vitProb));
	}

	public double translatProbNormReturn(int key, Double alphaTotal, Double LAMBDA, HashMap translationTrain) {
		// p(ta|e,f)
		Double temp1, temp2, temp3;
		Double oneMinusLambda = Math.log10(1 - Math.pow(10, LAMBDA));
		temp1 = oneMinusLambda + alphaTotal;
		temp2 = LAMBDA + (Double) translationTrain.get(key);
		temp3 = temp1 - temp2;

		Double prob = 1 / (1 + Math.pow(10, temp3));
		//Double logProb = Math.log10(prob);
		return prob;
	}

	private void predictSeed(HashMap gemmaOld, ArrayList seed, int inputSize, HashMap aGivenb,
			HashMap backoffEstimates, double lambda, HashMap translationTrain)
	{
		int i =0;
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter("seedProb"));

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

				eg.put("<s>", gemmaOld.get("<s>"));
				eg.put("<-s>", gemmaOld.get("<-s>"));

				String viterbiString = calcViterbiStr(graphDepth) + "NNN<-s>";
				// calculate bigram transliteration probability for the viterbi string and use that value as alphaTotal
				bigramViterbiProb(gemmaOld, aGivenb, viterbiString, backoffEstimates);

				double prob = translatProbNormReturn(inputNum, alphaTotal, lambda, translationTrain); // return probability of a word pair

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

	private Double calcEditDistance(String src_tar, Double forwardLogProb){
		//Double forwardProb = Math.pow(10.0, forwardLogProb);

		Double src_tar_length = 1.0 * src_tar.length();
		Double newScore = forwardLogProb / src_tar_length; 

		return newScore;
	}

	private Double calcEditDistanceSeed(String src_tar, Double forwardLogProb){
		//Double forwardProb = Math.pow(10.0, forwardLogProb);

		Double src_tar_length =  1.0 * src_tar.length();
		Double newScore = forwardLogProb / src_tar_length; 

		return newScore;
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
}