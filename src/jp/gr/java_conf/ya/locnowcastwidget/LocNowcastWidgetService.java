package jp.gr.java_conf.ya.locnowcastwidget;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class LocNowcastWidgetService extends Service implements LocationListener, GpsStatus.Listener {
	private boolean notPositioning = true;
	private double currentLatitude = -91;
	private double currentLongitude = -181;
	private final int nowcastDataNum = 11;
	private final int radameDataNum = 6;
	private final long freqSec = 30 * 1000;
	private int icon = R.drawable.icon512;
	private Intent buttonIntent;
	private Location preLocation;
	private LocationManager locationManager;
	private final Pattern colorCodePattern = Pattern.compile("^#[0-9A-Fa-f]{6,8}$");
	private RemoteViews remoteViews;
	private SharedPreferences pref_app;
	private SpannableString preText = new SpannableString("");
	private final String BUTTON_CLICK_ACTION = "BUTTON_CLICK_ACTION";
	private String preStatus = "";
	private String preGpsStatus = "";

	private String reverseGeocoding(String lat, String lng) {
		return getJsonFromWeb("http://www.finds.jp/ws/rgeocode.php?json&lat=" + lat + "&lon=" + lng);
	}

	private String getJsonFromWeb(String uri) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean pref_view_footer_revgeocoding_p = pref_app.getBoolean("pref_view_footer_revgeocoding_p", false);
		final boolean pref_view_footer_revgeocoding_m = pref_app.getBoolean("pref_view_footer_revgeocoding_m", true);
		final boolean pref_view_footer_revgeocoding_s = pref_app.getBoolean("pref_view_footer_revgeocoding_s", true);

		try {
			URLConnection conn = new URL(uri).openConnection();
			( (HttpURLConnection) conn ).setRequestMethod("GET");
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			ByteArrayOutputStream responseArray = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int length;
			while (( length = bis.read(buff) ) != -1) {
				if (length > 0) {
					responseArray.write(buff, 0, length);
				}
			}
			bis.close();
			is.close();

			JSONObject root = new JSONObject(new String(responseArray.toByteArray()));
			JSONObject result = root.getJSONObject("result");

			String section = "";
			if (pref_view_footer_revgeocoding_s) {
				JSONArray local = result.getJSONArray("local");
				//			for (int i = 0; i < local.length(); i++) {
				//				JSONObject l = local.getJSONObject(i);
				//				section = l.getString("section");
				if (local.length() > 0) {
					JSONObject l = local.getJSONObject(0);
					section = l.getString("section");
				}
				//				log("section[" + i + "] : " + section);
				//			}
			}

			String pname = "";
			if (pref_view_footer_revgeocoding_p) {
				JSONObject prefecture = result.getJSONObject("prefecture");
				pname = prefecture.getString("pname");
				log("pname : " + pname);
			}

			String mname = "";
			if (pref_view_footer_revgeocoding_m) {
				JSONObject municipality = result.getJSONObject("municipality");
				mname = municipality.getString("mname");
				log("mname : " + mname);
			}

			return ( pname + mname + section ).replaceAll("[ 　]", "");
		} catch (IOException e) {
			log(e.toString() + " : " + e.getMessage());
		} catch (Exception e) {
			log(e.toString() + " : " + e.getMessage());
		}

		return "";
	}

	private Bitmap getBitmapFromWeb(String uri) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		Bitmap bmp = null;
		try {
			URLConnection conn = new URL(uri).openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			bmp = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
		} catch (IOException e) {
			log(e.toString() + " : " + e.getMessage());
			bmp = null;
		} catch (Exception e) {
			log(e.toString() + " : " + e.getMessage());
			bmp = null;
		}

		return bmp;
	}

	private final double[][] latlngRangeNc = { { 139.0, 143.5, 42.0, 45.8 }, { 143.5, 147.0, 42.0, 44.8 }, { 138.0, 144.0, 41.0, 44.0 }, { 138.0, 143.5, 38.0, 42.0 }, { 137.5, 142.8, 36.5, 40.0 },
			{ 137.0, 142.5, 34.0, 37.0 }, { 137.0, 140.8, 33.5, 34.0 }, { 136.0, 141.5, 34.0, 38.0 }, { 136.0, 141.5, 36.0, 38.8 }, { 134.0, 139.5, 34.5, 38.0 }, { 135.5, 140.8, 32.8, 36.5 },
			{ 133.0, 138.0, 36.5, 33.0 }, { 130.5, 135.0, 33.5, 37.5 }, { 131.0, 136.0, 32.0, 35.8 }, { 128.0, 133.0, 32.0, 35.5 }, { 128.0, 133.0, 30.0, 33.0 }, { 127.5, 132.0, 27.0, 30.0 },
			{ 127.1, 130.5, 25.0, 28.0 }, { 130.0, 132.0, 25.0, 28.0 }, { 122.0, 126.5, 23.0, 26.5 } };

	private final double[][] latlngBasisNowcastNc = { { 73.0, 101.0, 138.5479, 46.1881 }, { 73.0, 101.0, 140.8082, 45.9802 }, { 75.0, 101.0, 137.6133, 45.0594 }, { 78.0, 101.0, 137.4872, 42.3960 },
			{ 81.0, 101.0, 137.0741, 40.4752 }, { 83.0, 101.0, 136.6627, 37.8119 }, { 83.0, 101.0, 136.6627, 37.8119 }, { 83.0, 101.0, 135.6506, 38.3069 }, { 81.0, 101.0, 135.6667, 40.0594 },
			{ 82.0, 101.0, 133.7927, 38.8119 }, { 84.0, 101.0, 135.1071, 37.0594 }, { 83.0, 101.0, 132.3373, 37.1485 }, { 84.0, 101.0, 129.6786, 37.8416 }, { 84.0, 101.0, 130.4524, 36.2277 },
			{ 85.0, 102.0, 127.3882, 35.8824 }, { 87.0, 102.0, 127.9540, 33.7157 }, { 89.0, 102.0, 126.8539, 30.8824 }, { 91.0, 102.0, 125.4066, 28.7157 }, { 91.0, 101.0, 126.9121, 28.5644 },
			{ 93.0, 101.0, 121.6129, 27.0594 } };

	private final double[][] latlngBasisRadameNc = { { 73.0, 101.0, 138.5479, 45.9604 }, { 73.0, 101.0, 140.8082, 45.7525 }, { 75.0, 101.0, 137.6133, 44.8317 }, { 78.0, 101.0, 137.4872, 43.1683 },
			{ 81.0, 101.0, 137.0741, 40.2475 }, { 83.0, 101.0, 136.6627, 37.5842 }, { 83.0, 101.0, 136.6627, 37.5842 }, { 83.0, 102.0, 135.6506, 39.0686 }, { 81.0, 101.0, 135.6667, 39.8317 },
			{ 82.0, 101.0, 133.7927, 38.5842 }, { 84.0, 101.0, 135.1071, 36.8317 }, { 83.0, 101.0, 132.3373, 36.9208 }, { 84.0, 101.0, 129.6786, 37.6139 }, { 84.0, 101.0, 130.4524, 36.0000 },
			{ 85.0, 102.0, 127.3882, 35.6569 }, { 87.0, 102.0, 127.9540, 33.4902 }, { 89.0, 102.0, 126.8539, 30.6569 }, { 91.0, 102.0, 125.4066, 28.4902 }, { 91.0, 101.0, 126.9121, 28.3366 },
			{ 93.0, 101.0, 121.6129, 26.8317 } };

	private final String[] latlngNameNc = { "201", "202", "203", "204", "205", "206", "206", "207", "208", "209", "210", "211", "212", "213", "214", "215", "216", "217", "218", "219" };

	private final double[][] latlngRangeWr = { { 135.0, 140.0, 33.0, 38.0 }, { 130.0, 135.0, 32.0, 37.0 }, { 139.0, 146.1, 41.0, 46.0 }, { 137.0, 142.0, 34.0, 39.0 }, { 133.0, 138.0, 32.5, 37.5 },
			{ 128.0, 133.0, 30.0, 35.0 }, { 126.0, 131.0, 25.0, 30.0 }, { 138.0, 143.0, 37.0, 42.0 }, { 122.0, 127.0, 21.9, 27.0 } };

	private final double[][] latlngBasisNowcastWr = { { 120.0, 120.0, 135.0106, 37.9916 }, { 120.0, 118.0, 130.0070, 37.0313 }, { 84.0, 120.0, 138.9719, 45.9861 },
			{ 120.0, 121.0, 137.0152, 38.9809 }, { 120.0, 120.0, 133.0280, 37.4910 }, { 120.0, 120.0, 128.0267, 34.9954 }, { 120.0, 120.0, 125.9977, 29.9985 }, { 119.0, 119.0, 137.9827, 42.0077 },
			{ 119.0, 119.0, 121.9787, 26.9883 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 },
			{ -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0 } };

	private final String[] latlngNameWr = { "chubu", "chushikoku", "hokkaido", "kanto", "kinki", "kyushu", "okinawa", "tohoku", "yaeyama", "---", "---", "---", "---", "---", "---", "---", "---",
			"---", "---", "---" };

	private String[] getWeatherImageUrl(double latitude, double longitude, boolean modeNowcRad) {
		log("getWeatherImageUrl( " + Double.toString(latitude) + " , " + Double.toString(longitude) + " )");

		final boolean pref_source_weatherreport = pref_app.getBoolean("pref_source_weatherreport", false);

		String UrlCommon =
				( pref_source_weatherreport ) ? ( "http://www.weather-report.jp/img2/professional/radar/now/" ) : ( modeNowcRad ? "http://www.jma.go.jp/jp/radnowc/imgs/nowcast/"
						: "http://www.jma.go.jp/jp/radame/imgs/prec/" );
		final double[][] latlngRange = ( pref_source_weatherreport ) ? ( latlngRangeWr ) : ( latlngRangeNc );
		final double[][] latlngBasis = ( pref_source_weatherreport ) ? ( latlngBasisNowcastWr ) : ( modeNowcRad ? latlngBasisNowcastNc : latlngBasisRadameNc );
		final String[] latlngName = ( pref_source_weatherreport ) ? ( latlngNameWr ) : ( latlngNameNc );

		String num = "-1";
		double x = 0;
		double y = 0;

		if (( longitude >= latlngRange[0][0] ) && ( longitude <= latlngRange[0][1] ) && ( latitude >= latlngRange[0][2] ) && ( latitude <= latlngRange[0][3] )) {
			num = latlngName[0];
			x = ( longitude - latlngBasis[0][2] ) * latlngBasis[0][0];
			y = ( latlngBasis[0][3] - latitude ) * latlngBasis[0][1];
		} else if (( longitude >= latlngRange[1][0] ) && ( longitude <= latlngRange[1][1] ) && ( latitude >= latlngRange[1][2] ) && ( latitude <= latlngRange[1][3] )) {
			num = latlngName[1];
			x = ( longitude - latlngBasis[1][2] ) * latlngBasis[1][0];
			y = ( latlngBasis[1][3] - latitude ) * latlngBasis[1][1];
		} else if (( longitude >= latlngRange[2][0] ) && ( longitude <= latlngRange[2][1] ) && ( latitude >= latlngRange[2][2] ) && ( latitude <= latlngRange[2][3] )) {
			num = latlngName[2];
			x = ( longitude - latlngBasis[2][2] ) * latlngBasis[2][0];
			y = ( latlngBasis[2][3] - latitude ) * latlngBasis[2][1];
		} else if (( longitude >= latlngRange[3][0] ) && ( longitude <= latlngRange[3][1] ) && ( latitude >= latlngRange[3][2] ) && ( latitude <= latlngRange[3][3] )) {
			num = latlngName[3];
			x = ( longitude - latlngBasis[3][2] ) * latlngBasis[3][0];
			y = ( latlngBasis[3][3] - latitude ) * latlngBasis[3][1];
		} else if (( longitude >= latlngRange[4][0] ) && ( longitude <= latlngRange[4][1] ) && ( latitude >= latlngRange[4][2] ) && ( latitude <= latlngRange[4][3] )) {
			num = latlngName[4];
			x = ( longitude - latlngBasis[4][2] ) * latlngBasis[4][0];
			y = ( latlngBasis[4][3] - latitude ) * latlngBasis[4][1];
		} else if (( longitude >= latlngRange[5][0] ) && ( longitude <= latlngRange[5][1] ) && ( latitude >= latlngRange[5][2] ) && ( latitude <= latlngRange[5][3] )) {
			num = latlngName[5];
			x = ( longitude - latlngBasis[5][2] ) * latlngBasis[5][0];
			y = ( latlngBasis[5][3] - latitude ) * latlngBasis[5][1];
		} else if (( longitude >= latlngRange[6][0] ) && ( longitude <= latlngRange[6][1] ) && ( latitude >= latlngRange[6][2] ) && ( latitude <= latlngRange[6][3] )) {
			num = latlngName[6];
			x = ( longitude - latlngBasis[6][2] ) * latlngBasis[6][0];
			y = ( latlngBasis[6][3] - latitude ) * latlngBasis[6][1];
		} else if (( longitude >= latlngRange[7][0] ) && ( longitude <= latlngRange[7][1] ) && ( latitude >= latlngRange[7][2] ) && ( latitude <= latlngRange[7][3] )) {
			num = latlngName[7];
			x = ( longitude - latlngBasis[7][2] ) * latlngBasis[7][0];
			y = ( latlngBasis[7][3] - latitude ) * latlngBasis[7][1];
		} else if (( longitude >= latlngRange[8][0] ) && ( longitude <= latlngRange[8][1] ) && ( latitude >= latlngRange[8][2] ) && ( latitude <= latlngRange[8][3] )) {
			num = latlngName[8];
			x = ( longitude - latlngBasis[8][2] ) * latlngBasis[8][0];
			y = ( latlngBasis[8][3] - latitude ) * latlngBasis[8][1];
		} else if (( longitude >= latlngRange[9][0] ) && ( longitude <= latlngRange[9][1] ) && ( latitude >= latlngRange[9][2] ) && ( latitude <= latlngRange[9][3] )) {
			num = latlngName[9];
			x = ( longitude - latlngBasis[9][2] ) * latlngBasis[9][0];
			y = ( latlngBasis[9][3] - latitude ) * latlngBasis[9][1];
		} else if (( longitude >= latlngRange[10][0] ) && ( longitude <= latlngRange[10][1] ) && ( latitude >= latlngRange[10][2] ) && ( latitude <= latlngRange[10][3] )) {
			num = latlngName[10];
			x = ( longitude - latlngBasis[10][2] ) * latlngBasis[10][0];
			y = ( latlngBasis[10][3] - latitude ) * latlngBasis[10][1];
		} else if (( longitude >= latlngRange[11][0] ) && ( longitude <= latlngRange[11][1] ) && ( latitude >= latlngRange[11][2] ) && ( latitude <= latlngRange[11][3] )) {
			num = latlngName[11];
			x = ( longitude - latlngBasis[11][2] ) * latlngBasis[11][0];
			y = ( latlngBasis[11][3] - latitude ) * latlngBasis[11][1];
		} else if (( longitude >= latlngRange[12][0] ) && ( longitude <= latlngRange[12][1] ) && ( latitude >= latlngRange[12][2] ) && ( latitude <= latlngRange[12][3] )) {
			num = latlngName[12];
			x = ( longitude - latlngBasis[12][2] ) * latlngBasis[12][0];
			y = ( latlngBasis[12][3] - latitude ) * latlngBasis[12][1];
		} else if (( longitude >= latlngRange[13][0] ) && ( longitude <= latlngRange[13][1] ) && ( latitude >= latlngRange[13][2] ) && ( latitude <= latlngRange[13][3] )) {
			num = latlngName[13];
			x = ( longitude - latlngBasis[13][2] ) * latlngBasis[13][0];
			y = ( latlngBasis[13][3] - latitude ) * latlngBasis[13][1];
		} else if (( longitude >= latlngRange[14][0] ) && ( longitude <= latlngRange[14][1] ) && ( latitude >= latlngRange[14][2] ) && ( latitude <= latlngRange[14][3] )) {
			num = latlngName[14];
			x = ( longitude - latlngBasis[14][2] ) * latlngBasis[14][0];
			y = ( latlngBasis[14][3] - latitude ) * latlngBasis[14][1];
		} else if (( longitude >= latlngRange[15][0] ) && ( longitude <= latlngRange[15][1] ) && ( latitude >= latlngRange[15][2] ) && ( latitude <= latlngRange[15][3] )) {
			num = latlngName[15];
			x = ( longitude - latlngBasis[15][2] ) * latlngBasis[15][0];
			y = ( latlngBasis[15][3] - latitude ) * latlngBasis[15][1];
		} else if (( longitude >= latlngRange[16][0] ) && ( longitude <= latlngRange[16][1] ) && ( latitude >= latlngRange[16][2] ) && ( latitude <= latlngRange[16][3] )) {
			num = latlngName[16];
			x = ( longitude - latlngBasis[16][2] ) * latlngBasis[16][0];
			y = ( latlngBasis[16][3] - latitude ) * latlngBasis[16][1];
		} else if (( longitude >= latlngRange[17][0] ) && ( longitude <= latlngRange[17][1] ) && ( latitude >= latlngRange[17][2] ) && ( latitude <= latlngRange[17][3] )) {
			num = latlngName[17];
			x = ( longitude - latlngBasis[17][2] ) * latlngBasis[17][0];
			y = ( latlngBasis[17][3] - latitude ) * latlngBasis[17][1];
		} else if (( longitude >= latlngRange[18][0] ) && ( longitude <= latlngRange[18][1] ) && ( latitude >= latlngRange[18][2] ) && ( latitude <= latlngRange[18][3] )) {
			num = latlngName[18];
			x = ( longitude - latlngBasis[18][2] ) * latlngBasis[18][0];
			y = ( latlngBasis[18][3] - latitude ) * latlngBasis[18][1];
		} else if (( longitude >= latlngRange[19][0] ) && ( longitude <= latlngRange[19][1] ) && ( latitude >= latlngRange[19][2] ) && ( latitude <= latlngRange[19][3] )) {
			num = latlngName[19];
			x = ( longitude - latlngBasis[19][2] ) * latlngBasis[19][0];
			y = ( latlngBasis[19][3] - latitude ) * latlngBasis[19][1];
		}

		String[] ret = new String[3];
		ret[0] = UrlCommon + num + ( pref_source_weatherreport ? "" : "/" );
		ret[1] = Long.toString(Math.round(x));
		ret[2] = Long.toString(Math.round(y));
		return ret;
	}

	private void getWeather(double lat, double lng) {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);

		long lastUpdateTime;
		try {
			lastUpdateTime = Long.parseLong(pref_app.getString("last_update_time", "0"));
		} catch (NumberFormatException e1) {
			lastUpdateTime = 0;
		}

		//		if (lastUpdateTime < 0) {
		//			lastUpdateTime = 0;
		//		}

		final long currentTime = System.currentTimeMillis();

		if (currentTime - lastUpdateTime > freqSec) {
			log("getWeather( " + Double.toString(lat) + " , " + Double.toString(lng) + " )");

			if (( lat < -90 ) || ( lat > 90 ) || ( lng < -180 ) || ( lng > 180 )) {
				try {
					lat = Double.parseDouble(pref_app.getString("pref_lat", "35.681382"));
				} catch (NumberFormatException e) {
					lat = 35.681382;
				}
				try {
					lng = Double.parseDouble(pref_app.getString("pref_long", "139.766084"));
				} catch (NumberFormatException e) {
					lng = 139.766084;
				}
			}

			final double latitude = lat;
			final double longitude = lng;

			final boolean pref_source_weatherreport = pref_app.getBoolean("pref_source_weatherreport", false);
			final boolean pref_use_revgeocoding = pref_app.getBoolean("pref_use_revgeocoding", false);
			final boolean pref_use_radame = pref_source_weatherreport ? false : pref_app.getBoolean("pref_use_radame", false);
			final boolean pref_view_footer_gettime = pref_app.getBoolean("pref_view_footer_gettime", true);
			int pref_latlong_decimal;
			try {
				pref_latlong_decimal = Integer.parseInt(pref_app.getString("pref_latlong_decimal", "3"));
			} catch (Exception e) {
				pref_latlong_decimal = 3;
			}
			final String pref_latlong_decimal_string = Integer.toString(pref_latlong_decimal);
			final String pref_bgcolor = pref_app.getString("pref_bgcolor", "");
			final String pref_fontcolor_footer_start = pref_app.getString("pref_fontcolor_footer_start", "#0000ff");
			final String pref_fontcolor_footer_error = pref_app.getString("pref_fontcolor_footer_error", "#ff0000");
			final String pref_fontcolor_footer_latlong = pref_app.getString("pref_fontcolor_footer_latlong", "#666666");
			final boolean pref_notification_onreload = pref_app.getBoolean("pref_notification_onreload", false);

			new Thread(new Runnable() {
				public void run() {

					updateImageview(BitmapFactory.decodeResource(getResources(), icon));
					updateTextview(Html.fromHtml(getString(R.string.now_loading) + " <small>(" + ( notPositioning ? "X" : "O" ) + ")</small>..."));

					final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.JAPAN);
					final SimpleDateFormat sdf_s = new SimpleDateFormat("HH:mm", Locale.JAPAN);

					Calendar cal0 = Calendar.getInstance();
					cal0.setLenient(true);
					cal0.set(Calendar.MINUTE, ( cal0.get(Calendar.MINUTE) + -5 + ( -1 ) * ( ( cal0.get(Calendar.MINUTE) % 5 ) ) ));
					Date time0 = cal0.getTime();
					Calendar cal = cal0;
					cal.setLenient(true);
					cal.add(Calendar.MINUTE, 5);

					Calendar cal01 = Calendar.getInstance();
					cal01.setLenient(true);
					cal01.set(Calendar.MINUTE, ( cal01.get(Calendar.MINUTE) + -30 + ( -1 ) * ( ( cal01.get(Calendar.MINUTE) % 30 ) ) ));
					Date time01 = cal01.getTime();
					Calendar cal1 = cal01;
					cal1.setLenient(true);

					String error = "";
					String start = "";
					String start_noti = "";

					int[] precipitations = new int[nowcastDataNum + ( pref_use_radame ? radameDataNum : 0 )];
					StringBuilder sb = new StringBuilder();
					StringBuilder sb_noti = new StringBuilder();

					for (int i = 0; i < nowcastDataNum; i++) {
						updateTextview(Html.fromHtml(getString(R.string.now_loading) + " <small>(" + ( notPositioning ? "X" : "O" ) + ")</small>...<br>" + progressString(2 * i, 2 * nowcastDataNum)));

						cal.setLenient(true);
						cal.add(Calendar.MINUTE, 5);

						String[] bitmapPart = getWeatherImageUrl(latitude, longitude, true);

						if (bitmapPart[0].equals("-1")) {
							Double lat2, lng2;
							try {
								lat2 = Double.parseDouble(pref_app.getString("pref_lat", "35.681382"));
							} catch (NumberFormatException e) {
								lat2 = 35.681382;
							}
							try {
								lng2 = Double.parseDouble(pref_app.getString("pref_long", "139.766084"));
							} catch (NumberFormatException e) {
								lng2 = 139.766084;
							}
							bitmapPart = getWeatherImageUrl(lat2, lng2, true);
						}

						String uriString =
								bitmapPart[0] + ( pref_source_weatherreport ? "_600_" + Integer.toString(i) : sdf.format(time0) + "-" + String.format("%1$02d", i + 2) )
										+ ( pref_source_weatherreport ? ".gif" : ".png" );
						Bitmap bmp = getBitmapFromWeb(uriString);

						updateTextview(Html.fromHtml(getString(R.string.now_loading) + " <small>(" + ( notPositioning ? "X" : "O" ) + ")</small>...<br>"
								+ progressString(2 * i + 1, 2 * nowcastDataNum)));

						if (bmp == null) {
							precipitations[i] = -1;

							sb.append("* ");
							sb_noti.append("* ");
							error = "<br /> <font color=\"" + pref_fontcolor_footer_error + "\">" + getString(R.string.bmp_null) + " (" + Integer.toString(i) + ")" + "</font>";

							log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " (bmp == null)");
						} else {
							int[] ninePixels = getNinePixels(bmp, Integer.parseInt(bitmapPart[1]), Integer.parseInt(bitmapPart[2]));
							if (ninePixels == null) {
								precipitations[i] = -1;

								sb.append("* ");
								sb_noti.append("* ");
								error = "<br /> <font color=\"" + pref_fontcolor_footer_error + "\">" + getString(R.string.ninepixels_null) + " (" + Integer.toString(i) + ")" + "</font>";

								log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " (ninePixels == null)");
							} else {
								int precipitation = getNaxPrecipitation(ninePixels);
								precipitations[i] = precipitation;

								if (precipitation > -1) {
									if (start.equals("")) {
										log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " 降り始め予想時刻: " + sdf_s.format(cal.getTime()) + "" + " 現在時刻: "
												+ sdf_s.format(new Date(System.currentTimeMillis())) + " cal.after(new Date(System.currentTimeMillis())):"
												+ Boolean.toString(cal.after(Calendar.getInstance())));
										if (cal.after(Calendar.getInstance())) {
											start = "<font color=\"" + pref_fontcolor_footer_start + "\">降り始め予想時刻:" + sdf_s.format(cal.getTime()) + "</font><br />";
											start_noti = sdf_s.format(cal.getTime()) + "降り始め";
										} else {
											start = "<font color=\"" + pref_fontcolor_footer_start + "\">降り始め直前または既に降っています</font><br />";
											start_noti = "直前";
										}
									}

									sb.append("<font color=\"" + precipitationToColorcode(precipitation, true) + "\">");
									sb.append(precipitation);
									sb.append("</font>");
									sb.append(" ");
									sb_noti.append(precipitation);
									sb_noti.append(" ");
								} else {
									sb.append("- ");
									sb_noti.append("- ");
								}

								log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " precipitations: " + precipitation);
							}
						}
					}

					if (( pref_use_radame ) && ( !pref_source_weatherreport )) {
						sb.append("|| ");
						sb_noti.append("|| ");
						for (int i = 0; i < radameDataNum; i++) {
							cal1.setLenient(true);
							cal1.add(Calendar.HOUR, 1);

							String[] bitmapPart = getWeatherImageUrl(latitude, longitude, false);

							if (bitmapPart[0].equals("-1")) {
								Double lat2, lng2;
								try {
									lat2 = Double.parseDouble(pref_app.getString("pref_lat", "35.681382"));
								} catch (NumberFormatException e) {
									lat2 = 35.681382;
								}
								try {
									lng2 = Double.parseDouble(pref_app.getString("pref_long", "139.766084"));
								} catch (NumberFormatException e) {
									lng2 = 139.766084;
								}
								bitmapPart = getWeatherImageUrl(lat2, lng2, false);
							}

							String uriString = bitmapPart[0] + sdf.format(time01) + "-" + String.format("%1$02d", i + 1) + ".png";
							Bitmap bmp = getBitmapFromWeb(uriString);

							if (bmp == null) {
								precipitations[nowcastDataNum + i] = -1;

								sb.append("* ");
								sb_noti.append("* ");
								error = "<br /> <font color=\"" + pref_fontcolor_footer_error + "\">" + getString(R.string.bmp_null) + " (" + Integer.toString(i) + ")" + "</font>";

								log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " (bmp == null)");
							} else {
								int[] ninePixels = getNinePixels(bmp, Integer.parseInt(bitmapPart[1]), Integer.parseInt(bitmapPart[2]));
								if (ninePixels == null) {
									precipitations[nowcastDataNum + i] = -1;

									sb.append("* ");
									sb_noti.append("* ");
									error = "<br /> <font color=\"" + pref_fontcolor_footer_error + "\">" + getString(R.string.ninepixels_null) + " (" + Integer.toString(i) + ")" + "</font>";

									log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " (ninePixels == null)");
								} else {
									int precipitation = getNaxPrecipitation(ninePixels);
									precipitations[nowcastDataNum + i] = precipitation;

									if (precipitation > -1) {
										if (start.equals("")) {
											log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " 降り始め予想時刻: " + sdf_s.format(cal1.getTime()) + "" + " 現在時刻: "
													+ sdf_s.format(new Date(System.currentTimeMillis())) + " cal.after(new Date(System.currentTimeMillis())):"
													+ Boolean.toString(cal1.after(Calendar.getInstance())));
											if (cal1.after(Calendar.getInstance())) {
												start = "<font color=\"" + pref_fontcolor_footer_start + "\">降り始め予想時刻:" + sdf_s.format(cal1.getTime()) + "</font><br />";
												start_noti = sdf_s.format(cal1.getTime()) + "降り始め";
											} else {
												start = "<font color=\"" + pref_fontcolor_footer_start + "\">降り始め直前または既に降っています</font><br />";
												start_noti = "直前";
											}
										}

										sb.append("<font color=\"" + precipitationToColorcode(precipitation, true) + "\">");
										sb.append(precipitation);
										sb.append("</font>");
										sb.append(" ");
										sb_noti.append(precipitation);
										sb_noti.append(" ");
									} else {
										sb.append("- ");
										sb_noti.append("- ");
									}

									log("i:" + i + " URL: " + uriString + " : " + bitmapPart[1] + " , " + bitmapPart[2] + " precipitations: " + precipitation);
								}
							}
						}
					}

					String latlngPart = "";
					if (pref_use_revgeocoding) {
						latlngPart = reverseGeocoding(Double.toString(latitude), Double.toString(longitude));
					} else {
						latlngPart = String.format("%." + pref_latlong_decimal_string + "f", latitude) + "," + String.format("%." + pref_latlong_decimal_string + "f", longitude);
					}

					String gettimePart = "";
					String gettimePart_noti = "";
					if (pref_view_footer_gettime) {
						gettimePart = "<br /><small> 取得時刻: " + sdf_s.format(new Date(System.currentTimeMillis())) + "</small>";
						gettimePart_noti = sdf_s.format(new Date(System.currentTimeMillis())) + "取得";
					}

					SpannableString spannable =
							new SpannableString(Html.fromHtml(start + sb.toString() + "<br /> <font color=\"" + pref_fontcolor_footer_latlong + "\">" + latlngPart + " <small>"
									+ ( notPositioning ? "X" : "O" ) + "</small></font>" + gettimePart + error));
					if (pref_bgcolor.equals("") == false) {
						BackgroundColorSpan bgcolor = new BackgroundColorSpan(Color.parseColor(pref_bgcolor));
						spannable.setSpan(bgcolor, 0, spannable.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
					}

					if (error.equals("")) {
						Bitmap bitmap = drawPrecipitationsIcon(precipitations);
						updateImageview(bitmap);

						if (pref_notification_onreload) {
							notification(start_noti + " " + gettimePart_noti, sb_noti.toString(), bitmap); // latlngPart
						}
					} else {
						updateImageview(BitmapFactory.decodeResource(getResources(), icon));
					}
					updateTextview(spannable);
					preText = spannable;
				}
			}).start();

			try {
				SharedPreferences.Editor editor = pref_app.edit();
				editor.putString("last_update_time", Long.toString(currentTime));
				editor.commit();
			} catch (Exception e) {
			}

		} else {
			log("frequency");
			updateImageview(BitmapFactory.decodeResource(getResources(), icon));
			updateTextview(preText);

			Random rnd = new Random();
			if (0 == rnd.nextInt(10)) {
				initLocationManager();
			}
		}

	}

	private void notification(String title, String summary, Bitmap bitmap) {
		int notificationId = 0;

		Intent intent = new Intent();
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Notification.Builder notificationBuilder = new Notification.Builder(this) //
		.setAutoCancel(true) //
		.setSmallIcon(icon) //
		.setContentTitle(title) //
		.setContentText(summary) //
		.setContentIntent(pendingIntent) //
		// .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
		.setStyle(new Notification.BigPictureStyle().bigPicture(bitmap).setBigContentTitle(title).setSummaryText(summary)) // 16
		;
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, notificationBuilder.build()); // 16
		// notificationManager.notify(notificationId, notificationBuilder.getNotification());
	}

	private Bitmap drawPrecipitationsIcon(int[] precipitations) {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean pref_source_weatherreport = pref_app.getBoolean("pref_source_weatherreport", false);
		final boolean pref_use_radame = pref_source_weatherreport ? false : pref_app.getBoolean("pref_use_radame", false);
		int pref_icon_dif;
		try {
			pref_icon_dif = Integer.parseInt(pref_app.getString("pref_icon_dif", "10"));
		} catch (NumberFormatException e) {
			pref_icon_dif = 10;
		}

		if (pref_icon_dif <= 0) {
			pref_icon_dif = 1;
		}

		Bitmap sun = BitmapFactory.decodeResource(getResources(), R.drawable.sun);
		Bitmap umb = BitmapFactory.decodeResource(getResources(), R.drawable.umb);

		Bitmap bitmap =
				Bitmap.createBitmap(( nowcastDataNum + radameDataNum ) * ( pref_icon_dif * 2 ) + umb.getWidth(), ( nowcastDataNum + radameDataNum ) * ( pref_icon_dif * 2 ) + umb.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.TRANSPARENT);

		if (pref_use_radame) {
			Bitmap sunr = BitmapFactory.decodeResource(getResources(), R.drawable.sunr);
			Bitmap umbr = BitmapFactory.decodeResource(getResources(), R.drawable.umbr);
			for (int i = radameDataNum - 1; i >= 0; i--) {
				Paint paint = new Paint();
				if (precipitations[nowcastDataNum + i] > -1) {
					LightingColorFilter lightingColorFilter = new LightingColorFilter(Color.parseColor(precipitationToColorcode(precipitations[nowcastDataNum + i], false)), 0);
					paint.setFilterBitmap(true);
					paint.setColorFilter(lightingColorFilter);
					canvas.drawBitmap(umbr, ( nowcastDataNum + i ) * ( pref_icon_dif * 2 ), ( nowcastDataNum + i ) * pref_icon_dif, paint);
				} else {
					canvas.drawBitmap(sunr, ( nowcastDataNum + i ) * ( pref_icon_dif * 2 ), ( nowcastDataNum + i ) * pref_icon_dif, paint);
				}
			}
		}

		for (int i = nowcastDataNum - 1; i >= 0; i--) {
			Paint paint = new Paint();
			if (precipitations[i] > -1) {
				LightingColorFilter lightingColorFilter = new LightingColorFilter(Color.parseColor(precipitationToColorcode(precipitations[i], false)), 0);
				paint.setFilterBitmap(true);
				paint.setColorFilter(lightingColorFilter);
				canvas.drawBitmap(umb, i * ( pref_icon_dif * 2 ), i * pref_icon_dif, paint);
			} else {
				canvas.drawBitmap(sun, i * ( pref_icon_dif * 2 ), i * pref_icon_dif, paint);
			}
		}

		return bitmap;
	}

	private int getNaxPrecipitation(int[] colors) {
		int maxPrecipitation = -1;
		for (int color : colors) {
			int precipitation = colorToPrecipitations(color);
			if (precipitation > maxPrecipitation) {
				maxPrecipitation = precipitation;
			}
		}
		return maxPrecipitation;
	}

	private int[] getNinePixels(Bitmap bmp, int x, int y) {
		if (bmp == null) {
			return null;
		}

		int[] ninePixels = new int[9];

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				try {
					ninePixels[3 * i + j] = bmp.getPixel(x + ( j - 1 ), y + ( i - 1 ));
				} catch (Exception e) {
					ninePixels[3 * i + j] = -1;
				}
			}
		}

		return ninePixels;
	}

	private String precipitationToColorcode(int precipitation, boolean string) {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean pref_source_weatherreport = pref_app.getBoolean("pref_source_weatherreport", false);

		String colorCode = "#000000";
		if (pref_source_weatherreport) {

			final String pref_fontcolor_colorcode_wr_100 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_100", "#660000");
			final String pref_fontcolor_colorcode_wr_80 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_80", "#CC0000");
			final String pref_fontcolor_colorcode_wr_60 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_60", "#FF6666");
			final String pref_fontcolor_colorcode_wr_40 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_40", "#FF99FF");
			final String pref_fontcolor_colorcode_wr_30 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_30", "#FFCC33");
			final String pref_fontcolor_colorcode_wr_20 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_20", "#FFFF99");
			final String pref_fontcolor_colorcode_wr_10 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_10", "#33FF99");
			final String pref_fontcolor_colorcode_wr_5 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_5", "#0099FF");
			final String pref_fontcolor_colorcode_wr_1 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_1", "#66CCFF");
			final String pref_fontcolor_colorcode_wr_0 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_wr_0", "#CCFFFF");
			final String pref_fontcolor_colorcode_minus1 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_minus1", "#000000");

			colorCode = pref_fontcolor_colorcode_minus1;
			if (precipitation == 100) {
				colorCode = pref_fontcolor_colorcode_wr_100;
			} else if (precipitation == 80) {
				colorCode = pref_fontcolor_colorcode_wr_80;
			} else if (precipitation == 60) {
				colorCode = pref_fontcolor_colorcode_wr_60;
			} else if (precipitation == 40) {
				colorCode = pref_fontcolor_colorcode_wr_40;
			} else if (precipitation == 30) {
				colorCode = pref_fontcolor_colorcode_wr_30;
			} else if (precipitation == 20) {
				colorCode = pref_fontcolor_colorcode_wr_20;
			} else if (precipitation == 10) {
				colorCode = pref_fontcolor_colorcode_wr_10;
			} else if (precipitation == 5) {
				colorCode = pref_fontcolor_colorcode_wr_5;
			} else if (precipitation == 1) {
				colorCode = pref_fontcolor_colorcode_wr_1;
			} else if (precipitation == 0) {
				colorCode = pref_fontcolor_colorcode_wr_0;
			}

		} else {

			final String pref_fontcolor_colorcode_80 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_80", "#b40068");
			final String pref_fontcolor_colorcode_50 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_50", "#ff2800");
			final String pref_fontcolor_colorcode_30 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_30", "#ff9900");
			final String pref_fontcolor_colorcode_20 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_20", "#faf500");
			final String pref_fontcolor_colorcode_10 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_10", "#0041ff");
			final String pref_fontcolor_colorcode_5 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_5", "#218cff");
			final String pref_fontcolor_colorcode_1 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_1", "#c0ffff");
			final String pref_fontcolor_colorcode_0 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_0", "#d2d2d2");
			final String pref_fontcolor_colorcode_minus1 = pref_app.getString("pref_" + ( string ? "font" : "icon" ) + "color_colorcode_minus1", "#000000");

			colorCode = pref_fontcolor_colorcode_minus1;
			if (precipitation == 80) {
				colorCode = pref_fontcolor_colorcode_80;
			} else if (precipitation == 50) {
				colorCode = pref_fontcolor_colorcode_50;
			} else if (precipitation == 30) {
				colorCode = pref_fontcolor_colorcode_30;
			} else if (precipitation == 20) {
				colorCode = pref_fontcolor_colorcode_20;
			} else if (precipitation == 10) {
				colorCode = pref_fontcolor_colorcode_10;
			} else if (precipitation == 5) {
				colorCode = pref_fontcolor_colorcode_5;
			} else if (precipitation == 1) {
				colorCode = pref_fontcolor_colorcode_1;
			} else if (precipitation == 0) {
				colorCode = pref_fontcolor_colorcode_0;
			}
		}

		Matcher m = colorCodePattern.matcher(colorCode);

		if (m.find()) {
			return colorCode;
		} else {
			final String[] colorCodes =
					{ "red", "blue", "green", "black", "white", "gray", "cyan", "magenta", "yellow", "lightgray", "darkgray", "grey", "lightgrey", "darkgrey", "aqua", "fuschia", "lime", "maroon",
							"navy", "olive", "purple", "silver", "teal" };
			for (String cc : colorCodes) {
				if (colorCode.equals(cc)) {
					return colorCode;
				}
			}
		}

		return "#000000";
	}

	private int colorToPrecipitations(int color) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);

		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean pref_source_weatherreport = pref_app.getBoolean("pref_source_weatherreport", false);

		if (pref_source_weatherreport) {

			if (red == 102 && green == 0 && blue == 0) {
				return 100;
			} else if (red == 204 && green == 0 && blue == 0) {
				return 80;
			} else if (red == 255 && green == 102 && blue == 102) {
				return 60;
			} else if (red == 255 && green == 153 && blue == 255) {
				return 40;
			} else if (red == 255 && green == 204 && blue == 51) {
				return 30;
			} else if (red == 255 && green == 255 && blue == 153) {
				return 20;
			} else if (red == 51 && green == 255 && blue == 153) {
				return 10;
			} else if (red == 0 && green == 153 && blue == 255) {
				return 5;
			} else if (red == 102 && green == 204 && blue == 255) {
				return 1;
			} else if (red == 204 && green == 255 && blue == 255) {
				return 0;
			}

		} else {

			if (red == 180 && green == 0 && blue == 104) {
				return 80;
			} else if (red == 255 && green == 40 && blue == 0) {
				return 50;
			} else if (red == 255 && green == 153 && blue == 0) {
				return 30;
			} else if (red == 250 && green == 245 && blue == 0) {
				return 20;
			} else if (red == 0 && green == 65 && blue == 255) {
				return 10;
			} else if (red == 33 && green == 140 && blue == 255) {
				return 5;
			} else if (red == 160 && green == 210 && blue == 255) {
				return 1;
			} else if (red == 242 && green == 242 && blue == 255) {
				return 0;
			}

		}

		return -1;
	}

	private void initLocationManager() {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		int pref_mintime;
		try {
			pref_mintime = 1000 * Integer.parseInt(pref_app.getString("pref_mintime", "600"));
		} catch (NumberFormatException e) {
			pref_mintime = -1;
		}
		int pref_mindistance;
		try {
			pref_mindistance = Integer.parseInt(pref_app.getString("pref_mindistance", "10"));
		} catch (NumberFormatException e) {
			pref_mindistance = -1;
		}

		if (( pref_mintime >= 0 ) && ( pref_mindistance >= 0 )) {
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			locationManager.addGpsStatusListener(this);
			locationManager.removeUpdates(this);
			List<String> providers = locationManager.getProviders(true);
			if (providers.size() > 0) {
				for (String provider : providers) {
					if (provider.equals("passive") == false) {
						toast("provider:" + provider);
						locationManager.requestLocationUpdates(provider, pref_mintime, pref_mindistance, this);
					}
				}
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand()");

		try {
			super.onStartCommand(intent, flags, startId);
		} catch (Exception e) {
		}

		notPositioning = true;
		initLocationManager();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		log("onDestroy()");

		try {
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(this);
		} catch (Exception e) {
		}

		super.onDestroy();
	}

	@SuppressWarnings("deprecation")
	// 16
	@Override
	public void onStart(Intent intent, int startId) {
		log("onStart()");

		super.onStart(intent, startId);

		setButtonIntentIfNull();

		if (( AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()) ) || ( BUTTON_CLICK_ACTION.equals(intent.getAction()) )) {
			log("onStart() BUTTON_CLICK_ACTION");

			updateTextview(Html.fromHtml(getString(R.string.now_loading) + " <small>(" + ( notPositioning ? "X" : "O" ) + ")</small>"));

			pref_app = PreferenceManager.getDefaultSharedPreferences(this);
			String pref_place;
			try {
				pref_place = pref_app.getString("pref_place", "here");
			} catch (Exception e) {
				pref_place = "here";
			}
			if (pref_place.equals("here")) {
				if (( currentLatitude < -90 ) || ( currentLatitude > 90 ) || ( currentLongitude < -180 ) || ( currentLongitude > 180 )) {
					getWeather(currentLatitude, currentLongitude);
				}
			} else if (pref_place.equals("home")) {
				double pref_lat = Double.parseDouble(pref_app.getString("pref_lat", "35.681382"));
				double pref_long = Double.parseDouble(pref_app.getString("pref_long", "139.766084"));
				getWeather(pref_lat, pref_long);
			} else if (pref_place.equals("pre")) {
				double pref_lat = Double.parseDouble(pref_app.getString("pref_pre_lat", "35.681382"));
				double pref_long = Double.parseDouble(pref_app.getString("pref_pre_long", "139.766084"));
				getWeather(pref_lat, pref_long);
			} else {
				double pref_lat = Double.parseDouble(pref_app.getString("pref_lat", "35.681382"));
				double pref_long = Double.parseDouble(pref_app.getString("pref_long", "139.766084"));
				getWeather(pref_lat, pref_long);
			}
		} else {
			setRemoteViews();
			updateRemoteViews();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		final double lat = location.getLatitude();
		final double lng = location.getLongitude();

		toast("onLocationChanged: " + String.valueOf(lat) + " , " + String.valueOf(lng));

		if (( lat >= -90 ) || ( lat <= 90 ) || ( lng >= -180 ) || ( lng <= 180 )) {
			// if (notPositioning) {
			notPositioning = false;
			// }
		}

		if (preLocation == null) {
			try {
				preLocation = new Location("dummyprovider");
				preLocation.setLatitude(35.681382);
				preLocation.setLongitude(139.766084);
			} catch (Exception e) {
			}
		}
		preLocation = location;

		SharedPreferences.Editor editor = pref_app.edit();
		editor.putString("pref_pre_lat", Double.toString(currentLatitude));
		editor.putString("pref_pre_long", Double.toString(currentLongitude));
		editor.commit();

		currentLatitude = lat;
		currentLongitude = lng;

		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		String pref_place;
		try {
			pref_place = pref_app.getString("pref_place", "here");
		} catch (Exception e) {
			pref_place = "here";
		}
		if (pref_app.getBoolean("pref_getweather_onlocationchanged", true)) {
			if (pref_place.equals("here")) {
				getWeather(lat, lng);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		try {
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(this);
		} catch (Exception e) {
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int statusInt, Bundle extras) {
		String status = "Unknown";
		if (statusInt == LocationProvider.AVAILABLE) {
			status = "AVAILABLE";
		} else if (statusInt == LocationProvider.OUT_OF_SERVICE) {
			status = "OUT OF SERVICE";
		} else if (statusInt == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			status = "TEMP UNAVAILABLE";
		}

		if (status.equals(preStatus) == false) {
			toast("onStatusChanged: status: " + status);
			preStatus = status;
		}
	}

	@Override
	public void onGpsStatusChanged(int event) {
		String status = "";
		if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
			status = "FIRST FIX";
		} else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
			status = "SATELLITE STATUS";
		} else if (event == GpsStatus.GPS_EVENT_STARTED) {
			status = "STARTED";
		} else if (event == GpsStatus.GPS_EVENT_STOPPED) {
			status = "STOPPED";
		}

		if (status.equals(preGpsStatus) == false) {
			toast("onGpsStatusChanged: status: " + status);
			preGpsStatus = status;
		}
	}

	private void toast(final String str) {
		//		log(str);
		//
		//		try {
		//			final Handler handler = new Handler();
		//			handler.post(new Runnable() {
		//				@Override
		//				public void run() {
		//					Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
		//					return;
		//				}
		//			});
		//		} catch (Exception e) {
		//		}
	}

	private void log(String str) {
		Log.v("LocNowcastWidget", str);
	}

	private void setButtonIntentIfNull() {
		try {
			if (buttonIntent == null) {
				buttonIntent = new Intent();
				buttonIntent.setAction(BUTTON_CLICK_ACTION);
			}
		} catch (Exception e) {
		}
	}

	private void setRemoteViews() {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		int pref_fontsize;
		try {
			pref_fontsize = Integer.parseInt(pref_app.getString("pref_fontsize", "12"));
		} catch (NumberFormatException e) {
			pref_fontsize = 12;
		}

		if (pref_fontsize <= 0) {
			pref_fontsize = 12;
		}

		PendingIntent pendingIntent = PendingIntent.getService(this, 0, buttonIntent, 0);
		remoteViews = new RemoteViews(getPackageName(), R.layout.main);
		remoteViews.setFloat(R.id.textView1, "setTextSize", pref_fontsize);
		remoteViews.setFloat(R.id.textView2, "setTextSize", pref_fontsize);
		remoteViews.setOnClickPendingIntent(R.id.imageView1, pendingIntent);
		remoteViews.setOnClickPendingIntent(R.id.textView1, pendingIntent);
		remoteViews.setOnClickPendingIntent(R.id.textView2, pendingIntent);
	}

	public String progressString(int num, int max) {
		return "|" + repeatString("=", num) + ">>" + repeatString("_", max - num) + "|";
	}

	public String repeatString(String str, int num) {
		return new String(new char[num]).replace("\0", str);
	}

	public void updateTextview(CharSequence string) {
		pref_app = PreferenceManager.getDefaultSharedPreferences(this);
		boolean pref_gravity = pref_app.getBoolean("pref_gravity", false);

		setRemoteViews();

		if (pref_gravity) {
			remoteViews.setViewVisibility(R.id.textView1, View.INVISIBLE);
			remoteViews.setViewVisibility(R.id.textView2, View.VISIBLE);
			remoteViews.setTextViewText(R.id.textView2, string);
		} else {
			remoteViews.setViewVisibility(R.id.textView1, View.VISIBLE);
			remoteViews.setViewVisibility(R.id.textView2, View.INVISIBLE);
			remoteViews.setTextViewText(R.id.textView1, string);
		}

		updateRemoteViews();
	}

	public void updateImageview(Bitmap bitmap) {
		setRemoteViews();
		//	URL neturl = new URL(url);
		//	Drawable drawable = Drawable.createFromStream(neturl.openStream(), "src");
		//	Bitmap bitmap = ( (BitmapDrawable) drawable ).getBitmap();
		remoteViews.setImageViewBitmap(R.id.imageView1, bitmap);
		updateRemoteViews();
	}

	public void updateRemoteViews() {
		final ComponentName componentName = new ComponentName(this, LocNowcastWidget.class);
		final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.updateAppWidget(componentName, remoteViews);
	}
}

// <!-- Copyright 2014 (c) YA <ya.androidapp@gmail.com> All rights reserved. -->