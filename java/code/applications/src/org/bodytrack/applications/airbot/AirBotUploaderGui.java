package org.bodytrack.applications.airbot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.PropertyResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import edu.cmu.ri.createlab.userinterface.GUIConstants;
import edu.cmu.ri.createlab.userinterface.component.Spinner;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import edu.cmu.ri.createlab.util.net.HostAndPort;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.DataStorageCredentialsImpl;
import org.bodytrack.airbot.DataStorageCredentialsValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings("CloneableClassWithoutClone")
final class AirBotUploaderGui
   {
   private static final Logger LOG = Logger.getLogger(AirBotUploaderGui.class);
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(AirBotUploaderGui.class.getName());

   private final Spinner airBotConnectionSpinner = new Spinner(RESOURCES.getString("label.spinner"));
   private final JPanel airBotConnectionStatusPanel = new JPanel();

   @NotNull
   private final AirBotUploaderHelper helper;

   private final Font FONT_NORMAL_BOLD = new Font(GUIConstants.FONT_NAME, Font.BOLD, GUIConstants.FONT_NORMAL.getSize());
   private final JLabel airBotConnectionStatusLabelAirBotId = SwingUtils.createLabel("", FONT_NORMAL_BOLD);
   private final JLabel airBotConnectionStatusLabelPortName = SwingUtils.createLabel("", FONT_NORMAL_BOLD);

   private final JTextField hostAndPortTextField = new JTextField(30);
   private final JTextField usernameTextField = new JTextField(30);
   private final JTextField passwordTextField = new JTextField(30);
   private final JTextField deviceNameTextField = new JTextField(30);

   private final JButton fluxtreamButton = SwingUtils.createButton(RESOURCES.getString("label.begin-uploading"));
   private final JLabel fluxtreamConnectionStatus = SwingUtils.createLabel("");

   @NotNull
   private final JFrame jFrame;

   AirBotUploaderGui(@NotNull final JFrame jFrame)
      {
      this.jFrame = jFrame;
      helper = new AirBotUploaderHelper(
            new AirBotUploaderHelper.EventListener()
            {
            @Override
            public void handleInfoMessageEvent(@NotNull final String message)
               {
               // TODO: append to GUI console
               LOG.info(message);
               }

            @Override
            public void handleErrorMessageEvent(@NotNull final String message)
               {
               // TODO: append to GUI console
               LOG.error(message);
               }

            @Override
            public void handleConnectionEvent(@NotNull final AirBotConfig airBotConfig, @NotNull final String portName)
               {
               airBotConnectionSpinner.setVisible(false);
               airBotConnectionStatusPanel.setVisible(true);
               airBotConnectionStatusLabelAirBotId.setText(airBotConfig.getId());
               airBotConnectionStatusLabelPortName.setText(portName);
               }

            @Override
            public void handlePingFailureEvent()
               {
               airBotConnectionSpinner.setVisible(true);
               airBotConnectionStatusPanel.setVisible(false);
               fluxtreamButton.setVisible(true);
               fluxtreamConnectionStatus.setText("");
               setFluxtreamTextFieldsEnabled(true);
               validateFluxtreamForm();
               }
            });

      jFrame.setTitle(AirBotUploaderHelper.APPLICATION_NAME_AND_VERSION_NUMBER);

      final JPanel mainPanel = new JPanel();
      mainPanel.setBackground(Color.WHITE);

      final JPanel airBotPanel = createAirBotPanel();
      final JPanel fluxtreamPanel = createFluxtreamPanel();
      final JPanel statusPanel = createStatusPanel();

      final JPanel verticalDivider = new JPanel();
      verticalDivider.setBackground(Color.GRAY);
      verticalDivider.setMinimumSize(new Dimension(1, 10));
      verticalDivider.setMaximumSize(new Dimension(1, 2000));

      final JPanel horizontalDivider = new JPanel();
      horizontalDivider.setBackground(Color.GRAY);
      horizontalDivider.setMinimumSize(new Dimension(10, 1));
      horizontalDivider.setMaximumSize(new Dimension(4000, 1));

      // layout the various panels
      final GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
      mainPanel.setLayout(mainPanelLayout);
      mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup()
                  .addGroup(mainPanelLayout.createSequentialGroup()
                                  .addComponent(airBotPanel)
                                  .addGap(5)
                                  .addComponent(verticalDivider)
                                  .addGap(5)
                                  .addComponent(fluxtreamPanel))
                  .addComponent(horizontalDivider)
                  .addComponent(statusPanel));

      mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createSequentialGroup()
                  .addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(airBotPanel)
                                  .addComponent(verticalDivider)
                                  .addComponent(fluxtreamPanel))
                  .addGap(5)
                  .addComponent(horizontalDivider)
                  .addGap(5)
                  .addComponent(statusPanel));

      // add the main panel to the frame, pack, paint, center on the screen, and make it visible
      jFrame.add(mainPanel);
      jFrame.pack();
      jFrame.repaint();
      jFrame.setLocationRelativeTo(null);    // center the window on the screen
      jFrame.setVisible(true);

      airBotConnectionSpinner.setVisible(true);
      airBotConnectionStatusPanel.setVisible(false);

      // TODO: would be nice if this could work...
      //airBotConnectionStatusPanel.setMinimumSize(airBotConnectionSpinner.getSize());
      //airBotConnectionStatusPanel.setPreferredSize(airBotConnectionSpinner.getSize());
      //airBotConnectionStatusPanel.setMaximumSize(airBotConnectionSpinner.getSize());

      // Kick off a connection attempt to the AirBot
      final SwingWorker sw =
            new SwingWorker<Object, Object>()
            {
            @Nullable
            @Override
            protected Object doInBackground() throws Exception
               {
               helper.scanAndConnect();
               return null;
               }
            };
      sw.execute();
      }

   public void disconnect()
      {
      helper.disconnect();
      }

   @NotNull
   private JPanel createAirBotPanel()
      {
      final JLabel titleLabel = SwingUtils.createLabel(RESOURCES.getString("label.airbot"), GUIConstants.FONT_LARGE);

      final GroupLayout airBotConnectionStatusPanelLayout = new GroupLayout(airBotConnectionStatusPanel);
      airBotConnectionStatusPanel.setLayout(airBotConnectionStatusPanelLayout);
      airBotConnectionStatusPanel.setBackground(Color.WHITE);

      final JLabel airBotConnectionStatusLabel1 = SwingUtils.createLabel("Connected to AirBot");
      final JLabel airBotConnectionStatusLabel3 = SwingUtils.createLabel("on port");

      airBotConnectionStatusPanelLayout.setHorizontalGroup(
            airBotConnectionStatusPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(airBotConnectionStatusLabel1)
                  .addComponent(airBotConnectionStatusLabelAirBotId)
                  .addComponent(airBotConnectionStatusLabel3)
                  .addComponent(airBotConnectionStatusLabelPortName)
      );
      airBotConnectionStatusPanelLayout.setVerticalGroup(
            airBotConnectionStatusPanelLayout.createSequentialGroup()
                  .addComponent(airBotConnectionStatusLabel1)
                  .addGap(10)
                  .addComponent(airBotConnectionStatusLabelAirBotId)
                  .addGap(10)
                  .addComponent(airBotConnectionStatusLabel3)
                  .addGap(10)
                  .addComponent(airBotConnectionStatusLabelPortName)
      );

      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      final GroupLayout panelLayout = new GroupLayout(panel);
      panel.setLayout(panelLayout);

      panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(titleLabel)
                  .addComponent(airBotConnectionSpinner)
                  .addComponent(airBotConnectionStatusPanel)
      );
      panelLayout.setVerticalGroup(
            panelLayout.createSequentialGroup()
                  .addComponent(titleLabel)
                  .addGap(5)
                  .addComponent(airBotConnectionSpinner)
                  .addComponent(airBotConnectionStatusPanel)
      );

      return panel;
      }

   @NotNull
   private JPanel createFluxtreamPanel()
      {
      final JLabel titleLabel = SwingUtils.createLabel(RESOURCES.getString("label.fluxtream"), GUIConstants.FONT_LARGE);

      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      final GroupLayout panelLayout = new GroupLayout(panel);
      panel.setLayout(panelLayout);

      final JPanel formPanel = new JPanel();
      formPanel.setBackground(Color.WHITE);
      final GroupLayout formPanelLayout = new GroupLayout(formPanel);
      formPanel.setLayout(formPanelLayout);

      final JLabel hostAndPortLabel = SwingUtils.createLabel(RESOURCES.getString("label.host"));
      final JLabel usernameLabel = SwingUtils.createLabel(RESOURCES.getString("label.username"));
      final JLabel passwordLabel = SwingUtils.createLabel(RESOURCES.getString("label.password"));
      final JLabel deviceNameLabel = SwingUtils.createLabel(RESOURCES.getString("label.device-name"));
      final JLabel emptyLabel = SwingUtils.createLabel("");

      fluxtreamConnectionStatus.setBackground(Color.WHITE);

      // Horizontally, we want to align the labels and the text fields
      // along the left (LEADING) edge
      formPanelLayout.setHorizontalGroup(formPanelLayout.createSequentialGroup()
                                               .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                               .addComponent(hostAndPortLabel)
                                                               .addComponent(usernameLabel)
                                                               .addComponent(passwordLabel)
                                                               .addComponent(deviceNameLabel)
                                                               .addComponent(emptyLabel))
                                               .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                               .addComponent(hostAndPortTextField)
                                                               .addComponent(usernameTextField)
                                                               .addComponent(passwordTextField)
                                                               .addComponent(deviceNameTextField)
                                                               .addComponent(fluxtreamButton))
      );

      // Vertically, we want to align each label with his textfield
      // on the baseline of the components
      formPanelLayout.setVerticalGroup(formPanelLayout.createSequentialGroup()
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(hostAndPortLabel)
                                                             .addComponent(hostAndPortTextField))
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(usernameLabel)
                                                             .addComponent(usernameTextField))
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(passwordLabel)
                                                             .addComponent(passwordTextField))
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(deviceNameLabel)
                                                             .addComponent(deviceNameTextField))
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(emptyLabel)
                                                             .addComponent(fluxtreamButton))
      );

      panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(titleLabel)
                  .addComponent(formPanel)
                  .addComponent(fluxtreamConnectionStatus)
      );
      panelLayout.setVerticalGroup(
            panelLayout.createSequentialGroup()
                  .addComponent(titleLabel)
                  .addGap(5)
                  .addComponent(formPanel)
                  .addGap(20)
                  .addComponent(fluxtreamConnectionStatus)
      );

      final FluxtreamFormValidator fluxtreamFormValidator = new FluxtreamFormValidator();
      hostAndPortTextField.addKeyListener(fluxtreamFormValidator);
      usernameTextField.addKeyListener(fluxtreamFormValidator);
      passwordTextField.addKeyListener(fluxtreamFormValidator);
      deviceNameTextField.addKeyListener(fluxtreamFormValidator);

      fluxtreamButton.addActionListener(
            new AbstractTimeConsumingAction(jFrame)
            {
            /** Runs in the GUI event-dispatching thread before the time-consuming action is executed. */
            @Override
            protected void executeGUIActionBefore()
               {
               setFluxtreamTextFieldsEnabled(false);
               fluxtreamButton.setEnabled(false);
               }

            /**
             * Runs in a new thread so the GUI event-dispatching thread doesn't get bogged down. The {@link Object} returned from
             * this method will be passed to {@link #executeGUIActionAfter(Object)}.
             */
            @Nullable
            @Override
            protected Object executeTimeConsumingAction()
               {
               final HostAndPort hostAndPort = DataStorageCredentialsValidator.extractHostAndPort(hostAndPortTextField.getText());
               if (hostAndPort != null)
                  {
                  final DataStorageCredentialsImpl dataStorageCredentials = new DataStorageCredentialsImpl(hostAndPort.getHost(),
                                                                                                           Integer.parseInt(hostAndPort.getPort()),
                                                                                                           usernameTextField.getText(),
                                                                                                           passwordTextField.getText(),
                                                                                                           deviceNameTextField.getText());

                  return helper.validateAndSetDataStorageCredentials(dataStorageCredentials);
                  }
               return false;
               }

            /** Runs in the GUI event-dispatching thread after the time-consuming action is executed. */
            @Override
            protected void executeGUIActionAfter(final Object success)
               {
               if (success != null && (Boolean)success)
                  {
                  fluxtreamButton.setVisible(false);
                  fluxtreamConnectionStatus.setText("Uploading AirBot data files...");
                  }
               else
                  {
                  setFluxtreamTextFieldsEnabled(true);
                  fluxtreamButton.setEnabled(true);
                  fluxtreamConnectionStatus.setText("Connection failed.");
                  }
               }
            }
      );
      return panel;
      }

   private JPanel createStatusPanel()
      {
      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);

      return panel;
      }

   private void setFluxtreamTextFieldsEnabled(final boolean isEnabled)
      {
      hostAndPortTextField.setEnabled(isEnabled);
      usernameTextField.setEnabled(isEnabled);
      passwordTextField.setEnabled(isEnabled);
      deviceNameTextField.setEnabled(isEnabled);
      }

   private void validateFluxtreamForm()
      {
      final String hostAndPort = hostAndPortTextField.getText().trim();
      final String username = usernameTextField.getText().trim();
      final String password = passwordTextField.getText();
      final String deviceName = deviceNameTextField.getText();

      final boolean isDeviceNameValid = DataStorageCredentialsValidator.isDeviceNameValid(deviceName);
      final boolean isHostAndPortValid = DataStorageCredentialsValidator.isHostAndPortValid(hostAndPort);
      final boolean isFormValid = (isHostAndPortValid &&
                                   username.length() > 0 &&
                                   password.length() > 0 &&
                                   isDeviceNameValid);

      deviceNameTextField.setBackground(deviceName.length() <= 0 || isDeviceNameValid ? Color.WHITE : Color.PINK);
      fluxtreamButton.setEnabled(isFormValid);
      }

   private class FluxtreamFormValidator extends KeyAdapter
      {
      @Override
      public void keyReleased(final KeyEvent keyEvent)
         {
         validateFluxtreamForm();
         }
      }
   }
