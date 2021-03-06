package keikai.demo.zk;

import org.zkoss.zk.ui.*;

import java.util.function.Consumer;

/**
 * In a thread without ZK execution available, to update ZK components requires activating a desktop first and deactivating after accessing ZK Components. (Please refer to <a href="https://www.zkoss.org/wiki/ZK_Developer%27s_Guide/Advanced_ZK/Long_Operations/Alternative_1:_Server_Push">ZK Developer's_Guide/Advanced_ZK/Long_Operations/Alternative_1:_Server_Push</a>)
 * This class helps you activate and deactivate a desktop, so you just need to pass a function that implements your rendering logic that calls ZK components' API.
 * 
 */
public class AsyncRender {

	/**
	 * return a Consumer that executes UI update logic between activation / de-activation of a desktop. (Template Method pattern)
	 * it should be called in an ZK event listener.
	 * @param update a consumer function that performs UI update logic
	 * @return a Consumer that executes UI update logic between activation / de-activation of a desktop
	 */
	static public <T> Consumer<T> getUpdateConsumer(Consumer<T> update){
		Desktop desktop = getCurrentDesktop();
		return (T arg) -> {
			try {
				Executions.activate(desktop);
				update.accept(arg);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				Executions.deactivate(desktop);
			}
		};
	}

	private static Desktop getCurrentDesktop() {
		if (Executions.getCurrent() == null){
			throw new RuntimeException("No ZK execution found. You should call the method in an event listener.");
		}else{
			return Executions.getCurrent().getDesktop();
		}
	}

	/**
	 * return a Runnable that executes UI update logic between activation / de-activation of a desktop. (Template Method pattern)
	 * it should be called in an ZK event listener since it gets a Desktop from the current Execution.
	 * @param update a function that implements the rendering logic to access ZK components
	 * @return a Runnable that executes UI update logic between activation / de-activation of a desktop
	 */
	static public Runnable getUpdateRunner(Runnable update){
		return getUpdateRunner(getCurrentDesktop(), update);
	}

	/**
	 * return a Runnable that executes UI update logic between activation / de-activation of a desktop. (Template Method pattern)
	 * @param desktop ZK Desktop
	 * @param update a function that implements the rendering logic to access ZK components
	 * @return a Runnable that executes UI update logic between activation / de-activation of a desktop
	 */
	static public Runnable getUpdateRunner(Desktop desktop, Runnable update){
		return () -> {
			try {
				if(desktop.isAlive()){
					Executions.activate(desktop);
					update.run();
				}else{
					return;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				Executions.deactivate(desktop);
			}
		};
	}

	/**
	 * Executes UI update logic between activation / de-activation of a desktop. (Template Method pattern)
	 * @param desktop
	 * @param update
	 */
	static public void runUpdate(Desktop desktop, Runnable update){
		try {
			if(desktop.isAlive()){
				Executions.activate(desktop);
				update.run();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Executions.deactivate(desktop);
		}
	}
}
