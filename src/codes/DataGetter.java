package codes;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;

/*import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;*/

/**
 *
 * @author sakib_farhad
 */
public class DataGetter {
    int updateCount = 0;
    private String serviceName = "//blp/mktnews-content";
    //private String fundamentalFeed = "/fundamentals/eid/37983";
    private String americanTextual="/news/eid/54497";
    private String macroTextual="/news/eid/54500";
    private String americanTextualHeadlines="/news/eid/56897";
    private String macroTextualHeadline="/news/eid/56900";
    private String socialMedia="/news/eid/59991";
    private String twitter="/news/eid/61157";
    private String option = "?format=json";
    private String host =
    private int port = 
    
    SessionOptions sessionOptions;
    CorrelationID fundamentalFeedID;
    CorrelationID atID;
    CorrelationID mtID;
    CorrelationID athID;
    CorrelationID mthID;
    CorrelationID socialMediaID;
    CorrelationID twitterID;
    SubscriptionList subscriptionList;
    Session session;
    PrintWriter oPrintWriter;
    Request request;
    Service refService;
    Identity identity;
    ProcessData process;
    //PrintWriter pw;
    //MqttClient client;
    //PrintWriter logWriter;
    PrintWriter twitlog;
    
    public DataGetter(ProcessData p) throws IOException,InterruptedException{
    	//client=new MqttClient("tcp://127.0.0.1","sum");
    	//client.connect();
    	//logWriter=new PrintWriter(new File("datalog.txt"));
    	twitlog=new PrintWriter(new File("twitlog.txt"));
    	this.process=p;
        
    	onStartUP();
        onDataDelivary();
        onCancellation();
        
    }    
    
    public void onStartUP() throws IOException, InterruptedException{
        
        sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(host);
        sessionOptions.setServerPort(port);
        sessionOptions.setAuthenticationOptions("AuthenticationMode=APPLICATION_ONLY;ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=gacs:gacsfundamentalfeed;");
        
        session = new Session(sessionOptions);
        if(!session.start()){
            System.out.println("Could not Start Session!!");
            System.exit(1);
        }
        
        System.out.println("Session Start Successfull");
        
        fundamentalFeedID = new CorrelationID(200);
        atID = new CorrelationID(201);
        mtID = new CorrelationID(202);
        athID = new CorrelationID(203);
        mthID = new CorrelationID(204);
        socialMediaID=new CorrelationID(205);
        twitterID=new CorrelationID(206);
        subscriptionList = new SubscriptionList();
        
        
        if(!session.openService("//blp/apiauth")){
            System.out.print("Authorization service failed");
            return;
        }
        
        identity=session.createIdentity();
        authorize(session);
        
        
        if(!session.openService(serviceName)){
            System.out.println("Could not open Service" + serviceName);
        }
        
        System.out.println("Opening Service Successfull");
        
        subscriptionList.add(new Subscription(serviceName+americanTextual+option, atID));
        subscriptionList.add(new Subscription(serviceName+macroTextual+option, mtID));
        subscriptionList.add(new Subscription(serviceName+americanTextualHeadlines+option, athID));
        subscriptionList.add(new Subscription(serviceName+macroTextualHeadline+option, mthID));
        subscriptionList.add(new Subscription(serviceName+socialMedia+option, socialMediaID));
        subscriptionList.add(new Subscription(serviceName+twitter+option, twitterID));
        session.subscribe(subscriptionList, identity); 
        System.out.println("6 Subscription started");
        
    }
    
    public void onDataDelivary() throws InterruptedException, IOException{
        while (true) {
            Event event = session.nextEvent();
            if(event.eventType().intValue()==Event.EventType.Constants.SUBSCRIPTION_DATA){
               updateCount++;
               	handleDataEvent(event, updateCount);
            }
        }
    }
    
    
    public void onCancellation() throws IOException{
        session.unsubscribe(subscriptionList);
        System.out.println("Subscription Cancelled");
    }
    
    
    private void handleDataEvent(Event event, int updateCount) throws IOException{
    	MessageIterator messageIterator = event.messageIterator();
        
        while(messageIterator.hasNext()){
            Message message = messageIterator.next();
            
            if(message.correlationID().equals(mtID)) sendData(message.toString(),"mt");
            else if(message.correlationID().equals(atID)) sendData(message.toString(),"at");
            else if(message.correlationID().equals(athID)) sendData(message.toString(),"ath");
            else if(message.correlationID().equals(mthID)) sendData(message.toString(),"mth");
            else if(message.correlationID().equals(twitterID)){
            	new TwittProcess(message.toString(),this.process,twitlog);
            }
            else if(message.correlationID().equals(socialMediaID)){
            	new TwittProcess(message.toString(),this.process,twitlog);
            }
        }
    }
    
    private void sendData(String message, String source) throws IOException{
    	//new ProcessInput(message,this.process,logWriter,source);
    	//MqttMessage msg = new MqttMessage(message.getBytes());
    	//client.publish("gecs/blpData/", msg);
    	new ProcessInput(message,this.process,source);
    }

    private void authorize(Session session) throws InterruptedException, IOException {
        String authPath="//blp/apiauth";
        if(!session.openService(authPath)){
            System.out.print("Could not open auth service");
        }
        
        Service authService=session.getService(authPath);
        EventQueue tokenEventQueue=new EventQueue();
        session.generateToken(new CorrelationID(tokenEventQueue),tokenEventQueue);
        String token=null;
        int WAIT_TIME_MS=10*1000;
        Event event=tokenEventQueue.nextEvent(WAIT_TIME_MS);
        Name TOKEN_ELEMENT=Name.getName("token");
        
        if (event.eventType() == Event.EventType.TOKEN_STATUS
                || event.eventType() == Event.EventType.REQUEST_STATUS) {
            for (Message msg: event) {
                System.out.println(msg);
                if (msg.messageType() == Name.getName("TokenGenerationSuccess")) {
                    token = msg.getElementAsString(TOKEN_ELEMENT);
                }
            }
        }
        if (token == null) {
            System.out.println("Failed to get token");
        }
        
        Request authRequest=authService.createAuthorizationRequest();
        authRequest.set(TOKEN_ELEMENT, token);
        session.sendAuthorizationRequest(authRequest, identity, null);
        
        long waitDuration = WAIT_TIME_MS;
        for (long startTime = System.currentTimeMillis();
                waitDuration > 0;
                waitDuration -= (System.currentTimeMillis() - startTime)) {
            event = session.nextEvent(waitDuration);
            // Since no other requests were sent using the session queue, the response can
            // only be for the Authorization request
            if (event.eventType() != Event.EventType.RESPONSE
                    && event.eventType() != Event.EventType.PARTIAL_RESPONSE
                    && event.eventType() != Event.EventType.REQUEST_STATUS) {
                continue;
            }

            for (Message msg: event) {
                System.out.println(msg);
                if (msg.messageType() != Name.getName("AuthorizationSuccess")) {
                    System.out.println("Message says Authorization Failed");
                }
                else{
                    System.out.println("IT HAPPENED");
                }
            }
            return;
        }
        System.out.println("Authorization Failed Totally");
        
    }
}
