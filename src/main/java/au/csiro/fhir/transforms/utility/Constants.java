/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.utility;

/**
 * General constants used across the transformations
 */
public class Constants {
  /**
   * Official FHIR CodeSystem URI for LOINC
   */
  public static final String LOINC_CS_URI = "http://loinc.org";

  /**
   * Official FHIR CodeSystem URI of SNOMED CT
   */
  public static final String SCT_CS_URI = "http://snomed.info/sct";

  /**
   * URI of the Netherlands edition of SNOMED CT
   */
  public static final String NL_SCT_EDITION = "http://snomed.info/sct/11000146104";

  /**
   * OID CodeSystem URI for FHIR
   */
  public static final String OID_CS_URI = "urn:ietf:rfc:3986";

  /**
   * Official FHIR specification UCUM CodeSystem URI
   */
  public static final String UCUM_CS_URI = "http://unitsofmeasure.org";

  /**
   * Language code for Dutch used on all Labcodeset resources
   */
  public static final String DUTCH_LANGUAGE_CODE = "nl-NL";

  /**
   * UCUM version being used - fairly static
   */
  public static final String UCUM_VERSION = "2.1";

  /**
   * Publisher set on all Labcodeset generated resources
   */
  public static final String LABCODESET_RESOURCE_PUBLISHER = "Nictiz";

  /**
   * Copyright statement set on all Labcodeset generated resources
   */
  public static final String LABCODESET_RESOURCE_COPYRIGHT = "";

  /**
   * Prefix used for URIs for Labcodeset generated resources
   */
  public static final String LABCODESET_URI_PREFIX = "http://labterminologie.nl";
}
