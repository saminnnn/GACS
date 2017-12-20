package codes;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by samin on 1/8/17.
 * int geps=0,eps=1,rev=2
 */
public class ProcessInput {
    SearchString searchString;
    char[] content;
    Double geps=0.0;
    String ticker="";
    ProcessData processData;
    //PrintWriter logWriter;
    String source;

    //public ProcessInput(String contentText, ProcessData p, PrintWriter lw, String source) throws IOException {
    public ProcessInput(String contentText, ProcessData p, String source) throws IOException {
        //long start=System.currentTimeMillis();
        //logWriter=lw;
        this.source=source;
        processData=p;
        searchString=new SearchString(contentText);
        this.content=searchString.passage;

        int index=searchString.search("\"ERN\"");
        if(index!=-1){
            index=searchString.search("StockTwits");
            if(index==-1){
                int tickerIndex=searchString.search("\"ScoredEntity\"");
                if(tickerIndex!=-1){
                    tickerIndex=searchString.search("id",tickerIndex+1);
                    for(int i=0;content[i+7+tickerIndex]!='"';i++) ticker+=content[i+tickerIndex+7];
                    if(matchCompany(ticker)){
                        processHeadline();
                    }
                }
            }
        }
    }

    private void processHeadline() throws IOException {
        //long startTime=System.currentTimeMillis();
        int headLineIndex=searchString.search("\"Headline\"");
        if(headLineIndex==-1) return;
        boolean loss=false, share=false;
        String word="";
        String value = null,year=null;
        int type=0;
        char quarter='0';

        int start=headLineIndex+14;
        for(headLineIndex=start;;headLineIndex++){
            if(content[headLineIndex]==' ' || content[headLineIndex]==',' || content[headLineIndex]=='"'){
                if(word.length()>1){
                    System.out.print(word+", ");

                    if(word.contains("LOSS")) loss=true;
                    if(word.contains("SHARE") || word.contains("SHR")) share=true;

                    char c=word.charAt(0);
                    switch (c){
                        case '$': if(value==null) value=word.substring(1,word.length());
                            break;
                        case 'Q': if(word.charAt(1)>='1' && word.charAt(1)<='4' && quarter=='0') quarter=word.charAt(1);
                            break;
                        case 'E': if(word.length()>2) if(word.charAt(1)=='P' && word.charAt(2)=='S' && type==0) type=1;
                            break;
                        case 'L':  if(word.length()>2) if(word.charAt(1)=='P' && word.charAt(2)=='S' && type==0){
                            type=1;
                            loss=true;
                            share=true;
                        }
                            break;
                        case 'R': if(word.contains("REV") && type==0) type=3;
                            break;
                        case 'N': if(word.contains("SALES") && type==0) type=3;
                            break;
                        case 'S': if(word.matches("SALES") && type==0) type=3;
                            break;
                        case '2': if(word.charAt(1)=='0' && word.length()==4){
                            year=word;
                            break;
                        }
                    }

                    if(c>='0' && c<='9'){
                        int dash=word.indexOf("-");
                        if(dash>0) word=word.substring(0,dash);
                        if(word.length()>1){
                            if(word.charAt(1)=='Q' && word.length()==2) quarter=c;
                            else if(value==null && word.charAt(1)!='T') value=word;
                        }
                    }
                }

                word="";
                if(content[headLineIndex]==',' || content[headLineIndex]=='"') break;

            }
            else word+=content[headLineIndex];

        }

        if(loss && share) type=1;
        if(type!=0 && value!=null){
            //String date=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
           // logWriter.println(ticker+", "+quarter+", "+type+", "+value+", "+date);
           // new Thread(new LogWriting(logWriter,ticker+", "+quarter+", "+type+", "+value+", "+date));
            //logWriter.flush();
        }

        if(type==1 && value!=null && processData.needGEPS(ticker,quarter)){
            System.out.println("adding 1");
            try{
                if(value.charAt(value.length()-1)=='C'){
                    geps=Double.parseDouble(value.substring(0,value.length()-1));
                    geps=geps/100;
                }
                else geps=Double.parseDouble(value);
                if(loss && geps>0) geps=geps*-1.0;
                //System.out.println("Time "+(System.currentTimeMillis()-startTime));
                if(geps<25.0) processData.addData(ticker,1,geps.toString(),quarter);
            }
            catch(NumberFormatException e){
                System.out.println("Number format error for "+value);
            }
        }
        else if(type==3  && value!=null){
            System.out.println("adding 3");
            if(processData.needRev(ticker, quarter)) {
                Double revenueValue=0.0;
                try{
                    revenueValue = Double.parseDouble(value.substring(0, value.length() - 1));
                    if (value.charAt(value.length() - 1) == 'B') revenueValue = revenueValue * 1000000000;
                    else if (value.charAt(value.length() - 1) == 'M') revenueValue = revenueValue * 1000000;

                    if(revenueValue>100){
                        //System.out.println("Time "+(System.currentTimeMillis()-startTime));
                        if(quarter!='0') processData.addData(ticker, 2, String.format("%.0f", revenueValue),quarter);
                    }
                }
                catch(NumberFormatException e){
                    System.out.println("Number Format Exception");
                }
            }
        }
    }

    private boolean matchCompany(String ticker) {
        //long start=System.currentTimeMillis();
        System.out.print("Matching "+ticker+", ");
        char firstChar=ticker.charAt(0);
        if(firstChar<'A' || firstChar>'Z') return false;

        int start=Start.companyIndex.get(firstChar);
        int stop;
        if(firstChar=='Z') stop=Start.allCompanies.size();
        else stop=Start.companyIndex.get((char)(firstChar+1));

        for(int i=start;i<stop;i++) if(Start.allCompanies.get(i).matches(ticker)){
            return true;
        }
        //System.out.println("Searching time "+(System.currentTimeMillis()-start));
        return false;
    }

    /*private void printTen(int index) {
        for(int i=0;i<10;i++){
            System.out.print(content[i+index]);
        }
    }*/
}