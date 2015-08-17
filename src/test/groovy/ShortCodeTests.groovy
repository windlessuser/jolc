import com.windlessuser.olc.OpenLocationCode
import org.apache.commons.csv.CSVFormat
import spock.lang.Specification

/**
 * Created by marc on 8/17/15.
 */
class ShortCodeTests extends Specification{

    def "Test shortening and extending codes"(){
        setup: "Creating the olc"
        OpenLocationCode olc = new OpenLocationCode()

        expect:
        olc.shorten(code,latitude,longitude) == shortCode
        olc.recoverNearest(shortCode,latitude,longitude) == code

        where:
        record << CSVFormat.EXCEL.parse( new FileReader(ValidityTests.class.getResource("ShortCodeTests.csv").file)).records;
        code = record.get(0)
        latitude = Double.parseDouble(record.get(1))
        longitude = Double.parseDouble(record.get(2))
        shortCode = record.get(3)

    }
}
