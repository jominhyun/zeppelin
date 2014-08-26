package com.nflabs.zeppelin.server;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.repl.Repl;
import com.nflabs.zeppelin.repl.ReplResult;
import com.nflabs.zeppelin.scheduler.Job;
import com.nflabs.zeppelin.scheduler.JobListener;
import com.nflabs.zeppelin.server.form.Form;

/**
 * execution unit 
 */
public class Paragraph extends Job implements Serializable{
	String paragraph;
	private transient NoteReplLoader replLoader;
	public final Form form;
	
	public Paragraph(JobListener listener, NoteReplLoader replLoader){
		super(generateId(), listener);
		this.replLoader = replLoader;
		paragraph = null;
		form = new Form();
	}
	
	private static String generateId(){
		return "paragraph_"+System.currentTimeMillis()+"_"+new Random(System.currentTimeMillis()).nextInt();
	}
	
	public String getParagraph() {
		return paragraph;
	}

	public void setParagraph(String paragraph) {
		this.paragraph = paragraph;
	}

	public String getRequiredReplName(){
		if(paragraph==null) return null;

		// get script head
		int scriptHeadIndex = 0;
		for(int i=0; i < paragraph.length(); i++){
			char ch = paragraph.charAt(i);
			if(ch==' ' || ch == '\n'){
				scriptHeadIndex = i;
				break;
			}
		}
		if(scriptHeadIndex==0) return null;
		String head = paragraph.substring(0, scriptHeadIndex);
		if(head.startsWith("%")){
			return head.substring(1);
		} else {
			return null;
		}
	}
	
	
	private String getScriptBody(){
		if(paragraph==null) return null;
		
		String magic = getRequiredReplName();
		if(magic==null) return paragraph;
		if(magic.length()+2>=paragraph.length()) return "";
		return paragraph.substring(magic.length()+2);
	}
	
	public NoteReplLoader getNoteReplLoader(){
		return replLoader;
	}
	
	public Repl getRepl(String name, Properties properties) {
		return replLoader.getRepl(name, properties);
	}
	
	public void setNoteReplLoader(NoteReplLoader repls) {
		this.replLoader = repls;
	}
	
	public ReplResult getResult() {
		return (ReplResult) getReturn();
	}
	
	
	@Override
	public int progress() {
		return 0;
	}

	@Override
	public Map<String, Object> info() {
		return null;
	}

	@Override
	protected Object jobRun() throws Throwable {
		String replName = getRequiredReplName();
		Repl repl = getRepl(replName, new Properties());
		logger().info("run paragraph {} using {} "+repl, getId(), replName);
		if(repl==null) {	
			logger().error("Can not find interpreter name "+repl);
			throw new RuntimeException("Can not find interpreter for "+getRequiredReplName());
		}
		// inject form
		repl.bindValue("form", form);
		logger().info("RUN : "+getScriptBody());
		ReplResult ret = repl.interpret(getScriptBody());
		return ret;
	}

	@Override
	protected boolean jobAbort() {
		Repl repl = getRepl(getRequiredReplName(), new Properties());
		repl.cancel();
		return true;
	}
	
	private Logger logger(){
		Logger logger = LoggerFactory.getLogger(Paragraph.class);
		return logger;
	}
	
}
