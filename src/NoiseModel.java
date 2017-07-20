import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;


public class NoiseModel {
	private static int LOGADD_PRECISION = 6;
	private HashMap translationTrigramSrc = new HashMap();
	private HashMap translationBigramSrc = new HashMap();
	private HashMap translationUnigramSrc = new HashMap();
	private HashMap bigramTranslatProbSrc = new HashMap();
	private HashMap trigramTranslatProbSrc = new HashMap();
	
	private HashMap translationUnigramSrcProb = new HashMap();
	private HashMap translationUnigramTarProb = new HashMap();
	
	private HashMap translationTrigramTar = new HashMap();
	private HashMap translationBigramTar = new HashMap();
	private HashMap translationUnigramTar = new HashMap();
	private HashMap bigramTranslatProbTar = new HashMap();
	private HashMap trigramTranslatProbTar = new HashMap();
	
	private HashMap BigramSrcLogProb = new HashMap();
	private HashMap BigramTarLogProb = new HashMap();
	
	private HashMap TrigramSrcLogProb = new HashMap();
	private HashMap TrigramTarLogProb = new HashMap();
	
	private HashMap bigramTranslatWordProb = new HashMap();
	private HashMap trigramTranslatWordProb = new HashMap();
	private HashMap trigramTranslatTrainProb = new HashMap();
	
	private HashMap backoffEstimateBCSrc = new HashMap();
	private HashMap backoffEstimateBCTar = new HashMap();
	
	private HashMap backoffEstimateBSrc = new HashMap();
	private HashMap backoffEstimateBTar = new HashMap();
	
	private HashMap<Character, Double> srcChar = new HashMap<Character, Double>();
	private HashMap<Character, Double> tarChar = new HashMap<Character, Double>();
	private HashMap<String, Double> srcWord = new HashMap<String, Double>();
	private HashMap<String, Double> tarWord = new HashMap<String, Double>();
	
	private HashMap alphaSrcB = new HashMap();
	private HashMap alphaTarB = new HashMap();
	
	private HashMap alphaSrcBC = new HashMap();
	private HashMap alphaTarBC = new HashMap();
	
	private double srcCharTotal = 0, tarCharTotal = 0, unknownSrcProb = 0, unknownTarProb = 0;
	
	public void test(ArrayList test, HashMap<Integer,Double> translationTest, int ngram)
	{
		// probability calculation of test corpus
		HashMap testUnigramProb = new HashMap();
		HashMap testBigramProb = new HashMap();
		HashMap testTrigramProb = new HashMap();
		
				int inputNum = 0;
				Iterator itTes = test.iterator();
				while (itTes.hasNext()) {
					String src_tar = (String) itTes.next();
					StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
					String source = st.nextToken();
					String target = st.nextToken();
					//Double prob = translationLogProbTest(src_tar,srcChar, tarChar, srcWord, tarWord, srcCharTotal, tarCharTotal); // this calculates the translation probability
					//translationTest.put(inputNum, prob); // unigram noise probability
					
					Double srcProb = translatUnigramLogProbTest(source, translationUnigramSrcProb, srcCharTotal, unknownSrcProb);
					Double tarProb = translatUnigramLogProbTest(target, translationUnigramTarProb, tarCharTotal, unknownTarProb);
					testUnigramProb.put(inputNum, srcProb + tarProb);
					
					Double bigramLogProbSrc = calcBigramTranslatWordProb(source, BigramSrcLogProb, translationUnigramSrcProb, backoffEstimateBSrc, srcCharTotal);
					Double bigramLogProbTar = calcBigramTranslatWordProb(target, BigramTarLogProb, translationUnigramTarProb,backoffEstimateBTar, tarCharTotal);
					testBigramProb.put(inputNum, bigramLogProbSrc + bigramLogProbTar);
					
					Double trigramLogProbSrc = calcTrigramTranslatWordProb(source, TrigramSrcLogProb, BigramSrcLogProb, translationUnigramSrcProb,
							backoffEstimateBCSrc, backoffEstimateBSrc, srcCharTotal);
					Double trigramLogProbTar = calcTrigramTranslatWordProb(target, TrigramTarLogProb, BigramTarLogProb, translationUnigramTarProb,
							backoffEstimateBCTar, backoffEstimateBTar, tarCharTotal);
					testTrigramProb.put(inputNum, trigramLogProbSrc + trigramLogProbTar);

					inputNum++;
				}
				
				if(ngram == 1)
				{
					translationTest.putAll(testUnigramProb);
				}
				else if(ngram == 2)
				{
					translationTest.putAll(testBigramProb);
				}
				else if (ngram == 3)
				{
					translationTest.putAll(testTrigramProb);
				}
	}
	
	
	
