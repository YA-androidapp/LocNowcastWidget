package jp.gr.java_conf.ya.locnowcastwidget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class LocNowcastWidgetPreference extends PreferenceActivity {
	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	// 16
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);

		// リンクプリファレンス取得
		PreferenceScreen preferenceSettingLinkHrnowcast = (PreferenceScreen) getPreferenceScreen().findPreference("setting_link_hrnowcast");
		PreferenceScreen preferenceSettingLinkNowcast = (PreferenceScreen) getPreferenceScreen().findPreference("setting_link_nowcast");
		PreferenceScreen preferenceSettingLinkPlay = (PreferenceScreen) getPreferenceScreen().findPreference("setting_link_play");
		PreferenceScreen preferenceSettingLinkRevgeocoding = (PreferenceScreen) getPreferenceScreen().findPreference("setting_link_revgeocoding");
		PreferenceScreen preferenceSettingLinkShare = (PreferenceScreen) getPreferenceScreen().findPreference("setting_link_share");
		// リンククリックイベント追加
		preferenceSettingLinkHrnowcast.setOnPreferenceClickListener(new LinkOnPreference(getApplicationContext()));
		preferenceSettingLinkNowcast.setOnPreferenceClickListener(new LinkOnPreference(getApplicationContext()));
		preferenceSettingLinkPlay.setOnPreferenceClickListener(new LinkOnPreference(getApplicationContext()));
		preferenceSettingLinkRevgeocoding.setOnPreferenceClickListener(new LinkOnPreference(getApplicationContext()));
		preferenceSettingLinkShare.setOnPreferenceClickListener(new LinkOnPreference(getApplicationContext()));
	}

	// リンクイベントクラス
	private class LinkOnPreference extends AbstractOnPreference implements OnPreferenceClickListener {
		// construct
		public LinkOnPreference(Context context) {
			super(context);
		}

		// クリックイベント
		public boolean onPreferenceClick(Preference preference) {
			// 処理

			Uri uri = null;
			if (preference.getTitle().equals(getString(R.string.intent_play))) {
				uri = Uri.parse("https://play.google.com/store/apps/details?id=jp.gr.java_conf.ya.locnowcastwidget");
			} else if (preference.getTitle().equals(getString(R.string.intent_revgeocoding))) {
				uri = Uri.parse("http://www.finds.jp/wsdocs/rgeocode/index.html.ja");
			} else if (preference.getTitle().equals(getString(R.string.intent_share))) {
				uri =
						Uri.parse("https://twitter.com/intent/tweet?text=LocNowcastWidget%20%E3%82%92%E3%83%81%E3%82%A7%E3%83%83%E3%82%AF%EF%BC%81"
								+ "%20https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Djp.gr.java_conf.ya.locnowcastwidget");
			} else if (preference.getTitle().equals(getString(R.string.intent_nowcast))) {
				uri = Uri.parse("http://www.jma.go.jp/jp/radnowc/");
			} else if (preference.getTitle().equals(getString(R.string.intent_hrnowcast))) {
				uri = Uri.parse("http://www.jma.go.jp/jp/highresorad/");
			}
			if (uri != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
			return true;
		}
	}

	// 基底イベントクラス
	protected abstract class AbstractOnPreference {
		protected Context mContext;

		protected AbstractOnPreference(Context context) {
			mContext = context;
		}
	}
}

//<!-- Copyright 2014 (c) YA <ya.androidapp@gmail.com> All rights reserved. -->