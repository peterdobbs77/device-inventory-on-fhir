import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java.util.Optional;

public class ReadInventoryCsv {

    static String CUSTOM_INVENTORY_DEVICE_LOCATION = "Location";
    static String CUSTOM_INVENTORY_DEVICE_CODE = "Code";
    static String CUSTOM_INVENTORY_DEVICE_SERIAL_NUMBER = "Serial Number";
    static String CUSTOM_INVENTORY_DEVICE_EQUIPMENT_TYPE = "Equipment Type";
    static String CUSTOM_INVENTORY_DEVICE_MODEL = "Model";
    static String CUSTOM_INVENTORY_DEVICE_STATUS = "Status";
    static String CUSTOM_INVENTORY_DEVICE_MANUFACTURER = "Manufacturer";

    static String[] CUSTOM_INVENTORY_HEADERS = {
        CUSTOM_INVENTORY_DEVICE_LOCATION,
        CUSTOM_INVENTORY_DEVICE_CODE,
        CUSTOM_INVENTORY_DEVICE_SERIAL_NUMBER,
        CUSTOM_INVENTORY_DEVICE_EQUIPMENT_TYPE,
        CUSTOM_INVENTORY_DEVICE_MODEL,
        CUSTOM_INVENTORY_DEVICE_STATUS,
        CUSTOM_INVENTORY_DEVICE_MANUFACTURER
    };

    static CodeableConcept IDENTIFIER_TYPE_SERIAL_NUMBER =
        new CodeableConcept(new Coding()
            .setCode("SNO")
            .setDisplay("Serial Number")
            .setSystem("http://hl7.org/fhir/ValueSet/identifier-type"));

    private static Device customInventoryRecordToDevice(CSVRecord record) {
        var device = new Device();

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_SERIAL_NUMBER))
            .map(serialNumber -> new Identifier()
                .setValue(serialNumber)
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setType(IDENTIFIER_TYPE_SERIAL_NUMBER))
            .ifPresent(serialNumberIdentifier -> {
                device.addIdentifier(serialNumberIdentifier);
                device.setSerialNumber(serialNumberIdentifier.getValue());
            });

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_CODE))
            .map(code -> new Identifier()
                .setValue(code)
                .setUse(Identifier.IdentifierUse.SECONDARY))
            .ifPresent(device::addIdentifier);

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_STATUS))
            .map(status -> {
                switch(status){
                    case "Operational":
                        return Device.FHIRDeviceStatus.ACTIVE;
                    case "UnderMaintenance":
                        return Device.FHIRDeviceStatus.INACTIVE;
                    default:
                        return Device.FHIRDeviceStatus.UNKNOWN;
                }
            })
            .ifPresent(device::setStatus);

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_MANUFACTURER))
            .ifPresent(device::setManufacturer);

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_MODEL))
            .ifPresent(device::setModelNumber);

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_EQUIPMENT_TYPE))
            .map(equipmentType -> new Coding()
                .setCode(equipmentType.replaceAll("\\s",""))
                .setDisplay(equipmentType)
                .setSystem("http://ewh.org"))
            .map(CodeableConcept::new)
            .ifPresent(device::setType);

        Optional
            .ofNullable(record.get(CUSTOM_INVENTORY_DEVICE_LOCATION))
            .map(location -> new Reference()
                .setDisplay(location)
                .setType(Location.class.getSimpleName()))
            .ifPresent(device::setLocation);

        return device;
    }

    public Bundle readInventoryCsv(String filename) {
        // create a reader
        try (Reader reader = new FileReader(filename)) {

            Bundle devices = new Bundle();
            devices.setType(Bundle.BundleType.TRANSACTION);

            // read csv file
            CSVFormat.EXCEL
                .withHeader(CUSTOM_INVENTORY_HEADERS)
                .withFirstRecordAsHeader()
                .parse(reader)
                .forEach(record -> {
                    var device = customInventoryRecordToDevice(record);
                    devices.addEntry()
                        .setFullUrl(device.getIdElement().getValue())
                        .setResource(device)
                        .getRequest().setUrl("Device")
                        .setIfNoneExist("identifier=http://acme.org/udi|000")
                        .setMethod(Bundle.HTTPVerb.POST);
                });

//            // log the result
//            System.out.println(FhirContext.forR4()
//                .newJsonParser()
//                .setPrettyPrint(true)
//                .encodeResourceToString(devices));
//
//            var client = FhirContext.forR4()
//                .newRestfulGenericClient("http://localhost:8080/baseR4");
//            var response = client.transaction()
//                .withBundle(devices)
//                .execute();
//
//            // log the response
//            System.out.println(FhirContext.forR4()
//                .newJsonParser()
//                .setPrettyPrint(true)
//                .encodeResourceToString(response));

            return devices;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
