package pl.gra;

import java.io.IOException;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.music.MusicManager;

import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;

import android.graphics.Color;

/**
 * Klasa ³aduj¹ca grafikê i dŸwiêki do gry
 * 
 * @author Kamil Kunikowski
 * @author Krzysztof Longa
 * 
 */
public class Resources {
	public BitmapTextureAtlas mFlyTextureAtlas;
	public TiledTextureRegion mFlyTextureRegion;

	public BitmapTextureAtlas mDeadFlyTextureAtlas;
	public TiledTextureRegion mDeadFlyTextureRegion;

	public Music mMusic;
	public Music mMusicFlyKill;

	public Font font;

	/**
	 * metoda ³aduje grafikê do gry
	 */
	public void loadGameGraphics(SimpleBaseGameActivity activity) {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mFlyTextureAtlas = new BitmapTextureAtlas(
				activity.getTextureManager(), 128, 64, TextureOptions.BILINEAR);
		this.mFlyTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mFlyTextureAtlas, activity,
						"mucha.png", 0, 0, 2, 1);
		this.mFlyTextureAtlas.load();

		this.mDeadFlyTextureAtlas = new BitmapTextureAtlas(
				activity.getTextureManager(), 64, 32, TextureOptions.BILINEAR);
		this.mDeadFlyTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(mDeadFlyTextureAtlas, activity,
						"muchaDead.png", 0, 0, 1, 1);
		this.mDeadFlyTextureAtlas.load();
	}

	/**
	 * Metoda ³aduje dŸwiêki do gry
	 */
	public void loadGameAudio(SimpleBaseGameActivity activity,
			MusicManager musicManager) {
		MusicFactory.setAssetBasePath("mfx/");
		try {
			this.mMusic = MusicFactory.createMusicFromAsset(musicManager,
					activity, "mucha_fly.wav");
			this.mMusic.setLooping(true);
			this.mMusicFlyKill = MusicFactory.createMusicFromAsset(
					musicManager, activity, "mucha_kill.wav");
		} catch (final IOException e) {
			Debug.e(e);
		}
	}

	/**
	 * Metoda ³aduje czcionki do gry
	 */
	public void loadFonts(SimpleBaseGameActivity activity) {
		FontFactory.setAssetBasePath("font/");
		final ITexture mainFontTexture = new BitmapTextureAtlas(
				activity.getTextureManager(), 256, 256,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		font = FontFactory.createStrokeFromAsset(activity.getFontManager(),
				mainFontTexture, activity.getAssets(), "font.ttf", 30, true,
				Color.WHITE, 2, Color.BLACK);
		font.load();
	}
}