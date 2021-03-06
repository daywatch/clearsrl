package edu.colorado.clear.srl.align;

import gnu.trove.set.hash.TIntHashSet;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.colorado.clear.common.propbank.PBArg;
import edu.colorado.clear.common.propbank.PBInstance;
import edu.colorado.clear.common.treebank.TBNode;

public class Alignment implements Comparable<Alignment>{

    private static final float COREARGFACTOR = 1.5f;
    private static final float ARGFACTOR = 1.0f;
    private static final float PREDICATEFACTOR = 2.0f;
    
    public SentencePair sentence;
    public int          srcPBIdx;
    public int          dstPBIdx;
    public float        beta_sqr;
    
    public float        srcScore;
    public float        dstScore;
    
    public float[]      srcTerminalWeights;
    public float[]      dstTerminalWeights;
    
    public float[][]    srcDstScore;
    public float[][]    dstSrcScore;
    
    public ArgAlignment[] srcDstAlignment;
    public ArgAlignment[] dstSrcAlignment;
    
    AlignmentProb       probMap;
    float               predProbWeight;
    float               argProbWeight;
    boolean             useFMeasure;
    
    public Alignment(SentencePair sentence, int srcPBIdx, int dstPBIdx, 
    		float beta_sqr, AlignmentProb probMap, float predProbWeight, 
    		float argProbWeight, boolean useFMeasure) {
        this.sentence = sentence;
        this.srcPBIdx = srcPBIdx;
        this.dstPBIdx = dstPBIdx;
        //this.beta_sqr = beta_sqr;
        this.beta_sqr = (float)sentence.src.indices.length/sentence.dst.indices.length;
        this.probMap = probMap;
        this.predProbWeight = predProbWeight;
        this.argProbWeight = argProbWeight;
        this.useFMeasure = useFMeasure;
    }
    
    static float getFScore(float lhs, float rhs, float bias) {
        float denom = bias*lhs + rhs;
        return denom==0?0:(1+bias)*lhs*rhs/denom;
    }
    
    float measureArgAlignment(PBArg lhs, PBArg rhs, 
                              float[] lhsWeights, float[] rhsWeights, 
                              int lhsTreeIdx, int rhsTreeIdx, 
                              SortedMap<Long, int[]> wordAlignment, 
                              Sentence rhsSentence)  {           
        TBNode[] lhsNodes = lhs.getTokenNodes();
        
        float argScore = 0;

        for (int i=0; i<lhsNodes.length; ++i)
        {   
            int[] dstIndices = wordAlignment.get(Sentence.makeIndex(lhsTreeIdx, lhsNodes[i].getTerminalIndex()));
            float score=0;
            float weight=0;

            if (dstIndices!=null) //possibly false when part of argument is not part of the "sentence"
            {
                for (int dstIdx: dstIndices)
                {
                    int treeIdx = Sentence.getTreeIndex(rhsSentence.indices[dstIdx]);
    
                    // rare (ignore): when GIZA aligns word to a different tree
                    if (treeIdx != rhsTreeIdx) continue;
                    
                    int terminalIdx = Sentence.getTerminalIndex(rhsSentence.indices[dstIdx]);
    
                    if (rhs.hasTerminal(terminalIdx)) 
                        score+=rhsWeights[terminalIdx];
                    
                    weight+=rhsWeights[terminalIdx];
                }
                if (weight>0) score/=weight;
            }
            argScore += score*lhsWeights[lhsNodes[i].getTerminalIndex()];
        }
        
        return argScore;
    }

    public float getCompositeScore() {
        return getFScore(srcScore, dstScore, beta_sqr);
    }

    void computeArgWeight(PBArg arg, float[] weights) {
        //TODO: better weight computation
        BitSet terminals = arg.getTerminalSet();
        for (int i=terminals.nextSetBit(0); i>=0; i=terminals.nextSetBit(i+1))
            weights[i] = 1.0f;
    }
    
