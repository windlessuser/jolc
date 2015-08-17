package com.windlessuser.olc;

/**
 * // Licensed under the Apache License, Version 2.0 (the 'License');
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * // http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an 'AS IS' BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 * <p/>
 * /**
 * Convert locations to and from short codes.
 * Open Location Codes are short, 10-11 character codes that can be used instead
 * of street addresses. The codes can be generated and decoded offline, and use
 * a reduced character set that minimises the chance of codes including words.
 * Codes are able to be shortened relative to a nearby location. This means that
 * in many cases, only four to seven characters of the code are needed.
 * To recover the original code, the same location is not required, as long as
 * a nearby location is provided.
 * Codes represent rectangular areas rather than points, and the longer the
 * code, the smaller the area. A 10 character code represents a 13.5x13.5
 * meter area (at the equator. An 11 character code represents approximately
 * a 2.8x3.5 meter area.
 * Two encoding algorithms are used. The first 10 characters are pairs of
 * characters, one for latitude and one for latitude, using base 20. Each pair
 * reduces the area of the code by a factor of 400. Only even code lengths are
 * sensible, since an odd-numbered length would have sides in a ratio of 20:1.
 * At position 11, the algorithm changes so that each character selects one
 * position from a 4x5 grid. This allows single-character refinements.
 * <p/>
 *
 * @author Marc Byfield
 * @version 0.1.0
 *
 */
public class OpenLocationCode {

    // A separator used to break the code into two parts to aid memorability.
    public static final String SEPARATOR_ = "+";

    // The number of characters to place before the separator.
    public static final int SEPARATOR_POSITION_ = 8;

    // The character used to pad codes.
    public static final String PADDING_CHARACTER_ = "0";

    // The character set used to encode the values.
    public static final String CODE_ALPHABET_ = "23456789CFGHJMPQRVWX";

    // The base to use to convert numbers to/from.
    public static final int ENCODING_BASE_ = CODE_ALPHABET_.length();

    // The maximum value for latitude in degrees.
    public static final int LATITUDE_MAX_ = 90;

    // The maximum value for longitude in degrees.
    public static final int LONGITUDE_MAX_ = 180;

    // Maximum code length using lat/lng pair encoding. The area of such a
    // code is approximately 13x13 meters (at the equator), and should be suitable
    // for identifying buildings. This excludes prefix and separator characters.
    public static final int PAIR_CODE_LENGTH_ = 10;

    // The resolution values in degrees for each position in the lat/lng pair
    // encoding. These give the place value of each position, and therefore the
    // dimensions of the resulting area.
    public static final double[] PAIR_RESOLUTIONS_ = {20.0, 1.0, .05, .0025, .000125};

    // Number of columns in the grid refinement method.
    public static final int GRID_COLUMNS_ = 4;

    // Number of rows in the grid refinement method.
    public static final int GRID_ROWS_ = 5;

    // Size of the initial grid in degrees.
    public static final double GRID_SIZE_DEGREES_ = 0.000125;

    // Minimum length of a code that can be shortened.
    public static final int MIN_TRIMMABLE_CODE_LEN_ = 6;


    public String getAlphabet() {
        return CODE_ALPHABET_;
    }


    /**
     * Determines if a code is valid.
     * To be valid, all characters must be from the Open Location Code character
     * set with at most one separator. The separator can be in any even-numbered
     * position up to the eighth digit.
     */
    public boolean isValid(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }

        // The separator is required.
        if (!code.contains(SEPARATOR_)) {
            return false;
        }
        if (code.indexOf(SEPARATOR_) != code.lastIndexOf(SEPARATOR_)) {
            return false;
        }
        // Is it the only character?
        if (code.length() == 1) {
            return false;
        }
        // Is it in an illegal position?
        if (code.indexOf(SEPARATOR_) > SEPARATOR_POSITION_ ||
                code.indexOf(SEPARATOR_) % 2 == 1) {
            return false;
        }
        // We can have an even number of padding characters before the separator,
        // but then it must be the final character.
        if (code.contains(PADDING_CHARACTER_)) {
            // Not allowed to start with them!
            if (code.indexOf(PADDING_CHARACTER_) == 0) {
                return false;
            }

            String[] padMatch = code.split("(" + PADDING_CHARACTER_ + "+)", 'g');
            if (padMatch.length > 1 || padMatch[0].length() % 2 == 1 ||
                    padMatch[0].length() > SEPARATOR_POSITION_ - 2) {
                return false;
            }
            // If the code is long enough to end with a separator, make sure it does.
            if (code.charAt(code.length() - 1) != SEPARATOR_.charAt(0)) {
                return false;
            }
        }