	public void train(ArrayList noiseCorpus)
	{
		
		Iterator itT = noiseCorpus.iterator();
		while (itT.hasNext()) {
			String src_tar = (String) itT.next();
			if(src_tar.length() > 1)
			{	
		//	System.out.println(src_tar);
		//	tokenize(src_tar , srcChar, tarChar, srcWord, tarWord);
			
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();
			
			bigramTranslationCounts(source, translationUnigramSrc, translationBigramSrc);
			bigramTranslationCounts(target, translationUnigramTar, translationBigramTar);
			
			trigramTranslationCounts(source, translationUnigramSrc, translationBigramSrc, translationTrigramSrc);
			trigramTranslationCounts(target, translationUnigramTar, translationBigramTar, translationTrigramTar);
			}
		}
		
		srcCharTotal = translatUnigramLogProb(translationUnigramSrc, translationUnigramSrcProb, srcCharTotal);
		unknownSrcProb = translatUnknownLogProb(translationUnigramSrc, translationUnigramSrcProb, srcCharTotal);
		tarCharTotal = translatUnigramLogProb(translationUnigramTar, translationUnigramTarProb, tarCharTotal);
		unknownTarProb = translatUnknownLogProb(translationUnigramTar, translationUnigramTarProb, tarCharTotal);
				
		alphaOfB(translationBigramSrc, alphaSrcB);
		alphaOfB(translationBigramTar, alphaTarB);
		
		alphaOfBC(translationTrigramSrc, alphaSrcBC);
		alphaOfBC(translationTrigramTar, alphaTarBC);
		
		smoothedBigramTranslationProbability(translationUnigramSrc, translationUnigramSrcProb, translationBigramSrc, alphaSrcB, BigramSrcLogProb);
		smoothedBigramTranslationProbability(translationUnigramTar, translationUnigramTarProb, translationBigramTar, alphaTarB, BigramTarLogProb);
		
		smoothedTrigramTranslationProbability(translationBigramSrc, BigramSrcLogProb, translationTrigramSrc, alphaSrcBC, TrigramSrcLogProb);
		smoothedTrigramTranslationProbability(translationBigramTar, BigramTarLogProb, translationTrigramTar, alphaTarBC, TrigramTarLogProb);
		
		bigramBackoffEstimates(alphaSrcBC, backoffEstimateBCSrc, translationBigramSrc, TrigramSrcLogProb);
		bigramBackoffEstimates(alphaTarBC, backoffEstimateBCTar, translationBigramTar, TrigramTarLogProb);
		
		unigramBackoffEstimates(alphaSrcB, backoffEstimateBSrc, translationUnigramSrc, BigramSrcLogProb);
		unigramBackoffEstimates(alphaTarB, backoffEstimateBTar, translationUnigramTar, BigramTarLogProb);
	}
	
	public void trainSrc(ArrayList noiseCorpus)
	{
		
		Iterator itT = noiseCorpus.iterator();
		while (itT.hasNext()) {
			String source = (String) itT.next();
			
			bigramTranslationCounts(source, translationUnigramSrc, translationBigramSrc);
			trigramTranslationCounts(source, translationUnigramSrc, translationBigramSrc, translationTrigramSrc);
		}
		
		srcCharTotal = translatUnigramLogProb(translationUnigramSrc, translationUnigramSrcProb, srcCharTotal);
		unknownSrcProb = translatUnknownLogProb(translationUnigramSrc, translationUnigramSrcProb, srcCharTotal);
		
		alphaOfB(translationBigramSrc, alphaSrcB);
		alphaOfBC(translationTrigramSrc, alphaSrcBC);
		
		smoothedBigramTranslationProbability(translationUnigramSrc, translationUnigramSrcProb, translationBigramSrc, alphaSrcB, BigramSrcLogProb);
		
		smoothedTrigramTranslationProbability(translationBigramSrc, BigramSrcLogProb, translationTrigramSrc, alphaSrcBC, TrigramSrcLogProb);
		
		bigramBackoffEstimates(alphaSrcBC, backoffEstimateBCSrc, translationBigramSrc, TrigramSrcLogProb);
		
		unigramBackoffEstimates(alphaSrcB, backoffEstimateBSrc, translationUnigramSrc, BigramSrcLogProb);
	}
	
