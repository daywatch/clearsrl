package clearsrl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import clearcommon.propbank.PBArg;
import clearcommon.propbank.PBInstance;
import clearcommon.treebank.TBNode;
import clearcommon.treebank.TBTree;

public class SRInstance {

    enum OutputFormat {
        TEXT,
        PROPBANK
    };
    
	TBNode predicateNode;
	TBTree tree;
	ArrayList<SRArg> args;
	
	String rolesetId;
	
	public SRInstance(TBNode predicateNode, TBTree tree)
	{
		this.predicateNode = predicateNode;
		this.tree = tree;
		args = new ArrayList<SRArg>();
	}
	
	public SRInstance(TBNode predicateNode, TBTree tree, String rolesetId, double score)
	{
		this(predicateNode, tree);
		this.rolesetId = rolesetId;
		args.add(new SRArg("rel",predicateNode, score));
	}
	/*
	public SRInstance(PBInstance instance) {
		this(instance.predicateNode, instance.tree);
		for (Entry<String, PBArg> entry : instance.getArgs().entrySet())
		{
			BitSet tokenSet = new BitSet(tree.getTokenCount());
			for (TBNode node:entry.getValue().getTokenNodes())
				if (node.tokenIndex>=0) tokenSet.set(node.tokenIndex);
			if (tokenSet.isEmpty()) continue;
			addArg(new SRArg(SRLUtil.removeArgModifier(entry.getKey()), tokenSet));
		}
	}
	*/
	public SRInstance(PBInstance instance) {
		this(instance.getPredicate(), instance.getTree(), instance.getRoleset(), 1.0);
		for (PBArg pbArg: instance.getArgs())
		{
		    if (!pbArg.getTokenSet().isEmpty())
		        addArg(new SRArg(SRLUtil.removeArgModifier(pbArg.getLabel()), pbArg.getNode()));

			for (PBArg nestedArg:pbArg.getNestedArgs())
			    if (!nestedArg.getTokenSet().isEmpty())
	                addArg(new SRArg(SRLUtil.removeArgModifier(nestedArg.getLabel()), nestedArg.getNode()));
		}
	}

	public void addArg(SRArg arg)
	{
		if (!arg.getTokenSet().isEmpty() && tree.getTokenCount() >= arg.getTokenSet().length())
			args.add(arg);
	}
	
	public TBNode getPredicateNode()
	{
		return predicateNode;
	}
	
	public TBTree getTree()
	{
		return tree;
	}
	
	public ArrayList<SRArg> getArgs()
	{
		return args;
	}
	
	public String getRolesetId() {
		return rolesetId;
	}

	public void setRolesetId(String rolesetId) {
		this.rolesetId = rolesetId;
	}
	
	/*
	public void removeOverlap()
	{		
		boolean overlapped = false;
		
		do {
			overlapped = false;
			for (int i=0; i<args.size();++i)
			{
				BitSet argi = args.get(i).tokenSet;
				for (int j=i+1; j<args.size();++j)
				{
					BitSet argj= args.get(j).tokenSet; 
					if (argj.intersects(argi))
					{
						//if (instance.args.get(i).label.equals(instance.args.get(j).label))
						{
							args.remove(argi.cardinality()<argj.cardinality()?i:j);
							overlapped = true;
							break;
						}
					}	
				}
				if (overlapped) break;
			}
		} while (overlapped);
		
		for (int i=0; i<args.size();++i)
		{
			BitSet argi = args.get(i).tokenSet;
			for (int j=i+1; j<args.size();++j)
			{
				BitSet argj= args.get(j).tokenSet; 
				if (argj.intersects(argi))
				{
					//System.out.println(instance);
					return;
				}
			}
		}
	}
*/
	public void removeOverlap()
	{
		System.out.println(args);
		removeOverlap(args);
	}
	
    static void removeOverlap(List<SRArg> args)
    {       
    	LinkedList<SRArg> argQueue = new LinkedList<SRArg>(args);
    	args.clear();
    	
        while (!argQueue.isEmpty())
        {
            LinkedList<SRArg> overlappedArgs = new LinkedList<SRArg>();
            
            overlappedArgs.add(argQueue.pop());
            BitSet tokenSet = (BitSet)overlappedArgs.element().tokenSet.clone();
            
            boolean overlapFound;
            do
            {
                overlapFound = false;
                for (ListIterator<SRArg> iter=argQueue.listIterator(); iter.hasNext();)
                {
                    SRArg arg = iter.next();
                    if (tokenSet.intersects(arg.tokenSet))
                    {
                        overlapFound = true;
                        tokenSet.or(arg.tokenSet);
                        overlappedArgs.add(arg);
                        iter.remove();
                        break;
                    }
                }
            } while (overlapFound);
          
            if (overlappedArgs.size()>1)
            {
                SRArg topArg = overlappedArgs.get(0);
                for (SRArg arg:overlappedArgs)
                    if (arg.score>topArg.score) topArg = arg;

                for (ListIterator<SRArg> iter=overlappedArgs.listIterator(); iter.hasNext();)
                {
                    SRArg arg = iter.next();
                    if (arg==topArg) continue;
                    if (arg.tokenSet.intersects(topArg.tokenSet))
                        iter.remove();
                }
                removeOverlap(overlappedArgs);
            }
     
            args.addAll(overlappedArgs);
        }
    }	

