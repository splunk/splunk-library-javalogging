package com.dtdsoftware.splunk.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Common base class for all Splunk Input types.
 * Currently just has shared logic for queuing up events.
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 *
 */
public abstract class SplunkInput {

	//data size multipliers
	private static final int KB = 1024;
	private static final int MB = KB*1024;
	private static final int GB = MB*1024;
	
	//default to 500K
	private long maxQueueSize = 500*KB; 
	//default. If true,queue will get emptied when it fills up to accommodate new data.
	private boolean dropEventsOnQueueFull = false;
	
	//Using this collection structure to implement the FIFO queue
	private List <String>queue = new ArrayList<String>();
	
	private long currentQueueSizeInBytes = 0;
	
	/**
	 * Add an event to the tail of the FIFO queue subject to there being capacity
	 * @param event
	 */
	protected void enqueue(String event){
		
		long eventSize = event.getBytes().length;
		
		if(queueHasCapacity(eventSize)){
		  queue.add(event);
		  currentQueueSizeInBytes += eventSize;
		}
		else if(dropEventsOnQueueFull){
			queue.clear();
			queue.add(event);
			currentQueueSizeInBytes = eventSize;
			
		}
		else{
			//bummer , queue is full up
			
		}
	}
	
	/**
	 * True if the queue has capacity for adding an event of the given size
	 * @param eventSize
	 * @return
	 */
	private boolean queueHasCapacity(long eventSize) {
		
		return (currentQueueSizeInBytes+eventSize) <= maxQueueSize;
	}

	/**
	 * True if there are pending events in the queue
	 * @return
	 */
	protected boolean queueContainsEvents(){
		return !queue.isEmpty();
	}
	
	/**
	 * Remove an event from the head of the FIFO queue or null if there are no items in the queue
	 * @return
	 */
	protected String dequeue(){
		
		if(queueContainsEvents()){
		  String event = queue.remove(0);
		  currentQueueSizeInBytes -= event.getBytes().length;
		  if(currentQueueSizeInBytes < 0){
			  currentQueueSizeInBytes = 0;
		  }
		  return event;
		}
		return null;
	}
	
	/**
	 * Set the queue size from the configured property String value.
	 * If parsing fails , the default of 500KB will be used.
	 * 
	 * @param rawProperty in format [<integer>|<integer>[KB|MB|GB]]
	 */
    public void setMaxQueueSize(String rawProperty) {
		
		int multiplier;
		int factor;
		
		if(rawProperty.startsWith("KB")){
			multiplier = KB;
		}
		else if(rawProperty.startsWith("MB")){
			multiplier = MB;
		}
		else if(rawProperty.startsWith("GB")){
			multiplier = GB;
		}
		else{
			return;
		}
		try {
			factor = Integer.parseInt(rawProperty.substring(2));
		} catch (NumberFormatException e) {
			return;
		}
		setMaxQueueSize(factor*multiplier);
		
	}
    
	public long getMaxQueueSize() {
		return maxQueueSize;
	}
	/**
	 * Max queue size in bytes
	 * @param maxQueueSize
	 */
	public void setMaxQueueSize(long maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}
	public boolean isDropEventsOnQueueFull() {
		return dropEventsOnQueueFull;
	}
	/**
	 * If true,queue will get emptied when it fills up to accommodate new data.
	 * @param dropEventsOnQueueFull
	 */
	public void setDropEventsOnQueueFull(boolean dropEventsOnQueueFull) {
		this.dropEventsOnQueueFull = dropEventsOnQueueFull;
	}
		
	
}
