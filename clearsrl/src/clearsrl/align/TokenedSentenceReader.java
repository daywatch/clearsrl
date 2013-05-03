package clearsrl.align;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedMap;

import clearcommon.propbank.DefaultPBTokenizer;
import clearcommon.propbank.OntoNotesTokenizer;
import clearcommon.propbank.PBInstance;
import clearcommon.propbank.PBUtil;
import clearcommon.treebank.TBReader;
import clearcommon.treebank.TBTree;
import clearcommon.treebank.TBUtil;

public class TokenedSentenceReader extends SentenceReader {

    Map<String, TBTree[]> treeBank;
    Map<String, SortedMap<Integer, List<PBInstance>>>  propBank;
    Scanner tokenScanner;
    boolean tokenIndexed;
    
    public TokenedSentenceReader(Properties props)
    {
       super(props);
    }
    
    @Override
    public void close() {
        if (tokenScanner!=null) {
            tokenScanner.close();
            tokenScanner=null;
        }
    }

    @Override
    public void initialize() throws FileNotFoundException {
        close();

        if (treeBank==null)
            treeBank = TBUtil.readTBDir(props.getProperty("tbdir"), props.getProperty("tb.regex"));
        if (propBank==null)
            propBank = PBUtil.readPBDir(props.getProperty("pbdir"), props.getProperty("pb.regex"), new TBReader(treeBank), props.getProperty("goldpb", "false").equals("false")?new DefaultPBTokenizer():new OntoNotesTokenizer());
        
        tokenScanner = new Scanner(new BufferedReader(new FileReader(props.getProperty("token_idx")))).useDelimiter("[\n\r]");
        
        tokenIndexed = !props.getProperty("tokenIndexed","false").equals("false");
    }

    @Override
    public Sentence nextSentence() {
        if (!tokenScanner.hasNext()) return null;
        return Sentence.parseSentence(tokenScanner.nextLine(), treeBank, propBank, tokenIndexed);
    }

}
