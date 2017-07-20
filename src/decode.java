import java.io.*;
import java.util.*;

public class decode {
	
	
//	HashMap<String,Double> multigramsSrc2 = new HashMap<String, Double>(); // to keep target sdie and prob
	
	HashMap<String,HashMap> multigramsTar = new HashMap<String, HashMap>(); // for opposite decoding
	HashMap<String,Double> multigramsTar2 = new HashMap<String, Double>();
	
	public void decoderMain(ArrayList input, HashMap multigrams)
	{
		
		//input.clear();
		//input.add("aach\tххая");
		//input.add("aze\tхяй");
		
		HashMap<String,ArrayList> multigramsSrc = new HashMap<String,ArrayList>(); // to keep source side of multigram
		HashMap<String,ArrayList> multigramsTar = new HashMap<String,ArrayList>(); // to keep target side of multigram
		
		ArrayList<String> decodeArr = new ArrayList<String>();
		ArrayList<Double> probArr = new ArrayList<Double>();
		ArrayList<String> srcOutputTransliteration = new ArrayList<String>();
		ArrayList<String> tarOutputTransliteration = new ArrayList<String>();
		
		processMultigrams(multigrams, multigramsSrc);
		processMultigramsReverse(multigrams, multigramsTar);
		sortAndPruneMultigrams(multigramsSrc);
		sortAndPruneMultigrams(multigramsTar);
		
		Iterator it11 = input.iterator();
		while(it11.hasNext())
		{
			String src_tar= (String) it11.next();
		
			StringTokenizer st = new StringTokenizer(src_tar, "\t", false);
			String source = st.nextToken();
			String target = st.nextToken();
			//for source
			double best=0.0;
			int bestIndex = 0;
			
			decodeword(source, decodeArr, probArr, multigramsSrc);
			String tmp = bestTransliteration(probArr);
			String [] strtmp = tmp.split(" ");
			best = Double.parseDouble(strtmp[0]);
			bestIndex = Integer.parseInt(strtmp[1]);
			
			srcOutputTransliteration.add(source + "\t" + decodeArr.get(bestIndex) + "\t" + probArr.get(bestIndex));
			
			decodeArr.clear();
			probArr.clear();
			
			//for target
			best=0.0;
			bestIndex = 0;
			
			decodeword(target, decodeArr, probArr, multigramsTar);
			tmp = bestTransliteration(probArr);
			strtmp = tmp.split(" ");
			best = Double.parseDouble(strtmp[0]);
			bestIndex = Integer.parseInt(strtmp[1]);
			tarOutputTransliteration.add(decodeArr.get(bestIndex) + "\t" + target + "\t" + probArr.get(bestIndex));
			
			decodeArr.clear();
			probArr.clear();
			
		}
		
		writeToFile(srcOutputTransliteration, "srcTotrg");
		writeToFile(tarOutputTransliteration, "trgTosrc");
	}
	
	private void writeToFile(ArrayList<String> alt, String file)
	{
		Output o = new Output();
		
		Iterator it = alt.iterator();
		while(it.hasNext())
		{
			o.printOutput((String) it.next(), Boolean.TRUE, file);	
		}
	}
	
	private String bestTransliteration(ArrayList<Double> probArr)
	{
		double best = probArr.get(0);
		int bestIndex = 0;
		
		for(int i=1; i<probArr.size(); i++)
		{
			if(probArr.get(i) > best)
			{
				best = probArr.get(i);
				bestIndex = i;
			}
		}
		return best + " " + bestIndex; 
	}
	
	public void decodeword(String word, ArrayList<String> decodeArr, ArrayList<Double> probArr, HashMap<String, ArrayList> multigramsS)
	{
		String [] wrdArr = word.split("");
		//ArrayList<String> decodeArr = new ArrayList<String>();
		//ArrayList<Double> probArr = new ArrayList<Double>();
		
		for(int i=1; i<wrdArr.length; i++)
		{
			if(multigramsS.containsKey(wrdArr[i]))
			{
				ArrayList lst = (ArrayList) multigramsS.get(wrdArr[i]);
				int length = lst.size();
							
				if(i == 1)
				{
					Iterator it = lst.iterator();
					while(it.hasNext())
					{
						pair p = (pair) it.next();
						decodeArr.add(p.getString());
						probArr.add(p.getValue());					
					}
				}
				else
				{
					ArrayList<String> decodtmp = new ArrayList<String>();
					ArrayList<Double> probtmp = new ArrayList<Double>();
					
					//decodtmp.addAll(decodeArr);
					//probtmp.addAll(probArr);
					
					Iterator it = lst.iterator(); // for all possible values in the pair
					while(it.hasNext())
					{
						pair p = (pair) it.next();
						String currStr = p.getString();
						double currProb = p.getValue();				
						
						for(int j=0; j<decodeArr.size(); j++)
						{
							String nowStr = decodeArr.get(j) + currStr;
							Double nowProb = probArr.get(j) + currProb;
							
							decodtmp.add(nowStr);
							probtmp.add(nowProb);
						}
						
					}
					
					// sort the array, prune it for next iteration
					sortAndBeamPrun(decodtmp, probtmp);
					
					decodeArr.clear();
					probArr.clear();
					
					for(int k=0; k<25; k++){
						decodeArr.add(decodtmp.get(k));
						probArr.add(probtmp.get(k));	
					}
					
					//decodeArr.addAll(decodtmp);
					//probArr.addAll(probtmp);
					
					decodtmp.clear();
					probtmp.clear();
					
				}
			}
			else // if unknown character
			{
				if(i == 1)
				{
					decodeArr.add(wrdArr[i]);
					probArr.add(-2000.0);					
				}
				else
				{
					ArrayList<String> decodtmp = new ArrayList<String>();
					ArrayList<Double> probtmp = new ArrayList<Double>();
					
					for(int j=0; j<decodeArr.size(); j++)
					{
						String nowStr = decodeArr.get(j) + wrdArr[i];
						Double nowProb = probArr.get(j) - 2000.0;
						
						decodtmp.add(nowStr);
						probtmp.add(nowProb);
					}
					decodeArr.clear();
					probArr.clear();
					
					decodeArr.addAll(decodtmp);
					probArr.addAll(probtmp);
					
					decodtmp.clear();
					probtmp.clear();
				}
				
			
			}
		}
	}
	
