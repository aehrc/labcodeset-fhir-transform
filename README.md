![Labcodeset transform build](https://github.com/aehrc/labcodeset-fhir-transform/workflows/Labcodeset%20transform%20build/badge.svg)

# Labcodeset to FHIR transformation

A stand-alone command-line tool that transforms Nederlandse Labcodeset XML to a collection of FHIR resources, specifically

| Type | URI | Description |
| ---- | --- | ----------- |
| CodeSystem supplement | http://labterminologie.nl/cs/labconcepts | CodeSystem supplement containing Dutch translations and properties as defined in the Labcodeset XML file |
| ValueSet | http://labterminologie.nl/vs/labconcepts | ValueSet containing the unique set of LOINC codes referenced by the Labcodeset XML file - note this contains LOINC codes in any state, not just active codes |
| CodeSystem fragment | http://unitsofmeasure.org | Fragment of the UCUM CodeSystem containing the union of UCUM expressions referenced by Labcodeset and the ["common" FHIR UCUM ValueSet](https://www.hl7.org/fhir/valueset-ucum-common.html). Language for this CodeSystem is nl_NL, and Dutch display terms are used where available with the English terms set as designations |
| ValueSet | http://labterminologie.nl/vs/labconcepts-ucum | ValueSet containing the unique set of UCUM expressions referenced by Labcodeset |
| ConceptMap | http://labterminologie.nl/cm/labconcepts-ucum | ConceptMap containing mappings for each LOINC code in Labcodeset where a unit is expressed |
| ValueSet | http://labterminologie.nl/vs/labconcepts-materials | ValueSet containing the unique set of SNOMED CT "material" codes referenced by Labcodeset |
| ConceptMap | http://labterminologie.nl/cm/labconcepts-materials | ConceptMap containing mappings from LOINC codes in Labcodeset that reference one or more SNOMED CT "material" codes |
| ValueSet | http://labterminologie.nl/vs/labconcepts-ordinal-xxx | A set of ValueSets, one for each "ordinal" list included in the Labcodeset XML file where xxx in the URI is replaced with the OID of the ordinal set |
| ConceptMap | http://labterminologie.nl/cm/labconcepts-outcomes | ConceptMap containing mappings from LOINC codes in Labcodeset to either a SNOMED CT Reference Set identifier or a ValueSet OID depending upon which is listed in Labcodeset for that LOINC code.
 

## Build
>mvn clean package

This will build a file called ./target/labcodeset-transforms-jar-with-dependencies.jar

## Run

The program requires a few command line parameters to execute, they are desribed by the usage message if you don't supply the correct parameters.

        usage: java -jar labcodeset-transforms-jar-with-dependencies.jar [-clientId <client_id>] [-clientSecret <client_secret>]
               -fhirEndpoint <fhir endpoint> -labcodesetFile <file_path> -loincVersion <loinc_version> [-outputDir <directory>]
               [-tokenEndpoint <token _endpoint>]
         -clientId <client_id>              Client id part of client credentials to use with the token endpoint to get a bearer
                                            token for use with the specified FHIR endpoin - optional if authorisation is
                                            required for the FHIR endpoint
         -clientSecret <client_secret>      Client secret part of client credentials to use with the token endpoint to get a
                                            bearer token for use with the specified FHIR endpoin  - optional if authorisation is
                                            required for the FHIR endpoint
         -fhirEndpoint <fhir endpoint>      FHIR terminology endpoint containing the appropriate LOINC and SNOMED CT-NL versions
                                            to reference during the transformation
         -labcodesetFile <file_path>        File path to the Labcodeset XML file to transform
         -loincVersion <loinc_version>      Version of LOINC this Labcodeset XML file has been built with
         -outputDir <directory>             Location to output the resulting FHIR resources and Bundle - defaults to the
                                            directory the program was executed from if not specified
         -tokenEndpoint <token _endpoint>   Token endpoint URL to get a bearer token with for the specified endpoint - optional
                                            if authorisation is required for the FHIR endpoint

Following is an example execution

        java -jar labcodeset-transforms-jar-with-dependencies.jar \
            -fhirEndpoint https://r4.ontoserver.csiro.au/fhir \
            -labcodesetFile /some/path/labcodeset.xml \
            -loincVersion 2.69

## Output
The transform will produce one JSON file for each of the generated resource as well as a Bundle resource JSON file containing all of the resources as a convenience.

The file names of the generated JSON files will contain the version number inside each of the resources, and the Bundle resource file name will carry the Labcodeset version it was generated from.
