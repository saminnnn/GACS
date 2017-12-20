package codes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by samin on 12/31/16.
 */
public class Start {
    public static ArrayList<String> allCompanies;
    public static Map<Character,Integer> companyIndex;
    public static char[] charArray={'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};

    public static void main(String[] args) throws Exception {
        System.out.println("Old version renewed");
        makeCompanyIndex();
        ProcessData p=new ProcessData();
        //new Thread(new OtherData(p)).start();
        new DataGetter(p);

    }

    private static void makeCompanyIndex() throws IOException {
        allCompanies =new ArrayList<>();
        companyIndex=new HashMap<>();
        Scanner scan=new Scanner(new File("tickerList.txt"));

        while (scan.hasNextLine()){
            allCompanies.add(scan.nextLine());
        }
        scan.close();

        int j,size= allCompanies.size();
        for(int i=0;i<26;i++){
            for(j=0;j<size;j++){
                if(allCompanies.get(j).charAt(0)==charArray[i]){
                    companyIndex.put(charArray[i], j);
                    break;
                }
            }
        }
    }
}
