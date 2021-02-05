/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import com.google.gson.JsonParser;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * Utility class to get information about code systems for the transformation
 */
public class TerminologyClient {

  /**
   * FHIR specification "common" UCUM codes ValueSet URL
   */
  private static final String UCUM_COMMON_CODES_VALUESET_URL = "https://www.hl7.org/fhir/valueset-ucum-common.json";

  private Map<String, String> snomedCache = new HashMap<>();
  private Map<String, Parameters> loincCache = new HashMap<>();
  private IGenericClient fhirClient;

  /**
   * @param fhirClient connection to a FHIR server to refer to
   */
  public TerminologyClient(IGenericClient fhirClient) {
    this.fhirClient = fhirClient;
  }

  /**
   * Looks up a LOINC code and returns a {@link Parameters} object from the FHIR server in response to
   * a $lookup for all properties of the code
   * 
   * @param code LOINC code to look up
   * @param loincVersion LOINC version to use
   * @return {@link Parameters} object returned by the FHIR server requesting all properties be
   *         returned
   */
  public Parameters getLoincConcept(String code, String loincVersion) {
    if (!loincCache.containsKey(code)) {
      Parameters inParams = new Parameters();
      inParams.addParameter().setName("code").setValue(new StringType(code));
      inParams.addParameter().setName("system").setValue(new UriType(Constants.LOINC_CS_URI));
      inParams.addParameter().setName("version").setValue(new StringType(loincVersion));
      inParams.addParameter().setName("property").setValue(new StringType("*"));
      loincCache.put(code,
          fhirClient.operation().onType(CodeSystem.class).named("$lookup").withParameters(inParams).useHttpGet().execute());
    } else {
      System.out.println("loinc cache hit");
    }

    return loincCache.get(code);
  }

  /**
   * Gets the preferred display term for a SNOMED CT code from the Netherlands edition
   * 
   * @param code SNOMED CT code to look up
   * @param defaultIfNotFound default text to return if the code is not found
   * @return the display term for the SNOMED CT code specified in the Netherlands edition of SNOMED
   *         CT, or the value passed as defaultIfNotFound if no SNOMED CT concept can be found for
   *         that code
   */
  public String getSnomedDisplay(String code, String defaultIfNotFound) {
    if (!snomedCache.containsKey(code)) {
      Parameters inParams = new Parameters();
      inParams.addParameter().setName("code").setValue(new StringType(code));
      inParams.addParameter().setName("system").setValue(new UriType(Constants.SCT_CS_URI));
      inParams.addParameter().setName("version").setValue(new StringType(Constants.NL_SCT_EDITION));
      inParams.addParameter().setName("property").setValue(new StringType("display"));
      String display;
      try {
        display = fhirClient.operation().onType(CodeSystem.class).named("$lookup").withParameters(inParams).useHttpGet().execute()
            .getParameters("display").get(0).primitiveValue();
      } catch (ResourceNotFoundException e) {
        System.err.println(
            "WARNING: SNOMED CT concept " + code + " not found, using the display term from the XML file '" + defaultIfNotFound + "'");
        display = defaultIfNotFound;
      }
      snomedCache.put(code, display);
    }

    return snomedCache.get(code);

  }

  /**
   * @return the FHIR specification common UCUM codes ValueSet
   * @throws IOException
   * @throws ClientProtocolException
   */
  public static ValueSet getCommonUcumCodes() throws ClientProtocolException, IOException {

    HttpGet request = new HttpGet(UCUM_COMMON_CODES_VALUESET_URL);
    try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {

      // Get HttpResponse Status
      System.out.println(response.getProtocolVersion()); // HTTP/1.1

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Unable to get common UCUM codes from the FHIR specification, response code was "
            + response.getStatusLine().getStatusCode() + " response was " + EntityUtils.toString(response.getEntity()));
      }

      HttpEntity entity = response.getEntity();
      if (entity != null) {
        return FhirContext.forR4().newJsonParser().parseResource(ValueSet.class, EntityUtils.toString(entity));
      } else {
        throw new RuntimeException("Unexpected emtpy response for " + UCUM_COMMON_CODES_VALUESET_URL);
      }
    }
  }

  public static String getToken(String tokenEndpoint, String clientId, String clientSecret) throws IOException {

    HttpPost post = new HttpPost(tokenEndpoint);

    List<NameValuePair> urlParameters = new ArrayList<>();
    urlParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
    urlParameters.add(new BasicNameValuePair("client_id", clientId));
    urlParameters.add(new BasicNameValuePair("client_secret", clientSecret));

    post.setEntity(new UrlEncodedFormEntity(urlParameters));

    try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
      return JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject().get("access_token").getAsString();
    }
  }

}
