package org.specksensor.applications;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import edu.cmu.ri.createlab.device.connectivity.BaseCreateLabDeviceConnectivityManager;
import edu.cmu.ri.createlab.userinterface.GUIConstants;
import edu.cmu.ri.createlab.userinterface.component.Spinner;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.ImageUtils;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import edu.cmu.ri.createlab.util.StandardVersionNumber;
import edu.cmu.ri.createlab.util.net.HostAndPort;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.DataSampleManager;
import org.specksensor.RemoteStorageCredentialsImpl;
import org.specksensor.RemoteStorageCredentialsValidator;
import org.specksensor.Speck;
import org.specksensor.SpeckConfig;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings("CloneableClassWithoutClone")
final class SpeckGatewayGui
   {
   private static final Logger LOG = Logger.getLogger(SpeckGatewayGui.class);

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(SpeckGatewayGui.class.getName());

   private static final int DEFAULT_PORT = 80;
   private static final int SMALL_GAP = 5;
   private static final int GAP = 10;
   private static final String STATISTICS_VALUE_ZERO = "0";
   private static final String EMPTY_LABEL_TEXT = " ";

   private static final int[] LOGGING_INTERVAL_VALUES = new int[]{1, 2, 5, 10, 20, 30, 45, 60, 120, 180, 240};
   private static final String[] LOGGING_INTERVAL_LABELS = new String[]{"1 second",
                                                                        "2 seconds",
                                                                        "5 seconds",
                                                                        "10 seconds",
                                                                        "20 seconds",
                                                                        "30 seconds",
                                                                        "45 seconds",
                                                                        "1 minute",
                                                                        "2 minutes",
                                                                        "3 minutes",
                                                                        "4 minutes"};

   private static final Font FONT_NORMAL_BOLD = new Font(GUIConstants.FONT_NAME, Font.BOLD, GUIConstants.FONT_NORMAL.getSize());

   private final Spinner connectionSpinner = new Spinner(RESOURCES.getString("label.spinner"), GUIConstants.FONT_NORMAL);
   private final JPanel softwareUpdatePanel = new JPanel();
   private final JPanel connectionStatusPanel = new JPanel();
   private final JPanel speckPanel;
   private final JPanel datastoreServerPanel;
   private final JPanel statisticsPanel;
   private final JPanel loggingIntervalPanel = new JPanel();
   private final JLabel alertIcon = new JLabel(ImageUtils.createImageIcon("/org/specksensor/applications/alert-icon.png"));
   private final JLabel infoIcon = new JLabel(ImageUtils.createImageIcon("/org/specksensor/applications/info-icon.png"));

   @NotNull
   private final SpeckGatewayHelper helper;

   private final JLabel connectionStatusLabelSpeckId = SwingUtils.createLabel(EMPTY_LABEL_TEXT, FONT_NORMAL_BOLD);

   private final JTextField hostAndPortTextField = new JTextField(30);
   private final JTextField usernameTextField = new JTextField(30);
   private final JTextField passwordTextField = new JTextField(30);
   private final JTextField deviceNameTextField = new JTextField(30);

   private final JComboBox loggingIntervalComboBox = new JComboBox(LOGGING_INTERVAL_LABELS);
   private final JButton enableUploadsButton = SwingUtils.createButton(RESOURCES.getString("label.begin-uploading"));
   private final JLabel datastoreServerConnectionStatus = SwingUtils.createLabel(EMPTY_LABEL_TEXT);

   private final JLabel statsDownloadsRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsDownloadsSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsDownloadsFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSavesRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSavesSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSavesFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsFileUploadsRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsFileUploadsSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsFileUploadsFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSampleUploadsRequested = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSampleUploadsSuccessful = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);
   private final JLabel statsSampleUploadsFailed = SwingUtils.createLabel(STATISTICS_VALUE_ZERO);

   private final Map<DataSampleManager.Statistics.Category, JLabel> statsCategoryToLabelMap = new HashMap<DataSampleManager.Statistics.Category, JLabel>(6);

   @NotNull
   private final JFrame jFrame;

   private final StatisticsListener statisticsListener = new StatisticsListener();

   SpeckGatewayGui(@NotNull final JFrame jFrame)
      {
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_REQUESTED, statsDownloadsRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_SUCCESSFUL, statsDownloadsSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.DOWNLOADS_FAILED, statsDownloadsFailed);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAVES_REQUESTED, statsSavesRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAVES_SUCCESSFUL, statsSavesSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAVES_FAILED, statsSavesFailed);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.FILE_UPLOADS_REQUESTED, statsFileUploadsRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.FILE_UPLOADS_SUCCESSFUL, statsFileUploadsSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.FILE_UPLOADS_FAILED, statsFileUploadsFailed);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAMPLE_UPLOADS_REQUESTED, statsSampleUploadsRequested);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAMPLE_UPLOADS_SUCCESSFUL, statsSampleUploadsSuccessful);
      statsCategoryToLabelMap.put(DataSampleManager.Statistics.Category.SAMPLE_UPLOADS_FAILED, statsSampleUploadsFailed);

      this.jFrame = jFrame;
      jFrame.setTitle(SpeckGatewayHelper.APPLICATION_NAME_AND_VERSION_NUMBER);

      speckPanel = createSpeckPanel();
      datastoreServerPanel = createDatastoreServerPanel();
      statisticsPanel = createStatisticsPanel();

      final JPanel mainPanel = new JPanel();
      mainPanel.setBackground(Color.WHITE);
      mainPanel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
      mainPanel.setLayout(createLayoutForWhenDisconnected(mainPanel));

      connectionSpinner.setVisible(true);
      connectionStatusPanel.setVisible(false);
      softwareUpdatePanel.setVisible(false);
      softwareUpdatePanel.setBackground(Color.WHITE);

      final UpdateChecker updateChecker = new UpdateChecker(StandardVersionNumber.parse(SpeckGatewayHelper.VERSION_NUMBER));
      updateChecker.addUpdateCheckResultListener(
            new UpdateChecker.UpdateCheckResultListener()
            {
            @Override
            public void handleUpdateCheckResult(final boolean wasCheckSuccessful,
                                                final boolean isUpdateAvailable,
                                                @Nullable final StandardVersionNumber versionNumberOfUpdate)
               {
               // Make sure this happens in the Swing thread...
               SwingUtilities.invokeLater(
                     new Runnable()
                     {
                     @Override
                     public void run()
                        {
                        final String text;
                        final JLabel icon;
                        if (wasCheckSuccessful)
                           {
                           if (isUpdateAvailable && versionNumberOfUpdate != null)
                              {
                              text = MessageFormat.format(RESOURCES.getString("label.update-available"),
                                                          SpeckGatewayHelper.VERSION_NUMBER,
                                                          versionNumberOfUpdate.toString());
                              icon = infoIcon;
                              }
                           else
                              {
                              text = null;
                              icon = null;
                              }
                           }
                        else
                           {
                           text = RESOURCES.getString("label.update-check-failed");
                           icon = alertIcon;
                           }

                        // only show the panel if an update is available, or if the update check failed
                        if (text != null)
                           {
                           setSoftwareUpdateContent(new HtmlPane(text), icon);
                           }
                        softwareUpdatePanel.setVisible(text != null);

                        jFrame.pack();
                        jFrame.repaint();
                        }
                     });
               }
            });
      // initiate the update check
      updateChecker.checkForUpdate();

      // add the main panel to the frame, pack, paint, center on the screen, and make it visible
      jFrame.add(mainPanel);
      jFrame.pack();
      jFrame.repaint();
      jFrame.setLocationRelativeTo(null);    // center the window on the screen
      jFrame.setVisible(true);

      helper = new SpeckGatewayHelper(
            new SpeckGatewayHelper.EventListener()
            {
            @Override
            public void handleConnectionEvent(@NotNull final SpeckConfig speckConfig, @NotNull final String portName)
               {
               SwingUtilities.invokeLater(
                     new Runnable()
                     {
                     @Override
                     public void run()
                        {
                        connectionSpinner.setVisible(false);
                        connectionStatusPanel.setVisible(true);
                        mainPanel.removeAll();
                        mainPanel.setLayout(createLayoutForWhenConnected(mainPanel));
                        deviceNameTextField.setText("Speck" + speckConfig.getId());
                        connectionStatusLabelSpeckId.setText(speckConfig.getId());
                        final boolean canMutateLoggingInterval = speckConfig.getApiSupport().canMutateLoggingInterval();
                        loggingIntervalPanel.setVisible(canMutateLoggingInterval);
                        if (canMutateLoggingInterval)
                           {
                           final int position = Arrays.binarySearch(LOGGING_INTERVAL_VALUES, speckConfig.getLoggingInterval());
                           final int loggingIntervalIndex = Math.max(0, Math.min(LOGGING_INTERVAL_VALUES.length - 1, position));

                           loggingIntervalComboBox.setSelectedIndex(loggingIntervalIndex);
                           }
                        validateDatastoreServerForm();

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
                        connectionSpinner.setVisible(true);
                        connectionStatusPanel.setVisible(false);
                        mainPanel.removeAll();
                        mainPanel.setLayout(createLayoutForWhenDisconnected(mainPanel));
                        enableUploadsButton.setVisible(true);
                        datastoreServerConnectionStatus.setText(EMPTY_LABEL_TEXT);
                        setDatastoreServerTextFieldsEnabled(true);
                        validateDatastoreServerForm();
                        resetStatisticsTable();
                        jFrame.pack();
                        jFrame.repaint();
                        jFrame.setLocationRelativeTo(null);    // center the window on the screen

                        // try to reconnect
                        doBackgroundScanAndConnect();
                        }
                     });
               }
            });

      doBackgroundScanAndConnect();
      }

   private void doBackgroundScanAndConnect()
      {
      // Kick off a connection attempt to the Speck
      final SwingWorker sw =
            new SwingWorker<Object, Object>()
            {
            @Nullable
            @Override
            protected Object doInBackground() throws Exception
               {
               final BaseCreateLabDeviceConnectivityManager<Speck> speckConnectivityManager =
                     new BaseCreateLabDeviceConnectivityManager<Speck>()
                     {
                     @Override
                     protected Speck scanForDeviceAndCreateProxy()
                        {
                        return helper.scanAndConnect();
                        }
                     };
               speckConnectivityManager.connect();
               return null;
               }
            };
      sw.execute();
      }

   private LayoutManager createLayoutForWhenConnected(@NotNull final JPanel panel)
      {
      datastoreServerPanel.setVisible(true);
      statisticsPanel.setVisible(true);

      final JPanel verticalDivider = new JPanel();
      verticalDivider.setBackground(Color.GRAY);
      verticalDivider.setMinimumSize(new Dimension(1, 10));
      verticalDivider.setMaximumSize(new Dimension(1, 2000));

      final JPanel horizontalDivider = new JPanel();
      horizontalDivider.setBackground(Color.GRAY);
      horizontalDivider.setMinimumSize(new Dimension(10, 1));
      horizontalDivider.setMaximumSize(new Dimension(4000, 1));

      // layout the various panels
      final GroupLayout layout = new GroupLayout(panel);
      layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(softwareUpdatePanel)
                  .addGroup(layout.createSequentialGroup()
                                  .addComponent(speckPanel)
                                  .addGap(GAP)
                                  .addComponent(verticalDivider)
                                  .addGap(GAP)
                                  .addComponent(datastoreServerPanel))
                  .addComponent(horizontalDivider)
                  .addComponent(statisticsPanel));

      layout.setVerticalGroup(
            layout.createSequentialGroup()
                  .addComponent(softwareUpdatePanel)
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(speckPanel)
                                  .addComponent(verticalDivider)
                                  .addComponent(datastoreServerPanel))
                  .addComponent(horizontalDivider)
                  .addGap(GAP)
                  .addComponent(statisticsPanel));

      return layout;
      }

   private LayoutManager createLayoutForWhenDisconnected(@NotNull final JPanel panel)
      {
      datastoreServerPanel.setVisible(false);
      statisticsPanel.setVisible(false);

      final GroupLayout layout = new GroupLayout(panel);
      layout.setHorizontalGroup(
            layout.createSequentialGroup()
                  .addComponent(speckPanel));
      layout.setVerticalGroup(
            layout.createSequentialGroup()
                  .addComponent(speckPanel));

      return layout;
      }

   public void disconnect()
      {
      helper.disconnect();
      }

   private void setSoftwareUpdateContent(@NotNull final Component content, @NotNull final JLabel icon)
      {
      softwareUpdatePanel.removeAll();
      softwareUpdatePanel.setFont(GUIConstants.FONT_NORMAL);

      icon.setBackground(Color.WHITE);

      final JPanel contentPanel = new JPanel();
      final GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
      contentPanel.setLayout(contentPanelLayout);
      contentPanel.setBackground(Color.WHITE);

      contentPanelLayout.setHorizontalGroup(
            contentPanelLayout.createSequentialGroup()
                  .addComponent(icon)
                  .addGap(GAP)
                  .addComponent(content)

      );
      contentPanelLayout.setVerticalGroup(
            contentPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(icon)
                  .addComponent(content)
      );

      contentPanel.setMaximumSize(new Dimension(contentPanel.getPreferredSize().width,
                                                Math.max(contentPanel.getPreferredSize().height,
                                                         icon.getPreferredSize().height)));
      final JPanel horizontalDivider = new JPanel();
      horizontalDivider.setBackground(Color.GRAY);
      horizontalDivider.setMinimumSize(new Dimension(10, 1));
      horizontalDivider.setMaximumSize(new Dimension(4000, 1));

      final GroupLayout softwareUpdatePanelLayout = new GroupLayout(softwareUpdatePanel);
      softwareUpdatePanel.setLayout(softwareUpdatePanelLayout);
      softwareUpdatePanelLayout.setHorizontalGroup(
            softwareUpdatePanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(contentPanel)
                  .addComponent(horizontalDivider)

      );
      softwareUpdatePanelLayout.setVerticalGroup(
            softwareUpdatePanelLayout.createSequentialGroup()
                  .addComponent(contentPanel)
                  .addGap(GAP)
                  .addComponent(horizontalDivider)
      );
      }

   @NotNull
   private JPanel createSpeckPanel()
      {
      final JLabel titleLabel = SwingUtils.createLabel(RESOURCES.getString("label.speck"), GUIConstants.FONT_LARGE);

      final GroupLayout speckConnectionStatusPanelLayout = new GroupLayout(connectionStatusPanel);
      connectionStatusPanel.setLayout(speckConnectionStatusPanelLayout);
      connectionStatusPanel.setBackground(Color.WHITE);
      loggingIntervalPanel.setBackground(Color.WHITE);

      final JLabel connectionStatusLabel = SwingUtils.createLabel(RESOURCES.getString("label.connected-to-speck"));  // "Connected to Speck"
      final JLabel loggingIntervalLabel = SwingUtils.createLabel(RESOURCES.getString("label.logging-interval"));     // Logging Interval

      loggingIntervalComboBox.setFont(GUIConstants.FONT_NORMAL);
      loggingIntervalComboBox.addActionListener(
            new AbstractTimeConsumingAction(this.jFrame)
            {
            int selectedIndex = 0;

            @Override
            protected void executeGUIActionBefore()
               {
               selectedIndex = loggingIntervalComboBox.getSelectedIndex();
               loggingIntervalComboBox.setEnabled(false);
               }

            @Override
            protected Object executeTimeConsumingAction()
               {
               if (helper.isConnected())
                  {
                  final Speck speck = helper.getSpeck();
                  if (speck != null)
                     {
                     try
                        {
                        speck.setLoggingInterval(LOGGING_INTERVAL_VALUES[selectedIndex]);
                        }
                     catch (Exception e)
                        {
                        LOG.error("Exception while trying to set the logging interval", e);
                        }
                     }
                  }
               return null;
               }

            @Override
            protected void executeGUIActionAfter(final Object resultOfTimeConsumingAction)
               {
               loggingIntervalComboBox.setEnabled(true);
               }
            });

      // Put the logging interval in its own panel, so we can optionally show/hide it
      // depending on whether logging interval mutation is supported by this Speck.
      final GroupLayout loggingIntervalLayout = new GroupLayout(loggingIntervalPanel);
      loggingIntervalPanel.setLayout(loggingIntervalLayout);
      loggingIntervalLayout.setHorizontalGroup(
            loggingIntervalLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(loggingIntervalLabel)
                  .addComponent(loggingIntervalComboBox)
      );
      loggingIntervalLayout.setVerticalGroup(
            loggingIntervalLayout.createSequentialGroup()
                  .addComponent(loggingIntervalLabel)
                  .addGap(GAP)
                  .addComponent(loggingIntervalComboBox)
      );

      // Must set the max size for the combo box!  Otherwise, resizing the window
      // causes the layout to stretch out all weird around the combo box. Setting
      // the max size to the preferred size prevents it from resizing at all, which
      // is what I want.
      loggingIntervalComboBox.setMaximumSize(loggingIntervalComboBox.getPreferredSize());

      speckConnectionStatusPanelLayout.setHorizontalGroup(
            speckConnectionStatusPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(connectionStatusLabel)
                  .addComponent(connectionStatusLabelSpeckId)
                  .addComponent(loggingIntervalPanel)
      );
      speckConnectionStatusPanelLayout.setVerticalGroup(
            speckConnectionStatusPanelLayout.createSequentialGroup()
                  .addComponent(connectionStatusLabel)
                  .addGap(GAP)
                  .addComponent(connectionStatusLabelSpeckId)
                  .addGap(GAP * 4)
                  .addComponent(loggingIntervalPanel)
      );

      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      final GroupLayout panelLayout = new GroupLayout(panel);
      panel.setLayout(panelLayout);

      panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(titleLabel)
                  .addComponent(connectionSpinner)
                  .addComponent(connectionStatusPanel)
      );
      panelLayout.setVerticalGroup(
            panelLayout.createSequentialGroup()
                  .addComponent(titleLabel)
                  .addGap(GAP)
                  .addComponent(connectionSpinner)
                  .addComponent(connectionStatusPanel)
                  .addGap(GAP)
      );

      return panel;
      }

   @NotNull
   private JPanel createDatastoreServerPanel()
      {
      final JLabel titleLabel = SwingUtils.createLabel(RESOURCES.getString("label.datastore-server"), GUIConstants.FONT_LARGE);

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

      datastoreServerConnectionStatus.setBackground(Color.WHITE);

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
                                                               .addComponent(enableUploadsButton))
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
                                                             .addComponent(enableUploadsButton))
      );

      panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(titleLabel)
                  .addComponent(formPanel)
                  .addComponent(datastoreServerConnectionStatus)
      );
      panelLayout.setVerticalGroup(
            panelLayout.createSequentialGroup()
                  .addComponent(titleLabel)
                  .addGap(GAP)
                  .addComponent(formPanel)
                  .addGap(20)
                  .addComponent(datastoreServerConnectionStatus)
                  .addGap(20)
      );

      final KeyAdapter datastoreServerFormValidator =
            new KeyAdapter()
            {
            @Override
            public void keyReleased(final KeyEvent keyEvent)
               {
               validateDatastoreServerForm();
               }
            };
      hostAndPortTextField.addKeyListener(datastoreServerFormValidator);
      usernameTextField.addKeyListener(datastoreServerFormValidator);
      passwordTextField.addKeyListener(datastoreServerFormValidator);
      deviceNameTextField.addKeyListener(datastoreServerFormValidator);

      enableUploadsButton.addActionListener(
            new AbstractTimeConsumingAction(jFrame)
            {
            /** Runs in the GUI event-dispatching thread before the time-consuming action is executed. */
            @Override
            protected void executeGUIActionBefore()
               {
               setDatastoreServerTextFieldsEnabled(false);
               enableUploadsButton.setEnabled(false);
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
                  enableUploadsButton.setVisible(false);
                  datastoreServerConnectionStatus.setText(RESOURCES.getString("label.uploading-speck-data-files"));
                  }
               else
                  {
                  setDatastoreServerTextFieldsEnabled(true);
                  enableUploadsButton.setEnabled(true);
                  datastoreServerConnectionStatus.setText(RESOURCES.getString("label.connection-failed"));
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
      final JLabel savesToComputerLabel = SwingUtils.createLabel(RESOURCES.getString("label.saves-to-computer"), FONT_NORMAL_BOLD);
      final JLabel fileUploadsToServerLabel = SwingUtils.createLabel(RESOURCES.getString("label.file-uploads-to-server"), FONT_NORMAL_BOLD);
      final JLabel sampleUploadsToServerLabel = SwingUtils.createLabel(RESOURCES.getString("label.sample-uploads-to-server"), FONT_NORMAL_BOLD);

      panelLayout.setHorizontalGroup(panelLayout.createSequentialGroup()
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(emptyLabel)
                                                           .addComponent(downloadsFromDeviceLabel)
                                                           .addComponent(savesToComputerLabel)
                                                           .addComponent(sampleUploadsToServerLabel)
                                                           .addComponent(fileUploadsToServerLabel)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(requestedLabel)
                                                           .addComponent(statsDownloadsRequested)
                                                           .addComponent(statsSavesRequested)
                                                           .addComponent(statsSampleUploadsRequested)
                                                           .addComponent(statsFileUploadsRequested)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(successfulLabel)
                                                           .addComponent(statsDownloadsSuccessful)
                                                           .addComponent(statsSavesSuccessful)
                                                           .addComponent(statsSampleUploadsSuccessful)
                                                           .addComponent(statsFileUploadsSuccessful)
                                           )
                                           .addGap(GAP)
                                           .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                           .addComponent(failedLabel)
                                                           .addComponent(statsDownloadsFailed)
                                                           .addComponent(statsSavesFailed)
                                                           .addComponent(statsSampleUploadsFailed)
                                                           .addComponent(statsFileUploadsFailed)
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
                                                         .addComponent(savesToComputerLabel)
                                                         .addComponent(statsSavesRequested)
                                                         .addComponent(statsSavesSuccessful)
                                                         .addComponent(statsSavesFailed)
                                         )
                                         .addGap(GAP)
                                         .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                         .addComponent(sampleUploadsToServerLabel)
                                                         .addComponent(statsSampleUploadsRequested)
                                                         .addComponent(statsSampleUploadsSuccessful)
                                                         .addComponent(statsSampleUploadsFailed)
                                         )
                                         .addGap(GAP)
                                         .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                         .addComponent(fileUploadsToServerLabel)
                                                         .addComponent(statsFileUploadsRequested)
                                                         .addComponent(statsFileUploadsSuccessful)
                                                         .addComponent(statsFileUploadsFailed)
                                         )
      );

      return panel;
      }

   private void setDatastoreServerTextFieldsEnabled(final boolean isEnabled)
      {
      hostAndPortTextField.setEnabled(isEnabled);
      usernameTextField.setEnabled(isEnabled);
      passwordTextField.setEnabled(isEnabled);
      deviceNameTextField.setEnabled(isEnabled);
      }

   private void validateDatastoreServerForm()
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
      enableUploadsButton.setEnabled(isFormValid);
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

   private static final class HtmlPane extends JEditorPane
      {
      private HtmlPane(@Nullable final String htmlContent)
         {
         super("text/html", htmlContent);
         this.setEditable(false);
         ((HTMLDocument)this.getDocument()).getStyleSheet().addRule("body{font-family:Verdana, Arial, sans-serif; font-size:11pt;}");
         this.setBackground(Color.WHITE);
         this.addHyperlinkListener(
               new HyperlinkListener()
               {
               public void hyperlinkUpdate(final HyperlinkEvent event)
                  {
                  try
                     {
                     if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType()))
                        {
                        Desktop.getDesktop().browse(event.getURL().toURI());
                        }
                     }
                  catch (Exception e)
                     {
                     LOG.debug("SpeckGatewayGui.HtmlPane.hyperlinkUpdate(): Exception while trying to launch the link in the browser.", e);
                     }
                  }
               });
         }
      }
   }
