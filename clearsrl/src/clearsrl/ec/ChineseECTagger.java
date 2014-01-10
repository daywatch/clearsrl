package clearsrl.ec;

import clearsrl.ec.ECCommon.Feature;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import clearcommon.alg.FeatureSet;
import clearcommon.propbank.PBInstance;
import clearcommon.propbank.PBUtil;
import clearcommon.treebank.TBNode;
import clearcommon.treebank.TBReader;
import clearcommon.treebank.TBTree;
import clearcommon.treebank.TBUtil;
import clearcommon.util.ChineseUtil;
import clearcommon.util.LanguageUtil;
import clearcommon.util.PropertyUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ChineseECTagger {

    enum Task {
        EXTRACT,
        TRAIN,
        VALIDATE,
        DECODE
    }
    
    private static Logger logger = Logger.getLogger("clearsrl.ec");

    @Option(name="-prop",usage="properties file")
    private File propFile = null; 
    
    @Option(name="-t",usage="task: write/train/decode")
    private Task task = Task.DECODE; 
    
    @Option(name="-c",usage="corpus name")
    private String corpus = null;
    
    @Option(name="-m",usage="model file")
    private String modelName = null;   
    
    @Option(name="-v",usage="verbose")
    private boolean verbose = false;
    
    @Option(name="-h",usage="help message")
    private boolean help = false;

    static String removeTraceIndex(String trace) {
        return TBNode.WORD_PATTERN.matcher(trace).group(1);
        
        /*
        Matcher matcher = TBTree.TRACE_PATTERN.matcher(trace);
        if (matcher.matches())
            return matcher.group(1);
        
        return trace;
        */
    }
    
    static String findLittlePro(TBNode node, String cLabel) {
        if (node.getWord().equals("*pro*"))
            return node.getWord();
        return ECCommon.NOT_EC;
    }
    
    static String findAllTraces(TBNode node, String cLabel, boolean uniTrace) {
        /*
        if (node.word.equals("*OP*"))
            return ECCommon.NOT_EC;
        if (node.trace!=null && (node.trace.pos.equals("WHNP") || node.trace.pos.equals("WHPP")))
            return cLabel;
        */
        if (cLabel.equals(ECCommon.NOT_EC) || uniTrace)
            return removeTraceIndex(node.getWord());
        return cLabel+"-"+removeTraceIndex(node.getWord());
    }
    
    static String getFeatures(TBNode node, ArrayList<String> tokens, ArrayList<String> poses, ArrayList<String> labels, String cLabel){
        if (node.isTerminal()) {
            if (!node.isEC()) {
                tokens.add(node.getWord());
                poses.add(node.getPOS());
                labels.add(cLabel);
                return ECCommon.NOT_EC;
            }
            //return ECModel.IS_EC;
            //return findLittlePro(node, cLabel);
            return findAllTraces(node, cLabel, true);
        }
        for (TBNode child:node.getChildren())
            cLabel = getFeatures(child, tokens, poses, labels, cLabel);
        return cLabel;
    }
    
    static void extract(Properties props) throws FileNotFoundException, IOException {
        /*
        Set<EnumSet<Feature>> features = new HashSet<EnumSet<Feature>>();
        {
            String[] tokens = props.getProperty("feature.value").trim().split(",");
            for (String token:tokens)
                try {
                    features.add(FeatureSet.toEnumSet(Feature.class, token));
                } catch (IllegalArgumentException e) {
                    System.err.println(e);
                }
        }

        ECModel model = new ECModel(features);
        
        Map<String, TBTree[]> tbMapTrain = TBUtil.readTBDir(props.getProperty("corpus"), props.getProperty("train.regex"));
        System.out.println(props.getProperty("corpus"));
        System.out.println(props.getProperty("train.regex"));
        
        
        Map<String, TBTree[]> tbMapTest = TBUtil.readTBDir(props.getProperty("corpus"), props.getProperty("test.regex"));
        
        for (Map.Entry<String, TBTree[]> entry : tbMapTrain.entrySet())
            for (TBTree tree:entry.getValue())
                model.addTrainingSentence(tree, true);
        
        model.features.rebuildMap(5, 5);
        
        for (Map.Entry<String, TBTree[]> entry : tbMapTrain.entrySet())
            for (TBTree tree:entry.getValue())
                model.addTrainingSentence(tree, false);
        
        //System.out.printf("hit: %d, total: %d\n", model.hit, model.total);
    */
        
        /*
        PrintStream tout = new PrintStream(new FileOutputStream(props.getProperty("test.file")));
        
        for (Map.Entry<String, TBTree[]> entry : tbMapTest.entrySet())
            for (TBTree tree:entry.getValue())
            {
                tokens.clear(); poses.clear(); labels.clear();
                getFeatures(tree.getRootNode(), tokens, poses, labels, ECCommon.NOT_EC);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
                
                model.writeSample(tout, tokens.toArray(new String[tokens.size()]), poses.toArray(new String[poses.size()]), labels.toArray(new String[labels.size()]), InstanceFormat.valueOf(props.getProperty("file_format")));               
                //model.addTrainingSentence(tokens.toArray(new String[tokens.size()]), poses.toArray(new String[poses.size()]), labels.toArray(new String[labels.size()]));
            }
    
        tout.close();
        */
        /*
        ObjectOutputStream mOut = new ObjectOutputStream(new FileOutputStream(props.getProperty("model_file")));
        mOut.writeObject(model);
        mOut.close();
        */
        //model.writeTrainingData(props.getProperty("train.file"), InstanceFormat.valueOf(props.getProperty("file_format")));
    }
    
    
    static void validate(ECModel model, Properties props, ChineseUtil langUtil) throws IOException, ClassNotFoundException {
        Properties validateProps = PropertyUtil.filterProperties(props, "validate.", true);
        
        if (model == null) {
            ObjectInputStream mIn = new ObjectInputStream(new GZIPInputStream(new FileInputStream(validateProps.getProperty("model_file"))));
            model = (ECModel)mIn.readObject();
            mIn.close();
            model.setLangUtil(langUtil);
        }

        if (model instanceof ECDepModel) {
        	((ECDepModel)model).setQuickClassify(!validateProps.getProperty("quickClassify","false").equals("false"));
        	((ECDepModel)model).setFullPredict(!validateProps.getProperty("fullPredict","false").equals("false"));
        }
        
        ECScore score = new ECScore(new TreeSet<String>(Arrays.asList(model.labelStringMap.keys(new String[model.labelStringMap.size()]))));
        ECScore score2 = new ECScore(new TreeSet<String>(Arrays.asList(model.labelStringMap.keys(new String[model.labelStringMap.size()]))));
        
        ECScore dScore = null;
        ECScore dScore2 = null;

        if (model instanceof ECDepModel) {
        	dScore = new ECScore(new TreeSet<String>(Arrays.asList(model.labelStringMap.keys(new String[model.labelStringMap.size()]))));
        	dScore2 = new ECScore(new TreeSet<String>(Arrays.asList(model.labelStringMap.keys(new String[model.labelStringMap.size()]))));
        	
        }
        
        String corpus = validateProps.getProperty("corpus");
        corpus=corpus==null?"":corpus+"."; 
        
        Map<String, TBTree[]> tbMapValidate = null;
        if (validateProps.getProperty(corpus+"tbdepdir")!=null)
        	tbMapValidate = TBUtil.readTBDir(validateProps.getProperty(corpus+"tbdir"), validateProps.getProperty(corpus+"tb.regex"), validateProps.getProperty(corpus+"tbdepdir"), 8, 10);
        else 
        	tbMapValidate = TBUtil.readTBDir(validateProps.getProperty(corpus+"tbdir"), validateProps.getProperty(corpus+"tb.regex"), langUtil.getHeadRules());
        
        Map<String, TBTree[]> parseValidate = null;
        if (validateProps.getProperty(corpus+"parsedepdir")!=null)
        	parseValidate = TBUtil.readTBDir(validateProps.getProperty(corpus+"parsedir"), validateProps.getProperty(corpus+"tb.regex"), validateProps.getProperty(corpus+"parsedepdir"), 8, 10);
        else
        	parseValidate = TBUtil.readTBDir(validateProps.getProperty(corpus+"parsedir"), validateProps.getProperty(corpus+"tb.regex"), langUtil.getHeadRules());
        
        Map<String, SortedMap<Integer, List<PBInstance>>>  propValidate = 
                PBUtil.readPBDir(validateProps.getProperty(corpus+"propdir"), validateProps.getProperty(corpus+"pb.regex"), new TBReader(parseValidate));
        
        int ecCount=0;
        int ecDepCount=0;
        
        for (Map.Entry<String, TBTree[]> entry : parseValidate.entrySet()) {
            logger.info("Validating: "+entry.getKey());
            
            TBTree[] tbTrees = tbMapValidate.get(entry.getKey());
            TBTree[] parseTrees = entry.getValue();
            SortedMap<Integer, List<PBInstance>> pbInstances = propValidate.get(entry.getKey());

            for (int i=0; i<parseTrees.length; ++i) {
                String[] goldLabels = ECCommon.getECLabels(tbTrees[i], model.labelType);

                List<PBInstance> propList = pbInstances==null?null:pbInstances.get(i);
                
                String[] labels = null;
                if (model instanceof ECDepModel) {
                	((ECDepModel)model).setQuickClassify(true);
                	String[][] depLabels = ((ECDepModel)model).predictDep(parseTrees[i], propList);
                	labels = ECDepModel.makeLinearLabel(parseTrees[i], depLabels);
                	
                	String[][] goldDepLabels = ECCommon.getECDepLabels(tbTrees[i], model.labelType);
                	
                	for (int h=0; h<depLabels.length;++h)
                		for (int t=0; t<depLabels[h].length;++t) {
                			//if (depLabels[h][t]!=null&&!ECCommon.NOT_EC.equals(depLabels[h][t]) ||
                			//		goldDepLabels[h][t]!=null&&!ECCommon.NOT_EC.equals(goldDepLabels[h][t]))
                				dScore.addResult(depLabels[h][t]==null?ECCommon.NOT_EC:depLabels[h][t],
                						goldDepLabels[h][t]==null?ECCommon.NOT_EC:goldDepLabels[h][t]);
                				if (goldDepLabels[h][t]!=null&&!ECCommon.NOT_EC.equals(goldDepLabels[h][t])) {
                					String[] tokens = goldDepLabels[h][t].trim().split("\\s+");
                					if (tokens.length>1)
                						System.err.println(Arrays.asList(tokens));
                					ecDepCount+=tokens.length;
                				}
                		}
                	
                } else 
                	labels = model.predict(parseTrees[i], propList);
 
                for (int l=0; l<labels.length; ++l) {
                    score.addResult(labels[l], goldLabels[l]);
                }
                
                String[] labels2 = null;
                if (model instanceof ECDepModel) {
                	((ECDepModel)model).setQuickClassify(false);
                	String[][] depLabels = ((ECDepModel)model).predictDep(parseTrees[i], propList);
                	labels2 = ECDepModel.makeLinearLabel(parseTrees[i], depLabels);
                	
                	String[][] goldDepLabels = ECCommon.getECDepLabels(tbTrees[i], model.labelType);
                	
                	for (int h=0; h<depLabels.length;++h)
                		for (int t=0; t<depLabels[h].length;++t)
                			if (depLabels[h][t]!=null&&!ECCommon.NOT_EC.equals(depLabels[h][t]) ||
                					goldDepLabels[h][t]!=null&&!ECCommon.NOT_EC.equals(goldDepLabels[h][t]))
                				dScore2.addResult(depLabels[h][t]==null?ECCommon.NOT_EC:depLabels[h][t],
                						goldDepLabels[h][t]==null?ECCommon.NOT_EC:goldDepLabels[h][t]);
                } else 
                	labels2 = model.predict(parseTrees[i], propList);
                for (int l=0; l<labels2.length; ++l) {
                    score2.addResult(labels2[l], goldLabels[l]);
                    if (goldLabels[l]!=null&&!ECCommon.NOT_EC.equals(goldLabels[l]))
                    	ecCount+=goldLabels[l].trim().split("\\s+").length;
                }
                    
                boolean same=true;
                for (int l=0; l<labels.length; ++l)
                	if (!labels[l].equals(labels2[l])) {
                		same = false;
                		break;
                	}
                if (!same) {
                	/*
                	System.out.println(tbTrees[i].toPrettyParse());
                	System.out.println(parseTrees[i].toPrettyParse());
	                TBNode[] tokens = tbTrees[i].getTokenNodes();
	                printEC(tokens, goldLabels);
	                printEC(tokens, labels);
	                printEC(tokens, labels2);
	                if (pbInstances!=null) 
	                	for (PBInstance prop:propList)
	                		System.out.println(prop.toText());
	                System.out.println(score.toString(score.countMatrix, true));
	                System.out.println(score2.toString(score2.countMatrix, true));	                
	                System.out.println("");*/
                }
                /*
                for (int l=0; l<labels.length; ++l)
                	if (ECCommon.NOT_EC.equals(goldLabels[l]))
                		System.out.print(goldLabels);*/
                
            }
        }
        System.out.println(score.toString());
        System.out.println(score2.toString());
        if (dScore!=null)
        	System.out.println(dScore.toString());
        if (dScore2!=null)
        	System.out.println(dScore2.toString());
        
        System.out.printf("EC count: %d, dep count: %d\n", ecCount, ecDepCount);
        
    }
            
    static void printEC(TBNode[] nodes, String[] labels) {
    	for (int i=0; i<nodes.length;++i) {
    		if (!ECCommon.NOT_EC.equals(labels[i]))
    			System.out.print(labels[i]+' ');
    		System.out.print(nodes[i].getWord()+' ');
    	}
    	System.out.println(ECCommon.NOT_EC.equals(labels[nodes.length])?"":labels[nodes.length]);
    }
    
    static void train(Properties props, ChineseUtil langUtil) throws IOException, ClassNotFoundException {  
        Properties trainProps = PropertyUtil.filterProperties(props, "train.", true);
        Set<EnumSet<Feature>> features = new HashSet<EnumSet<Feature>>();
        {
            String[] tokens = trainProps.getProperty("feature").trim().split(",");
            for (String token:tokens)
                try {
                    features.add(FeatureSet.toEnumSet(Feature.class, token));
                } catch (IllegalArgumentException e) {
                    System.err.println(e);
                }
        }
        features = FeatureSet.getBigramSet(features);
        
        logger.info("features: "+features);
        
        boolean useDepModel = !trainProps.getProperty("dependency", "false").equals("false");

        ECModel model = useDepModel?new ECDepModel(features):new ECModel(features);
        model.setLangUtil(langUtil);
        
        String corpus = trainProps.getProperty("corpus");
        corpus=corpus==null?"":corpus+"."; 

        Map<String, TBTree[]> tbMapTrain = null;
        if (trainProps.getProperty(corpus+"tbdepdir")!=null)
        	tbMapTrain = TBUtil.readTBDir(trainProps.getProperty(corpus+"tbdir"), trainProps.getProperty(corpus+"tb.regex"), trainProps.getProperty(corpus+"tbdepdir"), 8, 10);
        else 
        	tbMapTrain = TBUtil.readTBDir(trainProps.getProperty(corpus+"tbdir"), trainProps.getProperty(corpus+"tb.regex"), langUtil.getHeadRules());
        
        Map<String, TBTree[]> parseTrain = null;
        if (trainProps.getProperty(corpus+"parsedepdir")!=null)
        	parseTrain = TBUtil.readTBDir(trainProps.getProperty(corpus+"parsedir"), trainProps.getProperty(corpus+"tb.regex"), trainProps.getProperty(corpus+"parsedepdir"), 8, 10);
        else
        	parseTrain = TBUtil.readTBDir(trainProps.getProperty(corpus+"parsedir"), trainProps.getProperty(corpus+"tb.regex"), langUtil.getHeadRules());
        
        Map<String, SortedMap<Integer, List<PBInstance>>>  propTrain = 
                PBUtil.readPBDir(trainProps.getProperty(corpus+"propdir"), trainProps.getProperty(corpus+"pb.regex"), new TBReader(parseTrain));

        for (Map.Entry<String, TBTree[]> entry : parseTrain.entrySet()) {
            TBTree[] tbTrees = tbMapTrain.get(entry.getKey());
            TBTree[] parseTrees = entry.getValue();
            SortedMap<Integer, List<PBInstance>> pbInstances = propTrain.get(entry.getKey());
            
            for (int i=0; i<parseTrees.length; ++i) {
                model.addTrainingSentence(tbTrees[i], parseTrees[i], pbInstances==null?null:pbInstances.get(i), true);
                
                /*
                logger.info(tbTrees[i].toString());
                
                StringBuilder builder = new StringBuilder();
                for (TBNode node : tbTrees[i].getTerminalNodes()) {
                    if (node.isEC())
                        if (node.getParent().hasFunctionTag("SBJ")) {
                            builder.append(node.getWord()+"-sbj-"+node.getHeadOfHead().getWord()+' ');
                            continue;
                        } else if (node.getParent().hasFunctionTag("OBJ")) {
                            builder.append(node.getWord()+"-obj-"+node.getHeadOfHead().getWord()+' ');
                            continue;
                        }
                    builder.append(node.getWord()+' ');
                }
                logger.info(builder.toString());
                */
            }
        }
        model.finalizeDictionary(Integer.parseInt(trainProps.getProperty("dictionary.cutoff", "5")));
        
        for (Map.Entry<String, TBTree[]> entry : parseTrain.entrySet()) {
            TBTree[] tbTrees = tbMapTrain.get(entry.getKey());
            TBTree[] parseTrees = entry.getValue();
            SortedMap<Integer, List<PBInstance>> pbInstances = propTrain.get(entry.getKey());
            
            for (int i=0; i<parseTrees.length; ++i)
                model.addTrainingSentence(tbTrees[i], parseTrees[i], pbInstances==null?null:pbInstances.get(i), false);
        }

        model.train(trainProps);
        
        ObjectOutputStream mOut = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(trainProps.getProperty("model_file"))));
        mOut.writeObject(model);
        mOut.close();
        
        //validate(model, props);   
    }
    
    public static void main(String[] args) throws Exception {
        ChineseECTagger options = new ChineseECTagger();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
            if (options.task==null)
                options.help = true;
        } catch (CmdLineException e) {
            System.err.println("invalid options:"+e);
            parser.printUsage(System.err);
            System.exit(0);
        }
        
        if (options.help){
            parser.printUsage(System.err);
            System.exit(0);
        }
        
        if (options.verbose)
            logger.setLevel(Level.FINE);
        
        Properties props = new Properties();
        Reader in = new InputStreamReader(new FileInputStream(options.propFile), "UTF-8");
        props.load(in);
        in.close();
        props = PropertyUtil.resolveEnvironmentVariables(props);
        props = PropertyUtil.filterProperties(props, "ectagger.", true);
        
        if (options.modelName!=null)
            props.setProperty("model_file", options.modelName);
        
        if (options.corpus!=null)
            props.setProperty("validate.corpus", options.corpus);
        
        logger.info(PropertyUtil.toString(props));
        
        ChineseUtil chLangUtil = new ChineseUtil();
        if (!chLangUtil.init(PropertyUtil.filterProperties(props, "chinese.", true)))
            System.exit(-1);
        
        switch (options.task) {
        case EXTRACT:
            extract(props);
            break;
        case TRAIN:
            train(props, chLangUtil);
            break;
        case VALIDATE:
            validate(null, props, chLangUtil);
            break;
        case DECODE:
            break;
        }
    }
}