package org.bodytrack.applications;

import java.util.PropertyResourceBundle;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class AirBotUploaderHelper
   {
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(AirBotUploaderHelper.class.getName());

   public static final String APPLICATION_NAME = RESOURCES.getString("application.name");
   public static final String VERSION_NUMBER = RESOURCES.getString("version.number");
   public static final String APPLICATION_NAME_AND_VERSION_NUMBER = APPLICATION_NAME + " v" + VERSION_NUMBER;
   }
