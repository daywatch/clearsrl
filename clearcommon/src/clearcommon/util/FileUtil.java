package clearcommon.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class FileUtil
{
    static public List<String> getFiles(File dir, String regex)
    {
        return getFiles(dir, regex, false);
    }
    
    static public List<String> getFileList(File dir, File file)
    {
    	Set<String> fileList = new TreeSet<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
	    	while ((line=reader.readLine())!=null)
	    		if (new File(dir, line).isFile())
	    			fileList.add(line);
	    	reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
        return new ArrayList<String>(fileList);
    }
    
	static public List<String> getFiles(File dir, String regex, boolean fullName)
	{
		List<String> fileNames = new ArrayList<String>();
		if (!dir.isDirectory())
		{
		    if (Pattern.matches(regex, dir.getName()))
                fileNames.add(fullName?dir.getAbsolutePath():dir.getName());
	          return fileNames;
		}
		
		File[] files = dir.listFiles();
		Arrays.sort(files);
		
		for (File file:files)
		{
			if (file.isDirectory())
			{
				if (file.getName().startsWith("."))
					continue;
				List<String> moreFileNames = getFiles(file, regex, fullName);
				for (String fileName:moreFileNames)
					fileNames.add(fullName?fileName:file.getName()+File.separatorChar+fileName);
			}
			else if (Pattern.matches(regex, file.getName()))
				fileNames.add(fullName?file.getAbsolutePath():file.getName());
		}
		return fileNames;
	}
}
