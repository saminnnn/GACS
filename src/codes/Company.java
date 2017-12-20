package codes;

/**
 * Created by samin on 12/29/16.
 * 0=ngeps                      4=Quarterly revenue guidance
 * 1=geps                       5=Comp Sales
 * 2=rev & sales                6=
 * 3=Quarterly EPS guidance
 */
public class Company {
    String name;
    public String[] inputs;
    public String year;
    char quarter;

    public Company(String ticker){
        quarter='0';
        name=ticker;
        inputs=new String[7];

        for(int i=5;i>=0;i--){
            inputs[i]=null;
        }
    }

    /*public void setInput(String type, String value){
        if(type.matches("eps")) inputs[0]=value;
        else  if(type.matches("revenue")) inputs[2]=value;
    }*/

}