    ArgAlignment[] initArgAlignment(PBInstance instance, float[] weights)
    {
        PBArg[] args = instance.getArgs();
        
        ArgAlignment[] argAlignments = new ArgAlignment[args.length];
        for (int i=0; i<args.length; ++i) {
            computeArgWeight(args[i], weights);
            argAlignments[i]=new ArgAlignment(instance, i, new TIntHashSet(), weights);
        }
        float predicateFactor = 0.0f;
        float predicateWeight = 0.0f;
        float argWeight       = 0.0f;
        float coreArgFactor    = 1.0f;
        float coreArgWeight    = 0.0f;
        float modArgFactor      = 1/COREARGFACTOR;
        float modArgWeight      = 0.0f;
        
        for (ArgAlignment argAlignment:argAlignments)
        {
            if (argAlignment.getSrcArg().getLabel().equals("rel"))
                predicateWeight += argAlignment.weight;
            else if (argAlignment.getSrcArg().getLabel().matches(".*ARG\\d"))
                coreArgWeight += argAlignment.weight;
            else
                modArgWeight += argAlignment.weight;
        }
        
        if (coreArgWeight>0 && coreArgWeight<modArgWeight)
            modArgFactor = coreArgWeight/modArgWeight*modArgFactor;
        
        argWeight = coreArgFactor*coreArgWeight+modArgFactor*modArgWeight;
        
        if (predicateWeight>0 && argWeight>predicateWeight*ARGFACTOR)
            predicateFactor = argWeight/ARGFACTOR/predicateWeight;
        else
            predicateFactor =  PREDICATEFACTOR/predicateWeight;
        
        float denom = predicateFactor*predicateWeight+argWeight;
        
        predicateFactor/=denom;
        coreArgFactor/=denom;
        modArgFactor/=denom;
        
        for (ArgAlignment argAlignment:argAlignments)
        {
            if (argAlignment.getSrcArg().getLabel().equals("rel"))
                argAlignment.factor = predicateFactor;
            else if (argAlignment.getSrcArg().getLabel().matches(".*ARG\\d"))
                argAlignment.factor = coreArgFactor;
            else
                argAlignment.factor = modArgFactor;
        }
        
        return argAlignments;
    }
    
    float computeAlignment(float[][] lhsScore, float[][] rhsScore, ArgAlignment[] lhsAlignments, ArgAlignment[] rhsAlignments, float bias)
    {
        float fScore=0.0f;
        float precision = 0.0f;
        float recall = 0.0f;
        
        float fScoreTmp=0.0f;
        float pDelta, rDelta;
        
        int[] argIndices = new int[lhsScore.length];
        float[] argScores = new float[lhsScore.length];
        for (int i=0; i<lhsScore.length; ++i)
        {
            argIndices[i] = getMaxIndex(lhsScore[i]);
            argScores[i] = lhsScore[i][argIndices[i]];
        }
        for (;;)
        {
            int index = getMaxIndex(argScores);
            if (argScores[index]<0) break;
            
            ArgAlignment lhsAlignment = lhsAlignments[index];
            ArgAlignment rhsAlignment = rhsAlignments[argIndices[index]];
            
            pDelta = lhsAlignment.getFactoredWeight()*lhsScore[index][argIndices[index]];
            rDelta = rhsAlignment.getFactoredWeight()*rhsScore[argIndices[index]][index];
            
            fScoreTmp = getFScore(precision+pDelta, recall+rDelta, bias);
            
            if (fScoreTmp>fScore)
            {
                precision += pDelta;
                recall += rDelta;
                fScore = fScoreTmp;

                lhsAlignment.dstArgList.add(rhsAlignment.srcArgIdx);
                lhsAlignment.score+=lhsScore[index][argIndices[index]];
                
            }
            argScores[index] = Float.NEGATIVE_INFINITY;
        }
        
        return fScore;
    }
    
    static int getMaxIndex(float[] vals) {
        int index=0;
        float maxVal = vals[0];
        for (int i=1; i<vals.length; ++i)
            if (vals[i]>maxVal)
            {
                maxVal = vals[i];
                index=i;
            }
        return index;
    }
    
