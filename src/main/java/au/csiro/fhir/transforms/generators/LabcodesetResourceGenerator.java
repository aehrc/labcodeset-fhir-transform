/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;

import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Materials.Material;
import au.csiro.fhir.transform.xml.nl.labcodeset.MaterialDefinition;
import au.csiro.fhir.transform.xml.nl.labcodeset.Publication;
import au.csiro.fhir.transform.xml.nl.labcodeset.UnitDefinition;
import au.csiro.fhir.transforms.utility.TerminologyClient;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;

/**
 * Main Labcodeset resource generation class that coordinates the specific transforms for LOINC,
 * UCUM, Materials and Outcome resources, generates a bundle and writes out files to disk
 */
public class LabcodesetResourceGenerator {

  private static final String LABCODESET_BUNDLE_FILENAME = "Labcodeset-bundle-%s.json";
  private static final String MATERIAL_VS_FILENAME = "MaterialValueset-%s.json";
  private static final String MATERIAL_CM_FILENAME = "MaterialConceptMap-%s.json";
  private static final String OUTCOME_CM_FILENAME = "OutcomeConceptMap-%s.json";
  private static final String OUCTOME_VS_FILENAME_PREFIX = "OutcomeValueSet";
  private static final String LOINC_VS_FILENAME = "LOINCValueset-%s.json";
  private static final String LOINC_CS_SUPPLEMENT_FILENAME = "LOINCCodeSystemSupplement-%s.json";
  private static final String UCUM_VS_FILENAME = "UcumValueSet-%s.json";
  private static final String UCUM_CS_FILENAME = "UcumCodeSystemFragment-%s.json";
  private static final String UCUM_CONCEPT_MAP_FILENAME = "UcumConceptMap-%s.json";

  private final FhirContext ctx = FhirContext.forR4();
  private final IParser fhirParser = ctx.newJsonParser().setPrettyPrint(true);

  private String loincVersion;
  private Publication pub;
  private File outputDir;
  private String labcodesetVersion;
  private Map<String, Material> materialMap = new HashMap<>();
  private Map<String, UnitDefinition> unitMap = new HashMap<>();

  private TerminologyClient terminologyClient;

  /**
   * @param labcodesetFile {@link File} containing the Labcodeset file to transform to FHIR resources
   * @param outputDir {@link File} representing the directory location to write out the resulting
   *        resources to
   * @param loincVersion LOINC version the Labcodeset file should be used with
   * @param fhirEndpoint FHIR terminology endpoint to be consulted for SNOMED CT and LOINC content
   *        during the transformation
   * @throws IOException
   */
  public LabcodesetResourceGenerator(File labcodesetFile, File outputDir, String loincVersion, String fhirEndpoint, String tokenEndpoint,
      String clientId, String clientSecret) throws IOException {
    this.outputDir = outputDir;
    this.loincVersion = loincVersion;
    IGenericClient fhirClient = ctx.newRestfulGenericClient(fhirEndpoint);
    if (tokenEndpoint != null && clientId != null && clientSecret != null) {
      BearerTokenAuthInterceptor authInterceptor =
          new BearerTokenAuthInterceptor(TerminologyClient.getToken(tokenEndpoint, clientId, clientSecret));
      fhirClient.registerInterceptor(authInterceptor);
    }
    this.terminologyClient = new TerminologyClient(fhirClient);

    JAXBContext context;
    try {
      context = JAXBContext.newInstance(Publication.class);
      pub = (Publication) context.createUnmarshaller().unmarshal(new FileReader(labcodesetFile));
    } catch (JAXBException | FileNotFoundException e) {
      System.err.println("Failed parsing Labcodeset file " + e.getLocalizedMessage());
      System.exit(1);
    }

    this.labcodesetVersion = pub.getEffectiveDate().split("-")[0];

    for (LabConcept concept : pub.getLabConcepts().getLabConcept()) {
      for (Material material : concept.getMaterials().getMaterial()) {
        materialMap.put(material.getCode(), material);
      }
    }

    for (UnitDefinition unit : pub.getUnits().getUnit()) {
      unitMap.put(unit.getId(), unit);
    }
  }

