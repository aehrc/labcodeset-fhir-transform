/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r4.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Materials.Material;
import au.csiro.fhir.transform.xml.nl.labcodeset.MaterialDefinition;
import au.csiro.fhir.transform.xml.nl.labcodeset.MaterialOrMethodStatus;
import au.csiro.fhir.transform.xml.nl.labcodeset.Publication;
import au.csiro.fhir.transforms.utility.Constants;
import au.csiro.fhir.transforms.utility.TerminologyClient;

/**
 * Class that encapsulates the logic used to generate Materials SNOMED CT code link related FHIR
 * resources for Labcodeset
 */
public class MaterialsResourceGenerator {

  private static final String LABCODESET_MATERIALS_CM_DESCRIPTION = "Map of LOINC Nederlandse Labcodeset codes to SNOMED CT Materials";
  private static final String LABCODESET_MATERIALS_CM_URI = Constants.LABCODESET_URI_PREFIX + "/cm/labconcepts-materials";
  private static final String LABCODESET_MATERIALS_CM_TITLE = "Nederlandse Labcodeset Materials map";

  private static final String LABCODESET_MATERIALS_VS_DESCRIPTION = "SNOMED CT materials codes referenced in the Nederlandse Labcodeset";
  private static final String LABCODESET_MATERIALS_VS_URI = Constants.LABCODESET_URI_PREFIX + "/vs/labconcepts-materials";
  private static final String LABCODESET_MATERIALS_VS_TITLE = "Nederlandse Labcodeset Materials";

  private String labcodesetVersion;
  private String loincVersion;
  private TerminologyClient terminologyClient;
  private Map<String, MaterialDefinition> materialsMap;

  /**
   * @param labcodesetVersion version of the Labcodeset being transformed
   * @param loincVersion LOINC version the Labcodeset file should be used with
   * @param terminologyClient {@link TerminologyClient} that can be used to lookup details of LOINC
   *        codes
   * @param materialsMap Map of the materials references and their details in the Labcodeset file
   */
  public MaterialsResourceGenerator(String labcodesetVersion, String loincVersion, TerminologyClient terminologyClient,
      Map<String, MaterialDefinition> materialsMap) {
    this.labcodesetVersion = labcodesetVersion;
    this.loincVersion = loincVersion;
    this.terminologyClient = terminologyClient;
    this.materialsMap = materialsMap;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link ValueSet} containing the unique set of SNOMED CT codes referenced as Materials in
   *         Labcodeset
   */
  public ValueSet createMaterialsValueSet(Publication pub) {

    ValueSet valueSet = new ValueSet();
    String identifier = "Labconcepts-materials-" + labcodesetVersion;
    valueSet.setId(identifier);
    valueSet.setName(identifier).setVersion(labcodesetVersion).setTitle(LABCODESET_MATERIALS_VS_TITLE).setStatus(PublicationStatus.DRAFT)
        .setUrl(LABCODESET_MATERIALS_VS_URI).setDescription(LABCODESET_MATERIALS_VS_DESCRIPTION).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    ValueSetComposeComponent component = new ValueSetComposeComponent();

    List<ConceptSetComponent> theInclude = new ArrayList<ConceptSetComponent>();
    ConceptSetComponent conceptSet = new ConceptSetComponent();
    conceptSet.setSystem(Constants.SCT_CS_URI);
    conceptSet.setVersion(Constants.NL_SCT_EDITION);

    for (MaterialDefinition material : pub.getMaterials().getMaterial()) {
      if (material.getStatus() != null && !material.getStatus().equals(MaterialOrMethodStatus.ACTIVE)) {
        System.err.println("All materials should be active, yet material " + material.getCode() + " is " + material.getStatus());
      }

      ConceptReferenceComponent con = new ConceptReferenceComponent();

      con.setCode(material.getCode().toString());
      con.setDisplay(terminologyClient.getSnomedDisplay(material.getCode().toString(), material.getDisplayName()));
      conceptSet.addConcept(con);
    }

    theInclude.add(conceptSet);
    component.setInclude(theInclude);
    valueSet.setCompose(component);

    return valueSet;

  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link ConceptMap} containing all references from LOINC codes in Labcodeset to SNOMED CT
   *         Materials codes
   */
  public ConceptMap createMaterialsConceptMap(Publication pub) {

    ConceptMap map = new ConceptMap();

    String identifier = "Labconcepts-materials-" + labcodesetVersion;
    map.setId(identifier);
    map.setTitle(LABCODESET_MATERIALS_CM_TITLE).setVersion(labcodesetVersion).setName(identifier).setStatus(PublicationStatus.ACTIVE)
        .setUrl(LABCODESET_MATERIALS_CM_URI).setDescription(LABCODESET_MATERIALS_CM_DESCRIPTION).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    ConceptMapGroupComponent group = new ConceptMapGroupComponent();
    group.setSource(Constants.LOINC_CS_URI);
    group.setSourceVersion(loincVersion);
    group.setTarget(Constants.SCT_CS_URI);
    group.setTargetVersion(Constants.NL_SCT_EDITION);

    for (LabConcept labConcept : pub.getLabConcepts().getLabConcept()) {

      if (labConcept.getMaterials() != null) {
        for (Material material : labConcept.getMaterials().getMaterial()) {
          if (material.getStatus() != null && !material.getStatus().equals(MaterialOrMethodStatus.ACTIVE)) {
            System.err.println("All materials should be active, yet material " + material.getRef() + " on code "
                + labConcept.getLoincConcept().getLoincNum() + " is " + material.getStatus());
          }

          SourceElementComponent element = new SourceElementComponent();
          element.setCode(labConcept.getLoincConcept().getLoincNum());
          if (labConcept.getLoincConcept().getTranslation() != null
              && labConcept.getLoincConcept().getTranslation().getLongName() != null) {
            element.setDisplay(labConcept.getLoincConcept().getTranslation().getLongName().getValue());
          } else {
            element.setDisplay(labConcept.getLoincConcept().getLongName().getValue());
          }

          TargetElementComponent targetElement = new TargetElementComponent();
          MaterialDefinition cachedMaterial = materialsMap.get(material.getRef());
          targetElement.setCode(cachedMaterial.getCode().toString());
          targetElement.setDisplay(terminologyClient.getSnomedDisplay(targetElement.getCode(), cachedMaterial.getDisplayName()));
          targetElement.setEquivalence(ConceptMapEquivalence.RELATEDTO);
          element.addTarget(targetElement);
          group.addElement(element);
        }
      }
    }

    map.addGroup(group);

    return map;

  }
}
