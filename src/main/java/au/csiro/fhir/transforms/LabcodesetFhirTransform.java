/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import au.csiro.fhir.transforms.generators.LabcodesetResourceGenerator;

/**
 * Command line interface class for the Labcodeset transformation to FHIR
 */
public class LabcodesetFhirTransform {

  private static final String CLIENT_SECRET_PARAM = "clientSecret";
  private static final String CLIENT_ID_PARAM = "clientId";
  private static final String TOKEN_ENDPOINT_PARAM = "tokenEndpoint";
  private static final String OUTPUT_DIR_PARAM = "outputDir";
  private static final String FHIR_ENDPOINT_PARAM = "fhirEndpoint";
  private static final String LOINC_VERSION_PARAM = "loincVersion";
  private static final String LABCODESET_FILE_PARAM = "labcodesetFile";

  public static void main(String[] args) {
    // create the command line parser
    CommandLineParser parser = new DefaultParser();

    // create the Options
    Options options = new Options();
    options.addOption(Option.builder(LABCODESET_FILE_PARAM).required(true).argName("file_path").hasArg(true)
        .desc("File path to the Labcodeset XML file to transform").build());
    options.addOption(Option.builder(LOINC_VERSION_PARAM).required(true).argName("loinc_version").hasArg(true)
        .desc("Version of LOINC this Labcodeset XML file has been built with").build());
    options.addOption(Option.builder(FHIR_ENDPOINT_PARAM).required(true).argName("fhir endpoint").hasArgs().type(File.class)
        .desc("FHIR terminology endpoint containing the appropriate LOINC and SNOMED CT-NL versions to reference during the transformation")
        .build());
    options.addOption(Option.builder(TOKEN_ENDPOINT_PARAM).required(false).argName("token _endpoint").hasArg(true).desc(
        "Token endpoint URL to get a bearer token with for the specified endpoint - optional if authorisation is required for the FHIR endpoint")
        .build());
    options.addOption(Option.builder(CLIENT_ID_PARAM).required(false).argName("client_id").hasArg(true).desc(
        "Client id part of client credentials to use with the token endpoint to get a bearer token for use with the specified FHIR endpoin - optional if authorisation is required for the FHIR endpoint")
        .build());
    options.addOption(Option.builder(CLIENT_SECRET_PARAM).required(false).argName("client_secret").hasArg(true).desc(
        "Client secret part of client credentials to use with the token endpoint to get a bearer token for use with the specified FHIR endpoin  - optional if authorisation is required for the FHIR endpoint")
        .build());
    options.addOption(Option.builder(OUTPUT_DIR_PARAM).required(false).argName("directory").hasArg(true).desc(
        "Location to output the resulting FHIR resources and Bundle - defaults to the directory the program was executed from if not specified")
        .build());

    boolean initFailed = false;
    File labcodesetFile = null;
    String loincVersion = null;
    String fhirEndpoint = null;
    File outputDir = null;
    String tokenEndpoint = null;
    String clientId = null;
    String clientSecret = null;
    try {
      CommandLine line = parser.parse(options, args);

      labcodesetFile = new File(line.getOptionValue(LABCODESET_FILE_PARAM));

      loincVersion = line.getOptionValue(LOINC_VERSION_PARAM);
      if (!loincVersion.matches("\\d{1}\\.\\d{2,}")) {
        System.err.println("LOINC version specified " + loincVersion + " is not valid, expected x.yy");
        initFailed = true;
      }

      fhirEndpoint = line.getOptionValue(FHIR_ENDPOINT_PARAM);

      if (line.hasOption(OUTPUT_DIR_PARAM)) {
        outputDir = new File(line.getOptionValue(OUTPUT_DIR_PARAM));
        if (outputDir.exists() && (!outputDir.isDirectory() || !outputDir.canWrite())) {
          System.err.println("Output directory " + outputDir + " exists but is not a directory or not writable");
          initFailed = true;
        } else if (!outputDir.exists()) {
          try {
            Files.createDirectories(outputDir.toPath());
          } catch (IOException e) {
            System.err.println("Output directory " + outputDir + " does not exist but cannot be created " + e.getLocalizedMessage());
            initFailed = true;
          }
        }
      } else {
        outputDir = new File(System.getProperty("user.dir"));
      }

      if (options.hasOption(CLIENT_ID_PARAM) || options.hasOption(CLIENT_SECRET_PARAM) || options.hasOption(TOKEN_ENDPOINT_PARAM)) {
        if (options.hasOption(CLIENT_ID_PARAM) && options.hasOption(CLIENT_SECRET_PARAM) && options.hasOption(TOKEN_ENDPOINT_PARAM)) {
          tokenEndpoint = line.getOptionValue(TOKEN_ENDPOINT_PARAM);
          clientId = line.getOptionValue(CLIENT_ID_PARAM);
          clientSecret = line.getOptionValue(CLIENT_SECRET_PARAM);
        } else {
          System.err.println("Parameters " + TOKEN_ENDPOINT_PARAM + ", " + CLIENT_ID_PARAM + ", and " + CLIENT_SECRET_PARAM
              + " must either all be supplied together or none can be set");
          initFailed = true;
        }
      }

    } catch (ParseException e1) {
      System.err.println("Failed to parse arguments " + e1.getLocalizedMessage());
      initFailed = true;
    }

    if (initFailed) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(120, "java -jar labcodeset-transforms-jar-with-dependencies.jar", "", options, "", true);
    } else {
      try {
        LabcodesetResourceGenerator generator =
            new LabcodesetResourceGenerator(labcodesetFile, outputDir, loincVersion, fhirEndpoint, tokenEndpoint, clientId, clientSecret);
        generator.generateFhirResources();
      } catch (IOException e) {
        System.err.println("Failed to initialise transform " + e.getLocalizedMessage());
      }
    }
  }
}