	public void trainTar(ArrayList noiseCorpus)
	{
		
		Iterator itT = noiseCorpus.iterator();
		while (itT.hasNext()) {
			String target = (String) itT.next();
	
			bigramTranslationCounts(target, translationUnigramTar, translationBigramTar);
			trigramTranslationCounts(target, translationUnigramTar, translationBigramTar, translationTrigramTar);

		}
		tarCharTotal = translatUnigramLogProb(translationUnigramTar, translationUnigramTarProb, tarCharTotal);
		unknownTarProb = translatUnknownLogProb(translationUnigramTar, translationUnigramTarProb, tarCharTotal);
		
		alphaOfB(translationBigramTar, alphaTarB);
		
		alphaOfBC(translationTrigramTar, alphaTarBC);
		
		smoothedBigramTranslationProbability(translationUnigramTar, translationUnigramTarProb, translationBigramTar, alphaTarB, BigramTarLogProb);
		
		smoothedTrigramTranslationProbability(translationBigramTar, BigramTarLogProb, translationTrigramTar, alphaTarBC, TrigramTarLogProb);
		
		bigramBackoffEstimates(alphaTarBC, backoffEstimateBCTar, translationBigramTar, TrigramTarLogProb);
		
		unigramBackoffEstimates(alphaTarB, backoffEstimateBTar, translationUnigramTar, BigramTarLogProb);
	}
	
