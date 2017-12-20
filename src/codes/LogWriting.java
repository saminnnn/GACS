package codes;

import java.io.PrintWriter;

public class LogWriting implements Runnable{
	PrintWriter pw;
	String log;
	
	public LogWriting(PrintWriter p, String s){
		pw=p;
		log=s;
	}

	@Override
	public void run() {
		pw.println(log);
		pw.flush();
		pw.close();
	}

}