  /**
   * Runs the transformation process
   */
  public void generateFhirResources() {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.COLLECTION);

    generateLoincResources(bundle);

    generateUcumResources(bundle);

    generateMaterialResources(bundle);

    generateOutcomeResources(bundle);

    outputResource(bundle, LABCODESET_BUNDLE_FILENAME, labcodesetVersion);
  }

  private void generateOutcomeResources(Bundle bundle) {
    OutcomeResourceGenerator outcomeResourceGenerator = new OutcomeResourceGenerator(labcodesetVersion, loincVersion, terminologyClient);

    ConceptMap outcomeConceptMap = outcomeResourceGenerator.createOutcomesConceptMap(pub);

    bundle.addEntry().setResource(outcomeConceptMap);
    outputResource(outcomeConceptMap, OUTCOME_CM_FILENAME);

    Collection<ValueSet> outcomeValeSets = outcomeResourceGenerator.createPublicationOutcomeValueSets(pub);

    outcomeValeSets.forEach(vs -> {
      bundle.addEntry().setResource(vs);
      outputResource(vs, OUCTOME_VS_FILENAME_PREFIX + "_" + vs.getId() + "-%s.json");
    });
  }

  private void generateMaterialResources(Bundle bundle) {
    MaterialsResourceGenerator materialsResourceGenerator =
        new MaterialsResourceGenerator(labcodesetVersion, loincVersion, terminologyClient, materialMap);

    ValueSet materialsValueSet = materialsResourceGenerator.createMaterialsValueSet(pub);
    bundle.addEntry().setResource(materialsValueSet);
    outputResource(materialsValueSet, MATERIAL_VS_FILENAME);

    ConceptMap materialsConceptMap = materialsResourceGenerator.createMaterialsConceptMap(pub);
    bundle.addEntry().setResource(materialsConceptMap);
    outputResource(materialsConceptMap, MATERIAL_CM_FILENAME);
  }

  private void generateUcumResources(Bundle bundle) {
    UcumResourceGenerator ucumResourceGenerator = new UcumResourceGenerator(labcodesetVersion, loincVersion, unitMap);

    CodeSystem ucumCodeSystem = ucumResourceGenerator.createUcumCodeSystem(pub);
    outputResource(ucumCodeSystem, UCUM_CS_FILENAME);
    bundle.addEntry().setResource(ucumCodeSystem);
    ValueSet ucumValueSet = ucumResourceGenerator.createUcumValueSet(pub);
    bundle.addEntry().setResource(ucumValueSet);
    outputResource(ucumValueSet, UCUM_VS_FILENAME, labcodesetVersion);
    ConceptMap ucumConceptMap = ucumResourceGenerator.createUcumMap(pub);
    bundle.addEntry().setResource(ucumConceptMap);
    outputResource(ucumConceptMap, UCUM_CONCEPT_MAP_FILENAME);
  }

  private void generateLoincResources(Bundle bundle) {
    LoincResourceGenerator loincResourceGenerator =
        new LoincResourceGenerator(labcodesetVersion, loincVersion, terminologyClient, unitMap);

    CodeSystem loincSupplement = loincResourceGenerator.createLoincCodeSystemSupplement(pub);
    bundle.addEntry().setResource(loincSupplement);
    outputResource(loincSupplement, LOINC_CS_SUPPLEMENT_FILENAME);

    ValueSet loincValueSet = loincResourceGenerator.createValueSetPublication(pub);
    bundle.addEntry().setResource(loincValueSet);
    outputResource(loincValueSet, LOINC_VS_FILENAME);
  }


  private void outputResource(MetadataResource resource, String filename) {
    outputResource(resource, filename, resource.getVersion());
  }

  private void outputResource(Resource resource, String filename, String version) {
    File file = new File(outputDir, String.format(filename, version));
    FileUtils.deleteQuietly(file);

    try {
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.write(fhirParser.encodeResourceToString(resource));
      fileWriter.close();
    } catch (IOException e) {
      System.err.println("Failed to write output file " + file + " due to " + e.getLocalizedMessage());
      System.exit(1);
    }
    System.out.println("\nOutput to release file : " + file.getName());
  }
}
