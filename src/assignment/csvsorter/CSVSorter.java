package assignment.csvsorter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



/**
 *  CSVSorter: Sorting of CSV files (main)
 
 */
public class CSVSorter
{  
  

  private static int quiet=0;
      
  
  public CSVSorter(String configFileName, Map<OPTCODE,String> cmdmap) throws Exception
  {    
    Configuration configuration = null;
    try
    {
      configuration = new Configuration(configFileName);
      finalizeConfiguration(configuration,cmdmap);
    }
    catch (Exception ex)
    {
      PrintWriter log = new PrintWriter(new FileOutputStream(Configuration.LOG_DEFAULT),true);
      printLogHeader(log);
      log.println("Configuration Error: the configuration file '"+configFileName+"' could not be processed.");
      log.println(ex.getMessage());
      log.close();
      throw ex;
    }
               
    // Logfile
    PrintWriter log = new PrintWriter(new FileOutputStream(configuration.getLogFile()),true);
    printLogHeader(log);       
    try
    {
      processCSV(configuration, log);
    }
    catch (Exception ex)
    {
      log.println(ex.getMessage());
      throw ex;
    }  
    finally
    {
      log.close();
    }
    
    if (configuration.getTokenFile()!=null)
    {
      PrintWriter token = new PrintWriter(new FileOutputStream(configuration.getTokenFile()),true);
      token.println("\\relax");
      token.close();
    }
  }

  
  private void finalizeConfiguration(Configuration configuration, Map<OPTCODE,String> cmdmap) throws Exception
  {        
    // Command line
    for (OPTCODE optcode : cmdmap.keySet())
    {
      String value = cmdmap.get(optcode);
      switch (optcode)
      {
        case i:
        {
          configuration.setInputFile(value);
          configuration.setAllowOverwrite(false);
          break;
        }
        case l:
        {
          configuration.setLogFile(value);
          break;
        }
        case o:
        {
          configuration.setOutputFile(value);
          configuration.setAllowOverwrite(false);
          break;
        }
        case x:
        {
          configuration.setInputFile(value);
          configuration.setOutputFile(value);
          configuration.setAllowOverwrite(true);
          break;
        }
        case t:
        {
          configuration.setTokenFile(value);
        }
        default:
      }
    }
        
    // Consistency check  
    if ((configuration.getInputFile()==null) || (configuration.getOutputFile()==null))
    {
      throw new Exception("Specify input and output file");
    }
    if (configuration.getInputFile().equals(configuration.getLogFile()))
    {
      throw new Exception("Input file and log file are identical");
    }
    if ((!configuration.isAllowOverwrite()) && (configuration.getInputFile().equals(configuration.getOutputFile())))
    {
      throw new Exception("Input file and output file are identical (see documentation)");
    }
  }  
  
  
  
