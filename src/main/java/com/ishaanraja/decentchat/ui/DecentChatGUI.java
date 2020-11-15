package com.ishaanraja.decentchat.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;

import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JLabel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class DecentChatGUI implements Display {

	private JFrame frame;
	private JTextArea input;
	private JTextField usernameField;
	private JTextArea chatLog;
	private JButton sendButton;
	private JLabel characterCount;
	private JLabel identifierLabel;
	private JScrollPane chatScrollPane;
	
	private static final Color NEUTRAL_COLOR = new Color(28,28,28);
	private static final Color PRIMARY_COLOR = new Color(43,43,43,255);
	
	private DecentChatClient client;

	/**
	 * Create the application.
	 */
	public DecentChatGUI() {
		initialize();
		frame.setVisible(true);
		client = new DecentChatClient(this);
		identifierLabel.setText("Identifier: "+client.getIdentifier());
		identifierLabel.setBounds(identifierLabel.getX(), identifierLabel.getY(), determineTextWidth(identifierLabel), identifierLabel.getHeight());
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		UIManager.put("TabbedPane.selected", PRIMARY_COLOR);
		UIManager.put("TabbedPane.focus", PRIMARY_COLOR);
		UIManager.put("TabbedPane.borderHightlightColor", NEUTRAL_COLOR);
		UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
		frame = new JFrame();
		frame.setMinimumSize(new Dimension(775, 500));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setLocationRelativeTo(null);
		frame.setTitle("DecentChat");
		try {
			frame.setIconImage(ImageIO.read(getClass().getResource("/icon.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setSize(frame.getWidth(), frame.getHeight());
		frame.getContentPane().add(tabbedPane);
		
		JPanel chatPanel = new JPanel();
		chatPanel.setSize(frame.getWidth(), frame.getHeight());
		tabbedPane.addTab("Chat", null, chatPanel, "Chat");
		chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
		chatPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		chatScrollPane = new JScrollPane();
		chatScrollPane.setPreferredSize(new Dimension(chatPanel.getWidth(), (int)Math.ceil(0.6*chatPanel.getHeight())));
		chatPanel.add(chatScrollPane);
		
		chatLog = new JTextArea();
		chatLog.setBorder(new EmptyBorder(8, 12, 12, 12));
		chatLog.setEditable(false);
		chatLog.setMaximumSize(new Dimension(chatScrollPane.getWidth(), chatScrollPane.getHeight()));
		chatLog.setLineWrap(true);
		chatLog.setWrapStyleWord(false);
		chatScrollPane.setViewportView(chatLog);
		
		chatScrollPane.getVerticalScrollBar().setBackground(PRIMARY_COLOR);
		UIManager.put("ScrollBar.thumb", new ColorUIResource(PRIMARY_COLOR));
		UIManager.put("ScrollBar.foreground", new ColorUIResource(Color.white));
		chatScrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
		    @Override
		    protected void configureScrollBarColors() {
		        this.thumbColor = Color.white;
		    }
		});
		chatPanel.add(Box.createVerticalStrut(10));
		
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
		inputPanel.setOpaque(false);
		inputPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		inputPanel.setPreferredSize(new Dimension(chatPanel.getWidth(), (int)Math.ceil(0.25*frame.getMinimumSize().height)));
		inputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)Math.ceil(0.25*frame.getMinimumSize().height)));
		JScrollPane inputScrollPane = new JScrollPane();
		inputScrollPane.setPreferredSize(new Dimension((int)Math.ceil(0.75*inputPanel.getWidth()), inputPanel.getHeight()));
		input = new JTextArea();
		input.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && (input.getText().startsWith("/") || input.getText().length() <= DecentConfig.MAX_MESSAGE_LENGTH)) {
					sendMessage();
				}
				else {
					if(input.getText().length() <= DecentConfig.MAX_MESSAGE_LENGTH) {
						characterCount.setForeground(Color.white);
						characterCount.setText(String.valueOf(input.getText().length()));
					}
					else {
						characterCount.setForeground(Color.red);
						characterCount.setText(String.valueOf(input.getText().length()));
					}
				}
				input.grabFocus();
			}
		});
		input.setAutoscrolls(true);
		input.setLineWrap(true);
        input.setWrapStyleWord(false);
		input.setMaximumSize(new Dimension(inputScrollPane.getWidth(), inputScrollPane.getHeight()));
		input.setBorder(chatLog.getBorder());
		input.grabFocus();
		
		inputScrollPane.setViewportView(input);
		inputPanel.add(inputScrollPane);
		
		inputPanel.add(Box.createHorizontalStrut(15));
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(new EmptyBorder(0, 0, 0, 15));
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		buttonsPanel.setOpaque(false);
		sendButton = new JButton();
		sendButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		sendButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		sendButton.setPreferredSize(new Dimension(50,25));
		sendButton.setMaximumSize(new Dimension(50,25));
		sendButton.setFont(new Font("Arial", Font.PLAIN, 10));
		sendButton.setText("Send");
		sendButton.setBorder(new EmptyBorder(0,0,0,0));
		sendButton.setOpaque(true);
		sendButton.setBackground(PRIMARY_COLOR);
        sendButton.setForeground(Color.white);
		sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
		    public void mouseEntered(java.awt.event.MouseEvent evt) {
		    	if(sendButton.getText().equals("Send")) {
			    	sendButton.setBackground(Color.white);
			    	sendButton.setForeground(NEUTRAL_COLOR);
		    	}
		    }

		    public void mouseExited(java.awt.event.MouseEvent evt) {
		    	if(sendButton.getText().equals("Send")) {
			    	sendButton.setBackground(PRIMARY_COLOR);
			        sendButton.setForeground(Color.white);
		    	}
		    }
		});
		sendButton.addActionListener(new ActionListener()
		{
			  public void actionPerformed(ActionEvent e)
			  {
				  sendMessage();
			  }
		});
		buttonsPanel.add(sendButton);
		
		characterCount = new JLabel("0");
		characterCount.setFont(new Font("Arial", Font.PLAIN, 12));
		characterCount.setAlignmentX(Component.LEFT_ALIGNMENT);
		characterCount.setAlignmentY(Component.CENTER_ALIGNMENT);
		buttonsPanel.add(characterCount);
		inputPanel.add(buttonsPanel);
		chatPanel.add(inputPanel);
		
		//Set color for Chat pane
		chatPanel.setBackground(NEUTRAL_COLOR);
		frame.getContentPane().setBackground(NEUTRAL_COLOR);
		tabbedPane.setBackground(NEUTRAL_COLOR);
		tabbedPane.setForeground(Color.white);
		tabbedPane.setOpaque(false);
		chatScrollPane.setBorder(new EmptyBorder(0,0,0,0));
		inputScrollPane.setBorder(new EmptyBorder(0,0,0,0));
		chatLog.setBackground(PRIMARY_COLOR);
		input.setBackground(PRIMARY_COLOR);
		input.setCaretColor(Color.white);
		chatLog.setForeground(Color.white);
		input.setForeground(Color.white);
		characterCount.setForeground(Color.white);
		
		//Start of settings panel
		JPanel settingsPanel = new JPanel();
		tabbedPane.addTab("Settings", null, settingsPanel, "Settings");
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		settingsPanel.setBorder(new EmptyBorder(10, 10, 0, 0));
		
		JLabel numPeersLabel = new JLabel("Number of Peers: 0");
		numPeersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		settingsPanel.add(numPeersLabel);
		settingsPanel.add(Box.createVerticalStrut(10));
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				numPeersLabel.setText("Number of Peers: "+client.getPeers().size());
				usernameField.grabFocus();
			}
		});
		
		identifierLabel = new JLabel("Identifier: ");
		identifierLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		settingsPanel.add(identifierLabel);
		settingsPanel.add(Box.createVerticalStrut(10));
		
		JLabel versionLabel = new JLabel("Version: "+DecentConfig.VERSION);
		versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		settingsPanel.add(versionLabel);
		settingsPanel.add(Box.createVerticalStrut(10));
		
		JCheckBox upnpBox = new JCheckBox("UPNP Enabled: ");
		upnpBox.setBorder(new EmptyBorder(0, 0, 0, 0));
		upnpBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		upnpBox.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent event)
		    {
		        DecentConfig.setUPNPEnabled(upnpBox.isSelected());
		    }
		});
		upnpBox.setHorizontalTextPosition(SwingConstants.LEFT);
		upnpBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		upnpBox.setSelected(DecentConfig.getUPNPEnabled());
		settingsPanel.add(upnpBox);
		settingsPanel.add(Box.createVerticalStrut(10));
		
		JPanel usernamePanel = new JPanel(new BorderLayout());
		JLabel usernameLabel = new JLabel("Username: ");	
		usernameLabel.setLabelFor(usernameField);
		usernamePanel.add(usernameLabel,BorderLayout.WEST);
		
		usernameField = new JTextField();
		usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
		usernameField.addFocusListener(new FocusAdapter() {
			
			@Override
			public void focusGained(FocusEvent e) {
				usernameField.setText(DecentConfig.getUsername());
			}
			
			@Override
			public void focusLost(FocusEvent e) {
				if(usernameField.getText().length() >= 1 && usernameField.getText().length() <= DecentConfig.MAX_USERNAME_LENGTH) {
					DecentConfig.setUsername(usernameField.getText());
				}
			}
		
		});
		usernameField.setColumns(16);
		usernamePanel.add(usernameField,BorderLayout.CENTER);
		usernamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		usernamePanel.setOpaque(false);
		usernamePanel.setMaximumSize(new Dimension(200, 25));
		settingsPanel.add(usernamePanel);
		
		//Set colors for settings pane
		settingsPanel.setBackground(PRIMARY_COLOR);
		numPeersLabel.setForeground(Color.white);
		identifierLabel.setForeground(Color.white);
		versionLabel.setForeground(Color.white);
		upnpBox.setForeground(Color.white);
		upnpBox.setBackground(PRIMARY_COLOR);
		usernameLabel.setForeground(Color.white);
		usernameField.setForeground(Color.white);
		usernameField.setCaretColor(Color.white);
		usernameField.setBackground(PRIMARY_COLOR);
		scrollToBottom();
	}
	public void display(String text) {
		Font chatLogFont = chatLog.getFont();
		String displayText = "";
		//Ensure that the font the client is using can display a character
		//This bug is related to emojis - which for some reason are not displayable by certain Swing fonts
		for(char c: text.toCharArray()) {
			if(chatLogFont.canDisplay(c)) {
				displayText+=c;
			}
		}
		//If user is already at the lowest scroll, then we will autoscroll to any new line
		boolean atBottom = isAtBottom();
		chatLog.setText(chatLog.getText()+"\n"+displayText);
		chatScrollPane.setViewportView(chatLog);
		if(atBottom) {
			scrollToBottom();
		}
	}
	/**
	 * Sends the message in the input area. if the message starts with a "/", it 
	 * interprets it as a command. If not, the message is broadcast to the network.
	 */
	public void sendMessage() {
		if(input.getText().startsWith("/")) {
			input.setEnabled(false);
			client.executeCommand(input.getText());
			input.setText("");
			characterCount.setText("0");
			input.setEnabled(true);
		}
		else if(input.getText().length() <= DecentConfig.MAX_MESSAGE_LENGTH && input.getText().trim().length() > 0) {
			characterCount.setText("0");
			sendButton.setBackground(PRIMARY_COLOR.darker());
			sendButton.setText("Sending");
			sendButton.setEnabled(false);
			input.setEnabled(false);
			Thread t = new Thread(new Runnable() {
			    public void run() {
			    	client.sendChatMessage(input.getText());
					input.setText("");
					sendButton.setBackground(PRIMARY_COLOR);
					sendButton.setText("Send");
					sendButton.setEnabled(true);
					input.setEnabled(true);
			    }
			});
			t.start();
		}
		else if(input.getText().length() > DecentConfig.MAX_MESSAGE_LENGTH) {
			display("Messages must be between 1-"+DecentConfig.MAX_MESSAGE_LENGTH+" characters");
		}
	}
	private boolean isAtBottom() {
	    JScrollBar sb = chatScrollPane.getVerticalScrollBar();
	    int val = sb.getModel().getExtent();
	    if(sb.getValue()+val == sb.getMaximum()) {
	    	return true;
	    }
	    int maxVal = sb.getMaximum();
	    //25 px "bottom" tolerance
	    boolean atBottom = sb.getValue()+val >= maxVal-25;
	    return atBottom;
	}
	private void scrollToBottom() {
		JScrollBar scrollbar = chatScrollPane.getVerticalScrollBar();
		scrollbar.setValue(scrollbar.getMaximum());
	}
	private static int determineTextWidth(JLabel label) {
		return label.getFontMetrics(label.getFont()).stringWidth(label.getText());
	}
}
