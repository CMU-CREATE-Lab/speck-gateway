package org.bodytrack.applications.airbot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import edu.cmu.ri.createlab.userinterface.GUIConstants;
import edu.cmu.ri.createlab.userinterface.component.Spinner;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import edu.cmu.ri.createlab.util.net.HostAndPort;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.DataSampleManager;
import org.bodytrack.airbot.RemoteStorageCredentialsImpl;
import org.bodytrack.airbot.RemoteStorageCredentialsValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings("CloneableClassWithoutClone")
final class AirBotUploaderGui
   {
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(AirBotUploaderGui.class.getName());

   private static final int DEFAULT_PORT = 80;
   private static final int SMALL_GAP = 5;
   private static final int GAP = 10;
   private static final String STATISTICS_VALUE_ZERO = "0";
   private static final String EMPTY_LABEL_TEXT = " ";

   private final Spinner airBotConnectionSpinner = new Spinner(RESOURCES.getString("label.spinner"), GUIConstants.FONT_NORMAL);
   private final JPanel airBotConnectionStatusPanel = new JPanel();

   @NotNull
   private final AirBotUploaderHelper helper;

   private final Font FONT_NORMAL_BOLD = new Font(GUIConstants.FONT_NAME, Font.BOLD, GUIConstants.FONT_NORMAL.getSize());
   private final JLabel airBotConnectionStatusLabelAirBotId = SwingUtils.createLabel(EMPTY_LABEL_TEXT, FONT_NORMAL_BOLD);
   private final JLabel airBotConnectionStatusLabelPortName = SwingUtils.createLabel(EMPTY_LABEL_TEXT, FONT_NORMAL_BOLD);

   private final JTextField hostAndPortTextField = new JTextField(30);
   private final JTextField usernameTextField = new JTextField(30);
   private final JTextField passwordTextField = new JTextField(30);
   private final JTextField deviceNameTextField = new JTextField(30);

   private final JButton fluxtreamButton = SwingUtils.createButton(RESOURCES.getString("label.begin-uploading"));
   private final JLabel fluxtreamConnectionStatus = SwingUtils.createLabel(EMPTY_LABEL_TEXT);

   private final JLabel statsDownloadsRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsDownloadsSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsDownloadsFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsUploadsRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsUploadsSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsUploadsFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);

   private final Map<DataSampleManager.Statistics.Category, JLabel> statsCategoryToLabelMap = new HashMap<DataSampleManager.Statistics.Category, JLabel>(6);

   @NotNull
   private final JFrame jFrame;

   private StatisticsListener statisticsListener = new StatisticsListener();

   AirBotUploaderGui(@NotNull final JFrame jFrame)
      {
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_REQUESTED, statsDownloadsRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_SUCCESSFUL, statsDownloadsSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_FAILED, statsDownloadsFailed);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.UPLOADS_REQUESTED, statsUploadsRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.UPLOADS_SUCCESSFUL, statsUploadsSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.UPLOADS_FAILED, statsUploadsFailed);

      this.jFrame = jFrame;
      helper = new AirBotUploaderHelper(
            new AirBotUploaderHelper.EventListener()
            {
            @Override
            public void handleConnectionEvent(@NotNull final AirBotConfig airBotConfig, @NotNull final String portName)
               {
               SwingUtilities.invokeLater(
                     new Runnable()
                     {
                     @Override
                     public void run()
                        {
                        airBotConnectionSpinner.setVisible(false);
                        airBotConnectionStatusPanel.setVisible(true);
                        airBotConnectionStatusLabelAirBotId.setText(airBotConfig.getId());
                        airBotConnectionStatusLabelPortName.setText(portName);
                        helper.addStatisticsListener(statisticsListener);
                        jFrame.pack();
                        jFrame.repaint();
                        jFrame.setLocationRelativeTo(null);    // center the window on the screen
                        }
                     });
               }

            @Override
            public void handlePingFailureEvent()
               {
               SwingUtilities.invokeLater(
                     new Runnable()
                     {
                     @Override
                     public void run()
                        {
                        airBotConnectionSpinner.setVisible(true);
                        airBotConnectionStatusPanel.setVisible(false);
                        fluxtreamButton.setVisible(true);
                        fluxtreamConnectionStatus.setText(EMPTY_LABEL_TEXT);
                        setFluxtreamTextFieldsEnabled(true);
                        validateFluxtreamForm();
                        resetStatisticsTable();
                        jFrame.pack();
                        jFrame.repaint();
                        jFrame.setLocationRelativeTo(null);    // center the window on the screen
                        }
                     });
               }
            });

      jFrame.setTitle(AirBotUploaderHelper.APPLICATION_NAME_AND_VERSION_NUMBER);

      final JPanel mainPanel = new JPanel();
      mainPanel.setBackground(Color.WHITE);

      final JPanel airBotPanel = createAirBotPanel();
      final JPanel fluxtreamPanel = createFluxtreamPanel();
      final JPanel statisticsPanel = createStatisticsPanel();

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
      mainPanel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

      mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addGroup(mainPanelLayout.createSequentialGroup()
                                  .addComponent(airBotPanel)
                                  .addGap(GAP)
                                  .addComponent(verticalDivider)
                                  .addGap(GAP)
                                  .addComponent(fluxtreamPanel))
                  .addComponent(horizontalDivider)
                  .addComponent(statisticsPanel));

      mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createSequentialGroup()
                  .addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(airBotPanel)
                                  .addComponent(verticalDivider)
                                  .addComponent(fluxtreamPanel))
                  .addComponent(horizontalDivider)
                  .addGap(GAP)
                  .addComponent(statisticsPanel));

      // add the main panel to the frame, pack, paint, center on the screen, and make it visible
      jFrame.add(mainPanel);
      jFrame.pack();
      jFrame.repaint();
      jFrame.setLocationRelativeTo(null);    // center the window on the screen
      jFrame.setVisible(true);

      airBotConnectionSpinner.setVisible(true);
      airBotConnectionStatusPanel.setVisible(false);

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

      final JLabel airBotConnectionStatusLabel1 = SwingUtils.createLabel(RESOURCES.getString("label.connected-to-airbot"));//"Connected to AirBot"
      final JLabel airBotConnectionStatusLabel3 = SwingUtils.createLabel(RESOURCES.getString("label.on-port"));// on port

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
                  .addGap(GAP)
                  .addComponent(airBotConnectionStatusLabelAirBotId)
                  .addGap(GAP)
                  .addComponent(airBotConnectionStatusLabel3)
                  .addGap(GAP)
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
                  .addGap(GAP)
                  .addComponent(airBotConnectionSpinner)
                  .addComponent(airBotConnectionStatusPanel)
                  .addGap(GAP)
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
      final JLabel emptyLabel = SwingUtils.createLabel(EMPTY_LABEL_TEXT);

      fluxtreamConnectionStatus.setBackground(Color.WHITE);

      formPanelLayout.setHorizontalGroup(formPanelLayout.createSequentialGroup()
                                               .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                               .addComponent(hostAndPortLabel)
                                                               .addComponent(usernameLabel)
                                                               .addComponent(passwordLabel)
                                                               .addComponent(deviceNameLabel)
                                                               .addComponent(emptyLabel))
                                               .addGap(SMALL_GAP)
                                               .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                               .addComponent(hostAndPortTextField)
                                                               .addComponent(usernameTextField)
                                                               .addComponent(passwordTextField)
                                                               .addComponent(deviceNameTextField)
                                                               .addComponent(fluxtreamButton))
      );

      formPanelLayout.setVerticalGroup(formPanelLayout.createSequentialGroup()
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(hostAndPortLabel)
                                                             .addComponent(hostAndPortTextField))
                                             .addGap(SMALL_GAP)
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(usernameLabel)
                                                             .addComponent(usernameTextField))
                                             .addGap(SMALL_GAP)
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(passwordLabel)
                                                             .addComponent(passwordTextField))
                                             .addGap(SMALL_GAP)
                                             .addGroup(formPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                             .addComponent(deviceNameLabel)
                                                             .addComponent(deviceNameTextField))
                                             .addGap(SMALL_GAP)
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
                  .addGap(GAP)
                  .addComponent(formPanel)
                  .addGap(20)
                  .addComponent(fluxtreamConnectionStatus)
                  .addGap(20)
      );

      final KeyAdapter fluxtreamFormValidator =
            new KeyAdapter()
            {
            @Override
            public void keyReleased(final KeyEvent keyEvent)
               {
               validateFluxtreamForm();
               }
            };
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
               final HostAndPort hostAndPort = RemoteStorageCredentialsValidator.extractHostAndPort(hostAndPortTextField.getText());
               if (hostAndPort != null)
                  {
                  final String portStr = hostAndPort.getPort();
                  final RemoteStorageCredentialsImpl dataStorageCredentials = new RemoteStorageCredentialsImpl(hostAndPort.getHost(),
                                                                                                           (portStr == null) ? DEFAULT_PORT : Integer.parseInt(portStr),
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
                  fluxtreamConnectionStatus.setText(RESOURCES.getString("label.uploading-airbot-data-files"));
                  }
               else
                  {
                  setFluxtreamTextFieldsEnabled(true);
                  fluxtreamButton.setEnabled(true);
                  fluxtreamConnectionStatus.setText(RESOURCES.getString("label.connection-failed"));
                  }
               }
            }
      );
      return panel;
      }

   private JPanel createStatisticsPanel()
      {
      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      final GroupLayout panelLayout = new GroupLayout(panel);
      panel.setLayout(panelLayout);

      final JLabel emptyLabel = SwingUtils.createLabel(EMPTY_LABEL_TEXT);

      final JLabel requestedLabel = SwingUtils.createLabel(RESOURCES.getString("label.requested"), FONT_NORMAL_BOLD);
      final JLabel successfulLabel = SwingUtils.createLabel(RESOURCES.getString("label.successful"), FONT_NORMAL_BOLD);
      final JLabel failedLabel = SwingUtils.createLabel(RESOURCES.getString("label.failed"), FONT_NORMAL_BOLD);
      final JLabel downloadsFromDeviceLabel = SwingUtils.createLabel(RESOURCES.getString("label.downloads-from-device"), FONT_NORMAL_BOLD);
      final JLabel uploadsToServerLabel = SwingUtils.createLabel(RESOURCES.getString("label.uploads-to-server"), FONT_NORMAL_BOLD);

      panelLayout.setHorizontalGroup(panelLayout.createSequentialGroup()
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(emptyLabel)
                                                           .addComponent(downloadsFromDeviceLabel)
                                                           .addComponent(uploadsToServerLabel)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(requestedLabel)
                                                           .addComponent(statsDownloadsRequested)
                                                           .addComponent(statsUploadsRequested)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(successfulLabel)
                                                           .addComponent(statsDownloadsSuccessful)
                                                           .addComponent(statsUploadsSuccessful)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(failedLabel)
                                                           .addComponent(statsDownloadsFailed)
                                                           .addComponent(statsUploadsFailed)
                                           )
      );

      panelLayout.setVerticalGroup(panelLayout.createSequentialGroup()
                                         .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                         .addComponent(emptyLabel)
                                                         .addComponent(requestedLabel)
                                                         .addComponent(successfulLabel)
                                                         .addComponent(failedLabel)
                                         )
                                         .addGap(GAP)
                                         .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                         .addComponent(downloadsFromDeviceLabel)
                                                         .addComponent(statsDownloadsRequested)
                                                         .addComponent(statsDownloadsSuccessful)
                                                         .addComponent(statsDownloadsFailed)
                                         )
                                         .addGap(GAP)
                                         .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                         .addComponent(uploadsToServerLabel)
                                                         .addComponent(statsUploadsRequested)
                                                         .addComponent(statsUploadsSuccessful)
                                                         .addComponent(statsUploadsFailed)
                                         )
      );

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

      final boolean isDeviceNameValid = RemoteStorageCredentialsValidator.isDeviceNameValid(deviceName);
      final boolean isHostAndPortValid = RemoteStorageCredentialsValidator.isHostAndPortValid(hostAndPort);
      final boolean isFormValid = (isHostAndPortValid &&
                                   username.length() > 0 &&
                                   password.length() > 0 &&
                                   isDeviceNameValid);

      hostAndPortTextField.setBackground(hostAndPortTextField.getText().length() <= 0 || isHostAndPortValid ? Color.WHITE : Color.PINK);
      deviceNameTextField.setBackground(deviceName.length() <= 0 || isDeviceNameValid ? Color.WHITE : Color.PINK);
      fluxtreamButton.setEnabled(isFormValid);
      }

   private void resetStatisticsTable()
      {
      SwingUtils.runInGUIThread(new Runnable()
      {
      @Override
      public void run()
         {
         for (final JLabel label : statsCategoryToLabelMap.values())
            {
            label.setText(STATISTICS_VALUE_ZERO);
            }
         }
      });
      }

   private final class StatisticsListener implements DataSampleManager.Statistics.Listener
      {
      @Override
      public void handleValueChange(@NotNull final DataSampleManager.Statistics.Category category, final int newValue)
         {
         SwingUtilities.invokeLater(
               new Runnable()
               {
               @Override
               public void run()
                  {
                  final JLabel label = statsCategoryToLabelMap.get(category);
                  if (label != null)
                     {
                     label.setText(String.valueOf(newValue));
                     }
                  }
               }
         );
         }
      }
   }
