package org.bodytrack.applications;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class AirBotUploaderGui
   {
   private final JPanel mainPanel = new JPanel();

   AirBotUploaderGui(@NotNull final JFrame jFrame)
      {
      jFrame.setTitle(AirBotUploaderHelper.APPLICATION_NAME_AND_VERSION_NUMBER);

      mainPanel.add(new JLabel("This is the main panel"));
      mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));

      jFrame.add(mainPanel);

      jFrame.pack();
      jFrame.repaint();
      jFrame.setLocationRelativeTo(null);    // center the window on the screen
      jFrame.setVisible(true);
      }
   }