    public void computeSymmetricAlignment() {
        
        PBArg[] srcArgs = getSrcPBInstance().getArgs();
        PBArg[] dstArgs = getDstPBInstance().getArgs();
            
        srcTerminalWeights = new float[getSrcPBInstance().getTree().getTerminalCount()];
        dstTerminalWeights = new float[getDstPBInstance().getTree().getTerminalCount()];
        
        srcDstAlignment = initArgAlignment(getSrcPBInstance(), srcTerminalWeights);
        dstSrcAlignment = initArgAlignment(getDstPBInstance(), dstTerminalWeights);
        
        srcDstScore = new float[srcArgs.length][dstArgs.length];
        dstSrcScore = new float[dstArgs.length][srcArgs.length];
        
        double[][] srcDstProb = probMap==null?null:probMap.getArgProbs(true, getSrcPBInstance(), getDstPBInstance(), true);
        double[][] dstSrcProb = probMap==null?null:probMap.getArgProbs(false, getDstPBInstance(), getSrcPBInstance(), true);
        
        for (int i=0; i<srcDstScore.length; ++i)
            for (int j=0; j<srcDstScore[i].length;++j) {
                srcDstScore[i][j] = measureArgAlignment(srcArgs[i], dstArgs[j],
                        srcTerminalWeights, dstTerminalWeights, 
                        getSrcPBInstance().getTree().getIndex(), 
                        getDstPBInstance().getTree().getIndex(),
                        sentence.srcAlignment, sentence.dst);
                srcDstScore[i][j] /= srcDstAlignment[i].weight;
                dstSrcScore[j][i] = measureArgAlignment(dstArgs[j], srcArgs[i],
                        dstTerminalWeights, srcTerminalWeights, 
                        getDstPBInstance().getTree().getIndex(),
                        getSrcPBInstance().getTree().getIndex(), 
                        sentence.dstAlignment, sentence.src);
                dstSrcScore[j][i] /= dstSrcAlignment[j].weight;
                
                if (argProbWeight!=0 && srcDstProb!=null && dstSrcProb!=null) {
                	if (useFMeasure) {
                		srcDstScore[i][j] = getFScore(srcDstScore[i][j], (float)srcDstProb[i][j], argProbWeight);
                		dstSrcScore[i][j] = getFScore(dstSrcScore[i][j], (float)dstSrcProb[i][j], argProbWeight);
                	} else {
                    	srcDstScore[i][j] *= (float)(1-argProbWeight+argProbWeight*srcDstProb[i][j]);
                    	dstSrcScore[j][i] *= (float)(1-argProbWeight+argProbWeight*dstSrcProb[j][i]);
                		//srcDstScore[i][j] = (float)((1-argProbWeight)*srcDstScore[i][j]+argProbWeight*srcDstProb[i][j]);
                		//dstSrcScore[j][i] = (float)((1-argProbWeight)*dstSrcScore[j][i]+argProbWeight*dstSrcProb[j][i]);
                	}
                }
            }
        
        computeAlignment(srcDstScore, dstSrcScore, srcDstAlignment, dstSrcAlignment, beta_sqr);
        computeAlignment(dstSrcScore, srcDstScore, dstSrcAlignment, srcDstAlignment, 1/beta_sqr);
        
        srcScore = 0.0f;
        for (ArgAlignment argAlign: srcDstAlignment)
            srcScore += argAlign.score*argAlign.getFactoredWeight();        
        
        dstScore = 0.0f;
        for (ArgAlignment argAlign: dstSrcAlignment)
            dstScore += argAlign.score*argAlign.getFactoredWeight();
        
        if (probMap!=null && predProbWeight != 0) {
        	float srcPredProb = probMap==null?1:(float)probMap.getPredProb(true, getSrcPBInstance(), getDstPBInstance(), true);
        	float dstPredProb = probMap==null?1:(float)probMap.getPredProb(false, getDstPBInstance(), getSrcPBInstance(), true);
//        	if (useFMeasure) {
//        		srcScore = getFScore(srcScore, srcPredProb, predProbWeight);
//        		dstScore = getFScore(dstScore, dstPredProb, predProbWeight);
//        	} else {
	        	srcScore *= (1-predProbWeight)+predProbWeight*srcPredProb;
	        	dstScore *= (1-predProbWeight)+predProbWeight*dstPredProb;
//        	}
        	
        	if (srcScore>1) srcScore = 1;
        	if (dstScore>1) dstScore = 1;
        }
    }
    
