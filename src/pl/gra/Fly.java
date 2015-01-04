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

import pl.gra.MainActivity.MoveFaceServerMessage;

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
	//pozycje startowe
	public int startPosX = 0;
	public int startPosY = 0;

	// szerokość ekranu
	private int cameraWidth = 720;
	// wysokość ekranu
	private int cameraHeight = 480;
	// zmienna do pętli generującej ruch muchy
	private int updateCounter = 0;
	// zwrot na osi X. Przyjmuje wartości 1(zgodnie z osią X) oraz -1
	// (przeciwnie do osi X)
	private byte xTurn = 1;
	// zwrot na osi Y. Przyjmuje wartości 1(zgodnie z osią Y) oraz -1
	// (przeciwnie do osi Y)
	private byte yTurn = 1;
	// Czy jest w trakcie lotu.
	private boolean isFlying = false;
	// id obiektu
	private int pID;


	// dźwięk lotu muchy
	private Music flyNoise;
	private Music killFlyNoise;
	
	private MainActivity activity;
	/**
	 * Konstruktor
	 * @param pX - początkowa wartość X muchy
	 * @param pY - początkowa wartość Y muchy
	 * @param pTextureRegion - region grafiki muchy
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
		// jak często (w ms) mają zmieniać się animowane obrazki
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
	 * metoda ustawia wartość początkową muchy
	 */
	public void setStartPosition(int x, int y) {
		this.startPosX = x;
		this.startPosY = y;
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
	
	public void setActivity(MainActivity activity){
		this.activity = activity;
	}
	
	//Losuje krawędź ekranu z której ma startować mucha i zapisuje współrzędne początkowe
	public void randomStartPosition(){
		this.startPosX = 0; 
		this.startPosY = 0;
		Random rand = new Random(); 
		int direction = rand.nextInt(4);
		switch(direction) { 
			case 0: //lewa krawędź 
				this.startPosY = rand.nextInt(this.cameraHeight); 
			break; 
			case 1: // górna krawędź 
				this.startPosX = rand.nextInt(this.cameraWidth); 
			break; 
			case 2: // prawa krawędź 
				this.startPosY = rand.nextInt(this.cameraHeight);
				this.startPosX = this.cameraWidth; 
			break; 
			case 3: //dolna krawędź 
				this.startPosX = rand.nextInt(this.cameraWidth); this.startPosY = this.cameraHeight; 
			break; 
		}
	}
	
	@Override
	protected void onManagedUpdate(final float pSecondsElapsed) {
		if (this.isFlying() == true) {
			
			try {
				if (updateCounter > 10) {
					updateCounter = 0;

					// losujemy czy zwrot ma byc zgodny czy przeciwny do
					// osi.
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

				updateCounter++;

				// ////////////////////////////////////////////////////////////////////////
				// jesli obiekt chce przejsc poza granice ekranu to
				// zmieniamy jego zwrot
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
				
				final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage) this.activity.mMessagePool
						.obtainMessage(this.activity.FLAG_MESSAGE_SERVER_MOVE_FACE);
				moveFaceServerMessage.set(this.pID, this.xTurn, this.yTurn);

				this.activity.mSocketServer.sendBroadcastServerMessage(moveFaceServerMessage);
				this.activity.mMessagePool.recycleMessage(moveFaceServerMessage);

			} catch (final IOException e) {
				Debug.e(e);
			}
		}
		super.onManagedUpdate(pSecondsElapsed);
	}
	
	/**
	 * Metoda odpowiedzialna za poruszanie siê muchy. Wykorzystywana zmienna
	 * VELOCITY posiada wartoœæ prêdkoœci oraz zwrot. Jeœli jest dodatnia to
	 * zwrot zgodnie z osi¹, jeœli ujemna to przeciwny
	 */
	/*
	@Override
	protected void onManagedUpdate(final float pSecondsElapsed) {
		this.startFlying();
		if (this.isFlying) {
			// Warunek potrzebny, aby kierunek ruchy muchy nie by³ zmieniany co
			// ka¿d¹ aktualizacjê, tylko co konkretn¹ liczbê uaktualnieñ
			if (updateCounter > 10) {
				updateCounter = 0;
				// losujemy czy zwrot ma byæ zgodny czy przeciwny do osi.
				Random rand = new Random();
				xTurn = (byte) rand.nextInt(2);
				xTurn = (byte) rand.nextInt(2);
				if (xTurn == 0) {
					xTurn = -1;
				}
				if (yTurn == 0) {
					yTurn = -1;
				}
			}
			updateCounter++;
			// ////////////////////////////////////////////////////////////////////////
			// jeœli obiekt chce przejœæ poza granice ekranu to zmieniamy jego
			// zwrot
			// ////////////////////////////////////////////////////////////////////////
			if (this.mX < 0) {
				xTurn = 1;
			} else if (this.mX + this.getWidth() > this.cameraWidth) {
				xTurn = -1;
			}
			if (this.mY < 0) {
				yTurn = 1;
			} else if (this.mY + this.getHeight() > this.cameraHeight) {
				yTurn = -1;
			}
			// ////////////////////////////////////////////////////////////////////////
			// Ustawiamy prêdkoœæ i zwrot dla ruchu muchy na osiach X oraz Y
			this.mPhysicsHandler.setVelocityX(xTurn * this.velocity);
			this.mPhysicsHandler.setVelocityY(yTurn * this.velocity);
		} else {
			this.mPhysicsHandler.setVelocityX(0);
			this.mPhysicsHandler.setVelocityY(0);
		}
		super.onManagedUpdate(pSecondsElapsed);
	}
	*/

}
