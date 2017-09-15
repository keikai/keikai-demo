package keikai.demo;

import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.impl.StaticIdGenerator;
import org.zkoss.zk.ui.metainfo.*;
import org.zkoss.zk.ui.sys.IdGenerator;

/**
 * Use component's ID first if exists. If not, then use static ID generator.
 * @author hawk
 *
 */
public class ComponentIDFirstGenerator implements IdGenerator {

	static StaticIdGenerator staticIdGenerator = new StaticIdGenerator();
	@Override
	public String nextComponentUuid(Desktop desktop, Component comp,
			ComponentInfo compInfo) {
		String id = "";
		if (compInfo != null && compInfo.getProperties() != null){
			for (Property p :compInfo.getProperties()){
				if ("id".equals(p.getName())){
					id = p.getValue(comp).toString();
				}
			}
		}
		if (id.isEmpty()){
			return staticIdGenerator.nextComponentUuid(desktop, comp, compInfo);
		}else{
			return id;
		}
	}

	@Override
	public String nextPageUuid(Page page) {
		return staticIdGenerator.nextPageUuid(page);
	}

	@Override
	public String nextDesktopId(Desktop desktop) {
		return staticIdGenerator.nextDesktopId(desktop);
	}

}