    public ArgAlignmentPair[] getArgAlignmentPairs() {
        Map<Long, ArgAlignmentPair> argPairMap = new TreeMap<Long, ArgAlignmentPair>();
        
        for (ArgAlignment argAlignment:srcDstAlignment)
            for (int id:argAlignment.dstArgList.toArray()) {
                long idx = (((long)argAlignment.srcArgIdx)<<32)|id;
                ArgAlignmentPair alignmentPair = argPairMap.get(idx);
                if (alignmentPair==null) {
                    alignmentPair = new ArgAlignmentPair(argAlignment.srcArgIdx, id, argAlignment.score);
                    argPairMap.put(idx, alignmentPair);
                }
            }
        for (ArgAlignment argAlignment:dstSrcAlignment)
            for (int id:argAlignment.dstArgList.toArray()) {
                long idx = (((long)id)<<32)|argAlignment.srcArgIdx;
                ArgAlignmentPair alignmentPair = argPairMap.get(idx);
                if (alignmentPair==null) {
                    alignmentPair = new ArgAlignmentPair(id, argAlignment.srcArgIdx, argAlignment.score);
                    argPairMap.put(idx, alignmentPair);
                }
                else
                    alignmentPair.score = getFScore(argAlignment.score, alignmentPair.score, beta_sqr);
            }
        return argPairMap.values().toArray(new ArgAlignmentPair[argPairMap.size()]);
    }
    
    public PBArg getSrcPBArg(int idx) {
        return getSrcPBInstance().getArgs()[idx];
    }
    
    public PBArg getDstPBArg(int idx) {
        return getDstPBInstance().getArgs()[idx];
    }

    public PBInstance getSrcPBInstance() {
        return sentence.src.pbInstances[srcPBIdx];
    }
    
    public PBInstance getDstPBInstance() {
        return sentence.dst.pbInstances[dstPBIdx];
    }

    public void printScoreTable(PrintStream out)
    {
        PBArg[] srcArgs = getSrcPBInstance().getArgs();
        PBArg[] dstArgs = getDstPBInstance().getArgs();
        out.print("          ");
        for (PBArg arg:dstArgs)
            out.printf("%10s",arg.getLabel());
        out.print("\n");
        for (int i=0; i<srcDstScore.length; ++i)
        {
            out.printf("%10s",srcArgs[i].getLabel());
            for (int j=0; j<srcDstScore[i].length; ++j)
                out.printf("%10.5f", srcDstScore[i][j]);
            out.printf("%10.5f%10.5f", srcDstAlignment[i].score, srcDstAlignment[i].getFactoredWeight());
            out.print("\n");
        }
        out.print("          ");
        for (PBArg arg:srcArgs)
            out.printf("%10s",arg.getLabel());
        out.print("\n");
        for (int i=0; i<dstSrcScore.length; ++i)
        {
            out.printf("%10s",dstArgs[i].getLabel());
            for (int j=0; j<dstSrcScore[i].length; ++j)
                out.printf("%10.5f", dstSrcScore[i][j]);
            out.printf("%10.5f%10.5f", dstSrcAlignment[i].score, dstSrcAlignment[i].getFactoredWeight());
            out.print("\n");
        }
        
    }
    
    @Override
	public String toString() {
    	return toString(false);
    }
    
