/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.generators;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
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
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Units;
import au.csiro.fhir.transform.xml.nl.labcodeset.Publication;
import au.csiro.fhir.transform.xml.nl.labcodeset.UnitDefinition;
import au.csiro.fhir.transforms.utility.Constants;
import au.csiro.fhir.transforms.utility.TerminologyClient;

public class UcumResourceGenerator {

  private static final String LABCODESET_UCUM_CM_DESCRIPTION = "Map from Nederlandse Labcodeset LOINC to UCUM units";
  private static final String LABCODESET_UCUM_CM_URI = Constants.LABCODESET_URI_PREFIX + "/cm/labconcepts-ucum";
  private static final String LABCODESET_UCUM_CM_TITLE = "Nederlandse Labcodeset UCUM ConceptMap";

  private static final String LABCODESET_UCUM_VS_TITLE = "Nederlandse UCUM";
  private static final String LABCODESET_UCUM_VS_URI = Constants.LABCODESET_URI_PREFIX + "/vs/labconcepts-ucum";
  private static final String LABCODESET_UCUM_VS_DESCRIPTION = "Nederlandse Labcodeset UCUM codes";

  private static final String UCUM_FRAGMENT_TITLE = "Unified Code for Units of Measure (UCUM)";
  private static final String UCUM_FRAGMENT_DESCRIPTION =
      "Fragment of the Unified Code for Units of Measure (UCUM) representing all commonly used UCUM codes specified in the "
          + "FHIR specification at https://www.hl7.org/fhir/valueset-ucum-common.html with additional Nederlandse Labcodeset expressions and Dutch translations";

  private String labcodesetVersion;
  private String loincVersion;
  private Map<String, UnitDefinition> unitMap;

