package com.csci3310project.simplealarmclock;

import java.util.List;

import com.csci3310project.simplealarmclock.database.Database;
import com.csci3310project.simplealarmclock.preferences.AlarmPreferencesActivity;
import com.csci3310project.simplealarmclock.service.AlarmServiceBroadcastReciever;

import android.os.Bundle;
import android.os.Handler;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmActivity extends ListActivity implements
		android.view.View.OnClickListener {

	ImageButton newButton;
	TextView newAlarm;
	ListView mathAlarmListView;
	AlarmListAdapter alarmListAdapter;
	String nextAlarmString;
	Alarm closestAlarm;
	TextView nextAlarm;
	Runnable mStatusChecker;
	
	private int mInterval = 1000; // 5 seconds by default, can be changed later
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alarm_activity);

		newButton = (ImageButton) findViewById(R.id.button_new);
		newAlarm = (TextView) findViewById(R.id.textView_title_bar);
		newButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					newButton.setBackgroundColor(getResources().getColor(
							R.color.holo_blue_light));
					newAlarm.setBackgroundColor(getResources().getColor(
							R.color.holo_blue_light));
					break;
				case MotionEvent.ACTION_UP:
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					Intent newAlarmIntent = new Intent(AlarmActivity.this,
							AlarmPreferencesActivity.class);
					startActivity(newAlarmIntent);
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_CANCEL:
					newButton.setBackgroundColor(getResources().getColor(
							android.R.color.transparent));
					newAlarm.setBackgroundColor(getResources().getColor(
							android.R.color.transparent));
					break;
				}
				return true;
			}
		});
		
		newAlarm.setClickable(true);
		newAlarm.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					newButton.setBackgroundColor(getResources().getColor(
							R.color.holo_blue_light));
					newAlarm.setBackgroundColor(getResources().getColor(
							R.color.holo_blue_light));
					break;
				case MotionEvent.ACTION_UP:
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					Intent newAlarmIntent = new Intent(AlarmActivity.this,
							AlarmPreferencesActivity.class);
					startActivity(newAlarmIntent);
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_CANCEL:
					newButton.setBackgroundColor(getResources().getColor(
							android.R.color.transparent));
					newAlarm.setBackgroundColor(getResources().getColor(
							android.R.color.transparent));
					break;
				}
				return true;
			}
		});

		mathAlarmListView = (ListView) findViewById(android.R.id.list);

		mathAlarmListView.setLongClickable(true);
		mathAlarmListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> adapterView,
							View view, int position, long id) {
						view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
						final Alarm alarm = (Alarm) alarmListAdapter
								.getItem(position);
						Builder dialog = new AlertDialog.Builder(
								AlarmActivity.this);
						dialog.setTitle("Delete");
						dialog.setMessage("Delete this alarm?");
						dialog.setPositiveButton("Ok", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								alarmListAdapter.getMathAlarms().remove(alarm);
								alarmListAdapter.notifyDataSetChanged();

								Database.init(AlarmActivity.this);
								Database.deleteEntry(alarm);

								AlarmActivity.this
										.callMathAlarmScheduleService();
							}
						});
						dialog.show();
						alarmListAdapter.getItem(position);
						return true;
					}
				});
		
		mHandler = new Handler();
		mStatusChecker = new Runnable() {
			@Override 
		    public void run() {
				displayClosestAlarm();
				mHandler.postDelayed(mStatusChecker, mInterval);
		    }
		};
		
		startRepeatingTask();

		callMathAlarmScheduleService();
	}

	void startRepeatingTask() {
	    mStatusChecker.run(); 
	}

	void stopRepeatingTask() {
	    mHandler.removeCallbacks(mStatusChecker);
	}
	
	private void callMathAlarmScheduleService() {
		Intent mathAlarmServiceIntent = new Intent(AlarmActivity.this,
				AlarmServiceBroadcastReciever.class);
		sendBroadcast(mathAlarmServiceIntent, null);
	}
	
	private void displayClosestAlarm(){
		closestAlarm = getClosestAlarm();
		nextAlarm = (TextView) findViewById(R.id.nextAlarm);
		if (closestAlarm != null)
			nextAlarmString = new String(closestAlarm.getTimeUntilNextAlarmMessage());
		else
			nextAlarmString = "No alarm has been set";
		nextAlarm.setText(nextAlarmString);
	}
	
	public Alarm getClosestAlarm(){
		Alarm closestAlarm = null;
		if (alarmListAdapter != null){
			List<Alarm> alarms = alarmListAdapter.getMathAlarms();
			long size = alarmListAdapter.getCount();
			int i = 0, j = 0;
			for (i = 0; i < size; i++){
				closestAlarm = alarms.get(i);
				if (!closestAlarm.getAlarmActive()){
					closestAlarm = null;
				} else{
					break;
				}
			}
			if (closestAlarm != null){
				for (j = i + 1; j < size; j++){
					long timeInterval = closestAlarm.getAlarmTime().getTimeInMillis();
					Alarm tmp = null;
					tmp = alarms.get(j);
					if (tmp.getAlarmActive()){
						long timeRemaining = tmp.getAlarmTime().getTimeInMillis();
						if (timeInterval > timeRemaining){
							closestAlarm = tmp;
							timeInterval = timeRemaining;
						}
					}
				}
			}
		}
		return closestAlarm;
	}

	@Override
	protected void onPause() {
		// setListAdapter(null);
		Database.deactivate();
		stopRepeatingTask();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		@SuppressWarnings("deprecation")
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			alarmListAdapter = new AlarmListAdapter(this);
		} else {
			alarmListAdapter = (AlarmListAdapter) data;
		}

		this.setListAdapter(alarmListAdapter);
		startRepeatingTask();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return alarmListAdapter;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		Alarm alarm = (Alarm) alarmListAdapter.getItem(position);
		Intent intent = new Intent(AlarmActivity.this,
				AlarmPreferencesActivity.class);
		intent.putExtra("alarm", alarm);
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.checkBox_alarm_active) {
			CheckBox checkBox = (CheckBox) v;
			Alarm alarm = (Alarm) alarmListAdapter.getItem((Integer) checkBox
					.getTag());
			alarm.setAlarmActive(checkBox.isChecked());
			Database.update(alarm);
			AlarmActivity.this.callMathAlarmScheduleService();
			if (checkBox.isChecked()) {
				Toast.makeText(AlarmActivity.this,
						alarm.getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG)
						.show();
			}
		}
	}
}