    public String toString(boolean indexed) {
        /*
        boolean foundm = false;
        for (ArgAlignment argAlignment:dstSrcAlignment)
            if (argAlignment.dstArgList.size()>1)
                foundm = true;
        for (ArgAlignment argAlignment:srcDstAlignment)
            if (argAlignment.dstArgList.size()>1)
                foundm = true;
        if (!foundm) return "";*/
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%d,%d,%.3f;",
        		indexed?Arrays.binarySearch(sentence.src.indices, Sentence.makeIndex(getSrcPBInstance().getTree().getIndex(), getSrcPBInstance().getPredicate().getTerminalIndex())):srcPBIdx+1,
        		indexed?Arrays.binarySearch(sentence.dst.indices, Sentence.makeIndex(getDstPBInstance().getTree().getIndex(), getDstPBInstance().getPredicate().getTerminalIndex())):dstPBIdx+1,
        		getCompositeScore()));

        BitSet srcArgBitSet=new BitSet();
        BitSet dstArgBitSet=new BitSet();
        
        for (ArgAlignmentPair alignmentPair:getArgAlignmentPairs())
        {
            builder.append(String.format("%s,%s,%.3f;",getSrcPBArg(alignmentPair.srcArgIdx).getLabel(),getDstPBArg(alignmentPair.dstArgIdx).getLabel(),alignmentPair.score));
            srcArgBitSet.set(alignmentPair.srcArgIdx);
            dstArgBitSet.set(alignmentPair.dstArgIdx);
        }
        
        builder.append("[");
        for (int i=srcArgBitSet.nextClearBit(0); i<getSrcPBInstance().getArgs().length; i=srcArgBitSet.nextClearBit(i+1))
             builder.append(getSrcPBArg(i).getLabel()+',');
        builder.append("][");
        for (int i=dstArgBitSet.nextClearBit(0); i<getDstPBInstance().getArgs().length; i=dstArgBitSet.nextClearBit(i+1))
             builder.append(getDstPBArg(i).getLabel()+',');
        
        builder.append("]");
        return builder.toString();
    }
    
    static String getTokenString(PBArg arg)
    {
        StringBuilder tokenBuilder = new StringBuilder(arg.getLabel());
        BitSet srcSet = arg.getTokenSet();
        for (int i=srcSet.nextSetBit(0); i>=0; i=srcSet.nextSetBit(i+1))
            tokenBuilder.append("_"+i);
        return tokenBuilder.toString();
    }
    
    public String toArgTokenString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%d,%d,%.3f;",srcPBIdx+1,dstPBIdx+1,getCompositeScore()));

        BitSet srcArgBitSet=new BitSet();
        BitSet dstArgBitSet=new BitSet();
        
        for (ArgAlignmentPair alignmentPair:getArgAlignmentPairs())
        {           
            builder.append(String.format("%s,%s,%.3f;",getTokenString(getSrcPBArg(alignmentPair.srcArgIdx)),getTokenString(getDstPBArg(alignmentPair.dstArgIdx)),alignmentPair.score));
            srcArgBitSet.set(alignmentPair.srcArgIdx);
            dstArgBitSet.set(alignmentPair.dstArgIdx);
        }
        
        builder.append("[");
        for (int i=srcArgBitSet.nextClearBit(0); i<getSrcPBInstance().getArgs().length; i=srcArgBitSet.nextClearBit(i+1))
             builder.append(getSrcPBArg(i).getLabel()+',');
        builder.append("][");
        for (int i=dstArgBitSet.nextClearBit(0); i<getDstPBInstance().getArgs().length; i=dstArgBitSet.nextClearBit(i+1))
             builder.append(getDstPBArg(i).getLabel()+',');
        
        builder.append("]");
        
        return builder.toString();
    }

    @Override
    public int compareTo(Alignment rhs) {
    	return srcPBIdx - rhs.srcPBIdx;
    	
        //float scoreDiff = getCompositeScore()-rhs.getCompositeScore();
        //return scoreDiff>0?-1:(scoreDiff==0?0:1);
    }
    
}