/*******************************************************************************
 * Copyright © 2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 ******************************************************************************/
package au.csiro.fhir.transforms.generators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeSystem.ConceptPropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Materials;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Materials.Material;
import au.csiro.fhir.transform.xml.nl.labcodeset.LabConcept.Units;
import au.csiro.fhir.transform.xml.nl.labcodeset.LoincAxis;
import au.csiro.fhir.transform.xml.nl.labcodeset.LoincConcept;
import au.csiro.fhir.transform.xml.nl.labcodeset.Publication;
import au.csiro.fhir.transform.xml.nl.labcodeset.UnitDefinition;
import au.csiro.fhir.transforms.utility.Constants;
import au.csiro.fhir.transforms.utility.TerminologyClient;

/**
 * Class that encapsulates the logic used to generate LOINC related FHIR resources for Labcodeset
 */
public class LoincResourceGenerator {

  private static final String LABCODESET_VS_DESCRIPTION = "Unique set of LOINC codes referenced by Nederlandse Labcodeset";
  private static final String LABCODESET_VS_TITLE = "Nederlandse Labcodeset";
  private static final String LABCODESET_VS_URI = Constants.LABCODESET_URI_PREFIX + "/vs/labconcepts";
  private static final String LABCODESET_VS_OID = "2.16.840.1.113883.2.4.3.11.22.250";

  private static final String LABCODESET_SUPPLEMENT_CS_TITLE = "Nederlandse Labcodeset LOINC Supplement";
  private static final String LABCODESET_SUPPLEMENT_CS_DESCRIPTION =
      "Supplement to LOINC incorporating Dutch translations and Nederlandse Labcodeset additional properties";
  private static final String LABCODESET_SUPPLEMENT_CS_URI = Constants.LABCODESET_URI_PREFIX + "/cs/labconcepts";

  private static final String LOINC_COMPONENT = "COMPONENT";
  private static final String LOINC_PROPERTY = "PROPERTY";
  private static final String LOINC_SCALE_TYP = "SCALE_TYP";
  private static final String LOINC_SYSTEM = "SYSTEM";
  private static final String LOINC_TIME_ASPCT = "TIME_ASPCT";
  private static final String LOINC_METHOD_TYP = "METHOD_TYP";
  private static final String LOINC_ORDER_OBS = "ORDER_OBS";
  private static final String LOINC_CLASS = "CLASS";

  private static final String LABCODESET_STATUS_PROPERTY = "STATUS";
  private static final String LABCODESET_MATERIAL_PROPERTY = "MATERIAL";
  private static final String LABCODESET_UNITS_PROPERTY = "EXAMPLE_UCUM_UNITS";

  private String labcodesetVersion;
  private String loincVersion;
  private TerminologyClient terminologyClient;
  private Map<String, UnitDefinition> unitMap;

  /**
   * @param labcodesetVersion version of the Labcodeset being transformed
   * @param loincVersion LOINC version the Labcodeset file should be used with
   * @param terminologyClient {@link TerminologyClient} that can be used to lookup details of LOINC
   *        codes
   * @param unitMap Map of the unit references and their details in the Labcodeset file
   */
  public LoincResourceGenerator(String labcodesetVersion, String loincVersion, TerminologyClient terminologyClient,
      Map<String, UnitDefinition> unitMap) {
    this.labcodesetVersion = labcodesetVersion;
    this.loincVersion = loincVersion;
    this.terminologyClient = terminologyClient;
    this.unitMap = unitMap;
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return a {@link CodeSystem} object containing the generated supplement to LOINC for the passed
   *         Labcodeset version
   */
  public CodeSystem createLoincCodeSystemSupplement(Publication pub) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId("labconcepts");
    codeSystem.setUrl(LABCODESET_SUPPLEMENT_CS_URI).setDescription(LABCODESET_SUPPLEMENT_CS_DESCRIPTION).setName("Labconcepts")
        .setVersion(labcodesetVersion).setTitle(LABCODESET_SUPPLEMENT_CS_TITLE).setStatus(PublicationStatus.ACTIVE).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setContent(CodeSystemContentMode.SUPPLEMENT)
        .setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT).setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    codeSystem.setSupplements(Constants.LOINC_CS_URI + "|" + loincVersion);

    codeSystem.addProperty(createProperty(LABCODESET_STATUS_PROPERTY, "Labcodeset LOINC concept status", PropertyType.STRING));
    codeSystem.addProperty(createProperty(LABCODESET_MATERIAL_PROPERTY, "Labcodeset SNOMED CT material", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LABCODESET_UNITS_PROPERTY, "Labcodeset UCUM units", PropertyType.CODING));

