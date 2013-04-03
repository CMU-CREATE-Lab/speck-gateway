package org.specksensor.applications;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class SpeckGateway extends JPanel
   {
   private static final Logger LOG = Logger.getLogger(SpeckGateway.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(SpeckGateway.class.getName());

   private static final String LOGGING_LEVEL_SWITCH = "--logging-level";
   private static final String COMMAND_LINE_SWITCH = "--command-line";
   private static final String CONFIG_SWITCH = "--config";

   public static void main(final String[] args)
      {
      final Map<String, String> arguments = new HashMap<String, String>(args.length);
      for (final String arg : args)
         {
         final int equalsPosition = arg.indexOf('=');
         final String key;
         final String val;
         if (equalsPosition < 0)
            {
            key = arg;
            val = "";
            }
         else
            {
            key = arg.substring(0, equalsPosition);
            val = arg.substring(equalsPosition + 1);
            }
         arguments.put(key, val);
         }

      Level loggingLevel = LogManager.getRootLogger().getLevel();
      if (arguments.containsKey(LOGGING_LEVEL_SWITCH))
         {
         String desiredLoggingLevel = arguments.get(LOGGING_LEVEL_SWITCH);
         if (desiredLoggingLevel != null)
            {
            desiredLoggingLevel = desiredLoggingLevel.toLowerCase();
            }
         if ("trace".equals(desiredLoggingLevel))
            {
            loggingLevel = Level.TRACE;
            }
         else if ("debug".equals(desiredLoggingLevel))
            {
            loggingLevel = Level.DEBUG;
            }
         else if ("info".equals(desiredLoggingLevel))
            {
            loggingLevel = Level.INFO;
            }
         }
      LogManager.getRootLogger().setLevel(loggingLevel);
      logInfo("Log file logging level is '" + loggingLevel + "'");
      arguments.remove(LOGGING_LEVEL_SWITCH);

      // see whether we should launch the command-line version of the app, or the GUI
      if (arguments.containsKey(COMMAND_LINE_SWITCH))
         {
         new SpeckGatewayCommandLine(arguments.get(CONFIG_SWITCH)).run();
         }
      else
         {
         //Schedule a job for the event-dispatching thread: creating and showing this application's GUI.
         SwingUtilities.invokeLater(
               new Runnable()
               {
               public void run()
                  {
                  final JFrame jFrame = new JFrame();
                  final SpeckGatewayGui application = new SpeckGatewayGui(jFrame);

                  // set various properties for the JFrame
                  jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                  jFrame.setBackground(Color.WHITE);
                  jFrame.setResizable(true);
                  jFrame.addWindowListener(
                        new WindowAdapter()
                        {
                        public void windowClosing(final WindowEvent event)
                           {
                           // ask if the user really wants to exit
                           final int selectedOption = JOptionPane.showConfirmDialog(jFrame,
                                                                                    RESOURCES.getString("dialog.message.exit-confirmation"),
                                                                                    RESOURCES.getString("dialog.title.exit-confirmation"),
                                                                                    JOptionPane.YES_NO_OPTION,
                                                                                    JOptionPane.QUESTION_MESSAGE);

                           if (selectedOption == JOptionPane.YES_OPTION)
                              {
                              final SwingWorker<Object, Object> worker =
                                    new SwingWorker<Object, Object>()
                                    {
                                    @Override
                                    @Nullable
                                    protected Object doInBackground() throws Exception
                                       {
                                       application.disconnect();

                                       return null;
                                       }

                                    @Override
                                    protected void done()
                                       {
                                       System.exit(0);
                                       }
                                    };
                              worker.execute();
                              }
                           }
                        });

                  jFrame.addWindowStateListener(
                        new WindowStateListener()
                        {
                        @Override
                        public void windowStateChanged(final WindowEvent e)
                           {
                           jFrame.setPreferredSize(((JFrame)e.getSource()).getSize());
                           jFrame.pack();
                           jFrame.repaint();
                           }
                        });
                  }
               });
         }
      }

   private static void logInfo(@NotNull final String message)
      {
      LOG.info(message);
      CONSOLE_LOG.info(message);
      }

   private SpeckGateway()
      {
      // private to prevent instantiation
      }
   }
