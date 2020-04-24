package com.soon.karat.exoplayer.complex_examples;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.Util;
import com.soon.karat.exoplayer.R;
import com.soon.karat.exoplayer.ThumbNailPlayerView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The code in this file was based on the ExoPlayer demo app, you can find it in
 * their github repository here:
 * https://github.com/google/ExoPlayer
 * <p>
 * The explanation for the code in this file is in this repository's README, you can find it here:
 * https://github.com/soonsam123/UsagesOfExoplayer
 */
public class PlayerActivity extends AppCompatActivity implements View.OnClickListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlayerActivity";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    private static final String TYPE_DASH = "dash";
    private static final String TYPE_OTHER = "other";

    private SimpleExoPlayer player;
    private ThumbNailPlayerView mPlayerView;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private ImageButton mBack;
    private ImageButton mLike;
    private ImageButton mShare;
    private ImageButton mFullscreen;
    private ImageButton mVolumeOff;
    private ImageButton mVolumeOn;
    private TextView mMinus10seconds;
    private TextView mPlus10seconds;

    private Spinner spinner;

    private LinearLayout mPlayPauseLayout;
    private ProgressBar mProgressBar;

    private LinearLayout debugRootView;
    private SeekBar volumeSeekbar, brightnessSeekbar;

    private long startPosition;
    private int startWindow;
    private boolean isFullScreen;
    private boolean startAutoPlay;
    private AudioManager audioManager;

    private TextView txtPer;

    //Variable to store brightness value
    private int brightness;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;

    AudioManager.OnAudioFocusChangeListener afChangeListener;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private Button txtPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_complex);

        setupWidgets();
        setupClickListeners();
        setPlayerViewDimensions();

        if (savedInstanceState != null) {
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
            clearStartPosition();
        }
        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                float speed = Float.parseFloat(parent.getSelectedItem().toString());
                setVideoSpeed(speed);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // volume increase decrease with system
        setVolumeSeekbar();

        // brightness change
        setBrightness();

        // textview randomly display on screen
        setRandomTextViewPosition();

        //register the listener for notication and incoming calls
        audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


    }

    private void setVolumeSeekbar() {
        volumeSeekbar = findViewById(R.id.volume_seekbar);

        volumeSeekbar.setMax(audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeSeekbar.setProgress(audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC));

        volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        progress, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setRandomTextViewPosition() {
        txtPlayer = findViewById(R.id.my_text);
        final DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Random R = new Random();
                        final float dx = R.nextFloat() * displaymetrics.widthPixels;
                        final float dy = R.nextFloat() * displaymetrics.heightPixels;
                        txtPlayer.animate()
                                .x(dx)
                                .y(dy)
                                .setDuration(0)
                                .start();
                    }
                });
            }
        }, 0, 1000);

    }

    private void setBrightness() {
        brightnessSeekbar = findViewById(R.id.brightness_seekBar);
        txtPer = findViewById(R.id.txt_per);
        //Get the content resolver
        cResolver = getContentResolver();

        //Get the current window
        window = getWindow();

        // get current brightness of the system
        brightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, -1);

        //Set the seekbar range between 0 and 255
        //seek bar settings//
        //sets the range between 0 and 255
        brightnessSeekbar.setMax(255);
        //set the seek bar progress to 1
        brightnessSeekbar.setKeyProgressIncrement(1);

        try {
            //Get the current system brightness
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }

        //Set the progress of the seek bar based on the system's brightness
        if (brightness > 255) {
            brightnessSeekbar.setProgress(brightness - 255);
        } else {
            brightnessSeekbar.setProgress(255 - brightness);
        }

        //Register OnSeekBarChangeListener, so it can actually change values
        brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onStopTrackingTouch(SeekBar seekBar) {
                //changeScreenBrightness(seekBar.getProgress());
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                //Nothing handled here
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightness = progress;
                //Calculate the brightness percentage
                float perc = (brightness / (float) 255) * 100;
                //Set the brightness percentage
                txtPer.setText((int) perc + " %");
                changeScreenBrightness(progress);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void changeScreenBrightness(int mBrightness) {
        brightness = mBrightness;

        if (!Settings.System.canWrite(this)) {
            Intent perminssionIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            perminssionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(perminssionIntent);

        } else {
            //Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, (mBrightness*255));
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, (mBrightness));
        }

        WindowManager.LayoutParams mLayoutParams = window.getAttributes();
        mLayoutParams.screenBrightness = mBrightness;
        window.setAttributes(mLayoutParams);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        releasePlayer();
        clearStartPosition();
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //later unregister it when you do not need it anymore

        audioManager.abandonAudioFocus(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_button_back:
                finish();
                break;
            case R.id.image_button_like:
                Toast.makeText(this, "Like", Toast.LENGTH_SHORT).show();
                break;
            case R.id.image_button_share:
                Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show();
                break;
            case R.id.image_button_full_screen:
                Toast.makeText(this, "Fullscreen", Toast.LENGTH_SHORT).show();
                if (!isFullScreen) {
                    mPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    isFullScreen = true;
                } else {
                    mPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    isFullScreen = false;
                }
                break;
            case R.id.text_minus_10_seconds:
                player.seekTo(player.getCurrentPosition() - 10000);
                break;
            case R.id.text_plus_10_seconds:
                player.seekTo(player.getCurrentPosition() + 10000);
                break;
        }
        if (v.getParent() == debugRootView) {
            MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                CharSequence title = ((Button) v).getText();
                int rendererIndex = (int) v.getTag();
                int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
               /* boolean allowAdaptiveSelections =
                        rendererType == C.TRACK_TYPE_VIDEO ||
                                (rendererType == C.TRACK_TYPE_AUDIO
                                        && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                        == MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS);*/
                boolean allowAdaptiveSelections =
                        rendererType == C.TRACK_TYPE_VIDEO ||
                                (rendererType == C.TRACK_TYPE_AUDIO
                                        && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                        == MappedTrackInfo.RENDERER_SUPPORT_PLAYABLE_TRACKS);
                Pair<AlertDialog, TrackSelectionView> dialogPair =
                        TrackSelectionView.getDialog(this, title, trackSelector, rendererIndex);
                dialogPair.second.setShowDisableOption(true);
                dialogPair.second.setAllowAdaptiveSelections(allowAdaptiveSelections);
                dialogPair.second.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING);
                dialogPair.first.show();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setPlayerViewDimensionsForLandScapeMode();

        } else {
            setPlayerViewDimensionsForPortraitMode();
        }
    }

    private void setupWidgets() {

        mPlayerView = findViewById(R.id.player_view);
        mPlayerView.setErrorMessageProvider(new PlayerErrorMessageProvider());

        mBack = findViewById(R.id.image_button_back);
        mLike = findViewById(R.id.image_button_like);
        mShare = findViewById(R.id.image_button_share);
        mFullscreen = findViewById(R.id.image_button_full_screen);
        mMinus10seconds = findViewById(R.id.text_minus_10_seconds);
        mPlus10seconds = findViewById(R.id.text_plus_10_seconds);

        mPlayPauseLayout = findViewById(R.id.linear_layout_play_pause);
        mProgressBar = findViewById(R.id.progress_bar);

        debugRootView = findViewById(R.id.controls_root);
    }

    private void setupClickListeners() {
        mBack.setOnClickListener(this);
        mLike.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mFullscreen.setOnClickListener(this);
        mMinus10seconds.setOnClickListener(this);
        mPlus10seconds.setOnClickListener(this);
    }

    private void setPlayerViewDimensionsForLandScapeMode() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        hideSystemUi();
        mPlayerView.setDimensions(width, height);
    }

    private void setPlayerViewDimensionsForPortraitMode() {
        // 1 (width) : 1/1.5 (height) --> Height is 66% of the width when in Portrait mode.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        Double heightDouble = width / 1.5;
        Integer height = heightDouble.intValue();

        mPlayerView.setDimensions(width, height);
    }

    private void setPlayerViewDimensions() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setPlayerViewDimensionsForLandScapeMode();
        } else {
            setPlayerViewDimensionsForPortraitMode();
        }
    }

    private void initializePlayer() {
        if (player == null) {
            TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(trackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTrackGroupArray = null;
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            player.addListener(new PlayerEventListener());
            mPlayerView.setPlayer(player);
            player.setPlayWhenReady(startAutoPlay);
        }
        String liveUrl = "https://svs.itworkscdn.net/lanatvlive/lana/playlist.m3u8?DVR";
        String url = "http://yt-dash-mse-test.commondatastorage.googleapis.com/media/feelings_vp9-20130806-manifest.mpd";
        //MediaSource videoSource = buildMediaSource(TYPE_DASH, Uri.parse(url));
        MediaSource videoSource = buildMediaSource(TYPE_OTHER, Uri.parse(getString(R.string.video_mp4_sintel)));
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        player.prepare(videoSource, !haveStartPosition, true);
        updateButtonVisibilities();
    }

    private void releasePlayer() {
        if (player != null) {
            updateStartPosition();
            player.removeListener(new PlayerEventListener());
            player.addVideoListener(null); // Is it necessary to remove these listeners? afraid of memory leak. OOM
            player.release();
            player = null;
            trackSelector = null;
        }
    }

    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private MediaSource buildMediaSource(String type, Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)), BANDWIDTH_METER);
        switch (type) {
            case TYPE_DASH:
                DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(dataSourceFactory);
                return new DashMediaSource.Factory(dashChunkSourceFactory, dataSourceFactory).createMediaSource(uri);
            case TYPE_OTHER:
                return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @SuppressLint("InlinedApi")
    // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY min API is 19, current min is 18.
    private void hideSystemUi() {
        mPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void updateButtonVisibilities() {
        debugRootView.removeAllViews();
        if (player == null) {
            return;
        }

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                Button button = new Button(this);
                int label;
                switch (player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.track_selection_audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.track_selection_video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.track_selection_text;
                        break;
                    default:
                        continue;
                }
                button.setText(label);
                button.setTag(i);
                button.setOnClickListener(this);
                debugRootView.addView(button);
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.e("Audio Focus ", String.valueOf(focusChange));
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN: // 1
                initializePlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS: // - 1 * audio focus gain
                releasePlayer();
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT: // 2
                releasePlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: // -1 * audiofocusTransient
                break;
        }

    }

    private class PlayerEventListener extends Player.DefaultEventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE: // The player does not have any media to play.
                    stateString = "Player.STATE_IDLE";
                    mProgressBar.setVisibility(View.VISIBLE);
                    mPlayerView.hideController();
                    break;
                case Player.STATE_BUFFERING: // The player needs to load media before playing.
                    stateString = "Player.STATE_BUFFERING";
                    mProgressBar.setVisibility(View.VISIBLE);
                    mPlayPauseLayout.setVisibility(View.GONE);
                    break;
                case Player.STATE_READY: // The player is able to immediately play from its current position.
                    stateString = "Player.STATE_READY";
                    mProgressBar.setVisibility(View.GONE);
                    mPlayPauseLayout.setVisibility(View.VISIBLE);
                    break;
                case Player.STATE_ENDED: // The player has finished playing the media.
                    stateString = "Player.STATE_ENDED";
                    break;
                default:
                    stateString = "UNKNOWN_STATE";
                    break;
            }
            Log.i(TAG, "onPlayerStateChanged: Changed to State: " + stateString + " - startAutoPlay: " + playWhenReady);
            updateButtonVisibilities();
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            super.onPositionDiscontinuity(reason);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            super.onPlayerError(error);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            updateButtonVisibilities();
            if (trackGroups != lastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        Toast.makeText(PlayerActivity.this, getString(R.string.error_unsupported_video), Toast.LENGTH_SHORT).show();
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        Toast.makeText(PlayerActivity.this, getString(R.string.error_unsupported_audio), Toast.LENGTH_SHORT).show();
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException throwable) {
            String errorString = "Playback Error DEBUGGING THIS ERROR";
            return Pair.create(0, errorString);
        }
    }

    private void setVideoSpeed(float speed) {
        PlaybackParameters param = new PlaybackParameters(speed);
        player.setPlaybackParameters(param);
    }
}
