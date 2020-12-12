import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReadInventoryCsvTest {

    static String CUSTOM_INVENTORY_FILE = "TestCustomHospitalInventory.csv";

    ReadInventoryCsv testTarget;

    @BeforeEach
    public void setup() {
        testTarget = new ReadInventoryCsv();
    }

    @Test
    public void testExample() {
        var file = this.getClass().getResource(CUSTOM_INVENTORY_FILE).getFile();
        var bundle = testTarget.readInventoryCsv(file);

        assertEquals(23, bundle.getEntry().size());
    }
}
