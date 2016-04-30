package raptor.action.chat;

import raptor.Raptor;
import raptor.action.AbstractRaptorAction;
import raptor.chat.ChatEvent;
import raptor.chat.ChatType;
import raptor.connector.Connector;
import raptor.international.L10n;
import raptor.pref.PreferenceKeys;
import raptor.service.ConnectorService;

public class ZoomInAction extends AbstractRaptorAction {			
	protected static L10n local = L10n.getInstance();
	
	public ZoomInAction() {
		setName(local.getString("zoomin.name"));
		setDescription(local.getString("zoomin.description"));
		setCategory(Category.ConsoleCommands);
	}

	public void run() {
		double currentValue = Raptor.getInstance().getPreferences().getDouble(PreferenceKeys.APP_ZOOM_FACTOR);
		double newValue = 1.0;

		if (currentValue == .25) {
			newValue = .5;
		} else if (currentValue == .5) {
			newValue = .75;
		} else if (currentValue == .75) {
			newValue = 1.0;
		} else if (currentValue == 1.0) {
			newValue = 1.25;
		} else if (currentValue == 1.25) {
			newValue = 1.5;
		} else if (currentValue == 1.50) {
			newValue = 1.75;
		} else if (currentValue == 1.75) {
			newValue = 2.0;
		} else if (currentValue == 2.0) {
			newValue = 2.25;
		} else if (currentValue == 2.25) {
			newValue = 2.5;
		} else if (currentValue == 2.5) {
			newValue = 2.75;
		} else if (currentValue == 2.75) {
			newValue = 3.0;
		} else if (currentValue == 3.0) {
			newValue = 3.25;
		}

		Connector fics = ConnectorService.getInstance().getConnectors()[0];
		
		Raptor.getInstance().getPreferences().setValue(PreferenceKeys.APP_ZOOM_FACTOR, newValue);
		fics.publishEvent(new ChatEvent("", ChatType.INTERNAL, local.getString("rapWinZoomFactorSet")  + " " + newValue * 100 + "%"));		
	}
}