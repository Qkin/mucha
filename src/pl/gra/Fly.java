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
 * Klasa obsuguj�ca ruch muchy
 * 
 * @author Kamil Kunikowski
 * @author Krzysztof Longa
 * 
 */
public class Fly extends AnimatedSprite {
	public final PhysicsHandler mPhysicsHandler;
	// pr�dko�� poruszania si� muchy ( gdy warto�� ujemna, pr�dko�� przybiera przeciwny zwrot)
	public float velocity = 50.0f;
	//pozycje startowe
	public int startPosX = 0;
	public int startPosY = 0;

	// szeroko�� ekranu
	private int cameraWidth = 720;
	// wysoko�� ekranu
	private int cameraHeight = 480;
	// zmienna do p�tli generuj�cej ruch muchy
	private int updateCounter = 0;
	// zwrot na osi X. Przyjmuje warto�ci 1(zgodnie z osi� X) oraz -1
	// (przeciwnie do osi X)
	private byte xTurn = 1;
	// zwrot na osi Y. Przyjmuje warto�ci 1(zgodnie z osi� Y) oraz -1
	// (przeciwnie do osi Y)
	private byte yTurn = 1;
	// Czy jest w trakcie lotu.
	private boolean isFlying = false;
	// id obiektu
	private int pID;


	// d�wi�k lotu muchy
	private Music flyNoise;
	private Music killFlyNoise;
	
	private MainActivity activity;
	/**
	 * Konstruktor
	 * @param pX - pocz�tkowa warto�� X muchy
	 * @param pY - pocz�tkowa warto�� Y muchy
	 * @param pTextureRegion - region grafiki muchy
	 * @param pVertexBufferObjectManager
	 * @param pID - ID obiektu
	 * @param pMusic - d�wi�k poruszaj�cego si� obiektu
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
		// jak cz�sto (w ms) maj� zmienia� si� animowane obrazki
		this.animate(50);
	}

	/**
	 * metoda ustawia szeroko�� i wysoko�� ekranu
	 */
	public void setCamera(int cameraWidth, int cameraHeight) {
		this.cameraWidth = cameraWidth;
		this.cameraHeight = cameraHeight;
	}
	
	/**
	 * metoda ustawia warto�� pocz�tkow� muchy
	 */
	public void setStartPosition(int x, int y) {
		this.startPosX = x;
		this.startPosY = y;
	}

	/**
	 * metoda ustawia pr�dko�� obiektu
	 */
	public void setSpeed(float speed) {
		this.velocity = speed;
	}

	/**
	 * metoda zwraca aktualn� pr�dko�� obiektu
	 */
	public float getSpeed() {
		return this.velocity;
	}

	/**
	 * metoda usatwia d�wi�k zabicia muchy
	 */
	public void setKillFlyNoise(Music noise) {
		this.killFlyNoise = noise;
	}

	/**
	 * Metoda zatrzymuje ruchy muchy
	 */
	public void stopFlying() {
		this.isFlying = false;
		//this.flyNoise.stop();
		this.setSpeed(0);
	}

	public void startFlying() {
		this.isFlying = true;
	}

	/**
	 * metoda odpowiedzialna za zabicie muchy, czyli zatrzymanie jej ruch�w i
	 * wygenerowanie d�wi�ku zabicia.
	 */
	public void killTheFly() {
		this.stopFlying();
		this.killFlyNoise.play();
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
	
	//Losuje kraw�d� ekranu z kt�rej ma startowa� mucha i zapisuje wsp�rz�dne pocz�tkowe
	public void randomStartPosition(){
		this.startPosX = 0; 
		this.startPosY = 0;
		Random rand = new Random(); 
		int direction = rand.nextInt(4);
		switch(direction) { 
			case 0: //lewa kraw�d� 
				this.startPosY = rand.nextInt(this.cameraHeight); 
			break; 
			case 1: // g�rna kraw�d� 
				this.startPosX = rand.nextInt(this.cameraWidth); 
			break; 
			case 2: // prawa kraw�d� 
				this.startPosY = rand.nextInt(this.cameraHeight);
				this.startPosX = this.cameraWidth; 
			break; 
			case 3: //dolna kraw�d� 
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

					// losujemy czy zwrot ma by� zgodny czy przeciwny do
					// osi.
					Random rand = new Random();
					this.xTurn = (byte) rand.nextInt(2);
					this.xTurn = (byte) rand.nextInt(2);
					if (this.xTurn == 0) {
						this.xTurn = -1;
					}
					if (this.yTurn == 0) {
						this.yTurn = -1;
					}
				}

				updateCounter++;

				// ////////////////////////////////////////////////////////////////////////
				// je�li obiekt chce przej�� poza granice ekranu to
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
	 * Metoda odpowiedzialna za poruszanie si� muchy. Wykorzystywana zmienna
	 * VELOCITY posiada warto�� pr�dko�ci oraz zwrot. Je�li jest dodatnia to
	 * zwrot zgodnie z osi�, je�li ujemna to przeciwny
	 */
	/*@Override
	protected void onManagedUpdate(final float pSecondsElapsed) {

		this.startFlying();
		if (this.isFlying) {
			// Warunek potrzebny, aby kierunek ruchy muchy nie by� zmieniany co
			// ka�d� aktualizacj�, tylko co konkretn� liczb� uaktualnie�
			if (updateCounter > 10) {
				updateCounter = 0;

				// losujemy czy zwrot ma by� zgodny czy przeciwny do osi.
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
			// je�li obiekt chce przej�� poza granice ekranu to zmieniamy jego
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

			// Ustawiamy pr�dko�� i zwrot dla ruchu muchy na osiach X oraz Y
			this.mPhysicsHandler.setVelocityX(xTurn * this.velocity);
			this.mPhysicsHandler.setVelocityY(yTurn * this.velocity);
		} else {
			this.mPhysicsHandler.setVelocityX(0);
			this.mPhysicsHandler.setVelocityY(0);
		}

		super.onManagedUpdate(pSecondsElapsed);
	}*/

}
