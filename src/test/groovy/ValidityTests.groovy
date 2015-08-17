import com.windlessuser.olc.OpenLocationCode
import org.apache.commons.csv.CSVFormat
import spock.lang.Specification

/**
 * Created by marc on 8/14/15.
 */
class ValidityTests extends Specification {

    def "Testing Valid full codes"(){
        setup: "Creating the olc"
        OpenLocationCode olc = new OpenLocationCode()

        expect:
        olc.isValid(code) == isValid
        olc.isShort(code) == isShort
        olc.isFull(code) == isFull

        where:
        record << CSVFormat.EXCEL.parse( new FileReader(ValidityTests.class.getResource("ValidFullCodes.csv").file)).records;
        code = record.get(0)
        isValid = Boolean.parseBoolean(record.get(1).toUpperCase())
        isShort = Boolean.parseBoolean record.get(2).toUpperCase()
        isFull = Boolean.parseBoolean record.get(3).toUpperCase()

    }

    def "Testing Valid short codes"(){
        setup: "Creating the olc"
        OpenLocationCode olc = new OpenLocationCode()

        expect:
        olc.isValid(code) == isValid
        olc.isShort(code) == isShort
        olc.isFull(code) == isFull

        where:
        record << CSVFormat.EXCEL.parse( new FileReader(ValidityTests.class.getResource("ValidShortCodes.csv").file)).records;
        code = record.get(0)
        isValid = Boolean.parseBoolean(record.get(1).toUpperCase())
        isShort = Boolean.parseBoolean record.get(2).toUpperCase()
        isFull = Boolean.parseBoolean record.get(3).toUpperCase()

    }

    def "Should fail on invalid codes"(){
        setup: "Creating the olc"
        OpenLocationCode olc = new OpenLocationCode()

        expect:
        olc.isValid(code) == isValid
        olc.isShort(code) == isShort
        olc.isFull(code) == isFull

        where:
        record << CSVFormat.EXCEL.parse( new FileReader(ValidityTests.class.getResource("InvalidCodes.csv").file)).records;
        code = record.get(0)
        isValid = Boolean.parseBoolean(record.get(1).toUpperCase())
        isShort = Boolean.parseBoolean record.get(2).toUpperCase()
        isFull = Boolean.parseBoolean record.get(3).toUpperCase()

    }
}
