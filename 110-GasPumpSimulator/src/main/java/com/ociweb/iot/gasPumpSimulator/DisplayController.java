package com.ociweb.iot.gasPumpSimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.iot.grove.Grove_LCD_RGB;
import com.ociweb.iot.maker.CommandChannel;
import com.ociweb.iot.maker.DeviceRuntime;
import com.ociweb.iot.maker.PayloadReader;
import com.ociweb.iot.maker.PubSubListener;
import com.ociweb.iot.maker.StateChangeListener;
import com.ociweb.pronghorn.util.Appendables;

public class DisplayController implements PubSubListener, StateChangeListener<PumpState> {
	
	private final CommandChannel commandChannel;
	public PumpState activeState = PumpState.Idle;
	private final String topicTank;
	private final String topicPump;
	private final String topicTotal;
	private static final Logger loggger = LoggerFactory.getLogger(DisplayController.class);
	
	private long   lastTankTime;
	private int    lastTankDepth;
	private String lastTankFuel;
	
	private long   lastPumpTime;
	private String lastPumpFuel;
	private int    lastPumpPrice;
	private int    lastPumpUnits;
	
	private String lastTotalName;
	private int    lastTotalPrice;
	private int    lastTotalUnits;
	
	private String showingNow = "";
		
	public DisplayController(DeviceRuntime runtime, String topicTank, String topicPump, String topicTotal) {
		this.commandChannel = runtime.newCommandChannel();
		this.topicTank = topicTank;
		this.topicPump = topicPump;
		this.topicTotal = topicTotal;
		
	}

	@Override
	public void message(CharSequence topic, PayloadReader payload) {
		
		if (topic.equals(topicTank)) { //.toString().equals(topicTank.toString())) {
			
			lastTankTime = payload.readLong();
			lastTankDepth = payload.readInt();
			lastTankFuel = payload.readUTF();
	
		}

		if (topic.equals(topicPump)) {
			
			lastPumpTime = payload.readLong();
			lastPumpFuel = payload.readUTF();
			lastPumpPrice = payload.readInt();	
			lastPumpUnits = payload.readInt();
		
		}
		
		if (topic.equals(topicTotal)) {
			
			//now that we have the total the previous running values are no longer valid
			lastPumpTime = 0;				
			lastPumpUnits = 0;			
			
			long totalTime = payload.readLong();
			lastTotalName = payload.readUTF();
			int price = payload.readInt();
			lastTotalUnits = payload.readInt();
			
			lastTotalPrice = (price*lastTotalUnits)/100;
						
		}
		
		repaint();
	}

	@Override
	public void stateChange(PumpState oldState, PumpState newState) {
		activeState = newState;
		
		repaint();
	}
	
	private void repaint() {
		
		StringBuilder target = new StringBuilder();
		switch (activeState) {
			case Idle:
				buildTankDisplay(target);
				break;
			case Pump:
				buildPumpingDisplay(target);
				break;
			case Receipt:
				buildReceiptDisplay(target);
				break;
			default:
				loggger.error("error skipped unknown state :"+activeState);
		}
		
		String newText = target.toString();
		if (!newText.equals(showingNow)) {
		
			Grove_LCD_RGB.commandForColor(commandChannel, 255, 255, 200);
			Grove_LCD_RGB.commandForText(commandChannel, newText);
			showingNow = newText;
			
		}
	}
	

	private void buildReceiptDisplay(StringBuilder target) {

			if (null!=lastTotalName) {
				target.append(lastTotalName);
			}
			target.append(" total \n");
			
			Appendables.appendFixedDecimalDigits(target, lastTotalUnits/100, 1000);
			target.append('.');
			Appendables.appendFixedDecimalDigits(target, lastTotalUnits%100, 10);
			
			target.append(" ");

			target.append("$");			
			Appendables.appendFixedDecimalDigits(target, lastTotalPrice/100, 1000);
			target.append('.');
			Appendables.appendFixedDecimalDigits(target, lastTotalPrice%100, 10);
	}

	private void buildPumpingDisplay(StringBuilder target) {
			
			target.append(lastPumpFuel);
			target.append("\n");
			
			target.append("");
			Appendables.appendFixedDecimalDigits(target, lastPumpUnits/100, 100);
			target.append('.');
			Appendables.appendFixedDecimalDigits(target, lastPumpUnits%100, 10);
			
			int cents = (lastPumpUnits*lastPumpPrice)/100;
			target.append(" $");
			Appendables.appendFixedDecimalDigits(target, cents/100, 100);
			target.append('.');
			Appendables.appendFixedDecimalDigits(target, cents%100, 10);
		
	}
	
	
	private void buildTankDisplay(StringBuilder target) {

			if (null!=lastTankFuel) {
				target.append(lastTankFuel).append(" tank");
				target.append('\n');
				Appendables.appendFixedDecimalDigits(target, lastTankDepth, 1000);
				target.append(" cm2 volume");
			} else {
				target.append("No tank data\n");
			}

	}
	
	
}