    public void cleanUpArgs()
    {
    	removeOverlap();

    	Collections.sort(args);
    	
    	Map<String, SRArg> argMap = new TreeMap<String, SRArg>();
    	for (SRArg arg: args)
    	{
    		if (arg.label.startsWith("C-") && !argMap.containsKey(arg.label.substring(2)))
    		{
    			arg.label = arg.label.substring(2);
    		}
    		argMap.put(arg.label, arg);
    	}
    }
    
	public String toPropbankString()
	{
        StringBuilder buffer = new StringBuilder();
        buffer.append(tree.getFilename()); buffer.append(' ');
        buffer.append(tree.getIndex()); buffer.append(' ');
        buffer.append(predicateNode.getTerminalIndex()); buffer.append(' ');
        buffer.append("system "); 
        buffer.append(rolesetId==null?predicateNode.getWord()+".XX":rolesetId);
        buffer.append(" ----- ");
        
        Collections.sort(args);
        
        TreeMap<String, List<StringBuilder>> argMap = new TreeMap<String, List<StringBuilder>>();
        
        for (SRArg arg:args)
        {
           if (arg.label.equals(SRLModel.NOT_ARG)) continue;
           
           List<StringBuilder> argOut;
           
           String label = arg.label.startsWith("C-")?arg.label.substring(2):arg.label;
           if ((argOut = argMap.get(label))==null)
           {
        	   argOut = new LinkedList<StringBuilder>();
               argMap.put(label, argOut);
           }
           
           int[] id = arg.node.getPBId();
           if (arg.label.startsWith("C-"))
        	   argOut.get(argOut.size()-1).append(","+id[0]+":"+id[1]);
           else
        	   argOut.add(new StringBuilder(id[0]+":"+id[1]));
        }
        
        for (Map.Entry<String, List<StringBuilder>> entry:argMap.entrySet())
        {
        	for (StringBuilder builder:entry.getValue())
        	{
        		buffer.append(builder.toString());
                buffer.append('-');
                buffer.append(entry.getKey()); buffer.append(' ');   
        	}
        }
        
        return buffer.toString();
	}
	/*
	public String toPropbankString()
	{
        StringBuilder buffer = new StringBuilder();
        buffer.append(tree.getFilename()); buffer.append(' ');
        buffer.append(tree.getIndex()); buffer.append(' ');
        buffer.append(predicateNode.getTerminalIndex()); buffer.append(' ');
        buffer.append("system "); buffer.append(predicateNode.getWord());
        buffer.append(" ----- ");
        
        TreeMap<String, TreeSet<SRArg>> argMap = new TreeMap<String, TreeSet<SRArg>>();
        
        for (SRArg arg:args)
        {
           if (arg.label.equals(SRLModel.NOT_ARG)) continue;
           TreeSet<SRArg> argSet;
           if ((argSet = argMap.get(arg.label))==null)
           {
               argSet = new TreeSet<SRArg>();
               argMap.put(arg.label, argSet);
           }
           argSet.add(arg);
        }
        
        for (Map.Entry<String, TreeSet<SRArg>> entry:argMap.entrySet())
        {
            String argStr = "";
            for (SRArg arg:entry.getValue())
            {
                int depth=0;
                TBNode node = arg.node;
                while (!node.isTerminal())
                {
                    ++depth;
                    node=node.getChildren()[0];
                }
                argStr+=node.getTerminalIndex()+":"+depth+"*";
            }
            buffer.append(argStr.substring(0,argStr.length()-1));
            buffer.append('-');
            buffer.append(entry.getKey()); buffer.append(' ');   
        }
        
        return buffer.toString();
	}
*/
	
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append(tree.getFilename()); buffer.append(" ");
		buffer.append(tree.getIndex()); buffer.append(" ");
		
		List<TBNode> nodes = tree.getRootNode().getTokenNodes();
		String[] tokens = new String[nodes.size()];
		for (int i=0; i<tokens.length; ++i)
			tokens[i] = nodes.get(i).getWord();
		
		String[] labels = new String[nodes.size()];
		
		for (SRArg arg:args)
		{
		    if (arg.label.equals(SRLModel.NOT_ARG)) continue;
			BitSet bits = arg.getTokenSet();
			
			for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1))
			    labels[i] = arg.label;
		}
		
		String previousLabel = null;
		for (int i=0; i<tokens.length; ++i)
		{
		    if (labels[i]!=null && labels[i].startsWith("C-") && labels[i].substring(2).equals(previousLabel))
		            labels[i] = labels[i].substring(2);
		    previousLabel = labels[i];
		}
		
		for (int i=0; i<tokens.length; ++i)
        {
            if (labels[i]!=null && (i==0 || !labels[i].equals(labels[i-1])))
                buffer.append('['+labels[i]+' ');
            buffer.append(tokens[i]);
            if (labels[i]!=null && (i==tokens.length-1 || !labels[i].equals(labels[i+1])))
                buffer.append(']');
                    
            buffer.append(' ');      
        }
		
		return buffer.toString();
	}
	
    public String toString(OutputFormat outputFormat) {
        switch (outputFormat)
        {
        case TEXT:
            return toString();
        case PROPBANK:
            return toPropbankString();
        }
        return toString();
    }
	
}
