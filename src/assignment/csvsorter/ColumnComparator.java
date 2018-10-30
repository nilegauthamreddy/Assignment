package assignment.csvsorter;

import java.util.HashMap;

/**
 *  ColumnComparator: Comparator for columns
 */  
@SuppressWarnings("rawtypes")
interface ColumnComparator
{
    
  public Comparable generateKey(String[] parts) throws Exception;
  
  public void config(HashMap<String,Integer> nameToColumn, int maxNumber) throws Exception;
   
  public String getDescription();
  
}
