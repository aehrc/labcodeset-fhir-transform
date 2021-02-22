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
  public static final String LABCODESET_RESOURCE_COPYRIGHT = "The Labcodeset is developed and maintained by NVKC (Nederlandse Vereniging voor Klinische Chemie en Laboratoriumgeneeskunde) and NVMM (Nederlandse Vereniging voor Medische Microbiologie), with functional maintenance by Nictiz (Nationaal ICT Instituut in de Zorg). If you have any questions or change requests, please email labcodeset@nictiz.nl.\n" + 
  		"The Labcodeset is based on LOINC, UCUM and SNOMED. Its use requires a license for the Dutch SNOMED Edition, which can be obtained at https://mlds.ihtsdotools.org/.\n" + 
  		"This material contains content from LOINC (http://loinc.org). LOINC is copyright © 1995-2020, Regenstrief Institute, Inc. and the Logical Observation Identifiers Names and Codes (LOINC) Committee and is available at no cost under the license at http://loinc.org/license. LOINC® is a registered United States trademark of Regenstrief Institute, Inc.\n" + 
  		"In general, Regenstrief’s terms-of-use for LOINC and UCUM have had the same core principles since its first release back in 1995. Certainly, the license has gotten longer and more explicit about certain things, but the main premises remain:\n" + 
  		"1. LOINC and UCUM are free for use in both commercial and non-commercial applications,\n" + 
  		"2. LOINC and UCUM are protected against change by end-users or creation of another vocabulary so that they can fulfil their goal of being an international standard, and\n" + 
  		"3. When LOINC or UCUM are used in databases, software applications, etc. users need to acknowledge the license and copyright so that end users of that product know that LOINC and UCUM are protected and licensed in a specific way.\n" + 
  		"The license agreements for LOINC and UCUM are available respectively at https://loinc.org/license/ and https://unitsofmeasure.org/trac/wiki/TermsOfUse";

  /**
   * Prefix used for URIs for Labcodeset generated resources
   */
  public static final String LABCODESET_URI_PREFIX = "http://labterminologie.nl";
}
