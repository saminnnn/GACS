package codes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by samin on 3/6/17.
 */
public class TwittProcess {
    SearchString searchString;
    char[] content;
    String ticker="";
    ProcessData processData;
    PrintWriter pw;
	char quarter='0';

    public TwittProcess(String data, ProcessData p, PrintWriter twitlog) throws IOException{
        searchString=new SearchString(data);
        content=searchString.passage;
        processData=p;
        pw=twitlog;

        int index=searchString.search("StockTwits");
        if(index!=-1){
            int tickerIndex=searchString.search("\"ScoredEntity\"");
            if(tickerIndex!=-1){
                tickerIndex=searchString.search("id",tickerIndex+1);
                for(int i=0;content[i+7+tickerIndex]!='"';i++) ticker+=content[i+tickerIndex+7];
                if(matchCompany(ticker)){
                    int headlineIndex=searchString.search("\"Headline\"");
                    if(headlineIndex!=-1) process(headlineIndex+14);
                }
            }
        }
    }

    private void process(int start) throws IOException {
    	int sentenceStart=start;
        System.out.println("");
        int i;

        for(int ii=start;content[ii]!='"';ii++){
            if(isNumber(content[ii])){
                if(content[ii-1]=='Q' && content[ii+1]==' ') quarter=content[ii];
                else if(content[ii-1]==' ' && content[ii+1]=='Q') quarter=content[ii];
            }
        }
        System.out.println("Quarter "+quarter);
        
        for(i=start;;i++){
        	if(content[i]=='"') break;
        	switch(content[i]){
        	case ';': analyze(sentenceStart,i);
        		sentenceStart=i+1;
        		break;
        	case ':': analyze(sentenceStart,i);
    			sentenceStart=i+1;
    			break;
        	case '-': if(!isNumber(content[i+1])){
        			analyze(sentenceStart,i);
        			sentenceStart=i+1;
        		}
				break;
        	case ',': if(!isNumber(content[i+1])){
	    			analyze(sentenceStart,i);
	    			sentenceStart=i+1;
	    		}
				break;
        	case '.':if(!isNumber(content[i+1])) if(dotProcess(i)){
	        		analyze(sentenceStart,i);
	    			sentenceStart=i+1;
        		}
				break;
      
        	}            
        }
        analyze(sentenceStart,i);
    }

    private boolean dotProcess(int i) {
    	String s=""+content[i-3]+content[i-2]+content[i-1];
    	if(!s.matches("EVS") && !s.matches("REV")) return true;
    	return false;
	}

	private void analyze(int sentanceStart, int sentanceEnd) throws IOException {
		String word="";
		boolean loss=false,share=false;
		String value=null, year=null;
		int type=0;
		Double geps=0.0;
		pw.print(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
		
		for(int i=sentanceStart;;i++){
			pw.print(content[i]);
			pw.flush();
			if(content[i]==' ' || i>=sentanceEnd){
				if(word.length()>1){
					if(word.contains("LOSS")) loss=true;
	                if(word.contains("SHARE") || word.contains("SHR")) share=true;

	                char c=word.charAt(0);
	                switch (c){
	                    case '$': if(value==null) if(isNumber(word.charAt(1))) value=word.substring(1,word.length());
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
                            if(value==null && word.charAt(1)!='T') value=word;
                        }
                    }
				}				
				
				word="";
				if(i>=sentanceEnd) break;
			}
			else word+=content[i];
		}
		if(loss && share) type=1;
		
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
            value=extractValue(value);
            System.out.println("The value now is "+value);
            if(processData.needRev(ticker, quarter)) {
                Double revenueValue=0.0;
                try{
                    revenueValue = Double.parseDouble(value.substring(0, value.length() - 1));
                    if (value.charAt(value.length() - 1) == 'B') revenueValue = revenueValue * 1000000000;
                    else if (value.charAt(value.length() - 1) == 'M') revenueValue = revenueValue * 1000000;

                    if(revenueValue>100){
                        if(quarter!='0') processData.addData(ticker, 2, String.format("%.0f", revenueValue),quarter);
                    }
                }
                catch(NumberFormatException e){
                    System.out.println("Number Format Exception");
                }
            }
        }
		System.out.println("Type "+type+" Value: "+value);
		pw.println("");
		pw.flush();
		
	}

    private String extractValue(String value) {
        int i;
        int length=value.length();
        for(i=0;i<length;i++){
            char c=value.charAt(i);
            if(c=='.') continue;
            if(!isNumber(c)) break;
        }
        if(i==length) return value;
        else return value.substring(0, i + 1);
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
	
	private boolean isNumber(char c){
		if(c>='0' && c<='9') return true;
		return false;
	}
}
