package clearsrl;

import gnu.trove.TObjectIntHashMap;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

public class SRLScore {
	
	SortedSet<String> labelSet;
	TObjectIntHashMap<String> labelMap;
	int[][] microCount;
	int[][] macroCount;
	
	public SRLScore(SortedSet<String> labelSet)
	{
		this.labelSet = labelSet;
		labelMap = new TObjectIntHashMap<String>();
		
		int count=0;
		for (String argType:labelSet)
			labelMap.put(argType, count++);

		microCount = new int[count][count];
		macroCount = new int[count][count];
	}
	
	String[] getLabels(List<SRArg> args, int tokenCount)
	{
		String[] strs = new String[tokenCount];
		Arrays.fill(strs, SRLModel.NOT_ARG);
		for (SRArg arg:args)
			for (int i=arg.tokenSet.nextSetBit(0); i>=0; i=arg.tokenSet.nextSetBit(i+1))
				strs[i] = arg.label;
		return strs;
	}
	
	public void addResult(SRInstance systemSRL, SRInstance goldSRL)
	{
		List<SRArg> sysArgs = systemSRL.getScoringArgs();
		List<SRArg> goldArgs = goldSRL.getScoringArgs();
		
		String[] sysStr = getLabels(sysArgs, systemSRL.tree.getTokenCount());		
		String[] goldStr = getLabels(goldArgs, goldSRL.tree.getTokenCount());
		
		for (int i=0; i<sysStr.length; ++i)
		{
			if (sysStr[i]==SRLModel.NOT_ARG && goldStr[i]==SRLModel.NOT_ARG)
				continue;
			microCount[labelMap.get(sysStr[i])][labelMap.get(goldStr[i])]++;
		}

		for (int i=0, j=0; i<sysArgs.size() || j<goldArgs.size();)
		{
			if (i>=sysArgs.size())
			{
				macroCount[labelMap.get(SRLModel.NOT_ARG)][labelMap.get(goldArgs.get(j).label)]++;
				++j;
				continue;
			}
			if (j>=goldArgs.size())
			{
				macroCount[labelMap.get(sysArgs.get(i).label)][labelMap.get(SRLModel.NOT_ARG)]++;
				++i;
				continue;
			}
			
			int compare = sysArgs.get(i).compareTo(goldArgs.get(j));
			if (compare<0)
			{
				macroCount[labelMap.get(sysArgs.get(i).label)][labelMap.get(SRLModel.NOT_ARG)]++;
				++i;
			}
			else if (compare>0)
			{
				macroCount[labelMap.get(SRLModel.NOT_ARG)][labelMap.get(goldArgs.get(j).label)]++;
				++j;
			}
			else
			{
				if (sysArgs.get(i).tokenSet.equals(goldArgs.get(j).tokenSet))
					macroCount[labelMap.get(sysArgs.get(i).label)][labelMap.get(goldArgs.get(j).label)]++;
				else
				{
					macroCount[labelMap.get(sysArgs.get(i).label)][labelMap.get(SRLModel.NOT_ARG)]++;
					macroCount[labelMap.get(SRLModel.NOT_ARG)][labelMap.get(goldArgs.get(j).label)]++;
				}
				++i; ++j;
			}	
		}
	}
	
	public void printResults(PrintStream pStream)
	{
		System.out.println("\n********** Token Results **********");
		printResults(pStream, microCount);
		System.out.println("---------- Arg Results ------------");
		printResults(pStream, macroCount);
		System.out.println("************************\n");
	}
		
	void printResults(PrintStream pStream, int[][] count)	
	{
		int pTotal=0, rTotal=0, fTotal=0;
		double p, r, f;
		for (String label: labelSet)
		{
			if (label.equals(SRLModel.NOT_ARG)) continue;
			
			int idx = labelMap.get(label);
			
			int pArgT=0, rArgT=0, fArgT=0;
			
			for (int i:count[idx]) pArgT+=i;
			for (int i=0; i<count.length; ++i) rArgT+=count[i][idx];
			
			fArgT = count[idx][idx];

			p = pArgT==0?0:((double)fArgT)/pArgT;
			r = rArgT==0?0:((double)fArgT)/rArgT;
			f = p==0?0:(r==0?0:2*p*r/(p+r));

			System.out.printf("%s(%d,%d,%d): precision: %f recall: %f, f-measure: %f\n", label, fArgT, pArgT, rArgT, p, r, f);
			
			pTotal += pArgT;
			rTotal += rArgT;
			fTotal += fArgT;
		}
		
		p = pTotal==0?0:((double)fTotal)/pTotal;
		r = rTotal==0?0:((double)fTotal)/rTotal;
		f = p==0?0:(r==0?0:2*p*r/(p+r));
		System.out.printf("%s(%d,%d,%d): precision: %f recall: %f, f-measure: %f\n", "all", fTotal, pTotal, rTotal, p, r, f);
	}
}