    codeSystem.addProperty(createProperty(LOINC_CLASS, "Labcodeset translation of LOINC CLASS", PropertyType.STRING));
    codeSystem.addProperty(createProperty(LOINC_ORDER_OBS, "Labcodeset translation of LOINC ORDER_OBS", PropertyType.STRING));
    codeSystem.addProperty(createProperty(LOINC_METHOD_TYP, "Labcodeset translation of LOINC METHOD_TYP", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LOINC_TIME_ASPCT, "Labcodeset translation of LOINC TIME_ASPCT", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LOINC_SYSTEM, "Labcodeset translation of LOINC SYSTEM", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LOINC_SCALE_TYP, "Labcodeset translation of LOINC SCALE", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LOINC_PROPERTY, "Labcodeset translation of LOINC PROPERTY", PropertyType.CODING));
    codeSystem.addProperty(createProperty(LOINC_COMPONENT, "Labcodeset translation of LOINC COMPONENT", PropertyType.CODING));

    List<ConceptDefinitionComponent> concepts = new ArrayList<CodeSystem.ConceptDefinitionComponent>();

    Set<String> processedLoincParts = new HashSet<>();
    for (LabConcept labConcept : pub.getLabConcepts().getLabConcept()) {
      ConceptDefinitionComponent concept = new ConceptDefinitionComponent();
      concept.addProperty(
          new ConceptPropertyComponent(new CodeType(LABCODESET_STATUS_PROPERTY), new StringType(labConcept.getStatus().value())));

      setLoincConceptProperties(labConcept.getLoincConcept(), concept, concepts, processedLoincParts);

      setMaterialProperties(labConcept.getMaterials(), concept);

      setUnitProperties(labConcept.getUnits(), concept);

      concepts.add(concept);

    }
    codeSystem.setConcept(concepts);

