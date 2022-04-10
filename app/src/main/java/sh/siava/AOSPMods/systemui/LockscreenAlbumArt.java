package sh.siava.AOSPMods.systemui;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.IXposedModPack;

public class LockscreenAlbumArt implements IXposedModPack {
	public static final String listenPackage = "com.android.systemui";
	
	
	Context mContext;
	MediaSessionManager mediaSessionManager;
	MediaMetadata mCurrentMedia;
	boolean nowPlaying = false;
	List<MediaController> sessions = new ArrayList<>();
	Object WallpaperService;
	
	@Override
	public void updatePrefs(String... Key) {
	
	}
	
	@Override
	public String getListenPack() {
		return listenPackage;
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage)) return;
		
		Class NotificationListenerWithPluginsClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationListenerWithPlugins", lpparam.classLoader);
		Class LockscreenWallpaperClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.LockscreenWallpaper", lpparam.classLoader);
		Class Ldd = XposedHelpers.findClass("com.android.systemui.statusbar.BackDropView", lpparam.classLoader);
		
		XposedBridge.hookAllConstructors(Ldd, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						((FrameLayout) param.thisObject).setVisibility(View.VISIBLE);
					}
				});
		
		
		MediaSessionManager.OnActiveSessionsChangedListener activeSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
			@Override
			public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
				registerListeners(controllers);
			}
		};
		
		XposedBridge.hookAllConstructors(NotificationListenerWithPluginsClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mContext = AOSPMods.mContext;
				
				ComponentName cn = new ComponentName(mContext, NotificationListenerWithPluginsClass);
				mediaSessionManager = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
				mediaSessionManager.addOnActiveSessionsChangedListener(activeSessionsChangedListener, cn);
				XposedBridge.log("manager register");
				
				List<MediaController> sessions =  mediaSessionManager.getActiveSessions(cn);
				registerListeners(sessions);
			}
		});
		
		XposedBridge.hookAllConstructors(LockscreenWallpaperClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				WallpaperService = param.thisObject;
			}
		});
	}
	
	private void registerListeners(List<MediaController> sessions) {
		XposedBridge.log("registering");
		
		this.sessions = sessions;
		MediaController.Callback mediaCallback = new MediaController.Callback() {
			@Override
			public void onPlaybackStateChanged(@Nullable PlaybackState state) {
				super.onPlaybackStateChanged(state);
				
				playbackChanged(state);
			}
		};
		
		for(MediaController controller : sessions)
		{
			controller.registerCallback(mediaCallback);
		}
	}
	
	private void playbackChanged(PlaybackState state)
	{
		boolean nowPlaying = true;
		XposedBridge.log("state " + state);
		if((state.getState() & PlaybackState.STATE_PLAYING) != PlaybackState.STATE_PLAYING)
		{
			nowPlaying = false;
		}
		
		if(nowPlaying != this.nowPlaying)
		{
			this.nowPlaying = nowPlaying;
			if(nowPlaying)
			{
				setAlbumArt();
				return;
			}
			removeAlbumArt();
		}
	}
	
	private void setAlbumArt() {
		XposedBridge.log("get art");
		Bitmap art = getAlbumArt();
		XposedBridge.log("got art");

		if(art == null)
		{
			removeAlbumArt();
			return;
		}
		
		XposedHelpers.setObjectField(WallpaperService, "mCache", art);
		XposedHelpers.setObjectField(WallpaperService, "mCached", true);
		XposedHelpers.callMethod(WallpaperService, "postUpdateWallpaper");
		XposedBridge.log("posted");
	}
	
	private void removeAlbumArt() {
		XposedHelpers.setObjectField(WallpaperService, "mCached", false);
		XposedHelpers.callMethod(WallpaperService, "postUpdateWallpaper");
	}
	
	@Nullable
	private Bitmap getAlbumArt()
	{
		for(MediaController session : sessions)
		{
			if((session.getPlaybackState().getState() & PlaybackState.STATE_PLAYING) == PlaybackState.STATE_PLAYING)
			{
				return session.getMetadata().getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
			}
		}
		return null;
	}
}
