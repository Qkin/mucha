package pl.gra;

import java.util.Random;

import org.andengine.engine.Engine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;

import android.view.KeyEvent;

/**
 * Klasa g³ówna gry mucha
 * @author Qkin
 *
 */
public class MainActivityOriginal extends SimpleBaseGameActivity{
	
	//szerokoœæ ekranu
	private static final int CAMERA_WIDTH = 720;
	//wysokoœæ ekranu
	private static final int CAMERA_HEIGHT = 480;
	
	public Engine engine;
	
	private HUD gameHUD;
	private Text scoreText;
	private int score;
	
	private Camera camera;
	@Override
	public EngineOptions onCreateEngineOptions() {
		camera = new Camera(0, 0, MainActivityOriginal.CAMERA_WIDTH, MainActivityOriginal.CAMERA_HEIGHT);
		
		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(MainActivityOriginal.CAMERA_WIDTH, MainActivityOriginal.CAMERA_HEIGHT), camera);
		engineOptions.getAudioOptions().setNeedsMusic(true);
		
		return engineOptions;
	}
	private Resources resources ;
	
	@Override
	public void onCreateResources() {
		
		resources = new Resources();
		//resources.loadGameGraphics(this);
		//resources.loadGameAudio(this, this.mEngine.getMusicManager());
		//resources.loadFonts(this);
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		
		////////////////////////////////
		//Wybór losowej krawêdzi ekranu z której ma startowaæ mucha
		////////////////////////////////
		float xPos = 0;
		float yPos = 0;		
		
		Random rand = new Random();
		int direction = rand.nextInt(4);
		switch(direction){
			case 0: //lewa krawêdŸ
				yPos = rand.nextInt(MainActivityOriginal.CAMERA_HEIGHT);
			break;
			case 1: // górna krawêdŸ
				xPos = rand.nextInt(MainActivityOriginal.CAMERA_WIDTH);
			break;
			case 2: // prawa krawêdŸ
				yPos = rand.nextInt(MainActivityOriginal.CAMERA_HEIGHT);
				xPos = MainActivityOriginal.CAMERA_WIDTH;
			break;
			case 3: //dolna krawêdŸ 
				xPos = rand.nextInt(MainActivityOriginal.CAMERA_WIDTH);
				yPos = MainActivityOriginal.CAMERA_HEIGHT;
			break;
		}
		/////////////////////////////////////////////////////////

		//Tworzymy obiekt muchy
		final Fly fly = new Fly(xPos, yPos, resources.mFlyTextureRegion, this.getVertexBufferObjectManager(), 1/*Ta 1 do wyrzucenia, resources.mMusic*/);
		//dodajemy muchê do sceny
		scene.attachChild(fly);
		fly.setCamera(MainActivityOriginal.CAMERA_WIDTH, MainActivityOriginal.CAMERA_HEIGHT);
		//startujemy muchê
		fly.startFlying();
		
		gameHUD = new HUD();
		
		scoreText = new Text(20, 0, resources.font, "Score: 0123456789", new TextOptions(HorizontalAlign.LEFT), this.getVertexBufferObjectManager());
		//scoreText.setAnchorCenter(0, 0);	
		scoreText.setText("Score: 0");
		gameHUD.attachChild(scoreText);
		
		camera.setHUD(gameHUD);
		
		//stworzenie obiektu nie¿ywej muchy
		
		
		//final Fly fly2 = new Fly(100, 100, resources.mFlyTextureRegion, this.getVertexBufferObjectManager(), resources.mMusic);
		//scene.attachChild(fly2);
		
		//rejestrujemy obszar dotyku obiektu aby nastêpnie móc wychwyciæ i obs³u¿yæ naciœniêcie na niego
		scene.registerTouchArea(fly);
		
		//Nas³uchiwanie czy klikniêto w zarejestrowane wczeœniej obszary
		scene.setOnAreaTouchListener(new IOnAreaTouchListener() {
			@Override
			public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
				if(pSceneTouchEvent.isActionDown()) {
					if(fly.isFlying()){
						//odmontowanie grafiki muchy
						fly.detachSelf();
						//ustawienie pozycji na osiach X i Y dla nie¿ywej muchy
						float x = fly.getX();
						float y = fly.getY() + fly.getHeight()/2;
						final Sprite deadFly = new Sprite(x, y, resources.mDeadFlyTextureRegion, MainActivityOriginal.this.getVertexBufferObjectManager());
	
						//dodanie do sceny gry grafiki dla nie¿ywej muchy
						
						scene.attachChild(deadFly);
						fly.setKillFlyNoise(resources.mMusicFlyKill);
						fly.killTheFly();
						MainActivityOriginal.this.addScore();
						scoreText.setText("Score: " + score);
					}
				}

				return true;
			}
		});
		
		return scene;
	}
	
	/**
	 * Obs³uga przycisków
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		//klawisz wstecz
	    if (keyCode == KeyEvent.KEYCODE_BACK)
	    { 	
	    	resources.mMusic.stop();
	    	MainActivityOriginal.this.finish();
	    }

	    super.onKeyDown(keyCode, event);

	    return true;
	}
	
	private void addScore(){
		score++;
	}
	
}
