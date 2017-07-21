package keikai.demo.zk;

import java.util.function.*;

import org.zkoss.zk.ui.*;

/**
 * In an asynchronous thread, to update ZK components requires activating a desktop first and deactivating after the update.
 * This class help you activate and deactivate, so you just need to pass a function that implements your rendering logic.
 * Please refer to https://www.zkoss.org/wiki/ZK_Developer%27s_Guide/Advanced_ZK/Long_Operations/Alternative_1:_Server_Push
 * 
 */
public class AsyncRender {

	/**
	 * return a Consumer that executes UI update logic between activation / de-activation of a desktop. (Template Method pattern)
	 * @param update a consumer function thats perform UI update logic
	 * @return a Consumer that executes UI update logic between activation / de-activation of a desktop
	 */
	static public <T> Consumer<T> acceptUpdate(Consumer<T> update){
		Desktop desktop = Executions.getCurrent().getDesktop();
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
	
	static public Runnable runUpdate(Runnable update){
		Desktop desktop = Executions.getCurrent().getDesktop();
		return () -> {
			try {
				Executions.activate(desktop);
				update.run();;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				Executions.deactivate(desktop);
			}
		};
	}

}
