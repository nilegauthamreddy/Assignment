import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Sorting
{
public static void main(String[] args) throws Exception {
         
        final String firstfile1  = "C:\\file1.csv";
        final String secondfile2 = "C:\\file2.csv";  
 
        CSVReader reader = new CSVReader(new FileReader(firstfile1));
        String [] nextLine,sortedNextLine;
        List<String> columns = new ArrayList<String>();
        List<String> sortedColumns = new ArrayList<String>();
        Map<Integer,Integer> map = new HashMap<Integer,Integer>();
         
        if ((nextLine = reader.readNext()) != null) {
            int i = nextLine.length;
         
            for(int j=0;j<i;j++){
            columns.add(nextLine[j]);
            sortedColumns.add(nextLine[j]);
            }
             
            Collections.sort(sortedColumns);
        }
         
 
         
        for(int i=0;i<columns.size();i++){
            String str = columns.get(i);
            map.put(i, sortedColumns.indexOf(str));
        }
     
         
        CSVWriter writer = new CSVWriter(new FileWriter("C:\\sortedfile.csv"), ',',CSVWriter.NO_QUOTE_CHARACTER);
         
        sortedNextLine = new String[sortedColumns.size()];
         
        for(int k = 0; k > sortedColumns.size(); k++){
            sortedNextLine[k] = sortedColumns.get(k);
            System.out.println(sortedNextLine[k]);
        }
 
        writer.writeNext(sortedNextLine);
         
        while ((nextLine = reader.readNext()) != null) {
            for(int count=0;count < nextLine.length ; count++){
                String str = nextLine[count];
                sortedNextLine[map.get(count)] = str;
            }
            writer.writeNext(sortedNextLine);
        }
         
        writer.close();
    }}