package codes;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by samin on 2/2/17.
 */
public class OtherData implements MqttCallback, Runnable{
    ProcessData processData;
    MqttClient client;

    public OtherData(ProcessData p) throws Exception {
        processData=p;
        client=new MqttClient("tcp://127.0.0.1","samin");
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String message=mqttMessage.toString();
        String ticker="";
        String value="";
        char quarter;
        int i, messageLength=message.length();

        for(i=0;i<messageLength;i++){
            char c=message.charAt(i);
            if(c==' ') break;
            else ticker+=c;
        }

        i+=2;
        quarter=message.charAt(i);
        i++;

        for(;i<messageLength;i++) value+=message.charAt(i);

        if(s.contains("eps"))processData.addData(ticker,3,value,quarter);
        else if(s.contains("revenue")) processData.addData(ticker,4,value,quarter);
        else if(s.contains("compSales")){
            if(processData.needCompSales(ticker,quarter)) processData.addData(ticker,5,value,quarter);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

	@Override
	public void run() {
		try {
			client.connect();
			client.setCallback(this);
	        String[] topics={"/gacs/headline/eps/","/gacs/headline/revenue/","/gacs/body/eps/","/gacs/body/revenue/","/gacs/headline/compSales/"};
	        client.subscribe(topics);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
