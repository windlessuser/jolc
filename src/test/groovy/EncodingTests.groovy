import com.windlessuser.olc.OpenLocationCode
import org.apache.commons.csv.CSVFormat
import spock.lang.Specification
/**
 * Created by marc on 8/14/15.
 */
class EncodingTests extends Specification{


    def "Encoding and Decoding tests"(){
        setup: "Creating the olc"
        OpenLocationCode olc = new OpenLocationCode()

        expect:
        olc.encode(latitude,longitude,0).equalsIgnoreCase(code)
        validateDecoding(olc.decode(code),latitudeHi,latitudeLo,longitudeHi,latitudeLo)

        where:
        record << CSVFormat.EXCEL.parse( new FileReader(ValidityTests.class.getResource("EncodingTests.csv").file)).records;
        code = record.get(0)
        latitude = Double.parseDouble(record.get(1))
        longitude = Double.parseDouble(record.get(2))
        latitudeLo = Double.parseDouble(record.get(3))
        longitudeLo = Double.parseDouble(record.get(4))
        latitudeHi = Double.parseDouble(record.get(5))
        longitudeHi = Double.parseDouble(record.get(6))
    }

    def validateDecoding(OpenLocationCode.CodeArea grid, latHi,latLo,lonHi,lonLow){
        assert grid.latitudeHi == latHi
        assert grid.latitudeLo == latLo
        assert grid.longitudeHi == lonHi
        assert grid.longitudeLo == lonLow
    }

}