        // If there are characters after the separator, make sure there isn't just
        // one of them (not legal).
        if (code.length() - code.indexOf(SEPARATOR_) - 1 == 1) {
            return false;
        }

        // Strip the separator and any padding characters.
        code = code.replaceFirst("\\\\" + SEPARATOR_ + "+", "").replace(PADDING_CHARACTER_ + "+", "");
        // Check the code contains only valid characters.
        for (int i = 0, len = code.length(); i < len; i++) {
            char character = code.toUpperCase().charAt(i);
            if (character != SEPARATOR_.charAt(0) && CODE_ALPHABET_.indexOf(character) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if a code is a valid short code.
     * A short Open Location Code is a sequence created by removing four or more
     * digits from an Open Location Code. It must include a separator
     * character.
     */
    public boolean isShort(String code) {
        // Check it's valid.
        if (!isValid(code)) {
            return false;
        }
        // If there are less characters than expected before the SEPARATOR.
        return code.contains(SEPARATOR_) &&
                code.indexOf(SEPARATOR_) < SEPARATOR_POSITION_;
    }

    /**
     * Determines if a code is a valid full Open Location Code.
     * Not all possible combinations of Open Location Code characters decode to
     * valid latitude and longitude values. This checks that a code is valid
     * and also that the latitude and longitude values are legal. If the prefix
     * character is present, it must be the first character. If the separator
     * character is present, it must be after four characters.
     */
    public boolean isFull(String code) {
        if (!isValid(code)) {
            return false;
        }
        // If it's short, it's not full.
        if (isShort(code)) {
            return false;
        }

        // Work out what the first latitude character indicates for latitude.
        int firstLatValue = CODE_ALPHABET_.indexOf(
                code.toUpperCase().charAt(0)) * ENCODING_BASE_;
        if (firstLatValue >= LATITUDE_MAX_ * 2) {
            // The code would decode to a latitude of >= 90 degrees.
            return false;
        }
        if (code.length() > 1) {
            // Work out what the first longitude character indicates for longitude.
            int firstLngValue = CODE_ALPHABET_.indexOf(
                    code.toUpperCase().charAt(1)) * ENCODING_BASE_;
            if (firstLngValue >= LONGITUDE_MAX_ * 2) {
                // The code would decode to a longitude of >= 180 degrees.
                return false;
            }
        }
        return true;
    }

    /**
     * Encode a location into an Open Location Code.
     * Produces a code of the specified length, or the default length if no length
     * is provided.
     * The length determines the accuracy of the code. The default length is
     * 10 characters, returning a code of approximately 13.5x13.5 meters. Longer
     * codes represent smaller areas, but lengths > 14 are sub-centimetre and so
     * 11 or 12 are probably the limit of useful codes.
     *
     * @param latitude:   A latitude in signed decimal degrees. Will be clipped to the
     *                    range -90 to 90.
     * @param longitude:  A longitude in signed decimal degrees. Will be normalised to
     *                    the range -180 to 180.
     * @param codeLength: The number of significant digits in the output code, not
     *                    including any separator characters.
     */
    public String encode(double latitude,
                         double longitude, int codeLength) throws IllegalArgumentException {
        if (codeLength <= 0) {
            codeLength = PAIR_CODE_LENGTH_;
        }
        if (codeLength < 2 ||
                (codeLength < SEPARATOR_POSITION_ && codeLength % 2 == 1)) {
            throw new IllegalArgumentException("Invalid Open Location Code length");
        }
        // Ensure that latitude and longitude are valid.
        latitude = clipLatitude(latitude);
        longitude = normalizeLongitude(longitude);
        // Latitude 90 needs to be adjusted to be just less, so the returned code
        // can also be decoded.
        if (latitude == 90) {
            latitude = latitude - computeLatitudePrecision(codeLength);
        }
        String code = encodePairs(
                latitude, longitude, Math.min(codeLength, PAIR_CODE_LENGTH_));
        // If the requested length indicates we want grid refined codes.
        if (codeLength > PAIR_CODE_LENGTH_) {
            code += encodeGrid(
                    latitude, longitude, codeLength - PAIR_CODE_LENGTH_);
        }
        return code;
    }

    /**
     * Decodes an Open Location Code into the location coordinates.
     * Returns a CodeArea object that includes the coordinates of the bounding
     * box - the lower left, center and upper right.
     *
     * @param code: The Open Location Code to decode.
     * @return A CodeArea object that provides the latitude and longitude of two of the
     * corners of the area, the center, and the length of the original code.
     */
    public CodeArea decode(String code) {
        if (!isFull(code)) {
            throw new IllegalArgumentException("Passed Open Location Code is not a valid full code: " + code);
        }
        // Strip out separator character (we've already established the code is
        // valid so the maximum is one), padding characters and convert to upper
        // case.
        code = code.replace(SEPARATOR_, "");
        code = code.replace(PADDING_CHARACTER_ + "+", "");
        code = code.toUpperCase();
        // Decode the lat/lng pair component.
        CodeArea codeArea = decodePairs(code.substring(0, PAIR_CODE_LENGTH_));
        // If there is a grid refinement component, decode that.
        if (code.length() <= PAIR_CODE_LENGTH_) {
            return codeArea;
        }
        CodeArea gridArea = decodeGrid(code.substring(PAIR_CODE_LENGTH_));
        return new CodeArea(
                codeArea.latitudeLo + gridArea.latitudeLo,
                codeArea.longitudeLo + gridArea.longitudeLo,
                codeArea.latitudeLo + gridArea.latitudeHi,
                codeArea.longitudeLo + gridArea.longitudeHi,
                codeArea.codeLength + gridArea.codeLength);
    }

    /**
     * Recover the nearest matching code to a specified location.
     * Given a short Open Location Code of between four and seven characters,
     * this recovers the nearest matching full code to the specified location.
     * The number of characters that will be prepended to the short code, depends
     * on the length of the short code and whether it starts with the separator.
     * If it starts with the separator, four characters will be prepended. If it
     * does not, the characters that will be prepended to the short code, where S
     * is the supplied short code and R are the computed characters, are as
     * follows:
     * SSSS    -> RRRR.RRSSSS
     * SSSSS   -> RRRR.RRSSSSS
     * SSSSSS  -> RRRR.SSSSSS
     * SSSSSSS -> RRRR.SSSSSSS
     * Note that short codes with an odd number of characters will have their
     * last character decoded using the grid refinement algorithm.
     * Args:
     * shortCode: A valid short OLC character sequence.
     * referenceLatitude: The latitude (in signed decimal degrees) to use to
     * find the nearest matching full code.
     * referenceLongitude: The longitude (in signed decimal degrees) to use
     * to find the nearest matching full code.
     * Returns:
     * The nearest full Open Location Code to the reference location that matches
     * the short code. Note that the returned code may not have the same
     * computed characters as the reference location. This is because it returns
     * the nearest match, not necessarily the match within the same cell. If the
     * passed code was not a valid short code, but was a valid full code, it is
     * returned unchanged.
     */
    public String recoverNearest(String shortCode, double referenceLatitude, double referenceLongitude) {
        if (!isShort(shortCode)) {
            if (isFull(shortCode)) {
                return shortCode;
            } else {
                throw new IllegalArgumentException("ValueError: Passed short code is not valid: " + shortCode);
            }
        }
        // Ensure that latitude and longitude are valid.
        referenceLatitude = clipLatitude(referenceLatitude);
        referenceLongitude = normalizeLongitude(referenceLongitude);

        // Clean up the passed code.
        shortCode = shortCode.toUpperCase();
        // Compute the number of digits we need to recover.
        int paddingLength = SEPARATOR_POSITION_ - shortCode.indexOf(SEPARATOR_);
        // The resolution (height and width) of the padded area in degrees.
        double resolution = Math.pow(20, 2 - (paddingLength / 2));
        // Distance from the center to an edge (in degrees).
        double areaToEdge = resolution / 2.0;

        // Now round down the reference latitude and longitude to the resolution.
        double roundedLatitude = Math.floor(referenceLatitude / resolution) *
                resolution;
        double roundedLongitude = Math.floor(referenceLongitude / resolution) *
                resolution;

        // Use the reference location to pad the supplied short code and decode it.
        CodeArea codeArea = decode(encode(roundedLatitude, roundedLongitude, 0).substring(0, paddingLength)
                + shortCode);
        // How many degrees latitude is the code from the reference? If it is more
        // than half the resolution, we need to move it east or west.
        double degreesDifference = codeArea.latitudeCenter - referenceLatitude;
        if (degreesDifference > areaToEdge) {
            // If the center of the short code is more than half a cell east,
            // then the best match will be one position west.
            codeArea.latitudeCenter -= resolution;
        } else if (degreesDifference < -areaToEdge) {
            // If the center of the short code is more than half a cell west,
            // then the best match will be one position east.
            codeArea.latitudeCenter += resolution;
        }

        // How many degrees longitude is the code from the reference?
        degreesDifference = codeArea.longitudeCenter - referenceLongitude;
        if (degreesDifference > areaToEdge) {
            codeArea.longitudeCenter -= resolution;
        } else if (degreesDifference < -areaToEdge) {
            codeArea.longitudeCenter += resolution;
        }

        return encode(
                codeArea.latitudeCenter, codeArea.longitudeCenter, codeArea.codeLength);
    }


    /**
     * Remove characters from the start of an OLC code.
     * This uses a reference location to determine how many initial characters
     * can be removed from the OLC code. The number of characters that can be
     * removed depends on the distance between the code center and the reference
     * location.
     * The minimum number of characters that will be removed is four. If more than
     * four characters can be removed, the additional characters will be replaced
     * with the padding character. At most eight characters will be removed.
     * The reference location must be within 50% of the maximum range. This ensures
     * that the shortened code will be able to be recovered using slightly different
     * locations.
     * Args:
     * code: A full, valid code to shorten.
     * latitude: A latitude, in signed decimal degrees, to use as the reference
     * point.
     * longitude: A longitude, in signed decimal degrees, to use as the reference
     * point.
     * Returns:
     * Either the original code, if the reference location was not close enough,
     * or the .
     */
    public String shorten(String code, double latitude, double longitude) {
        if (!isFull(code)) {
            throw new IllegalArgumentException("ValueError: Passed code is not valid and full: " + code);
        }
        if (code.contains(PADDING_CHARACTER_)) {
            throw new IllegalArgumentException("ValueError: Cannot shorten padded codes: " + code);
        }
        code = code.toUpperCase();
        CodeArea codeArea = decode(code);
        if (codeArea.codeLength < MIN_TRIMMABLE_CODE_LEN_) {
            throw new IllegalArgumentException("ValueError: Code length must be at least " +
                    MIN_TRIMMABLE_CODE_LEN_);
        }
        // Ensure that latitude and longitude are valid.
        latitude = clipLatitude(latitude);
        longitude = normalizeLongitude(longitude);
        // How close are the latitude and longitude to the code center.
        double range = Math.max(
                Math.abs(codeArea.latitudeCenter - latitude),
                Math.abs(codeArea.longitudeCenter - longitude));
        for (int i = PAIR_RESOLUTIONS_.length - 2; i >= 1; i--) {
            // Check if we're close enough to shorten. The range must be less than 1/2
            // the resolution to shorten at all, and we want to allow some safety, so
            // use 0.3 instead of 0.5 as a multiplier.
            if (range < (PAIR_RESOLUTIONS_[i] * 0.3)) {
                // Trim it.
                return code.substring((i + 1) * 2);
            }
        }
        return code;
    }

    /**
     * Clip a latitude into the range -90 to 90.
     *
     * @param latitude: A latitude in signed decimal degrees.
     */
    private double clipLatitude(double latitude) {
        return Math.min(90, Math.max(-90, latitude));
    }

    /**
     * Compute the latitude precision value for a given code length. Lengths <=
     * 10 have the same precision for latitude and longitude, but lengths > 10
     * have different precisions due to the grid method having fewer columns than
     * rows.
     */
    private double computeLatitudePrecision(int codeLength) {
        if (codeLength <= 10) {
            return Math.pow(20, Math.floor(codeLength / -2 + 2));
        }
        return Math.pow(20, -3) / Math.pow(GRID_ROWS_, codeLength - 10);
    }

    /**
     * Normalize a longitude into the range -180 to 180, not including 180.
     *
     * @param longitude: A longitude in signed decimal degrees.
     */

    private double normalizeLongitude(double longitude) {
        while (longitude < -180) {
            longitude = longitude + 360;
        }
        while (longitude >= 180) {
            longitude = longitude - 360;
        }
        return longitude;
    }

    /**
     * Encode a location into a sequence of OLC lat/lng pairs.
     * This uses pairs of characters (longitude and latitude in that order) to
     * represent each step in a 20x20 grid. Each code, therefore, has 1/400th
     * the area of the previous code.
     *
     * @param latitude:   A latitude in signed decimal degrees.
     * @param longitude:  A longitude in signed decimal degrees.
     * @param codeLength: The number of significant digits in the output code, not
     *                    including any separator characters.
     */
    private String encodePairs(double latitude, double longitude, int codeLength) {
        StringBuilder code = new StringBuilder();
        // Adjust latitude and longitude so they fall into positive ranges.
        double adjustedLatitude = latitude + LATITUDE_MAX_;
        double adjustedLongitude = longitude + LONGITUDE_MAX_;
        // Count digits - can't use string length because it may include a separator
        // character.
        int digitCount = 0;
        while (digitCount < codeLength) {
            // Provides the value of digits in this place in decimal degrees.
            double placeValue = PAIR_RESOLUTIONS_[((int) Math.floor(digitCount / 2))];
            // Do the latitude - gets the digit for this place and subtracts that for
            // the next digit.
            double digitValue = Math.floor(adjustedLatitude / placeValue);
            adjustedLatitude -= digitValue * placeValue;
            code.append(CODE_ALPHABET_.charAt((int) digitValue));
            digitCount += 1;
            // And do the longitude - gets the digit for this place and subtracts that
            // for the next digit.
            digitValue = Math.floor(adjustedLongitude / placeValue);
            adjustedLongitude -= digitValue * placeValue;
            code.append(CODE_ALPHABET_.charAt((int) digitValue));
            digitCount += 1;
            // Should we add a separator here?
            if (digitCount == SEPARATOR_POSITION_ && digitCount < codeLength) {
                code.append(SEPARATOR_);
            }
        }
        if (code.length() < SEPARATOR_POSITION_) {
            code.insert(SEPARATOR_POSITION_ - code.length() + 1, PADDING_CHARACTER_);
        }
        if (code.length() == SEPARATOR_POSITION_) {
            code.append(SEPARATOR_);
        }
        return code.toString();
    }

    /**
     * Encode a location using the grid refinement method into an OLC string.
     * The grid refinement method divides the area into a grid of 4x5, and uses a
     * single character to refine the area. This allows default accuracy OLC codes
     * to be refined with just a single character.
     *
     * @param latitude:   A latitude in signed decimal degrees.
     * @param longitude:  A longitude in signed decimal degrees.
     * @param codeLength: The number of characters required.
     */
    private String encodeGrid(double latitude, double longitude, int codeLength) {
        StringBuilder code = new StringBuilder();
        double latPlaceValue = GRID_SIZE_DEGREES_;
        double lngPlaceValue = GRID_SIZE_DEGREES_;
        // Adjust latitude and longitude so they fall into positive ranges and
        // get the offset for the required places.
        double adjustedLatitude = (latitude + LATITUDE_MAX_) % latPlaceValue;
        double adjustedLongitude = (longitude + LONGITUDE_MAX_) % lngPlaceValue;
        for (int i = 0; i < codeLength; i++) {
            // Work out the row and column.
            double row = Math.floor(adjustedLatitude / (latPlaceValue / GRID_ROWS_));
            double col = Math.floor(adjustedLongitude / (lngPlaceValue / GRID_COLUMNS_));
            latPlaceValue /= GRID_ROWS_;
            lngPlaceValue /= GRID_COLUMNS_;
            adjustedLatitude -= row * latPlaceValue;
            adjustedLongitude -= col * lngPlaceValue;
            code.append(CODE_ALPHABET_.charAt((int) (row * GRID_COLUMNS_ + col)));
        }
        return code.toString();
    }

    /**
     * Decode an OLC code made up of lat/lng pairs.
     * This decodes an OLC code made up of alternating latitude and longitude
     * characters, encoded using base 20.
     *
     * @param code: A valid OLC code, presumed to be full, but with the separator
     *              removed.
     */
    private CodeArea decodePairs(String code) {
        // Get the latitude and longitude values. These will need correcting from
        // positive ranges.
        double[] latitude = decodePairsSequence(code, 0);
        double[] longitude = decodePairsSequence(code, 1);
        // Correct the values and set them into the CodeArea object.
        return new CodeArea(
                latitude[0] - LATITUDE_MAX_,
                longitude[0] - LONGITUDE_MAX_,
                latitude[1] - LATITUDE_MAX_,
                longitude[1] - LONGITUDE_MAX_,
                code.length());
    }

    /**
     * Decode either a latitude or longitude sequence.
     * This decodes the latitude or longitude sequence of a lat/lng pair encoding.
     * Starting at the character at position offset, every second character is
     * decoded and the value returned.
     *
     * @param code:   A valid OLC code, presumed to be full, with the separator removed.
     * @param offset: The character to start from.
     * @return A pair of the low and high values. The low value comes from decoding the
     * characters. The high value is the low value plus the resolution of the
     * last position. Both values are offset into positive ranges and will need
     * to be corrected before use.
     */
    public double[] decodePairsSequence(String code, int offset) {
        int i = 0;
        double value = 0;
        while (i * 2 + offset < code.length()) {
            value += CODE_ALPHABET_.indexOf(code.charAt(i * 2 + offset)) *
                    PAIR_RESOLUTIONS_[i];
            i += 1;
        }
        return new double[]{value, value + PAIR_RESOLUTIONS_[i - 1]};
    }

    /**
     * Decode the grid refinement portion of an OLC code.
     * This decodes an OLC code using the grid refinement method.
     * Args:
     * code: A valid OLC code sequence that is only the grid refinement
     * portion. This is the portion of a code starting at position 11.
     */
    private CodeArea decodeGrid(String code) {
        double latitudeLo = 0.0;
        double longitudeLo = 0.0;
        double latPlaceValue = GRID_SIZE_DEGREES_;
        double lngPlaceValue = GRID_SIZE_DEGREES_;
        int i = 0;
        while (i < code.length()) {
            int codeIndex = CODE_ALPHABET_.indexOf(code.charAt(i));
            double row = Math.floor(codeIndex / GRID_COLUMNS_);
            double col = codeIndex % GRID_COLUMNS_;

            latPlaceValue /= GRID_ROWS_;
            lngPlaceValue /= GRID_COLUMNS_;

            latitudeLo += row * latPlaceValue;
            longitudeLo += col * lngPlaceValue;
            i += 1;
        }
        return new CodeArea(
                latitudeLo, longitudeLo, latitudeLo + latPlaceValue,
                longitudeLo + lngPlaceValue, code.length());
    }


    public class CodeArea {

        private double latitudeLo;
        private double longitudeLo;
        private double latitudeHi;
        private double longitudeHi;
        private double latitudeCenter;
        private double longitudeCenter;
        private int codeLength;

        public CodeArea(double latitudeLo, double longitudeLo, double latitudeHi, double longitudeHi, int codeLength) {
            this.codeLength = codeLength;
            this.latitudeHi = latitudeHi;
            this.latitudeLo = latitudeLo;
            this.longitudeHi = longitudeHi;
            this.longitudeLo = longitudeLo;
            this.latitudeCenter = Math.min(
                    latitudeLo + (latitudeHi - latitudeLo) / 2, LATITUDE_MAX_);
            this.longitudeCenter = Math.min(
                    longitudeLo + (longitudeHi - longitudeLo) / 2, LONGITUDE_MAX_);
        }

        public int getCodeLength() {
            return codeLength;
        }

        public double getLatitudeCenter() {
            return latitudeCenter;
        }

        public double getLatitudeHi() {
            return latitudeHi;
        }

        public double getLatitudeLo() {
            return latitudeLo;
        }

        public double getLongitudeCenter() {
            return longitudeCenter;
        }

        public double getLongitudeHi() {
            return longitudeHi;
        }

        public double getLongitudeLo() {
            return longitudeLo;
        }
    }


}
