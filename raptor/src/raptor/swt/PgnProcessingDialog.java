/**
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright 2009-2016 RaptorProject (https://github.com/Raptor-Fics-Interface/Raptor)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.swt;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import raptor.Raptor;
import raptor.chess.Game;
import raptor.chess.pgn.LenientPgnParserListener;
import raptor.chess.pgn.PgnHeader;
import raptor.chess.pgn.PgnParserError;
import raptor.chess.pgn.PgnUtils;
import raptor.chess.pgn.StreamingPgnParser;
import raptor.international.L10n;
import raptor.service.ThreadService;
import raptor.swt.chess.PgnParseResultsRow;
import raptor.swt.chess.PgnParseResultsWindowItem;
import raptor.util.RaptorLogger;
import raptor.util.RaptorRunnable;

public class PgnProcessingDialog extends Dialog {
	public class ProfressPgnParserListener extends LenientPgnParserListener {
		private ArrayList<PgnParserError> errors = new ArrayList<PgnParserError>();

		private ArrayList<PgnParseResultsRow> rows = new ArrayList<PgnParseResultsRow>();

		public ProfressPgnParserListener() {
			super();
		}

		@Override
		public void errorEncountered(PgnParserError error) {
			if (isClosed) {
				throw new RuntimeException("Closed");
			} else {

				errors.add(error);
			}
		}

		@Override
		public boolean gameParsed(Game game, final int lineNumber) {
			if (isClosed) {
				throw new RuntimeException("Closed");
			} else {
				PgnParseResultsRow row = new PgnParseResultsRow();
				row.setVariant(StringUtils.defaultString(game.getHeader(PgnHeader.Variant), "?"));
				row.setDate(StringUtils.defaultString(game.getHeader(PgnHeader.Date), "?"));
				row.setEvent(StringUtils.defaultString(game.getHeader(PgnHeader.Event), "?"));
				row.setWhite(StringUtils.defaultString(game.getHeader(PgnHeader.White), "?"));
				row.setWhiteElo(StringUtils.defaultString(game.getHeader(PgnHeader.WhiteElo), "?"));
				row.setBlack(StringUtils.defaultString(game.getHeader(PgnHeader.Black), "?"));
				row.setBlackElo(StringUtils.defaultString(game.getHeader(PgnHeader.BlackElo), "?"));
				row.setResultDescription(StringUtils.defaultString(game.getHeader(PgnHeader.Result), "?"));
				row.setEco(StringUtils.defaultString(game.getHeader(PgnHeader.ECO), "?"));
				row.setOpening(StringUtils.defaultString(game.getHeader(PgnHeader.Opening), "?"));
				row.setLineNumber(lineNumber);
				row.setResult(game.getResult());
				rows.add(row);

				if (rows.size() % 20 == 0) {
					shell.getDisplay().asyncExec(new RaptorRunnable() {
						@Override
						public void execute() {
							processMessageLabel.setText(L10n.getInstance().getString("pgnParseWI30") + ": " + lineNumber
									+ " " + L10n.getInstance().getString("pgnParseWI3") + rows.size() + " "
									+ L10n.getInstance().getString("pgnParseWI27") + errors.size());
							progressBar.setSelection(rows.size());
						}
					});
				}
			}
			return false;
		}

		public ArrayList<PgnParserError> getErrors() {
			return errors;
		}

		public ArrayList<PgnParseResultsRow> getGames() {
			return rows;
		}

	}

	public static final int MAX_BYTES_IN_FILE = 1048576 * 15;

	private static final RaptorLogger LOG = RaptorLogger.getLog(PgnProcessingDialog.class);

	private Button cancelButton;
	private Composite cancelComposite;
	protected int executeTime = 50;
	protected File file;
	protected volatile boolean isClosed = false;
	private Label lineLabel;

	protected boolean mayCancel = true;
	private CLabel message;

	protected int processBarStyle = SWT.SMOOTH; // process bar style
	protected String processMessage = L10n.getInstance().getString("processing");
	private Label processMessageLabel;
	private ProgressBar progressBar = null;
	private Composite progressBarComposite;

	private Shell shell;

	public PgnProcessingDialog(Shell parent, String file) {
		super(parent);
		this.file = new File(file);

		if (this.file.length() > MAX_BYTES_IN_FILE) {
			Raptor.getInstance().alert(L10n.getInstance().getString("pgnProcD1").replaceAll("MAX_BYTES_IN_FILE",
					Integer.toString(MAX_BYTES_IN_FILE)));

		}
	}

	public void open() {
		createContents(); // create window
		shell.open();
		shell.layout();

		ThreadService.getInstance().scheduleOneShot(250, new Runnable() {
			public void run() {
				// FileReader reader = null;
				LenientPgnParserListener listener = null;
				StreamingPgnParser parser = null;
				try {

					// start work
					parser = new StreamingPgnParser(file, MAX_BYTES_IN_FILE);
					listener = new ProfressPgnParserListener();

					parser.addPgnParserListener(listener);

					long startTime = System.currentTimeMillis();
					parser.parse();

					if (LOG.isDebugEnabled()) {
						LOG.debug("Parsed in " + (System.currentTimeMillis() - startTime) + "ms");
					}

					shell.getDisplay().asyncExec(new RaptorRunnable() {
						@Override
						public void execute() {
							shell.close();
						}
					});

					PgnParseResultsWindowItem windowItem = new PgnParseResultsWindowItem(file.getName(),
							((ProfressPgnParserListener) listener).getErrors(),
							((ProfressPgnParserListener) listener).getGames(), file.getAbsolutePath());
					Raptor.getInstance().getWindow().addRaptorWindowItem(windowItem);

				} catch (Throwable t) {
					if (!isClosed) {
						LOG.error("Error parsing pgn file", t);
						Raptor.getInstance().onError(L10n.getInstance().getString("pgnProcD2") + file, t);
					}
				} finally {
					try {
						if (parser != null)
							parser.close();
					} catch (Throwable t) {
					}
				}
			}
		});

		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

	}

	public void setProcessBarStyle(boolean pStyle) {
		if (pStyle) {
			processBarStyle = SWT.SMOOTH;
		} else {
			processBarStyle = SWT.NONE;
		}

	}

	protected void cleanUp() {

	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.PRIMARY_MODAL);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;

		shell.setLayout(gridLayout);
		shell.setSize(483, 181);
		shell.setText(L10n.getInstance().getString("parsing", file.getName()));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		composite.setLayout(new GridLayout());

		message = new CLabel(composite, SWT.NONE);
		message.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		message.setText(processMessage);

		progressBarComposite = new Composite(shell, SWT.NONE);
		progressBarComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		progressBarComposite.setLayout(new FillLayout());

		progressBar = new ProgressBar(progressBarComposite, processBarStyle);
		progressBar.setMaximum(PgnUtils.getApproximateGameCount(file.getAbsolutePath()));

		processMessageLabel = new Label(shell, SWT.NONE);
		processMessageLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		lineLabel = new Label(shell, SWT.HORIZONTAL | SWT.SEPARATOR);
		lineLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		processMessageLabel.setText("                                     ");

		cancelComposite = new Composite(shell, SWT.NONE);
		cancelComposite.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		cancelComposite.setLayout(gridLayout_1);

		cancelButton = new Button(cancelComposite, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isClosed = true;
			}
		});
		cancelButton.setLayoutData(new GridData(78, SWT.DEFAULT));
		cancelButton.setText(L10n.getInstance().getString("cancel"));
		cancelButton.setEnabled(mayCancel);

		SWTUtils.center(shell);

	}
}
