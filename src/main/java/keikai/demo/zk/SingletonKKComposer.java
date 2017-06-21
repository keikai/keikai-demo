/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		4:50 PM 16/05/2017, Created by jumperchen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.keikai.client.api.Borders;
import com.keikai.client.api.Keikai;
import com.keikai.client.api.Range;
import com.keikai.client.api.Spreadsheet;
import com.keikai.client.api.event.Events;
import com.keikai.client.api.event.RangeEvent;
import kk.socket.thread.EventThread;
import org.zkoss.zk.device.Device;
import org.zkoss.zk.device.DeviceConfig;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Selectbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

/**
 * @author jumperchen
 */
public class SingletonKKComposer extends SelectorComposer<Vlayout> {
	int counts;
	static Spreadsheet spreadsheet;
	String selectedRange;

	@Wire Selectbox borderIndex;
	@Wire Selectbox borderLineStyle;
	@Wire Selectbox borderWeight;

	public void doAfterCompose(Vlayout comp) throws Exception {
		super.doAfterCompose(comp);
		comp.getDesktop().enableServerPush(true);
		if (spreadsheet == null) {
			spreadsheet = Keikai.newClient("http://localhost:8888/");
		}
		String uri = spreadsheet.getURI(comp.getFellow("mywin").getFellow("myss").getUuid());
		System.out.println(uri);

		final Desktop desktop = getSelf().getDesktop();
		ExceptionableFunction handler = (evt) -> {
			RangeEvent event = (RangeEvent) evt;
			selectedRange = event.getRange().getA1Notation();
			// get value first.
			event.getRange().loadValue().thenApply(activate()).thenAccept((rangeValue) -> {
				// ignore validation on null value
				((Textbox) comp.getFellow("cellValue")).setRawValue(rangeValue.getValue());
			}).whenComplete(deactivate());


			try {
				Executions.activate(desktop);
				((Label)comp.getFellow("msg")).setValue(event.toString());
				String a1 = event.getRange().getA1Notation();
				String[] refs = a1.split(":");
				a1 = refs.length > 1 ? refs[0].equals(refs[1]) ? refs[0] : a1 : a1;

				Label label = ((Label)comp.getFellow("cell"));

				if (!label.getValue().equals(a1)) {
					label.setValue(a1);

				}
			} finally {
				Executions.deactivate(desktop);
			}
			return null;
		};

		spreadsheet.addEventListener(Events.ON_UPDATE_SELECTION, handler::apply);
		spreadsheet.addEventListener(Events.ON_MOVE_FOCUS, handler::apply);

		spreadsheet.ready(() -> {
			long t1 = System.nanoTime();
			System.out.println("Start: " + Thread.currentThread());
			for (int k = 0; k < 1; k++) {
				long t2 = System.nanoTime();
				int total = 0;
				for (int row = 0; row < 30; row++) {
					for (int col = 0; col < 10; col++) {
						total++;
						spreadsheet.getRange(row, col).applyValue(counts++);
					}
				}
			}
			EventThread.nextTick(() -> {
				System.out.println("Sending time: " + TimeUnit.MILLISECONDS
						.convert(System.nanoTime() - t1, TimeUnit.NANOSECONDS) + "ms");
				System.out.println("Sent done.");
			});
			System.out.println("End: " + Thread.currentThread());
		});
		//		}
//		Executions.getCurrent().setAttribute("abc", uri);
//		Script script = ((Script)comp.getFellow("script"));
//		script.setSrc(uri);

		if (comp.getDesktop().getDevice().getEmbedded() != null) {
			Device device = comp.getDesktop().getDevice();
			device.init(device.getType(), new DeviceConfig() {
				public String getUnavailableMessage() {
					return device.getUnavailableMessage();
				}

				public Class getServerPushClass() {
					return device.getServerPushClass();
				}

				public String getEmbedded() {
					return ""; // reset;
				}
			});

		}

		comp.getDesktop().getDevice().addEmbedded("  <script defer type=\"application/dart\" src=\""+ uri +"\"></script>");
//		comp.getDesktop().getDevice().addEmbedded("  <script defer type=\"text/javascript\" src=\""+ uri +"\"></script>");

		initComponent();
	}

	static String[] borderIndexList = { "diagonalDown", "diagonalUp", "edgeBottom", "edgeLeft", "edgeRight", "edgeTop", "insideHorizontal", "insideVertical" };
	static String[] borderLineStyleList = { "continuous", "dash", "dashDot", "dashDotDot", "dot", "double", "none", "slantDashDot" };
	private void initComponent() {
		borderIndex.setModel(new ListModelArray<Object>(borderIndexList));
		borderLineStyle.setModel(new ListModelArray<Object>(borderLineStyleList));
		borderLineStyle.addEventListener("onSelect", (selectEvent) -> {
			if (borderIndex.getSelectedIndex() >= 0) {
				Range range = spreadsheet.getRange(selectedRange);
				Borders borders = range.createBorders(borderIndexList[borderIndex.getSelectedIndex()]);
				borders.setStyle((String) ((SelectEvent) selectEvent).getSelectedObjects().iterator().next());
				range.applyBorders(borders);
			} else {
				System.out.println("Please select border index first");
			}
		});
	}

	@Listen("onClick = button#btn")
	public void onClick(Event event) {
//		Clients.showNotification("test");
		spreadsheet.getRange(((Label)getSelf().getFellow("cell")).getValue()).applyValue(((Textbox)event.getTarget().getFellow("cellValue")).getValue());
	}
	@Listen("onChange = #focusTo")
	public void onChange(Event event) {
		spreadsheet.getRange(((Textbox)event.getTarget()).getText()).activate();
	}
	@Listen("onClick = #focusCell")
	public void focusCell(Event event) {
		spreadsheet.loadActiveCell().thenApply(activate()).thenAccept((range) -> {
				Clients.showNotification("Focus Cell: " + range.getA1Notation());
		}).whenComplete(deactivate());
	}
	@Listen("onCheck = #sheetVisible")
	public void sheetVisible(Event event) {
		Radio rg = ((Radio) event.getTarget());
		spreadsheet.setSheetVisible(rg.getLabel());
	}
	@Listen("onClick = #clearContents")
	public void clearContents(Event event) {
		spreadsheet.getRange(selectedRange).clearContents();
	}

	@Listen("onChange = #borderColor")
	public void changeBorderColor(Event evt){
		String color = ((Textbox)evt.getTarget()).getText();
		Range range = spreadsheet.getRange(selectedRange);
		Borders borders = range.createBorders(borderIndexList[borderIndex.getSelectedIndex()]);
		borders.setColor(color);
		range.applyBorders(borders);
	}
	private <T> ExceptionableFunction<T, T> activate() {
		Desktop desktop = getSelf().getDesktop();
		return  result -> {
			Executions.activate(desktop);
			return result;
		};
	}

	private <T> BiConsumer<T, ? super Throwable> deactivate() {
		Desktop desktop = getSelf().getDesktop();
		return  (v, ex) -> {
			if(ex != null) {
				ex.printStackTrace();
			}
			Executions.deactivate(desktop);
		};
	}

	public interface ExceptionableFunction<T, R> extends Function<T, R> {
		default R apply(T val) {
			try {
				return applyWithException(val);
			} catch (Exception e) {
				throw new RuntimeException("wrapped", e);
			}
		}
		R applyWithException(T val) throws Exception;
	}
}