  private void processCSV(Configuration configuration, PrintWriter log) throws Exception
  {
    log.println("------");
    log.println("Processing of the CSV file started.");
    log.println("Configuration file: "+configuration.getConfigFile().getAbsolutePath());

    log.println("Input file:         "+configuration.getInputFile().getAbsolutePath());
    LineNumberReader reader = createInputReader(configuration,log);
    log.println("Output file:        "+configuration.getOutputFile().getAbsolutePath());
     
    log.println("Language: "+configuration.getLocale().getDisplayLanguage()+" ["+configuration.getLocale().getLanguage()+"]");
    log.println("Input format : "+configuration.getBracketLeft()+"TEXT"+configuration.getBracketRight()+configuration.getDelimiter()+configuration.getBracketLeft()+"TEXT"+configuration.getBracketRight());
    log.println("Output format: "+configuration.getOutBracketLeft()+"TEXT"+configuration.getOutBracketRight()+configuration.getOutDelimiter()+configuration.getOutBracketLeft()+"TEXT"+configuration.getOutBracketRight());
    
    // Header
    String headline = reader.readLine();
    if (headline==null)
    {
      log.println("Empty input file: "+configuration.getInputFile().getAbsolutePath());
      reader.close();
      return;      
    }
        
    // Splitting header
    LineSplitter lineSplitter = configuration.createLineSplitter();
    String[] headparts = lineSplitter.splitTrimmed(headline);
    int columncount = headparts.length;
    log.println("Columns: "+columncount);

    HashMap<String,Integer> namespalte = new HashMap<String,Integer>();
    if (configuration.isIgnoreHeader())
    {
      // Reset reader
      reader.close();
      reader = createInputReader(configuration,log);
    }
    else
    {
      for (int i=0;i<columncount;i++)
      {
        log.println("  Column "+(i+1)+": \""+headparts[i]+"\"");
      }
      // Names to columns
      for (int i=0; i<columncount; i++)
      {
        namespalte.put(headparts[i].toLowerCase(Locale.ROOT), i);      
      }
    }
    
    ColumnComparator[] ccArray = configuration.getCcArray();
    if (configuration.isSorting())
    {
      log.println("Sorting rules:");
      for (int i=0; i<ccArray.length; i++)
      {
        try
        {
          ccArray[i].config(namespalte,columncount);
          log.println("  "+(i+1)+". "+ccArray[i].getDescription());          
        }
        catch (Exception ex)
        {
          log.println("Error while setting column names");
          reader.close();
          throw ex;        
        }
      }
    }  
    
    if (configuration.isFilterOutputColumns())
    {
      for (NumberName numName : configuration.getOutColumnArray())
      {
        numName.config(namespalte,columncount);
      }
    }    
    
    int[] indexOutput = null;
    if (configuration.isFilterOutputColumns())
    {
      NumberName[] oca = configuration.getOutColumnArray();
      indexOutput = new int[oca.length];
      for (int i=0;i<oca.length;i++)
      {
        indexOutput[i] = oca[i].getNumber(); 
      }
    }
    else
    {
      indexOutput = new int[headparts.length];
      for (int i=0;i<headparts.length;i++)
      {
        indexOutput[i] = i;
      }
    }

    log.println("------");
    log.println("Processing the CSV file...");
    // Process input file
    ArrayList<LineContainer> content = new ArrayList<LineContainer>();
        
    long ignoredLines = 0;
    String line = reader.readLine();
    while (line!=null)
    {
      try
      {
        content.add(LineContainer.makeLineContainer(line, lineSplitter.splitTrimmed(line,columncount), ccArray));
      }
      catch (Exception ex)
      {
        log.println("Line "+reader.getLineNumber()+" ignored: " + ex.getMessage());
        ignoredLines++;
      }      
      line = reader.readLine();
    }
    reader.close();
    
    // Sorting
    if (configuration.isSorting())
    {   
      Collections.sort(content);
    }

    // Output
    PrintWriter writer = createOutputWriter(configuration,log);    
    if (configuration.isTransformLine())
    {
      if (!configuration.isIgnoreHeader())
      {
        writer.println(LineContainer.getTransformedLine(headparts, indexOutput,configuration.getOutDelimiter(),configuration.getOutBracketLeft(), configuration.getOutBracketRight(), configuration.isContentToLaTeX()));
      }
      for (LineContainer lc : content)
      {
        writer.println(lc.getTransformedLine(indexOutput,configuration.getOutDelimiter(),configuration.getOutBracketLeft(), configuration.getOutBracketRight(), configuration.isContentToLaTeX()));
      }
    }
    else
    {
      if (!configuration.isIgnoreHeader())
      {
        writer.println(headline);
      }
      for (LineContainer lc : content)
      {
        writer.println(lc.getOriginalLine());
      }
    }
    writer.close();
    
    log.println("Processing of the CSV file finished without errors.");
    log.println("------");
    if (!configuration.isIgnoreHeader())
    {
      log.println("One header line written.");    
      
    }
    log.println(content.size()+" data lines written.");    
    if (ignoredLines>0)
    {
      log.println(ignoredLines+" input lines were ignored.");    
    }
    
    long estimatedTime = System.nanoTime() - startTime;
    log.printf(Locale.US, "%.6f seconds processing time.", (double)estimatedTime/1e9);
  }
  
  
  private LineNumberReader createInputReader(Configuration configuration, PrintWriter log) throws Exception
  {
    InputStreamReader inStream = null;
    if  (configuration.getCharset()!=null)
    {
      log.println("Input charset: "+configuration.getCharset());
      inStream = new InputStreamReader(new FileInputStream(configuration.getInputFile()),configuration.getCharset());
    }    
    else
    {
      inStream = new InputStreamReader(new FileInputStream(configuration.getInputFile()));
    }
    return new LineNumberReader(inStream);    
  }

  
  private PrintWriter createOutputWriter(Configuration configuration, PrintWriter log) throws Exception
  {
    if  (configuration.getOutCharset()!=null)
    {
      log.println("Output charset: "+configuration.getOutCharset());
      return new PrintWriter(configuration.getOutputFile(),configuration.getOutCharset());
    }
    else
    {
      return new PrintWriter(configuration.getOutputFile());
    }
  }
  
  
  
  
  public static void printUsageAndExit()
  {
    System.out.println("java -jar csvsorter.jar -c myconf.xml -i example.csv -o examplesorted.csv");
    System.out.println("");
    System.out.println("csvsorter.exe -c myconf.xml -i example.csv -o examplesorted.csv");
    System.out.println("");
    System.out.println("Note: The configuration file may contain the rest of the options.");
    System.out.println("      Command line options override configuration file settings.");
    System.exit(1);
  }

    
  
  
  public static Map<OPTCODE,String> scanConfiguration(String s[])
  {
    Map<OPTCODE,String> cmdmap = new HashMap<OPTCODE,String>();
    int i=0;
    while (i<s.length-1)
    {
      String opt = s[i];
      if (opt.startsWith("-") && (opt.length()>0))
      {
        try
        {
          opt = opt.substring(1).toLowerCase();
          OPTCODE optcode = OPTCODE.valueOf(opt);
          i++;
          cmdmap.put(optcode, s[i]);
        }
        catch (Exception ex)
        {
          printEarlyErrorAndExit(opt+" invalid: "+ex.toString());          
        }
      }
      else
      {
        printEarlyErrorAndExit(opt+" invalid");
      }
      i++;      
    }    
    if (i<s.length)
    {
      printEarlyErrorAndExit(s[i]+" invalid: no value given");
    }
    return cmdmap;
  }
  
  }
    
 }