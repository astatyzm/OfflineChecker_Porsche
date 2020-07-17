package OfflineChecker_Porsche;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Window_20200429 extends JFrame {

	public File fileIn;
	public File errorFile;
	public JFileChooser fileInPath = new JFileChooser();
	public File correctedOfflineFile;
	public String fileName;
	public String content = null;
	public String newContent = null;
	public String outputString = null;
	public StringBuilder sbVia = null;
	public StringBuilder sbFp = null;
	public StringBuilder sbKL = null;
	BufferedImage img;
	Path path;

	public Window_20200429() {
		createWindow();
	}

	public void checkOffline() throws FileNotFoundException {

		java.util.List<String> errorList = new ArrayList<String>();
		Charset charset = StandardCharsets.UTF_8;
		boolean extAchsEin = false;
		boolean extAchsAus = false;
		boolean startZwZeitMessage = false;
		boolean taktZeitStart = false;
		String correctName = fileInPath.getSelectedFile().getName();

		try {
			content = new String(Files.readAllBytes(path), charset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("*****************************************");

		errorList.removeAll(errorList);

		/*
		 * Adding to error list
		 */

		if (!content.contains("FB_Monitor;")) {
			errorList.add("Missing FB Monitor!\n");
		}

		// application FD has not extAchs
		if (correctName.startsWith("M_FD")) {
			extAchsEin = true;
			extAchsAus = true;
		} else if (content.contains("! Aktivierung Externe Achse")
				&& content.contains("! Deaktivierung Externe Achse")) {
			extAchsEin = true;
			extAchsAus = true;
		}

		if (content.contains("StartZwZeit")) {
			taktZeitStart = true;
		}

		if (!content.contains("EndeZwZeit"))
			errorList.add("Missing OLP TaktZeitEnd\n");

		Pattern regexTeilfertigmeldung = Pattern.compile("!\\s+Teilfertigmeldung\\s+\\d");
		Matcher matcherTeilfertigmeldung = regexTeilfertigmeldung.matcher(content);
		if (!matcherTeilfertigmeldung.find()) {
			errorList.add("Missing comment :! Teilfertigmeldung\n");
		}

		if (!content.contains("ArbFertigMeld"))
			errorList.add("Missing OLP Fertig Meldun\n");
		/*
		 * Adding errors to list.
		 */

		if (extAchsEin == false)
			errorList.add("Missing comment :! Aktivierung Externe Achse\n");
		if (extAchsAus == false)
			errorList.add("Missing comment :! Deaktivierung Externe Achse\n");
		if (taktZeitStart == false)
			errorList.add("Missing OLP TaktZeitStart\n");

		if (startZwZeitMessage == false)
			if (errorList.size() != 0) {
				errorFile = new File(fileInPath.getCurrentDirectory(), "ErrorList.txt");
				// System.out.println(errorList);
				Desktop desktop = Desktop.getDesktop();
				PrintWriter errorFileWriter = new PrintWriter(errorFile);
				errorFileWriter.println(errorList);
				errorFileWriter.close();
				if (errorFile.exists()) {
					try {
						JOptionPane.showMessageDialog(null, "You have an ERROR :( ", "",
								JOptionPane.INFORMATION_MESSAGE);
						desktop.open(errorFile);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			else {

				newContent = content.replaceAll("(?m)^(\\s)*(?=PERS|TASK PERS)", "!# ").replace("MOV_M_", "")
						.replace("MOV_", "").replace("MOV", "").replace("KE", "SPZ")
						.replaceAll("CONST robtarget", "LOCAL CONST robtarget").replaceAll("LOCAL PERS", "LOCAL CONST")
						.replaceAll("LOCAL VAR p", "LOCAL CONS")
						.replaceAll("LOCAL LOCAL CONST robtarget", "LOCAL CONST robtarget");

				pointOrder();
				insertString();
				saveNewOfflineFile();
				JOptionPane.showMessageDialog(null, "Everything OK :) ", "", JOptionPane.INFORMATION_MESSAGE);

			}
	}

	public void saveNewOfflineFile() {

		String folderName = "AfterOfllineChecker";
		fileName = fileInPath.getSelectedFile().getName();
		File saveFolder = new File(fileInPath.getCurrentDirectory(), folderName);
		boolean saveFolderBool = saveFolder.mkdir();

		if (saveFolder.exists()) {
			try {
				FileWriter newOfflineFile = new FileWriter(saveFolder + "/" + fileName);
				newOfflineFile.write(outputString);
				newOfflineFile.flush();
				newOfflineFile.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("nie ma folderu");
		outputString = null;
	}

	public void pointOrder() {

		java.util.List<String> viaList = new ArrayList<String>();
		java.util.List<String> spotList = new ArrayList<String>();
		java.util.List<String> klebenList = new ArrayList<String>();

		Pattern patternVia = Pattern
				.compile("\\s?LOCAL\\s?CONST\\s?robtarget\\s?(p|P)\\s?[10-99999999999999999999]+:=(.*)");
		Matcher matcherVia = patternVia.matcher(newContent);
		Pattern patternSpot = Pattern
				.compile("\\s?LOCAL\\s?CONST\\s?robtarget\\s?fp\\s?[10-9999999999999999999]+:=(.*)");
		Matcher matcherSpot = patternSpot.matcher(newContent);
		Pattern patternKleben = Pattern
				.compile("\\s?LOCAL\\s?CONST\\s?robtarget\\s?KL_\\s?[10-9999999999999999999]+:=(.*)");
		Matcher matcherKleben = patternKleben.matcher(newContent);

		System.out.println("************* pointOrder ******************");
		viaList.removeAll(viaList);
		spotList.removeAll(spotList);
		klebenList.removeAll(klebenList);

		while (matcherVia.find()) {
			viaList.add(matcherVia.group());
		}
		sbVia = new StringBuilder();
		for (String s : viaList) {
			sbVia.append(s);
			sbVia.append("\n");
		}

		while (matcherSpot.find()) {
			spotList.add(matcherSpot.group());
		}
		sbFp = new StringBuilder();
		for (String s : spotList) {
			sbFp.append(s);
			sbFp.append("\n");
		}

		while (matcherKleben.find()) {
			klebenList.add(matcherKleben.group());
		}
		sbKL = new StringBuilder();
		for (String s : klebenList) {
			sbKL.append(s);
			sbKL.append("\n");
		}
		System.out.println("*******************************");

	}

	public String insertString() {
		String newString = null;
		String orginalString = newContent;

		String viaStringToBeInserted = sbVia.toString();
		String fpStringToBeInserted = sbFp.toString();
		String klebenStringToBeInserted = sbKL.toString();

		int startIndex = newContent.indexOf("  !**********************************************************\r\n");
		int endIndex = newContent.indexOf("\n  !**********************************************************\r\n"
				+ "  !*            Raumpunkt-Deklarationen\r\n"
				+ "  !**********************************************************");
		String spaceToBeInserted = newContent.substring(startIndex + 165, endIndex);

		newContent = orginalString.replace(spaceToBeInserted, "\n\n\n");

		newString = newContent;
		for (int i = 0; i < newString.length(); i++) {
			outputString += newString.charAt(i);

			if (i == newString.indexOf("  !**********************************************************\r\n"
					+ "  !*            Raumpunkt-Deklarationen\r\n"
					+ "  !**********************************************************")) {
				outputString += fpStringToBeInserted + "\n";
			}

			if (i == newString.indexOf("  !**********************************************************\r\n"
					+ "  !*            Raumpunkt-Deklarationen\r\n"
					+ "  !**********************************************************")) {
				outputString += klebenStringToBeInserted + "\n";
			}

			if (i == newContent.indexOf("\n!# -----------------------------------------------")) {
				outputString += viaStringToBeInserted + "\n";
			}
		}

		if (outputString.contains("nullMODULE")) {
			System.out.println("jesssstt");
			outputString = outputString.replaceFirst("nullMODULE", "MODULE");

		}
		return outputString;

	}

	public void createWindow() {

		final FileNameExtensionFilter filter = new FileNameExtensionFilter("MOD files", "MOD");
		int x = 10, y = 10, width = 450, height = 450;

		File imageFile = new File("logo1.png");
		BufferedImage image = null;
		try {
			image = ImageIO.read(imageFile);
		} catch (Exception e) {
			System.out.println("Error load file.");
			e.printStackTrace();
		}

		JFrame frame = new JFrame();
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null);
		frame.setTitle("ABB OfflineChecker Porsche");
		frame.setAlwaysOnTop(false);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setLayout(null);
		frame.setVisible(true);

		JButton loadFile = new JButton();
		loadFile.setBounds(10, 10, 10, 10);
		loadFile.setSize(250, 20);
		loadFile.setText("Load MOD File");
		loadFile.setToolTipText("Click here to select MOD file and check offline program");

		loadFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileInPath.setFileFilter(filter);
				fileInPath.showOpenDialog(null);
				fileIn = fileInPath.getSelectedFile();
				fileName = fileIn.getAbsolutePath();
				path = Paths.get(fileName);
				try {
					checkOffline();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});

		JPanel panelUp = new JPanel();
		panelUp.setBounds(0, 0, width, 100);
		panelUp.setBackground(Color.WHITE);
		panelUp.add(loadFile, BorderLayout.WEST);

		JPanel panelMiddle = new JPanel();
		JLabel imageLabel = new JLabel(new ImageIcon(image));
		panelMiddle.setBounds(0, 100, width, 250);
		panelMiddle.setBackground(Color.WHITE);
		panelMiddle.add(imageLabel, BorderLayout.CENTER);

		JLabel author = new JLabel("Author astatyzm");
		author.setSize(150, 50);

		JPanel panelBottom = new JPanel();
		panelBottom.setBounds(0, 350, width - 10, 50);
		panelBottom.setBackground(Color.WHITE);
		panelBottom.setLayout(new BorderLayout());
		panelBottom.add(author, BorderLayout.EAST);

		frame.add(panelUp);
		frame.add(panelMiddle);
		frame.add(panelBottom);
	}
}
