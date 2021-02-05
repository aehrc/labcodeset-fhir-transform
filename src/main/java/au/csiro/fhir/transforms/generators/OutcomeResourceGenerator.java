/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.generators;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r4.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept;
import au.csiro.fhir.transform.xml.nl.labcodeset.Publication;
import au.csiro.fhir.transform.xml.nl.labcodeset.ValueSetDefinition;
import au.csiro.fhir.transform.xml.nl.labcodeset.ValueSetDefinition.ConceptList.Concept;
import au.csiro.fhir.transforms.utility.Constants;
import au.csiro.fhir.transforms.utility.TerminologyClient;

/**
 * Class that encapsulates the logic used to generate FHIR resources for Outcome links in Labcodeset
 */
public class OutcomeResourceGenerator {

  private static final String LABCODESET_OUTCOMES_CM_DESCRIPTION =
      "Map from Nederlandse Labcodeset LOINC codes to Outcomes SNOMED CT Reference Set identifiers and ValueSet OIDs";
  private static final String LABCODESET_OUTCOMES_CM_URI = Constants.LABCODESET_URI_PREFIX + "/cm/labconcepts-outcomes";
  private static final String LABCODESET_OUTCOMES_CM_TITLE = "Nederlandse Labcodeset Outcomes map";

  private String labcodesetVersion;
  private String loincVersion;

  /**
   * @param labcodesetVersion version of the Labcodeset being transformed
   * @param loincVersion LOINC version the Labcodeset file should be used with
   * @param terminologyClient {@link TerminologyClient} that can be used to lookup display terms of
   *        SNOMED CT codes
   */
  public OutcomeResourceGenerator(String labcodesetVersion, String loincVersion, TerminologyClient terminologyClient) {
    this.labcodesetVersion = labcodesetVersion;
    this.loincVersion = loincVersion;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link ConceptMap} containing all references from LOINC codes in Labcodeset to SNOMED CT
   *         Reference Set codes or ValueSet OIDs as specified in Labcodeset
   */
  public ConceptMap createOutcomesConceptMap(Publication pub) {
    ConceptMap map = new ConceptMap();

    String identifier = "Labconcepts-outcomes-" + labcodesetVersion;
    map.setId(identifier);
    map.setTitle(LABCODESET_OUTCOMES_CM_TITLE).setVersion(labcodesetVersion).setName(identifier).setStatus(PublicationStatus.ACTIVE)
        .setUrl(LABCODESET_OUTCOMES_CM_URI).setDescription(LABCODESET_OUTCOMES_CM_DESCRIPTION).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    ConceptMapGroupComponent sctGroup = new ConceptMapGroupComponent();
    sctGroup.setSource(Constants.LOINC_CS_URI);
    sctGroup.setSourceVersion(loincVersion);
    sctGroup.setTarget(Constants.SCT_CS_URI);
    sctGroup.setTargetVersion(Constants.NL_SCT_EDITION);

    ConceptMapGroupComponent oidGroup = new ConceptMapGroupComponent();
    oidGroup.setSource(Constants.LOINC_CS_URI);
    oidGroup.setSourceVersion(loincVersion);
    oidGroup.setTarget(Constants.OID_CS_URI);

    for (LabConcept labConcept : pub.getLabConcepts().getLabConcept()) {

      if (labConcept.getOutcomes() != null) {
        SourceElementComponent element = new SourceElementComponent();
        element.setCode(labConcept.getLoincConcept().getLoincNum());
        if (labConcept.getLoincConcept().getTranslation() != null && labConcept.getLoincConcept().getTranslation().getLongName() != null) {
          element.setDisplay(labConcept.getLoincConcept().getTranslation().getLongName().getValue());
        } else {
          element.setDisplay(labConcept.getLoincConcept().getLongName().getValue());
        }
        TargetElementComponent targetElement = new TargetElementComponent();
        element.addTarget(targetElement);
        targetElement.setEquivalence(ConceptMapEquivalence.RELATEDTO);

        if (labConcept.getOutcomes().getRefset() != null) {
          targetElement.setCode(labConcept.getOutcomes().getRefset().getConceptId().toString());
          targetElement.setDisplay(labConcept.getOutcomes().getRefset().getPreferredTerm());
          sctGroup.addElement(element);
        } else if (labConcept.getOutcomes().getValueSet() != null) {
          targetElement.setCode(labConcept.getOutcomes().getValueSet().getRef());
          oidGroup.addElement(element);
        } else {
          System.err.println("Unable to map outcome of LabConcept " + labConcept.getLoincConcept().getLoincNum());
        }
      }
    }

    map.addGroup(sctGroup);
    map.addGroup(oidGroup);

    return map;

  }

  /**
   * Creates an explicit FHIR ValueSet for each of the sets of codes described in the Ordinals section
   * of the Labcodeset XML
   * 
   * @param pub Parsed Labcodeset XML object
   * @return {@link Collection} of {@link ValueSet} obejcts for each of the "ordinal" lists in the
   *         Labcodeset
   */
  public Collection<ValueSet> createPublicationOutcomeValueSets(Publication pub) {
    Collection<ValueSet> outcomeValueSets = new ArrayList<>();
    for (ValueSetDefinition ordinal : pub.getOrdinals().getValueSet()) {
      ValueSet valueSet = new ValueSet();
      String identifier = "Labconcepts-" + ordinal.getId() + "-" + labcodesetVersion;
      valueSet.setId(identifier);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      String version = formatter.format(ordinal.getEffectiveDate().toGregorianCalendar().toZonedDateTime());
      valueSet.setName(identifier).setVersion(version).setTitle("Labcodeset ordinal '" + ordinal.getDisplayName() + "' set")
          .setStatus(PublicationStatus.ACTIVE).setUrl(Constants.LABCODESET_URI_PREFIX + "/labconcepts-ordinal-" + ordinal.getId())
          .setExperimental(false).setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER)
          .setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT).setLanguage(Constants.DUTCH_LANGUAGE_CODE);

      Identifier id = valueSet.addIdentifier();
      id.setSystem(Constants.OID_CS_URI);
      id.setId(ordinal.getId());

      ValueSetComposeComponent component = new ValueSetComposeComponent();
      valueSet.setCompose(component);

      ConceptSetComponent include = component.addInclude();
      include.setSystem(Constants.SCT_CS_URI);

      for (Concept concept : ordinal.getConceptList().getConcept()) {
        ConceptReferenceComponent includedConcept = include.addConcept();
        includedConcept.setCode(concept.getCode());
      }
      outcomeValueSets.add(valueSet);

    }

    return outcomeValueSets;
  }

}
