package pl.gra;

import java.io.IOException;
import java.util.Random;

import org.andengine.audio.music.Music;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.debug.Debug;

import android.util.Log;

/**
 * Klasa obsugująca ruch muchy
 * 
 * @author Kamil Kunikowski
 * @author Krzysztof Longa
 * 
 */
public class Fly extends AnimatedSprite {
	public final PhysicsHandler mPhysicsHandler;
	// prędkość poruszania się muchy ( gdy wartość ujemna, prędkość przybiera przeciwny zwrot)
	public float velocity = 150.0f;
	//szerokość i wysokość ekranu
	private int cameraWidth = 720;
	private int cameraHeight = 480;
	//zmienna do pętli generującej ruch muchy
	private int updateCounter = 0;
	//zwrot na osi X. Przyjmuje wartości 1(zgodnie z osią X) oraz -1 (przeciwnie do osi X)
	private byte xTurn = 1;
	//zwrot na osi Y. Przyjmuje wartości 1(zgodnie z osią Y) oraz -1 (przeciwnie do osi Y)
	private byte yTurn = 1;
	//czy jest w trakcie lotu.
	private boolean isFlying = false;
	//id obiektu
	private int pID;


	// dźwięk lotu muchy
	private Music flyNoise;
	private Music killFlyNoise;
	
	/**
	 * Konstruktor
	 * @param pX - początkowa wartość X
	 * @param pY - początkowa wartość Y
	 * @param pTextureRegion - region grafiki
	 * @param pVertexBufferObjectManager
	 * @param pID - ID obiektu
	 * @param pMusic - dźwięk poruszającego się obiektu
	 */
	public Fly(final float pX, final float pY, final TiledTextureRegion pTextureRegion,
			final VertexBufferObjectManager pVertexBufferObjectManager, int pID/*, Music pMusic*/) {
		super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		this.mPhysicsHandler = new PhysicsHandler(this);
		this.registerUpdateHandler(this.mPhysicsHandler);
		this.mPhysicsHandler.setVelocity(this.velocity, this.velocity);
		this.pID = pID;
		// this.flyNoise = pMusic;
		// this.flyNoise.play();
		//jak często (w ms) mają zmieniać się animowane obrazki
		this.animate(50);
	}

	/**
	 * metoda ustawia szerokość i wysokość ekranu
	 */
	public void setCamera(int cameraWidth, int cameraHeight) {
		this.cameraWidth = cameraWidth;
		this.cameraHeight = cameraHeight;
	}

	/**
	 * metoda ustawia prędkość obiektu
	 */
	public void setSpeed(float speed) {
		this.velocity = speed;
	}

	/**
	 * metoda zwraca aktualną prędkość obiektu
	 */
	public float getSpeed() {
		return this.velocity;
	}

	/**
	 * metoda usatwia dźwięk zabicia muchy
	 */
	public void setKillFlyNoise(Music noise) {
		this.killFlyNoise = noise;
	}

	/**
	 * Metoda zatrzymuje ruchy muchy
	 */
	public void stopFlying() {
		//this.isFlying = false;
		//this.flyNoise.stop();
		this.setSpeed(0);
	}

	public void startFlying() {
		this.isFlying = true;
	}

	/**
	 * metoda odpowiedzialna za zabicie muchy, czyli zatrzymanie jej ruchów i
	 * wygenerowanie dźwięku zabicia.
	 */
	public void killTheFly() {
		this.stopFlying();
		//this.killFlyNoise.play();
	}

	/**
	 * metoda sprawdza czy mucha jest w fazie lotu
	 */
	public boolean isFlying() {
		return this.isFlying;

	}

	/**
	 * Metoda odpowiedzialna za poruszanie sie muchy. Wykorzystywana zmienna
	 * VELOCITY posiada wartosc predkosci oraz zwrot. Jesli jest dodatnia to
	 * zwrot zgodny z osia, jesli ujemna to przeciwny
	 */
	@Override
	protected void onManagedUpdate(final float pSecondsElapsed) {
		if (this.isFlying()) {
			// Warunek potrzebny, aby kierunek ruchy muchy nie byl zmieniany co
			// kazda aktualizacje, tylko co konkretna liczbe uaktualnien
			if (this.updateCounter > 10) {
				this.updateCounter = 0;
				this.randomXYTurn();
			}
			this.updateCounter++;
			
			// ////////////////////////////////////////////////////////////////////////
			// jesli obiekt chce przejsc poza granice ekranu to zmieniamy jego
			// zwrot
			// ////////////////////////////////////////////////////////////////////////
			if (this.mX < 0) {
				this.xTurn = 1;
			} else if (this.mX + this.getWidth() > this.cameraWidth) {
				this.xTurn = -1;
			}
			if (this.mY < 0) {
				this.yTurn = 1;
			} else if (this.mY + this.getHeight() > this.cameraHeight) {
				this.yTurn = -1;
			}
			
			// ////////////////////////////////////////////////////////////////////////
			// Ustawiamy predkosc i zwrot dla ruchu muchy na osiach X oraz Y
			this.mPhysicsHandler.setVelocityX(this.xTurn * this.velocity);
			this.mPhysicsHandler.setVelocityY(this.yTurn * this.velocity);
		} else {
			this.mPhysicsHandler.setVelocityX(0);
			this.mPhysicsHandler.setVelocityY(0);
		}
		super.onManagedUpdate(pSecondsElapsed);
	}
	
	/**
	 * 
	 * @param xTurn
	 * @param yTurn
	 */
	public void setXYTurn(byte xTurn, byte yTurn) {
		this.xTurn = xTurn;
		this.yTurn = yTurn;
	}
	
	private void randomXYTurn() {
		//losujemy czy zwrot ma byc zgodny czy przeciwny do osi.
		Random rand = new Random();
		this.xTurn = (byte) rand.nextInt(2);
		this.yTurn = (byte) rand.nextInt(2);
		if (this.xTurn == 0) {
			this.xTurn = -1;
		}
		if (this.yTurn == 0) {
			this.yTurn = -1;
		}
	}

}
