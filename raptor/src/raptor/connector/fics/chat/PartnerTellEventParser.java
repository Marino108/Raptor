package raptor.connector.fics.chat;

import raptor.chat.ChatEvent;
import raptor.connector.fics.FicsUtils;
import raptor.util.RaptorStringTokenizer;

public class PartnerTellEventParser extends ChatEventParser {
	private static final String PTELL_IDENTIFIER = "(your partner) tells you: ";

	public PartnerTellEventParser() {
	}

	/**
	 * Returns null if text does not match the event this class produces.
	 */
	@Override
	public ChatEvent parse(String text) {
		if (text.length() < 1500) {
			int i = text.indexOf(PTELL_IDENTIFIER);
			if (i != -1) {
				RaptorStringTokenizer stringtokenizer = new RaptorStringTokenizer(
						text, " ");
				String s1 = stringtokenizer.nextToken();
				if (stringtokenizer.nextToken().equals("(your"))
					return new ChatEvent(FicsUtils.removeTitles(s1),
							PARTNER_TELL, text);
			}
			return null;
		}
		return null;
	}
}