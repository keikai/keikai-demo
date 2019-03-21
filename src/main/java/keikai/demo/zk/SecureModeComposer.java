package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.*;
import keikai.demo.*;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.*;

import javax.servlet.http.*;
import java.util.ArrayList;

/**
 * @author Hawk
 */
public class SecureModeComposer extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;

	Spreadsheet spreadsheet;

	@Wire
	org.zkoss.zhtml.Script uiClientScript;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		initSpreadsheet();
	}

	private void initSpreadsheet() {
		Settings settings = Settings.DEFAULT_SECURE_SETTINGS.clone(); // use the secure settings
		// optional: use session id as token
		settings.set(Settings.Key.SECURE_TOKEN, ((HttpSession) Executions.getCurrent().getSession().getNativeSession()).getId());
		// optional: whether to check request origin header
		settings.set(Settings.Key.SECURE_ORIGIN, "*//localhost*");
		//connect with above settings
		spreadsheet = Keikai.newClient(KeikaiUtil.getKeikaiServerAddress(Executions.getCurrent()), settings);
		spreadsheet.setUIActivityCallback(new CloseSpreadsheetActivity(spreadsheet));
		//pass target element's id and get keikai script URI
		String scriptUri = spreadsheet.getSecureURI((HttpServletRequest)Executions.getCurrent().getNativeRequest(),
				(HttpServletResponse) Executions.getCurrent().getNativeResponse(),
				getSelf().getFellow("myss").getUuid());
		uiClientScript.setSrc(scriptUri);
	}
}