	private void unigramBackoffEstimates(HashMap alphaB, HashMap backoffEstimateB, HashMap unigramMultigram, HashMap aGivenb)
	{
		// alphaB / freqB + alphaB
		// later when needed, we multipy it with p(a) which is gemmaNew
		
		Iterator it = aGivenb.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			
			String [] pair = key.split("<>"); // first one is b and second one is a. first,second format
			
			double currAlphaB = Math.log10((Double) alphaB.get(pair[0]));
			double freqB = Math.log10((Double) unigramMultigram.get(pair[0]));
			Double denomerator = addLogCount(currAlphaB, freqB);
			
			backoffEstimateB.put(pair[0], currAlphaB - denomerator);
		}
	}

	private void bigramBackoffEstimates(HashMap alphaBC, HashMap backoffEstimateBC,
			HashMap bigramMultigram, HashMap aGivenbc)
	{
		// alphaBC / freqBC + alphaBC
		// later when needed, we multipy it with p(a|c) which is bGivena
		
		Iterator it = aGivenbc.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			
			String [] pair = key.split("<>"); // first one is b and second one is a. first,second format
			
			double currAlphaBC = Math.log10((Double) alphaBC.get(pair[0]+"<>"+pair[1]));
			
			Double freqBC = Math.log10((Double) bigramMultigram.get(pair[0]+"<>"+pair[1])); 
			
			Double denomerator = addLogCount(currAlphaBC, freqBC);
			
			backoffEstimateBC.put(pair[0]+"<>"+pair[1], currAlphaBC - denomerator);
		}
	}

	
	private void smoothedTrigramTranslationProbability(HashMap translationBigram, HashMap translationBigramLogProb,
			HashMap translationTrigram, HashMap alphaBC, HashMap aGivenbc)
	{
		Iterator it = translationTrigram.keySet().iterator();
		while(it.hasNext())
		{
			String trigramPair = (String) it.next();
			Double trigramCount = (Double) translationTrigram.get(trigramPair);
			
			String [] pair = trigramPair.split("<>");
			String bigram = pair[1]+"<>"+pair[2];
			Double bigramProb = Math.pow(10, (Double) translationBigramLogProb.get(bigram));
			
			Double alpha = (Double) alphaBC.get(pair[0]+"<>"+pair[1]);
			
			Double freqBC = (Double) translationBigram.get(pair[0]+"<>"+pair[1]);
			
			Double numerator = trigramCount + (alpha * bigramProb);
			Double denomerator = alpha + freqBC;
			
			double value = Math.log10(numerator) - Math.log10(denomerator);
			aGivenbc.put(trigramPair, value);
		}
	}
	
	private void smoothedBigramTranslationProbability(HashMap translationUnigram, HashMap translationUnigramLogProb,
			HashMap translationBigram, HashMap alphaB, HashMap aGivenb)
	{
		Iterator it = translationBigram.keySet().iterator();
		while(it.hasNext())
		{
			String bigramPair = (String) it.next();
			Double bigramCount = (Double) translationBigram.get(bigramPair);
			
			String [] pair = bigramPair.split("<>");
			String unigram = pair[1];
			Double unigramProb = Math.pow(10, (Double) translationUnigramLogProb.get(unigram));
			
			Double alpha = (Double) alphaB.get(pair[0]);
			
			Double freqB = (Double) translationUnigram.get(pair[0]);
			
			Double numerator = bigramCount + (alpha * unigramProb);
			Double denomerator = alpha + freqB;
			
			double value = Math.log10(numerator) - Math.log10(denomerator);
			aGivenb.put(bigramPair, value);
			
		}
	}

	public double translatUnigramLogProbTest(String src, HashMap unigramProb, double charTotal, double unknown) {
		String curr = "";
		Double logProb = 0.0;
		//Double unknown = Math.log10(1.0/charTotal);
		Double aaaa=0.0;
		for(int i = 0; i < src.length(); i++){
			curr = Character.toString(src.charAt(i));
			
			if(unigramProb.containsKey(curr)){
				logProb = logProb + (Double) unigramProb.get(curr);
				aaaa = Math.pow(10, (Double) unigramProb.get(curr));
			}
			else
			{
				logProb = logProb + unknown;
			}	
		}
		return logProb;
	}
	
	public double translatUnigramLogProb(HashMap unigramCount, HashMap unigramProb, double charTotal) {
		
		Iterator it1 = unigramCount.keySet().iterator();
		while (it1.hasNext()) {
			String strTemp = (String) it1.next();
			
			charTotal = charTotal + (Double) unigramCount.get(strTemp);
		}

	//	charTotal++; // to make it true probability distribution , now everything is smoothed using WB
		
		Iterator it = unigramCount.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Double aa = (Double) unigramCount.get(key) / charTotal;

			unigramProb.put(key, Math.log10(aa));
		}
		return charTotal;
	}
	
	private double translatUnknownLogProb(HashMap unigramCount, HashMap unigramProb, double charTotalTokens)
	{
		HashMap unigramProbTemp = new HashMap();
		unigramProbTemp.putAll(unigramProb);
		
		Double alpha = Math.log10(unigramCount.size());
		Double totalCount = Math.log10(charTotalTokens);
		
		Iterator it = unigramCount.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			
			Double count = Math.log10((Double) unigramCount.get(key));
			
			Double unsmoothProb = (Double) unigramProbTemp.get(key);
			
			Double tmp1 = unsmoothProb + alpha;
			Double numenator = addLogCount(tmp1, count);
			
			Double denomenator = addLogCount(totalCount, alpha);
			
			Double newValue = numenator - denomenator;			
			
			unigramProb.put(key, newValue);
		}
		
		// for unknown
		Double tmp1 =  alpha + Math.log10(unigramCount.size() * 2); // this is an assumtion of M
		//Double tmp1 =  alpha + Math.log10(200.0);
		Double numenator = tmp1;
		
		Double denomenator = addLogCount(totalCount, alpha);
		
		Double unknownProb = numenator - denomenator;
		
		return unknownProb;

	}

	public void alphaOfB(HashMap bigramMultigrams, HashMap alphaB)
	{
		Iterator it = bigramMultigrams.keySet().iterator();
		while (it.hasNext()) {
			String [] key = ((String) it.next()).split("<>");
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

	public void alphaOfBC(HashMap trigramMultigrams, HashMap alphaBC)
	{
		Iterator it = trigramMultigrams.keySet().iterator();
		while (it.hasNext()) {
			String [] key = ((String) it.next()).split("<>");
			if(alphaBC.containsKey(key[0]+"<>"+key[1]))
			{
				alphaBC.put(key[0]+"<>"+key[1], (Double) alphaBC.get(key[0]+"<>"+key[1]) + 1.0);
			}
			else
			{
				alphaBC.put(key[0]+"<>"+key[1], 1.0);
			}
		}
	}

	
	private double calcBigramTranslatWordProb(String source, HashMap bigramTranslatProb, HashMap unigramTranslatProb
			, HashMap backoffEstimateB, Double totalChar)
	{
		Double prob = 0.0;
		Double unknownLogProb = Math.log10(1.0/totalChar);
		
		String curr = "", prev = "<s>";
		
		for(int i = 0; i < source.length(); i++){
			curr = Character.toString(source.charAt(i));
			String pair = prev+"<>"+curr;
			
			if(bigramTranslatProb.containsKey(pair))
			{
				prob = prob + (Double) bigramTranslatProb.get(pair);	
			}
			else if(unigramTranslatProb.containsKey(curr))
			{
				if(backoffEstimateB.containsKey(prev))
				{
					prob = prob + (Double) unigramTranslatProb.get(curr) + (Double) backoffEstimateB.get(prev);
				}
				else // nothing in alpha..then 1/1+1
				{
					prob = prob + (Double) unigramTranslatProb.get(curr) + Math.log10(0.5);
				}
			}
			else
			{
				if(backoffEstimateB.containsKey(prev))
				{
					prob = prob + unknownLogProb + (Double) backoffEstimateB.get(prev);
				}
				else // nothing in alpha..then 1/1+1
				{
					prob = prob + unknownLogProb + Math.log10(0.5);
				}
			}
			
			if(i+1 == source.length()){
				pair = curr+"<>"+"<-s>";
				
				if(bigramTranslatProb.containsKey(pair))
				{
					prob = prob + (Double) bigramTranslatProb.get(pair);	
				}
				else if(unigramTranslatProb.containsKey(curr))
				{
					if(backoffEstimateB.containsKey(prev))
					{
						prob = prob + (Double) unigramTranslatProb.get(curr) + (Double) backoffEstimateB.get(prev);
					}
					else // nothing in alpha..then 1/1+1
					{
						prob = prob + (Double) unigramTranslatProb.get(curr) + Math.log10(0.5);
					}
				}
				else
				{
					if(backoffEstimateB.containsKey(prev))
					{
						prob = prob + unknownLogProb + (Double) backoffEstimateB.get(prev);
					}
					else // nothing in alpha..then 1/1+1
					{
						prob = prob + unknownLogProb + Math.log10(0.5);
					}
				}
				
				//prob = prob + (Double) bigramTranslatProb.get(pair);
				pair = "<-s>"+"<>"+"<-ss>";
				prob = prob + (Double) bigramTranslatProb.get(pair);
				pair = "<ss>"+"<>"+"<s>";
				prob = prob + (Double) bigramTranslatProb.get(pair);
			}
				
			prev = curr;
		}	
			
		return prob;
	}
	
	private double calcTrigramTranslatWordProb(String source, HashMap trigramTranslatProb, HashMap bigramTranslatProb,
			HashMap unigramTranslatProb, HashMap backoffEstimateBC, HashMap backoffEstimateB, Double totalChar )
	{
		Double prob = 0.0;
		Double unknownLogProb = Math.log10(1.0/totalChar);
		
		String curr = "", prev0 = "<ss>", prev1 = "<s>";
		
		for(int i = 0; i < source.length(); i++){
			curr = Character.toString(source.charAt(i));
			String pair = prev0+"<>"+prev1+"<>"+curr;
			
			if(trigramTranslatProb.containsKey(pair))
			{
				prob = prob + (Double) trigramTranslatProb.get(pair);	
			}
			else
				if(backoffEstimateBC.containsKey(prev0 + "<>" + prev1) && bigramTranslatProb.containsKey(prev1 + "<>" + curr))
				{
					prob = prob + (Double) backoffEstimateBC.get(prev0 + "<>" + prev1) + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
				}
				else if(bigramTranslatProb.containsKey(prev1 + "<>" + curr))
				{
					prob = prob + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
				}
				else if(unigramTranslatProb.containsKey(curr))
				{
					if(backoffEstimateB.containsKey(prev1))
					{
						prob = prob + (Double) unigramTranslatProb.get(curr) + (Double) backoffEstimateB.get(prev1);
					}
					else // nothing in alpha..then 1/1+1
					{
						prob = prob + (Double) unigramTranslatProb.get(curr) + Math.log10(0.5);
					}
				}
				else
				{
					if(backoffEstimateB.containsKey(prev1))
					{
						prob = prob + unknownLogProb + (Double) backoffEstimateB.get(prev1);
					}
					else // nothing in alpha..then 1/1+1
					{
						prob = prob + unknownLogProb + Math.log10(0.5);
					}
				}
			
			if(i+1 == source.length()){				
				pair = prev1+"<>"+curr+"<>"+"<-s>";
				//prob = prob + (Double) trigramTranslatProb.get(pair);
				if(trigramTranslatProb.containsKey(pair))
				{
					prob = prob + (Double) trigramTranslatProb.get(pair);	
				}
				else
					if(backoffEstimateBC.containsKey(prev0 + "<>" + prev1) && bigramTranslatProb.containsKey(prev1 + "<>" + curr))
					{
						prob = prob + (Double) backoffEstimateBC.get(prev0 + "<>" + prev1) + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
					}
					else if(bigramTranslatProb.containsKey(prev1 + "<>" + curr))
					{
						prob = prob + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
					}
					else if(unigramTranslatProb.containsKey(curr))
					{
						if(backoffEstimateB.containsKey(prev1))
						{
							prob = prob + (Double) unigramTranslatProb.get(curr) + (Double) backoffEstimateB.get(prev1);
						}
						else // nothing in alpha..then 1/1+1
						{
							prob = prob + (Double) unigramTranslatProb.get(curr) + Math.log10(0.5);
						}
					}
					else
					{
						if(backoffEstimateB.containsKey(prev1))
						{
							prob = prob + unknownLogProb + (Double) backoffEstimateB.get(prev1);
						}
						else // nothing in alpha..then 1/1+1
						{
							prob = prob + unknownLogProb + Math.log10(0.5);
						}
					}

				pair = curr+"<>"+"<-s>"+"<>"+"<-ss>";
				//prob = prob + (Double) trigramTranslatProb.get(pair);
				if(trigramTranslatProb.containsKey(pair))
				{
					prob = prob + (Double) trigramTranslatProb.get(pair);	
				}
				else
					if(backoffEstimateBC.containsKey(prev0 + "<>" + prev1) && bigramTranslatProb.containsKey(prev1 + "<>" + curr))
					{
						prob = prob + (Double) backoffEstimateBC.get(prev0 + "<>" + prev1) + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
					}
					else if(bigramTranslatProb.containsKey(prev1 + "<>" + curr))
					{
						prob = prob + (Double) bigramTranslatProb.get(prev1 + "<>" + curr);
					}
					else if(unigramTranslatProb.containsKey(curr))
					{
						if(backoffEstimateB.containsKey(prev1))
						{
							prob = prob + (Double) unigramTranslatProb.get(curr) + (Double) backoffEstimateB.get(prev1);
						}
						else // nothing in alpha..then 1/1+1
						{
							prob = prob + (Double) unigramTranslatProb.get(curr) + Math.log10(0.5);
						}
					}
					else
					{
						if(backoffEstimateB.containsKey(prev1))
						{
							prob = prob + unknownLogProb + (Double) backoffEstimateB.get(prev1);
						}
						else // nothing in alpha..then 1/1+1
						{
							prob = prob + unknownLogProb + Math.log10(0.5);
						}
					}

			}
			prev0 = prev1;
			prev1 =  curr;
		}	
			
		return prob;
	}

	public Double translationLogProbTest(String src_tar, HashMap<Character, Double> srcChar,
			HashMap<Character, Double> tarChar, HashMap<String, Double> srcWord, HashMap<String, Double> tarWord
			, double srcCharTotal, double tarCharTotal) {
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
				srcChar.put(sr, tempSrcProb);
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
				tarChar.put(tr, tempTarProb);
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

	public void translatCharLogProb(HashMap<Character, Double> srcChar,
			HashMap<Character, Double> tarChar, HashMap<String, Double> srcWord, HashMap<String, Double> tarWord,
			double srcCharTotal, double tarCharTotal) {
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

	private void bigramTranslationProbability(HashMap translationUnigram, HashMap translationBigram, HashMap bigramTranslatProb){
		
		Iterator it = translationBigram.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String [] pair = key.split("<>");
			
			Double prob = (Double) translationBigram.get(key) / (Double) translationUnigram.get(pair[0]);
			
			bigramTranslatProb.put(key, prob);
		}		
		
	}
	
	
private void trigramTranslationProbability(HashMap translationBigram, HashMap translationTrigram, HashMap TrigramTranslatProb){
		
		Iterator it = translationTrigram.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String [] pair = key.split("<>");
			
			Double prob = (Double) translationTrigram.get(key) / (Double) translationBigram.get(pair[0]+"<>"+pair[1]);
			
			TrigramTranslatProb.put(key, prob);
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
				
				/*pair = "<ss>"+"<>"+"<s>";
				
				if(translationBigramSrc.containsKey(pair)){
					translationBigramSrc.put(pair, (Double) translationBigramSrc.get(pair) + 1);
				}
				else
				{  		translationBigramSrc.put(pair, 1.0);   	}*/
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
		
		/*if(translationUnigramSrc.containsKey("<ss>")){
			translationUnigramSrc.put("<ss>", (Double) translationUnigramSrc.get("<ss>") + 1);
		}
		else { 		translationUnigramSrc.put("<ss>", 1.0);    }*/

		if(translationUnigramSrc.containsKey("<s>")){
			translationUnigramSrc.put("<s>", (Double) translationUnigramSrc.get("<s>") + 1);
		}
		else
		{ 		translationUnigramSrc.put("<s>",  1.0);  }
		
}
	
	private void trigramTranslationCounts(String source, HashMap translationUnigramSrc, HashMap translationBigramSrc, 
			HashMap translationTrigramSrc){
		
		String curr = "", prev0 = "<ss>", prev1 = "<s>";
		for(int i = 0; i < source.length(); i++){
			curr = Character.toString(source.charAt(i));
			
			String pair = prev0+"<>"+prev1+"<>"+curr;
			
			if(translationTrigramSrc.containsKey(pair)){
				translationTrigramSrc.put(pair, (Double) translationTrigramSrc.get(pair) + 1);
			}
			else
			{ 		translationTrigramSrc.put(pair, 1.0);     }
			
			if(i+1 == source.length()){
				pair = prev1+"<>"+curr+"<>"+"<-s>";
				
				if(translationTrigramSrc.containsKey(pair)){
					translationTrigramSrc.put(pair, (Double) translationTrigramSrc.get(pair) + 1);
				}
				else
				{  		translationTrigramSrc.put(pair, 1.0);   	}
				
				pair = curr+"<>"+"<-s>"+"<>"+"<-ss>";
				
				if(translationTrigramSrc.containsKey(pair)){
					translationTrigramSrc.put(pair, (Double) translationTrigramSrc.get(pair) + 1);
				}
				else
				{  		translationTrigramSrc.put(pair, 1.0);   	}
			}
			prev0 = prev1;
			prev1 = curr;
		} // end of for
		
		if(translationUnigramSrc.containsKey("<-ss>")){
			translationUnigramSrc.put("<-ss>", (Double) translationUnigramSrc.get("<-ss>") + 1);
		}
		else { 		translationUnigramSrc.put("<-ss>", 1.0);    }
		
		if(translationBigramSrc.containsKey("<-s>"+"<>"+"<-ss>")){
			translationBigramSrc.put("<-s>"+"<>"+"<-ss>", (Double) translationBigramSrc.get("<-s>"+"<>"+"<-ss>") + 1);
		}
		else { 		translationBigramSrc.put("<-s>"+"<>"+"<-ss>", 1.0);    }
		
		if(translationUnigramSrc.containsKey("<ss>")){
			translationUnigramSrc.put("<ss>", (Double) translationUnigramSrc.get("<ss>") + 1);
		}
		else { 		translationUnigramSrc.put("<ss>", 1.0);    }
		
		if(translationBigramSrc.containsKey("<ss>"+"<>"+"<s>")){
			translationBigramSrc.put("<ss>"+"<>"+"<s>", (Double) translationBigramSrc.get("<ss>"+"<>"+"<s>") + 1);
		}
		else { 		translationBigramSrc.put("<ss>"+"<>"+"<s>", 1.0);    }
}

	
	public void tokenize(String src_tar, HashMap<Character, Double> srcChar,
			HashMap<Character, Double> tarChar, HashMap<String, Double> srcWord, HashMap<String, Double> tarWord) {
		StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
		String source = st.nextToken();
		String target = st.nextToken();

		srcWord.put(source, 0.0);
		tarWord.put(target, 0.0);

		char[] src = new char[source.length() + 1];
		char [] tar = new char[target.length() + 1];
		src[0] = ' ';
		tar[0] = ' ';

		for (int i = 0; i <= source.length() - 1; i++) {
			src[i + 1] = source.charAt(i);
			srcChar.put(src[i + 1], 0.0);
		}

		for (int i = 0; i <= target.length() - 1; i++) {
			tar[i + 1] = target.charAt(i);
			tarChar.put(tar[i + 1], 0.0);
		}
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

	
}