    return codeSystem;
  }

  private PropertyComponent createProperty(String name, String desc, PropertyType type) {
    PropertyComponent propertyComponent = new PropertyComponent();
    propertyComponent.setCode(name).setDescription(desc).setType(type);
    return propertyComponent;
  }

  private void setUnitProperties(Units units, ConceptDefinitionComponent concept) {
    if (units != null && units.getUnit() != null) {
      UnitDefinition unit = unitMap.get(units.getUnit().getRef());
      if (unit != null) {
        concept.addProperty(new ConceptPropertyComponent(new CodeType(LABCODESET_UNITS_PROPERTY),
            new Coding(Constants.UCUM_CS_URI, unit.getRm().toString(), unit.getNlname().toString())));
      } else {
        System.err.println("Could not find unit for reference " + units.getUnit().getRef() + " - omitting this property!");
      }
    }
  }

  private void setMaterialProperties(Materials materials, ConceptDefinitionComponent concept) {
    if (materials != null && materials.getMaterial() != null) {
      for (Material material : materials.getMaterial()) {
        concept.addProperty(new ConceptPropertyComponent(new CodeType(LABCODESET_MATERIAL_PROPERTY),
            new Coding(Constants.SCT_CS_URI, material.getCode().toString(),
                terminologyClient.getSnomedDisplay(material.getCode().toString(), material.getDisplayName()))));
      }
    }
  }

  private void setLoincConceptProperties(LoincConcept loincConcept, ConceptDefinitionComponent concept,
      List<ConceptDefinitionComponent> concepts, Set<String> processedLoincParts) {
    concept.setCode(loincConcept.getLoincNum());
    LoincConcept translation = loincConcept.getTranslation();

    if (translation == null) {
      System.err.println("No translation for " + loincConcept.getLoincNum());
    } else {
      if (translation.getLongName() != null) {
        concept.setDisplay(translation.getLongName().getValue());
        concept.addDesignation().setLanguage("en").setValue(loincConcept.getLongName().getValue())
            .setUse(new Coding(Constants.SCT_CS_URI, "900000000000013009", "Synonym"));
      } else {
        concept.setDisplay(loincConcept.getLongName().getValue());
        System.err.println("No translated long name for " + loincConcept.getLoincNum());
      }

      setOptionalAxisStringProperty(concept, translation.getOrderObs(), LOINC_ORDER_OBS);
      setOptionalAxisStringProperty(concept, translation.getClazz(), LOINC_CLASS);

      Parameters param = terminologyClient.getLoincConcept(concept.getCode(), loincVersion);

      for (ParametersParameterComponent parameter : param.getParameter()) {
        if (parameter.getName().equals("property") && parameter.getPart().size() == 2) {
          ParametersParameterComponent propertyName = parameter.getPart().get(0);
          ParametersParameterComponent propertyValue = parameter.getPart().get(1);

          LoincAxis axis = null;
          switch (propertyName.getValue().toString()) {
            case LOINC_METHOD_TYP:
              axis = translation.getMethod();
              break;
            case LOINC_TIME_ASPCT:
              axis = translation.getTiming();
              break;
            case LOINC_SYSTEM:
              axis = translation.getSystem();
              break;
            case LOINC_SCALE_TYP:
              axis = translation.getScale();
              break;
            case LOINC_PROPERTY:
              axis = translation.getProperty();
              break;
            case LOINC_COMPONENT:
              axis = translation.getComponent();
              break;
            default:
              break;
          }
          if (propertyValue.getValue() != null && axis != null) {
            concept.addProperty(new ConceptPropertyComponent(new CodeType(propertyName.getValue().toString()),
                new Coding(Constants.LOINC_CS_URI, propertyValue.getValue().toString(), axis.getValue())));

            if (!processedLoincParts.contains(propertyValue.getValue().toString())) {
              ConceptDefinitionComponent loincPartTranslation = new ConceptDefinitionComponent();
              loincPartTranslation.setCode(propertyValue.getValue().toString());
              loincPartTranslation.setDisplay(axis.getValue());
              concepts.add(loincPartTranslation);
              processedLoincParts.add(propertyValue.getValue().toString());
            }
          } else if (propertyValue.getValue() == null && axis != null) {
            System.err.println("Not setting translation " + axis.getValue() + " for property " + propertyName.getValue().toString() + " of "
                + loincConcept.getLoincNum()
                + " - LOINC part code cannot be retrieved for LOINC code, does this property exist on the base LOINC code?");
          }
        }
      }
    }
  }


  private void setOptionalAxisStringProperty(ConceptDefinitionComponent concept, LoincAxis axis, String propertyName) {
    if (axis != null) {
      concept.addProperty(new ConceptPropertyComponent(new CodeType(propertyName), new StringType(axis.getValue())));
    }
  }

  /**
   * @param pub Parsed Labcodeset XML object
   * @return a {@link ValueSet} object containing the unique set of LOINC codes referenced by
   *         Labcodeset
   */
  public ValueSet createValueSetPublication(Publication pub) {

    ValueSet valueSet = new ValueSet();
    String identifier = "Labconcepts" + labcodesetVersion;
    valueSet.setId(identifier);
    valueSet.setName(identifier).setVersion(labcodesetVersion).setTitle(LABCODESET_VS_TITLE).setStatus(PublicationStatus.ACTIVE)
        .setUrl(LABCODESET_VS_URI).setDescription(LABCODESET_VS_DESCRIPTION + " " + labcodesetVersion).setExperimental(false)
        .setPublisher(Constants.LABCODESET_RESOURCE_PUBLISHER).setCopyright(Constants.LABCODESET_RESOURCE_COPYRIGHT)
        .setLanguage(Constants.DUTCH_LANGUAGE_CODE);

    valueSet.addIdentifier().setSystem(Constants.OID_CS_URI).setValue(LABCODESET_VS_OID);

    ValueSetComposeComponent component = new ValueSetComposeComponent();

    ConceptSetComponent conceptSet = component.addInclude();
    conceptSet.setSystem(Constants.LOINC_CS_URI);
    conceptSet.setVersion(loincVersion);
    for (LabConcept labConcept : pub.getLabConcepts().getLabConcept()) {
      ConceptReferenceComponent con = new ConceptReferenceComponent();

      con.setCode(labConcept.getLoincConcept().getLoincNum());

      conceptSet.addConcept(con);
    }
    valueSet.setCompose(component);

    List<Extension> theExtension = new ArrayList<Extension>();

    Extension loincTranslate = new Extension();

    loincTranslate.setUrl("http://hl7.org/fhir/StructureDefinition/valueset-supplement");
    loincTranslate.setValue(new CanonicalType().setValue(LABCODESET_SUPPLEMENT_CS_URI + "|" + labcodesetVersion));

    theExtension.add(loincTranslate);

    valueSet.setExtension(theExtension);

    return valueSet;
  }
}
