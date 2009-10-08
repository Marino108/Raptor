package testcases;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;

import org.junit.Test;

import raptor.game.Game;
import raptor.game.Result;
import raptor.game.pgn.ListMaintainingPgnParserListener;
import raptor.game.pgn.Nag;
import raptor.game.pgn.PgnParseOutput;
import raptor.game.pgn.PgnParser;
import raptor.game.pgn.PgnParserError;
import raptor.game.pgn.PgnParserListener;
import raptor.game.pgn.SimplePgnParser;
import raptor.game.pgn.StreamingPgnParser;
import raptor.game.util.GameUtils;

public class TestPgnParsing {

	public static class LoggingPgnParserListener implements PgnParserListener {

		public void onAnnotation(PgnParser parser, String annotation) {
			System.out.println("onAnotation (" + parser.getLineNumber() + ") "
					+ annotation);
		}

		public void onGameEnd(PgnParser parser, Result result) {
			System.out.println("onGameEnd (" + parser.getLineNumber() + ") "
					+ result.name());
		}

		public void onGameStart(PgnParser parser) {
			System.out.println("onGameStart (" + parser.getLineNumber() + ") ");

		}

		public void onHeader(PgnParser parser, String headerName,
				String headerValue) {
			System.out.println("onHeader (" + parser.getLineNumber() + ") "
					+ headerName + " " + headerValue);
		}

		public void onMoveNag(PgnParser parser, Nag nag) {
			System.out.println("onMoveNag (" + parser.getLineNumber() + ") "
					+ nag.name());
		}

		public void onMoveNumber(PgnParser parser, int moveNumber) {
			System.out.println("onMoveNumber (" + parser.getLineNumber() + ") "
					+ moveNumber);
		}

		public void onMoveSublineEnd(PgnParser parser) {
			System.out.println("onMoveSublineEnd (" + parser.getLineNumber()
					+ ") ");
		}

		public void onMoveSublineStart(PgnParser parser) {
			System.out.println("onMoveSublineStart (" + parser.getLineNumber()
					+ ") ");
		}

		public void onMoveWord(PgnParser parser, String word) {
			System.out.println("onMoveWord (" + parser.getLineNumber() + ") "
					+ word);
		}

		public void onUnknown(PgnParser parser, String unknown) {
			System.out.println("onUnknown (" + parser.getLineNumber() + ") "
					+ unknown);

		}
	}

	public static class TimingPgnParserListener implements PgnParserListener {
		long lastStartTime = 0;

		public void onAnnotation(PgnParser parser, String annotation) {
		}

		public void onGameEnd(PgnParser parser, Result result) {
			System.out.println("onGameEnd duration="
					+ (System.currentTimeMillis() - lastStartTime));
		}

		public void onGameStart(PgnParser parser) {
			lastStartTime = System.currentTimeMillis();
		}

		public void onHeader(PgnParser parser, String headerName,
				String headerValue) {
		}

		public void onMoveNag(PgnParser parser, Nag nag) {
		}

		public void onMoveNumber(PgnParser parser, int moveNumber) {
		}

		public void onMoveSublineEnd(PgnParser parser) {
		}

		public void onMoveSublineStart(PgnParser parser) {
		}

		public void onMoveWord(PgnParser parser, String word) {
		}

		public void onUnknown(PgnParser parser, String unknown) {
		}
	}

	public static final String[] PGN_TEST_FILES = new String[] {// "error.pgn"};//,
	"nestedsublines.pgn" };// , "test2.pgn" };

	private String pgnFileAsString(String fileName) throws Exception {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(
				"resources/test/" + fileName));

		String currentLine = reader.readLine();
		while (currentLine != null) {
			builder.append(currentLine + "\n");
			currentLine = reader.readLine();
		}

		return builder.toString();
	}

	@Test
	public void speedTest() throws Exception {
		String pgn = pgnFileAsString("error.pgn");
		SimplePgnParser parser = new SimplePgnParser(pgn);
		ListMaintainingPgnParserListener listener = new ListMaintainingPgnParserListener();
		parser.addPgnParserListener(new LoggingPgnParserListener());
		parser.addPgnParserListener(listener);

		// long time = System.currentTimeMillis();
		parser.parse();

	}

	@Test
	public void testLargeFile() throws Exception {
		StreamingPgnParser parser = new StreamingPgnParser(new FileReader(
				"resources/test/Alekhine4Pawns.pgn"), Integer.MAX_VALUE);
		ListMaintainingPgnParserListener listener = new ListMaintainingPgnParserListener() {

			@Override
			public void gameParsed(Game game, int rowNum) {
				// Don't collect games because this causes memory problems when
				// a larget number are collected.
			}

		};
		parser.addPgnParserListener(new TimingPgnParserListener());
		parser.addPgnParserListener(listener);
		parser.parse();
	}

	@Test
	public void testTestFiiles() throws Exception {
		for (String element : PGN_TEST_FILES) {
			String pgn = pgnFileAsString(element);
			SimplePgnParser parser = new SimplePgnParser(pgn);
			ListMaintainingPgnParserListener listener = new ListMaintainingPgnParserListener();

			// parser.addPgnParserListener(new LoggingPgnParserListener());
			parser.addPgnParserListener(new TimingPgnParserListener());
			parser.addPgnParserListener(listener);

			// long time = System.currentTimeMillis();
			parser.parse();

			System.out.println("Errors:");
			for (PgnParserError error : listener.getErrors()) {
				System.out.println(error);
			}
			System.out.println("\n\nGames:");
			for (Game game : listener.getGames()) {
				System.out.println(game);
				System.out.println(GameUtils.toPgn(game));
			}

			PgnParseOutput output = new PgnParseOutput(listener.getGames(),
					listener.getErrors());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("test.object", false));
			objectOutputStream.writeObject(output);
			objectOutputStream.flush();
			objectOutputStream.close();
		}
	}

}