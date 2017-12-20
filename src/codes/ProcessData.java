package codes;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by samin on 12/31/16.
 */
public class ProcessData{
    protected Company[] companies;
    int currentIndex=0;
    PrintWriter pw;

    public ProcessData() throws IOException {
        companies=new Company[30];
        pw=new PrintWriter(new Socket("127.0.0.1",4000).getOutputStream());
    }

    public void addData(String company, int type, String value, char quarter) throws IOException {
        int adderIndex = 30;
        boolean match=false;

        for(int i=29;i>=0;i--){
            if(companies[i]!=null) if(companies[i].name.matches(company) && quarter==companies[i].quarter){
                adderIndex=i;
                match=true;
                break;
            }
        }
        if(!match){
            companies[currentIndex]=new Company(company);
            adderIndex=currentIndex;
            currentIndex++;
            currentIndex=currentIndex%30;
        }

        companies[adderIndex].inputs[type]=value;
        companies[adderIndex].quarter=quarter;
        makeJSON();
    }

    /*public void arrayPrint(){
        for(int i=0;i<companies.length;i++){
            if(companies[i]!=null) System.out.println(companies[i].name+": "+companies[i].inputs[0]);
        }
    }*/


    private void makeJSON() throws IOException {
        JSONArray object=new JSONArray();
        JSONObject company;
        JSONObject data;
        JSONArray dataList ;

        for(int i=0;i<30;i++){
            Company currentCompany=companies[currentIndex];
            currentIndex++;
            currentIndex=currentIndex%30;
            if(currentCompany==null) continue;

            company=new JSONObject();
            company.put("ticker", currentCompany.name);
            company.put("period", currentCompany.quarter);
            company.put("year", currentCompany.year);
            dataList=new JSONArray();

            for(int j=0;j<7;j++){
                if(currentCompany.inputs[j]!=null){
                    data=new JSONObject();
                    data.put("fieldtype",j);
                    data.put("fieldvalue",currentCompany.inputs[j]);

                    dataList.add(data);
                }
            }

            company.put("data",dataList);
            object.add(company);
        }


        String jsonString=object.toJSONString();
        System.out.println(jsonString);
        pw.print(jsonString);
        pw.flush();
    }

    public int matchTicker(String ticker, char quarter){
        for(int i=29;i>=0;i--){
            if(companies[i]!=null) if(companies[i].name.matches(ticker) && companies[i].quarter==quarter) return i;
        }
        return -1;
    }


    public boolean needGEPS(String ticker, char quarter) {
        int i=matchTicker(ticker,quarter);
        if(i==-1) return true;

        return companies[i].inputs[1]==null;
    }

    public boolean needRev(String ticker, char quarter) {
        int i=matchTicker(ticker, quarter);
        if(i==-1) return true;

        return companies[i].inputs[2]==null;
    }

    public boolean needCompSales(String ticker, char quarter) {
        int i=matchTicker(ticker, quarter);
        if(i==-1) return true;

        return companies[i].inputs[5]==null;
    }
}