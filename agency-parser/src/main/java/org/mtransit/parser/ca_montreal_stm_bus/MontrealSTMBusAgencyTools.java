package org.mtransit.parser.ca_montreal_stm_bus;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.Constants.SPACE_;
import static org.mtransit.commons.HtmlSymbols.AIRPORT_;
import static org.mtransit.commons.HtmlSymbols.SUBWAY_;
import static org.mtransit.commons.HtmlSymbols.TRAIN_;
import static org.mtransit.commons.RegexUtils.ANY;
import static org.mtransit.commons.RegexUtils.BEGINNING;
import static org.mtransit.commons.RegexUtils.DIGIT_CAR;
import static org.mtransit.commons.RegexUtils.END;
import static org.mtransit.commons.RegexUtils.WHITESPACE_CAR;
import static org.mtransit.commons.RegexUtils.WORD_CAR;
import static org.mtransit.commons.RegexUtils.any;
import static org.mtransit.commons.RegexUtils.atLeastOne;
import static org.mtransit.commons.RegexUtils.except;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.RegexUtils.mGroup;
import static org.mtransit.commons.RegexUtils.matchGroup;
import static org.mtransit.commons.RegexUtils.maybe;
import static org.mtransit.commons.RegexUtils.oneOrMore;
import static org.mtransit.commons.RegexUtils.or;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.RegexUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://www.stm.info/en/about/developers
// https://www.stm.info/fr/a-propos/developpeurs
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MontrealSTMBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_FR_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "STM";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Integer.parseInt(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final Pattern P1NUITP2 = Pattern.compile("(\\(nuit\\))", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final Pattern EXPRESS_ = CleanUtils.cleanWord("express");

	private static final Pattern NAVETTE_ = CleanUtils.cleanWord("navette");

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String result) {
		result = P1NUITP2.matcher(result).replaceAll(EMPTY);
		result = EXPRESS_.matcher(result).replaceAll(SPACE_);
		result = NAVETTE_.matcher(result).replaceAll(SPACE_);
		result = RegexUtils.replaceAllNN(result.trim(), START_WITH_ST, EMPTY);
		result = RegexUtils.replaceAllNN(result, SPACE_ST, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(result);
	}

	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String COLOR_GREEN_EXPRESS = "007339";
	private static final String COLOR_BLACK_NIGHT = "000000";
	private static final String COLOR_BLUE_REGULAR = "0060AA";

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String rln = gRoute.getRouteLongNameOrDefault();
		final long routeID = getRouteId(gRoute);
		if (rln.contains("express")
				|| 400L <= routeID && routeID <= 499L) {
			return COLOR_GREEN_EXPRESS;
		} else if (300L <= routeID && routeID <= 399L) {
			return COLOR_BLACK_NIGHT;
		} else {
			return COLOR_BLUE_REGULAR;
		}
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_RSN_DASH_ = Pattern.compile("(^\\d+-)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = STARTS_WITH_RSN_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep E/W/N/S
		return directionHeadSign;
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				MTrip.HEADSIGN_TYPE_DIRECTION
		);
	}

	private static final Pattern STARTS_WITH_RSN_DASH_BOUND_ = Pattern.compile("(^\\d+-[A-Z])");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_RSN_DASH_BOUND_.matcher(tripHeadsign).replaceAll(EMPTY); // E/W/N/W used for direction, not trip head-sign
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return super.cleanTripHeadsign(tripHeadsign);
	}

	private static final Pattern ETS_ = Pattern.compile(
			group(maybe(group("[eé]cole")) + " De Technologie Supérieure")
			, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String ETS_REPLACEMENT = "ETS";

	private static final Pattern NUMBER_ = Pattern.compile(
			group(or(BEGINNING, oneOrMore(WHITESPACE_CAR))) +
					group("no" + any(WHITESPACE_CAR)) +
					group(oneOrMore(DIGIT_CAR))
			, Pattern.CASE_INSENSITIVE);
	private static final String NUMBER_REPLACEMENT = mGroup(1) + "#" + mGroup(3);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String stopName) {
		stopName = P1_WORD_P2_.matcher(stopName).replaceAll(P1_WORD_P2_REPLACEMENT); // 1st
		stopName = POI_P1_STREET_SLASH_STREET_P2_.matcher(stopName).replaceAll(POI_P1_STREET_SLASH_STREET_P2_REPLACEMENT); // 2nd

		stopName = CLEAN_SUBWAY.matcher(stopName).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		stopName = CLEAN_SUBWAY2.matcher(stopName).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		stopName = CLEAN_SUBWAY_ONLY.matcher(stopName).replaceAll(CLEAN_SUBWAY_ONLY_REPLACEMENT);

		stopName = CLEAN_TRAIN.matcher(stopName).replaceAll(CLEAN_TRAIN_REPLACEMENT);
		stopName = CLEAN_TRAIN_ONLY.matcher(stopName).replaceAll(CLEAN_TRAIN_ONLY_REPLACEMENT);

		stopName = CLEAN_AIRPORT.matcher(stopName).replaceAll(CLEAN_AIRPORT_REPLACEMENT);
		stopName = CLEAN_AIRPORT_ONLY.matcher(stopName).replaceAll(CLEAN_AIRPORT_ONLY_REPLACEMENT);
		stopName = ETS_.matcher(stopName).replaceAll(ETS_REPLACEMENT);
		stopName = EXIT_BOUND_.matcher(stopName).replaceAll(EXIT_BOUND_REPLACEMENT);
		stopName = NUMBER_.matcher(stopName).replaceAll(NUMBER_REPLACEMENT);
		stopName = CleanUtils.cleanSlashes(stopName);
		stopName = RegexUtils.replaceAllNN(stopName.trim(), START_WITH_ST, StringUtils.EMPTY);
		stopName = RegexUtils.replaceAllNN(stopName, SPACE_ST, CleanUtils.SPACE);
		stopName = CleanUtils.cleanLabelFR(stopName);
		StringBuilder resultSB = new StringBuilder();
		String[] words = stopName.split(SLASH);
		for (String word : words) {
			if (!resultSB.toString().contains(word.trim())) {
				if (resultSB.length() > 0) {
					resultSB.append(SPACE_).append(SLASH).append(SPACE_);
				}
				resultSB.append(word.trim());
			}
		}
		return resultSB.toString();
	}

	private static final String P1 = "\\(";
	private static final String P2 = "\\)";
	private static final String SLASH = "/";

	private static final String CLEAN_SUBWAY_WORDS_ = "station ";
	private static final Pattern CLEAN_SUBWAY = makeStreetsP1POIP2Pattern(CLEAN_SUBWAY_WORDS_);
	private static final String CLEAN_SUBWAY_REPLACEMENT = makeStreetsP1POIP2ReplaceAll(SUBWAY_, false);

	// Station NAME EXIT / STREET
	private static final Pattern CLEAN_SUBWAY2 = Pattern.compile(
			group(CLEAN_SUBWAY_WORDS_) + group(any(except(SLASH))) + SLASH + group(any(ANY))
			, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY2_REPLACEMENT = matchGroup(3) + SPACE_ + P1 + SUBWAY_ + matchGroup(2) + P2;

	private static final Pattern CLEAN_SUBWAY_ONLY = makePOIOnlyPattern(CLEAN_SUBWAY_WORDS_);
	private static final String CLEAN_SUBWAY_ONLY_REPLACEMENT = makePOIOnlyReplaceAll(SUBWAY_);

	private static final String CLEAN_TRAIN_WORDS_ = "gare ";
	private static final Pattern CLEAN_TRAIN = makeStreetsP1POIP2Pattern(CLEAN_TRAIN_WORDS_);
	private static final String CLEAN_TRAIN_REPLACEMENT = makeStreetsP1POIP2ReplaceAll(TRAIN_, true);

	private static final Pattern CLEAN_TRAIN_ONLY = makePOIOnlyPattern(CLEAN_TRAIN_WORDS_);
	private static final String CLEAN_TRAIN_ONLY_REPLACEMENT = makePOIOnlyReplaceAll(TRAIN_);

	private static final String CLEAN_AIRPORT_WORDS_ = "a[ée]roport ";
	private static final Pattern CLEAN_AIRPORT = makeStreetsP1POIP2Pattern(CLEAN_AIRPORT_WORDS_);
	private static final String CLEAN_AIRPORT_REPLACEMENT = makeStreetsP1POIP2ReplaceAll(AIRPORT_, true);

	private static final Pattern CLEAN_AIRPORT_ONLY = makePOIOnlyPattern(CLEAN_AIRPORT_WORDS_);
	private static final String CLEAN_AIRPORT_ONLY_REPLACEMENT = makePOIOnlyReplaceAll(AIRPORT_);

	private static final Pattern P1_WORD_P2_ = Pattern.compile(P1 + group(atLeastOne(WORD_CAR)) + P2 + WHITESPACE_CAR, Pattern.CASE_INSENSITIVE);
	private static final String P1_WORD_P2_REPLACEMENT = mGroup(1) + SPACE_;

	private static final Pattern POI_P1_STREET_SLASH_STREET_P2_ = Pattern.compile(BEGINNING
			+ group(oneOrMore(except(P1))) + maybe(WHITESPACE_CAR) + P1
			+ group(any(except(SLASH))) + maybe(WHITESPACE_CAR) + SLASH + maybe(WHITESPACE_CAR)
			+ group(or(any(except(P2)), any(ANY))) + maybe(P2) // P2 can be missing in original data
			+ END, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String POI_P1_STREET_SLASH_STREET_P2_REPLACEMENT =
			mGroup(2) +
					SLASH + SPACE_ + mGroup(3) +
					SPACE_ + P1 + mGroup(1) + P2;

	@NotNull
	private static Pattern makeStreetsP1POIP2Pattern(@NotNull String words_) {
		return Pattern.compile(BEGINNING +
				group(any(except(P1))) + maybe(WHITESPACE_CAR) + P1 +
				group(words_) +
				group(or(any(except(P2)), any(ANY))) + maybe(P2) + // P2 can be missing in original data
				END, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	}

	@NotNull
	private static String makeStreetsP1POIP2ReplaceAll(@NotNull String replacement_, boolean keepWord) {
		return mGroup(1) +
				P1 +
				(replacement_.isEmpty() ? (keepWord ? mGroup(2) : EMPTY) : replacement_)
				+ mGroup(3) +
				P2;
	}

	@NotNull
	private static Pattern makePOIOnlyPattern(@NotNull String words_) {
		return Pattern.compile(BEGINNING
				+ group(words_) + group(any(ANY))
				+ END, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	}

	@NotNull
	private static String makePOIOnlyReplaceAll(@NotNull String replacement_) {
		return mGroup(1) + replacement_ + mGroup(2);
	}

	private static final Pattern EXIT_BOUND_ = Pattern.compile(
			P1 + group("[eé]dicule ") + group(any(except(P2))) + P2
			, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String EXIT_BOUND_REPLACEMENT = P1 + matchGroup(2) + P2;

	private static final String CHARS_VERS = "vers ";

	private static final String CHARS_STAR = "\\*";

	private static final String CHARS_SLASH = "/";

	private static final String CHARS_DASH = "-";

	private static final Pattern[] START_WITH_ST = new Pattern[]{ //
			Pattern.compile("(^" + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_STAR + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_SLASH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_DASH + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SPACE_ST = new Pattern[]{ //
			Pattern.compile("( " + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE) //
	};
}