  /**
   * @param labcodesetVersion version of the Labcodeset being transformed
   * @param loincVersion LOINC version the Labcodeset file should be used with
   * @param terminologyClient {@link TerminologyClient} that can be used to lookup details of LOINC
   *        codes
   * @param unitMap Map of the unit references and their details in the Labcodeset file
   */
  public UcumResourceGenerator(String labcodesetVersion, String loincVersion, Map<String, UnitDefinition> unitMap) {
    this.labcodesetVersion = labcodesetVersion;
    this.loincVersion = loincVersion;
    this.unitMap = unitMap;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link CodeSystem} resource containing the unique union of the Labcodeset UCUM
   *         expressions and expressions in the FHIR specification common UCUM codes with Dutch
   *         display names where available and English designations.
   */
  public CodeSystem createUcumCodeSystem(Publication pub) {
    CodeSystem codeSystem = new CodeSystem();
    String csIdentifier = "Ucum-" + Constants.UCUM_VERSION;
    codeSystem.setId(csIdentifier);
    codeSystem.setUrl(Constants.UCUM_CS_URI).setValueSet(Constants.UCUM_CS_URI + "/vs").setDescription(UCUM_FRAGMENT_DESCRIPTION)
        .setVersion(Constants.UCUM_VERSION).setTitle(UCUM_FRAGMENT_TITLE).setName(csIdentifier).setStatus(PublicationStatus.ACTIVE)
        .setExperimental(false).setContent(CodeSystemContentMode.FRAGMENT).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    Set<String> nlUcumCodeset = new HashSet<>();

    for (Entry<String, UnitDefinition> entry : unitMap.entrySet()) {
      UnitDefinition code = entry.getValue();
      if (!nlUcumCodeset.contains(code.getRm())) {
        ConceptDefinitionComponent ucumCode = codeSystem.addConcept();

        if (code.getRm() == null) {
          System.err.println("UCUM unit reference " + entry.getKey() + " has no defined UCUM expression - cannot continue");
          System.exit(1);
        }

        if (code.getNlname() == null) {
          System.err.println("UCUM unit reference " + entry.getKey() + " has no defined Dutch name - cannot continue");
          System.exit(1);
        }

        ucumCode.setCode(code.getRm());
        ucumCode.setDisplay(code.getNlname());

        if (code.getName() != null) {
          ucumCode.addDesignation().setLanguage("en").setValue(code.getName());
        } else {
          System.err.println("UCUM expression " + code.getRm() + " has no defined English name in Labcodeset");
        }

        nlUcumCodeset.add(code.getRm());
      }
    }

    ValueSet commonUcum = null;
    try {
      commonUcum = TerminologyClient.getCommonUcumCodes();
    } catch (IOException e) {
      System.err.println("Unable to get common UCUM codes from the FHIR spec due to " + e.getLocalizedMessage());
      System.exit(1);
    }

    for (ConceptSetComponent commonInclude : commonUcum.getCompose().getInclude()) {
      for (ConceptReferenceComponent concept : commonInclude.getConcept()) {
        if (!nlUcumCodeset.contains(concept.getCode())) {
          ConceptDefinitionComponent ucumCode = codeSystem.addConcept();
          ucumCode.setCode(concept.getCode());
          ucumCode.setDisplay(concept.getDisplay());

          nlUcumCodeset.add(concept.getCode());
        }
      }
    }

    return codeSystem;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link ValueSet} containing the unique set of UCUM expressions referenced by Labcodeset
   */
  public ValueSet createUcumValueSet(Publication pub) {
    ValueSet valueSet = new ValueSet();
    String vsIdentifier = "Labconcepts-ucum-" + labcodesetVersion;
    valueSet.setId(vsIdentifier);
    valueSet.setName(vsIdentifier).setVersion(labcodesetVersion).setTitle(LABCODESET_UCUM_VS_TITLE).setStatus(PublicationStatus.ACTIVE)
        .setUrl(LABCODESET_UCUM_VS_URI).setDescription(LABCODESET_UCUM_VS_DESCRIPTION).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    ValueSetComposeComponent component = new ValueSetComposeComponent();
    valueSet.setCompose(component);

    ConceptSetComponent include = component.addInclude();
    include.setSystem(Constants.UCUM_CS_URI);

    Set<String> nlUcumCodeset = new HashSet<>();

    for (UnitDefinition entry : unitMap.values()) {
      if (!nlUcumCodeset.contains(entry.getRm())) {
        ConceptReferenceComponent reference = include.addConcept();
        reference.setCode(entry.getRm());
        reference.setDisplay(entry.getNlname());

        nlUcumCodeset.add(entry.getRm());
      }
    }
    return valueSet;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return {@link ConceptMap} containing maps from LOINC codes to UCUM units as expressed in the
   *         references in Labcodeset
   */
  public ConceptMap createUcumMap(Publication pub) {
    ConceptMap map = new ConceptMap();

    String identifier = "Labconcepts-ucum-" + labcodesetVersion;
    map.setId(identifier);
    map.setTitle(LABCODESET_UCUM_CM_TITLE).setVersion(labcodesetVersion).setName(identifier).setStatus(PublicationStatus.ACTIVE)
        .setUrl(LABCODESET_UCUM_CM_URI).setDescription(LABCODESET_UCUM_CM_DESCRIPTION).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    ConceptMapGroupComponent group = new ConceptMapGroupComponent();
    group.setSource(Constants.LOINC_CS_URI);
    group.setSourceVersion(loincVersion);
    group.setTarget(Constants.UCUM_CS_URI);
    group.setTargetVersion(Constants.UCUM_VERSION);

    for (LabConcept labConcept : pub.getLabConcepts().getLabConcept()) {
      Units units = labConcept.getUnits();
      if (units != null) {
        UnitDefinition unit = unitMap.get(units.getUnit().getRef());

        if (unit != null) {
          SourceElementComponent element = new SourceElementComponent();
          element.setCode(labConcept.getLoincConcept().getLoincNum());
          if (labConcept.getLoincConcept().getTranslation() != null
              && labConcept.getLoincConcept().getTranslation().getLongName() != null) {
            element.setDisplay(labConcept.getLoincConcept().getTranslation().getLongName().getValue());
          } else {
            element.setDisplay(labConcept.getLoincConcept().getLongName().getValue());
          }
          TargetElementComponent targetElement = new TargetElementComponent();
          element.addTarget(targetElement);
          targetElement.setEquivalence(ConceptMapEquivalence.RELATEDTO);

          targetElement.setCode(unit.getRm());
          targetElement.setDisplay(unit.getNlname());
          group.addElement(element);
        } else {
          System.err.println(
              "Unable to find unit for reference " + units.getUnit().getRef() + " on " + labConcept.getLoincConcept().getLoincNum());
        }
      }
    }

    map.addGroup(group);

    return map;

  }
}