	private void sortAndBeamPrun(ArrayList<String> decodtmp, ArrayList<Double> probtmp)
	{
		for (int i=1; i<decodtmp.size(); i++)
		{
			int j = i;
			double B = probtmp.get(i);
			String Bstr = decodtmp.get(i);
			
			while((j>0) && (probtmp.get(j-1) < B))
			{
				probtmp.set(j, probtmp.get(j-1));
				j--;
			}
			probtmp.set(j, B);
			decodtmp.set(j, Bstr);
		}
	}
	
	public void processMultigrams(HashMap multigrams, HashMap<String, ArrayList> multigramsS)
	{
		// they are separated by -
		// loop though the file
		// split source characters from target consider all possible cases
		// arrange a possible options of a character and probabilities
	
		Iterator it1 = multigrams.keySet().iterator();
		while (it1.hasNext()) {
			String str = (String) it1.next();
			double val = (Double) multigrams.get(str);
			
			String [] strArr = str.split("");
			
			pair pTar = new pair();
			pTar.addString(strArr[3]);
			pTar.addValue(val);
			ArrayList al = new ArrayList();
			
			if(multigramsS.containsKey(strArr[1]))
			{
				al.addAll(multigramsS.get(strArr[1]));
				al.add(pTar);
				multigramsS.put(strArr[1], al);
			}
			else
			{
				al.add(pTar);
				multigramsS.put(strArr[1], al);
			}
		}
		
//		printMultigrams(multigramsS);	
	}
	
	public void processMultigramsReverse(HashMap multigrams, HashMap<String, ArrayList> multigramsS)
	{
		// they are separated by -
		// loop though the file
		// split source characters from target consider all possible cases
		// arrange a possible options of a character and probabilities
	
		Iterator it1 = multigrams.keySet().iterator();
		while (it1.hasNext()) {
			String str = (String) it1.next();
			double val = (Double) multigrams.get(str);
			
			String [] strArr = str.split("");
			
			pair pTar = new pair();
			pTar.addString(strArr[1]);
			pTar.addValue(val);
			ArrayList al = new ArrayList();
			
			if(multigramsS.containsKey(strArr[3]))
			{
				al.addAll(multigramsS.get(strArr[3]));
				al.add(pTar);
				multigramsS.put(strArr[3], al);
			}
			else
			{
				al.add(pTar);
				multigramsS.put(strArr[3], al);
			}
		}
		
//		printMultigrams(multigramsS);	
	}
	
	public void printMultigrams(HashMap<String, ArrayList> multigramsS)
	{
		Iterator it1 = multigramsS.keySet().iterator();
		while (it1.hasNext()) {
			String str = (String) it1.next();
			ArrayList al = (ArrayList) multigramsS.get(str);
		
			System.out.print("source = " + str + " ");
		
			Iterator alI = al.iterator();
			while(alI.hasNext())
			{
				pair a = (pair) alI.next();
				System.out.print(a.getString() + " = ");
				System.out.println(a.getValue());
			}
		}
	}
	
	private void sortAndPruneMultigrams(HashMap<String, ArrayList> multigramsS)
	{
		Iterator it1 = multigramsS.keySet().iterator();
		while(it1.hasNext()){
			String str = (String) it1.next();
			ArrayList al = (ArrayList) multigramsS.get(str);  
			
			Iterator alI = al.iterator();
			
			int i = 0;
			int limit = al.size();
			
			double [] arr = new double[limit];
			String[] strArr = new String [limit];
			
			if(limit <= 10){
				
			}
			else
				{
			
				while(alI.hasNext())
				{
					pair a = (pair) alI.next();
					String currStr = a.getString();
					double currProb = a.getValue();
				
					arr[i] = currProb;
					strArr[i] = currStr;
				
					if(i == 0)
					{
					}
					else
					{
						int j = i;
						double B = arr[i];
						String Bstr = strArr[i];
					
						while((j>0 && (arr[j-1] < B)))
						{
							arr[j] = arr[j-1];
							strArr[j] = strArr[j-1];
							j--;
						}
						arr[j] = B;
						strArr[j] = Bstr;
					}
					i++;
				}
				// sorted prob in arr and corresponding strings in strArr
				// prune it to keep only 20 options
			al.clear();
			for(int k=0; k<10; k++){
				pair a = new pair();
				a.addString(strArr[k]);
				a.addValue(arr[k]);
				al.add(a);	
			}
			}
		}
	}

}
