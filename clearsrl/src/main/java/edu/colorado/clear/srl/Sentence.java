package edu.colorado.clear.srl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.colorado.clear.common.propbank.DefaultPBTokenizer;
import edu.colorado.clear.common.propbank.OntoNotesTokenizer;
import edu.colorado.clear.common.propbank.PBInstance;
import edu.colorado.clear.common.propbank.PBTokenizer;
import edu.colorado.clear.common.propbank.PBUtil;
import edu.colorado.clear.common.treebank.TBReader;
import edu.colorado.clear.common.treebank.TBTree;
import edu.colorado.clear.common.treebank.TBUtil;
import edu.colorado.clear.common.util.FileUtil;
import edu.colorado.clear.common.util.LanguageUtil;
import edu.colorado.clear.srl.ec.ECCommon;

public class Sentence implements Serializable{
	
	/**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger("clearsrl");
    
	public enum Source {
		TREEBANK("tb", true),
		TB_HEAD("tb.headed", true),
		PROPBANK("pb"),
		PREDICATE_LIST("pred"),
		PARSE("parse", true),
		PARSE_HEAD("parse.headed", true),
		PARSE_DEP("parse.dep"),
		SRL("prop"),
		EC_DEP("ecdep"),
		NAMED_ENTITY("ne");
		
		Source(String prefix) {
			this(prefix, false);
		}
		
		Source(String prefix, boolean isTree) {
			this.prefix = prefix;
			this.isTree = isTree;
		}
		
		public String prefix;
		public boolean isTree;
	}

	public TBTree treeTB;
	public List<PBInstance> propPB;
	
	public BitSet predicates;

	public TBTree parse;
	public List<PBInstance> props;
	public String[][] depEC;

	public String[] namedEntities;
	
	public Set<String> annotatedNominals = null;
	
	public Sentence(TBTree treeTB, List<PBInstance> propPB, TBTree parse, List<PBInstance> props, BitSet predicates, String[][] depEC, String[] namedEntities) {
		this.treeTB = treeTB;
		this.propPB = propPB;
		this.parse = parse;
		this.props = props;
		this.predicates = predicates;
		this.depEC = depEC;
		this.namedEntities = namedEntities;
	}
	
	public static EnumSet<Source> readSources(String input) {
		List<Source> srcs = new ArrayList<Source>();
		for (String srcStr:input.trim().split("\\s*,\\s*"))
			srcs.add(Source.valueOf(srcStr));
		return EnumSet.copyOf(srcs);
	}
	
	static final Pattern nePattern = Pattern.compile("<(/??[A-Z]{3,}?)>");
	static String[][] readNE(File file, TBTree[] trees) {
		List<String[]> neList = new ArrayList<String[]>();
		logger.info("Reading NE from "+file.getPath());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
			String line;
			while ((line=reader.readLine())!=null) {
				String[] tokens = line.trim().split(" +");
				if (trees!=null) {
					if (neList.size()>=trees.length) {
						logger.severe("NE line length exceeded trees for "+file.getPath());
						break;
					}
					if (tokens.length!=trees[neList.size()].getTokenCount()) {
						logger.warning(String.format("NE mismatch found for %s:%d\n", file.getPath(), neList.size()));
						neList.add(null);
						break;
					}
				}
				String[] nes = new String[tokens.length];
				String currNE=null;
				for (int i=0; i<tokens.length; ++i) {
					Matcher matcher = nePattern.matcher(tokens[i]);
					while (matcher.find()) {
						String match = matcher.group(1);
						if (match.charAt(0)=='/')
							currNE = null;
						else
							nes[i] = currNE = match;
					}
					if (currNE!=null)
						nes[i] = currNE;
				}
				
				neList.add(nes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return neList.toArray(new String[trees==null?neList.size():trees.length][]);
	}
	
	static Map<String, String[][]> readNamedEntities(Properties props, String prefix, Map<String, TBTree[]> treeMap) {
		Map<String, String[][]> neMap = new HashMap<String, String[][]>();
		String neDir = props.getProperty(prefix+".dir");
		String neRegex = props.getProperty(prefix+".regex");
		
		List<String> fileList = FileUtil.getFiles(new File(neDir), neRegex, false);
		for (String fName:fileList) {
			String key = fName.endsWith(".ner")?fName.substring(0,fName.length()-4)+".parse":fName;
			
			if (treeMap!=null && !treeMap.containsKey(key)) continue;

			neMap.put(key, readNE(new File(neDir, fName), treeMap==null?null:treeMap.get(key)));
		}
		return neMap;
	}
	
	static Map<String, List<BitSet>> readPredicates(Properties props, String prefix, Map<String, TBTree[]> treeMap) {
		Map<String, List<BitSet>> predMap = new HashMap<String, List<BitSet>>();
		String predDir = props.getProperty(prefix+".dir");
		String predRegex = props.getProperty(prefix+".regex");
		
		List<String> fileList = FileUtil.getFiles(new File(predDir), predRegex, false);
		for (String fName:fileList) {
			String key = fName.endsWith(".pred")?fName.substring(0,fName.length()-5)+".parse":fName;
			
			System.out.println("reading predicates from "+fName+" "+key);
			System.out.println(treeMap.keySet());
			if (treeMap!=null && !treeMap.containsKey(key)) continue;
			
			System.out.println("reading predicates from "+key);

			List<BitSet> predList = new ArrayList<BitSet>();
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(predDir, fName))))) {
				String line;
				while ((line=reader.readLine())!=null) {
					BitSet predicates = new BitSet();
					line = line.trim();
					if (!line.isEmpty())
						for (String token:line.split("\\s+"))
							predicates.set(Integer.parseInt(token));
					predList.add(predicates);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			predMap.put(key, predList);
		}
		return predMap;
	}
	
	
	static Map<String, SortedMap<Integer, List<PBInstance>>> readProps(Properties props, String prefix, Map<String, TBTree[]> treeMap) {
		String propDir = props.getProperty(prefix+".dir");
		String filename = props.getProperty(prefix+".filelist");
		String propRegex = props.getProperty(prefix+".regex");
		
		List<String> fileList = filename==null?FileUtil.getFiles(new File(propDir), propRegex, true)
                :FileUtil.getFileList(new File(propDir), new File(filename), true);
		
		PBTokenizer tokenzier = null;
		try {
	        tokenzier = props.getProperty(prefix+".tokenizer")==null
	        		?(props.getProperty("data.format", "default").equals("ontonotes")?new OntoNotesTokenizer():new DefaultPBTokenizer())
	        		:(PBTokenizer)Class.forName(props.getProperty(prefix+".tokenizer")).newInstance();
        } catch (Exception e) {
	        e.printStackTrace();
	        return null;
        }
		return PBUtil.readPBDir(fileList, new TBReader(treeMap), tokenzier);
	}
			
	public static Map<String, Sentence[]> readCorpus(Properties props, Source headSource, EnumSet<Source> sources, LanguageUtil langUtil) {
		Map<String, Sentence[]> sentenceMap = new TreeMap<String, Sentence[]>();

		if (!headSource.isTree) {
			Logger.getLogger("clearsrl").warning("head source is not a tree source!!!");
			return null;
		}
		
		String treeDir = props.getProperty(headSource.prefix+".dir");
		String filename = props.getProperty(headSource.prefix+".filelist");
		String treeRegex = props.getProperty(headSource.prefix+".regex");
		
		List<String> fileList = filename==null?FileUtil.getFiles(new File(treeDir), treeRegex, false)
                 :FileUtil.getFileList(new File(treeDir), new File(filename), false);
		
		Map<String, TBTree[]> sourceMap = TBUtil.readTBDir(treeDir, fileList, headSource.equals(Source.PARSE)||headSource.equals(Source.TREEBANK)?langUtil.getHeadRules():null);

		Map<String, TBTree[]> treeMap=null;
		if (sources.contains(Source.TREEBANK))
			treeMap = headSource.equals(Source.TREEBANK)?sourceMap:TBUtil.readTBDir(props.getProperty(Source.TREEBANK.prefix+".dir"), fileList, langUtil.getHeadRules());
		else if (sources.contains(Source.TB_HEAD))
			treeMap = headSource.equals(Source.TB_HEAD)?sourceMap:TBUtil.readTBDir(props.getProperty(Source.TB_HEAD.prefix+".dir"), fileList);
		
		Map<String, SortedMap<Integer, List<PBInstance>>> pbMap=null;
		if (sources.contains(Source.PROPBANK) && treeMap!=null)
			pbMap = readProps(props, Source.PROPBANK.prefix, treeMap);
		
		Map<String, TBTree[]> parseMap=null;
		if (sources.contains(Source.PARSE))
			parseMap = headSource.equals(Source.PARSE)?sourceMap:TBUtil.readTBDir(props.getProperty(Source.PARSE.prefix+".dir"), fileList, langUtil.getHeadRules());
		else if (sources.contains(Source.PARSE_HEAD))
			parseMap = headSource.equals(Source.PARSE_HEAD)?sourceMap:TBUtil.readTBDir(props.getProperty(Source.PARSE_HEAD.prefix+".dir"), fileList);
		
		if (sources.contains(Source.PARSE_DEP))
			TBUtil.addDependency(parseMap, new File(props.getProperty(Source.PARSE_DEP.prefix+".dir")), 
					Integer.parseInt(props.getProperty(Source.PARSE_DEP.prefix+".idxcol", "6")), 
					Integer.parseInt(props.getProperty(Source.PARSE_DEP.prefix+".labelcol", "7")));
		
		Map<String, SortedMap<Integer, List<PBInstance>>> srlMap=null;
		if (sources.contains(Source.SRL) && parseMap!=null)
			srlMap = readProps(props, Source.SRL.prefix, parseMap);
		
		Map<String, Map<Integer, String[][]>> ecDepMap=null;
		if (sources.contains(Source.EC_DEP) && parseMap!=null)
			ecDepMap = ECCommon.readDepEC(new File(props.getProperty(Source.EC_DEP.prefix+".dir")), parseMap);
	
		Map<String, String[][]> neMap=null;
		if (sources.contains(Source.NAMED_ENTITY))
			neMap = readNamedEntities(props, Source.NAMED_ENTITY.prefix, parseMap);
		
		Map<String, List<BitSet>> predMap=null;
		if (sources.contains(Source.PREDICATE_LIST))
			predMap = readPredicates(props, Source.PREDICATE_LIST.prefix, parseMap);
		
		for (Map.Entry<String, TBTree[]> entry:sourceMap.entrySet()) {
			Sentence[] sentences = new Sentence[entry.getValue().length];
			sentenceMap.put(entry.getKey(), sentences);
			
			TBTree[] trees = treeMap==null?null:treeMap.get(entry.getKey());
			Map<Integer, List<PBInstance>> propPBs = pbMap==null?null:pbMap.get(entry.getKey());
			
			TBTree[] parses = parseMap==null?null:parseMap.get(entry.getKey());
			Map<Integer, List<PBInstance>> srls = srlMap==null?null:srlMap.get(entry.getKey());
			Map<Integer, String[][]> ecDeps = ecDepMap==null?null:ecDepMap.get(entry.getKey());
	
			String[][] namedEntities = neMap==null?null:neMap.get(entry.getKey());
			
			List<BitSet> predList = predMap==null?null:predMap.get(entry.getKey());
			
			for (int i=0; i<entry.getValue().length; ++i)
				sentences[i] = new Sentence(trees==null?null:trees[i], 
											!sources.contains(Source.PROPBANK)?null:(propPBs==null?new ArrayList<PBInstance>():(propPBs.get(i)==null?new ArrayList<PBInstance>():propPBs.get(i))), 
								            parses==null?null:parses[i], 
										    srls==null?null:srls.get(i), 
										    predList==null?null:i<predList.size()?predList.get(i):new BitSet(),
									        ecDeps==null?null:ecDeps.get(i), 
									        namedEntities==null?null:namedEntities[i]);
		}	
		return sentenceMap;
	}
}
