package pl.gra;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import org.andengine.audio.music.Music;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.examples.adt.messages.client.ClientMessageFlags;
import org.andengine.examples.adt.messages.server.ConnectionCloseServerMessage;
import org.andengine.examples.adt.messages.server.ServerMessageFlags;
import org.andengine.extension.multiplayer.protocol.adt.message.IMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.server.IServerMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.server.ServerMessage;
import org.andengine.extension.multiplayer.protocol.client.IServerMessageHandler;
import org.andengine.extension.multiplayer.protocol.client.connector.ServerConnector;
import org.andengine.extension.multiplayer.protocol.client.connector.SocketConnectionServerConnector;
import org.andengine.extension.multiplayer.protocol.client.connector.SocketConnectionServerConnector.ISocketConnectionServerConnectorListener;
import org.andengine.extension.multiplayer.protocol.server.SocketServer;
import org.andengine.extension.multiplayer.protocol.server.SocketServer.ISocketServerListener;
import org.andengine.extension.multiplayer.protocol.server.connector.ClientConnector;
import org.andengine.extension.multiplayer.protocol.server.connector.SocketConnectionClientConnector;
import org.andengine.extension.multiplayer.protocol.server.connector.SocketConnectionClientConnector.ISocketConnectionClientConnectorListener;
import org.andengine.extension.multiplayer.protocol.shared.Connector;
import org.andengine.extension.multiplayer.protocol.shared.SocketConnection;
import org.andengine.extension.multiplayer.protocol.util.MessagePool;
import org.andengine.extension.multiplayer.protocol.util.WifiUtils;
import org.andengine.input.touch.TouchEvent;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.debug.Debug;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Ekran g³ówny gry
 * 
 * @author Kamil Kunikowski
 * @author Krzysztof Longa
 */
public class CopyOfMainActivity extends SimpleBaseGameActivity implements
		ClientMessageFlags, ServerMessageFlags {
	// ===========================================================
	// Sta³e
	// ===========================================================

	private static final String LOCALHOST_IP = "127.0.0.1";

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	private static final int SERVER_PORT = 4444;

	public static final short FLAG_MESSAGE_SERVER_ADD_FACE = 1;
	public static final short FLAG_MESSAGE_SERVER_MOVE_FACE = FLAG_MESSAGE_SERVER_ADD_FACE + 1;
	public static final short FLAG_MESSAGE_SERVER_STOP_FLY = FLAG_MESSAGE_SERVER_MOVE_FACE + 1;

	private static final int DIALOG_CHOOSE_SERVER_OR_CLIENT_ID = 0;
	private static final int DIALOG_ENTER_SERVER_IP_ID = DIALOG_CHOOSE_SERVER_OR_CLIENT_ID + 1;
	private static final int DIALOG_SHOW_SERVER_IP_ID = DIALOG_ENTER_SERVER_IP_ID + 1;

	// ===========================================================
	// Zmienne
	// ===========================================================

	private int mFaceIDCounter = 0;
	private final SparseArray<Fly> mFaces = new SparseArray<Fly>();

	private String mServerIP = LOCALHOST_IP;
	public SocketServer<SocketConnectionClientConnector> mSocketServer;
	private ServerConnector<SocketConnection> mServerConnector;

	final MessagePool<IMessage> mMessagePool = new MessagePool<IMessage>();

	private Resources resources;
	private HUD gameHUD;
	private Text scoreText;
	private int score;
	private Camera camera;

	// ===========================================================
	// Konstruktor
	// ===========================================================

	public CopyOfMainActivity() {
		this.initMessagePool();
	}

	private void initMessagePool() {
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_STOP_FLY, StopFlyServerMessage.class);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.showDialog(DIALOG_CHOOSE_SERVER_OR_CLIENT_ID);

		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	public Music mMusic;

	@Override
	public void onCreateResources() {
		resources = new Resources();
		resources.loadGameGraphics(this);
		// resources.loadFonts(this);
	}
	public boolean gameStarted = false;
	
	@Override
	public Scene onCreateScene() {
		
		//tymczasowa inicjacja serwera na starcie
		//CopyOfMainActivity.this.initServerAndClient();
		// thread.start();
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));

		//gameHUD = new HUD();

		// scoreText = new Text(20, 0, resources.font, "Score: 0123456789", new TextOptions(HorizontalAlign.LEFT), this.getVertexBufferObjectManager()); 
		// scoreText.setAnchorCenter(0, 0);
		// scoreText.setText("Score: 0");
		// gameHUD.attachChild(scoreText);

		// camera.setHUD(gameHUD);
		
		scene.setOnAreaTouchListener(new IOnAreaTouchListener() {
			@Override
			public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
			//	if (CopyOfMainActivity.this.mSocketServer != null) {

				//	try {
						final Fly face = (Fly) pTouchArea;
						final Integer faceID = (Integer) face.getUserData();
						Log.d("FACE ID", "" + faceID);
				/*		final StopFlyServerMessage stopFlyServerMessage = (StopFlyServerMessage) CopyOfMainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_STOP_FLY);
						stopFlyServerMessage.set(faceID, pTouchAreaLocalX,pTouchAreaLocalY);

						CopyOfMainActivity.this.mSocketServer.sendBroadcastServerMessage(stopFlyServerMessage);

						CopyOfMainActivity.this.mMessagePool.recycleMessage(stopFlyServerMessage);
					} catch (final IOException e) {
						Debug.e(e);
						return false;
					}

				}*/
				return true;
			}
		});
		
		
		//starttime = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new MyTimer(), 0,2000);
        
      //  h2.postDelayed(run, 0);
      //  b.setText("stop");
		
	/*	if (CopyOfMainActivity.this.mSocketServer != null) {
			try {
				final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) CopyOfMainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
				addFaceServerMessage.set(CopyOfMainActivity.this.mFaceIDCounter++, 50, 50);

				CopyOfMainActivity.this.mSocketServer.sendBroadcastServerMessage(addFaceServerMessage);
				CopyOfMainActivity.this.mMessagePool.recycleMessage(addFaceServerMessage);
				Log.d("MUCHA", "lata!");
			} catch (final IOException e) {
				Debug.e(e);
			}
		}*/

		
		/* We allow only the server to actively send around messages. */

		/*scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
			@Override
			public boolean onSceneTouchEvent(final Scene pScene,
					final TouchEvent pSceneTouchEvent) {
				// CopyOfMainActivity.this.toast("tekst");
				if (CopyOfMainActivity.this.mSocketServer != null) {
					if (pSceneTouchEvent.isActionDown()) {

						try {
							final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) CopyOfMainActivity.this.mMessagePool
									.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
							addFaceServerMessage.set(
									CopyOfMainActivity.this.mFaceIDCounter++,
									pSceneTouchEvent.getX() + 50,
									pSceneTouchEvent.getY() + 50);

							CopyOfMainActivity.this.mSocketServer
									.sendBroadcastServerMessage(addFaceServerMessage);

							CopyOfMainActivity.this.mMessagePool
									.recycleMessage(addFaceServerMessage);
						} catch (final IOException e) {
							Debug.e(e);
						}
					}
				}
				return true;
			}
		});*/

		// final Fly fly = new Fly(xPos, yPos, resources.mFlyTextureRegion,
		// this.getVertexBufferObjectManager(), mFaceIDCounter++/*,
		// resources.mMusic*/);
		// dodajemy muchê do sceny
		// scene.attachChild(fly);
		// this.mFaces.put(mFaceIDCounter-1, fly);
		// startujemy muchê
		// fly.startFlying();

		//scene.setTouchAreaBindingOnActionDownEnabled(true);

		return scene;
	}
	
	Timer timer = new Timer();
	class MyTimer extends TimerTask {
			@Override
			public void run() {
				Log.d("START", ""+CopyOfMainActivity.this.gameStarted);
				if (CopyOfMainActivity.this.mSocketServer != null && CopyOfMainActivity.this.gameStarted == true) {
					
					try {
						
						final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) CopyOfMainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
						addFaceServerMessage.set(CopyOfMainActivity.this.mFaceIDCounter++, 50, 50);
	
						CopyOfMainActivity.this.mSocketServer.sendBroadcastServerMessage(addFaceServerMessage);
						CopyOfMainActivity.this.mMessagePool.recycleMessage(addFaceServerMessage);
						Log.d("MUCHA", "lata!");
						gameStarted = false;
						this.cancel();
					} catch (final IOException e) {
						Debug.e(e);
					}
				}
			}
		}
	
	//=========================================================
	//Okna dialgowe na starcie gry
	@Override
	protected Dialog onCreateDialog(final int pID) {
		switch (pID) {
		case DIALOG_SHOW_SERVER_IP_ID:
			try {
				return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle("Your Server-IP ...").setCancelable(false)
						.setMessage("The IP of your Server is:\n" + WifiUtils.getWifiIPv4Address(this)).setPositiveButton(android.R.string.ok, null).create();
			} catch (final UnknownHostException e) {
				return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Your Server-IP ...")
						.setCancelable(false).setMessage("Error retrieving IP of your Server: " + e).setPositiveButton(android.R.string.ok, new OnClickListener() {
									@Override
									public void onClick(final DialogInterface pDialog, final int pWhich) {
										CopyOfMainActivity.this.finish();
									}
								}).create();
			}
		case DIALOG_ENTER_SERVER_IP_ID:
			final EditText ipEditText = new EditText(this);
			return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle("Enter Server-IP ...").setCancelable(false)
					.setView(ipEditText).setPositiveButton("Connect", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface pDialog, final int pWhich) {
							CopyOfMainActivity.this.mServerIP = ipEditText.getText().toString();
							CopyOfMainActivity.this.initClient();
						}
					}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
								@Override
								public void onClick(final DialogInterface pDialog, final int pWhich) {
									CopyOfMainActivity.this.finish();
								}
							}).create();
		case DIALOG_CHOOSE_SERVER_OR_CLIENT_ID:
			return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle("Be Server or Client ...").setCancelable(false)
					.setPositiveButton("Client", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface pDialog, final int pWhich) {
							CopyOfMainActivity.this.showDialog(DIALOG_ENTER_SERVER_IP_ID);
						}
					}).setNeutralButton("Server", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface pDialog, final int pWhich) {
							CopyOfMainActivity.this.initServer();
							 CopyOfMainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
						}
					}).setNegativeButton("Both", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface pDialog, final int pWhich) {
							CopyOfMainActivity.this.initServerAndClient();
							// CopyOfMainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
						}
					}).create();

		default:
			return super.onCreateDialog(pID);
		}
	}
	//===================================================================================
	
	@Override
	protected void onPause() {
		timer.purge();
		if (this.mSocketServer != null) {
			try {
				this.mSocketServer.sendBroadcastServerMessage(new ConnectionCloseServerMessage());
			} catch (final IOException e) {
				Debug.e(e);
			}
			this.mSocketServer.terminate();
		}

		if (this.mServerConnector != null) {
			this.mServerConnector.terminate();
		}

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		timer.purge();
		if (this.mSocketServer != null) {
			try {
				this.mSocketServer.sendBroadcastServerMessage(new ConnectionCloseServerMessage());
			} catch (final IOException e) {
				Debug.e(e);
			}
			this.mSocketServer.terminate();
		}

		if (this.mServerConnector != null) {
			this.mServerConnector.terminate();
		}

		super.onDestroy();
	}

	@Override
	public boolean onKeyUp(final int pKeyCode, final KeyEvent pEvent) {
		switch (pKeyCode) {
		case KeyEvent.KEYCODE_BACK:
			this.finish();
			return true;
		}
		return super.onKeyUp(pKeyCode, pEvent);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void addFace(final int pID, final float pX, final float pY) {
		final Scene scene = this.mEngine.getScene();
		/* Create the face and add it to the scene. */
		// final Sprite face = new Sprite(0, 0, this.mFaceTextureRegion,
		// this.getVertexBufferObjectManager());
		final Fly fly = new Fly(0, 0, resources.mFlyTextureRegion, this.getVertexBufferObjectManager(), pID);
		fly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
		//fly.setActivity(CopyOfMainActivity.this);
		fly.randomStartPosition();
		fly.setPosition(fly.startPosX - fly.getWidth() * 0.5f, fly.startPosY - fly.getHeight() * 0.5f);
		fly.setUserData(pID);
		this.mFaces.put(pID, fly);

		scene.registerTouchArea(fly);
		scene.attachChild(fly);
		fly.setSpeed(100);
		fly.startFlying();

	}

	public void moveFace(final int pID, final float pX, final float pY) {
		/* Find and move the face. */
		final Fly face = this.mFaces.get(pID);
		face.mPhysicsHandler.setVelocityX(pX * face.getSpeed());
		face.mPhysicsHandler.setVelocityY(pY * face.getSpeed());
	}
	
	/* Znajduje muchê o podanym ID i uœmierca j¹*/
	public void stopFly(final int pID, final float pX, final float pY) {
		final Scene scene = this.mEngine.getScene();
		final Fly fly = this.mFaces.get(pID);
		fly.killTheFly();
		this.mFaces.remove(pID);
		fly.detachSelf();
		
		final Fly deadFly = new Fly(0, 0, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager(), CopyOfMainActivity.this.mFaceIDCounter++);
		deadFly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
		deadFly.setPosition(pX - deadFly.getWidth() * 0.5f, pY - deadFly.getHeight() * 0.5f);
		scene.attachChild(deadFly);
		//final Fly face = this.mFaces.get(pID);
		// CopyOfMainActivity.this.toast("tekst");
		//face.killTheFly();
		//this.mFaces.remove(pID);
		
		//face.detachSelf();
		//ustawienie pozycji na osiach X i Y dla nie¿ywej muchy
		//float x = face.getX();
		//float y = face.getY() + face.getHeight()/2;
		//final Sprite deadFly = new Sprite(x, y, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager());

		//dodanie do sceny gry grafiki dla nie¿ywej muchy

		// float x = face.getX();
		// float y = face.getY() + face.getHeight()/2;

		//final Sprite deadFly = new Sprite(pX, pY, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager());
		//deadFly.setPosition(pX - face.getWidth() * 0.5f, pY - face.getHeight()* 0.5f);

	}

	private void initServerAndClient() {
		this.initServer();
		this.initClient();
	}

	private void initServer() {
		this.mSocketServer = new SocketServer<SocketConnectionClientConnector>(SERVER_PORT, new ExampleClientConnectorListener(), new ExampleServerStateListener()) {
			@Override
			protected SocketConnectionClientConnector newClientConnector(final SocketConnection pSocketConnection) throws IOException {
				return new SocketConnectionClientConnector(pSocketConnection);
			}
		};

		this.mSocketServer.start();
		Log.d("SERWER", "podlaczony");
		
		try {
			Thread.sleep(300);
		} catch (final Throwable t) {
			Debug.e(t);
		}
	}

	private void initClient() {
		try {
			this.mServerConnector = new SocketConnectionServerConnector(new SocketConnection(new Socket(this.mServerIP, SERVER_PORT)), new ExampleServerConnectorListener());

			this.mServerConnector.registerServerMessage(
					FLAG_MESSAGE_SERVER_CONNECTION_CLOSE,
					ConnectionCloseServerMessage.class,
					new IServerMessageHandler<SocketConnection>() {
						@Override
						public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
							CopyOfMainActivity.this.finish();
						}
					});

			this.mServerConnector.registerServerMessage(
					FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class,
					new IServerMessageHandler<SocketConnection>() {
						@Override
						public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
							final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) pServerMessage;
							CopyOfMainActivity.this.addFace(addFaceServerMessage.mID, addFaceServerMessage.mX, addFaceServerMessage.mY);
						}
					});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class, new IServerMessageHandler<SocketConnection>() {
						@Override
						public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
							final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage) pServerMessage;
							CopyOfMainActivity.this.moveFace(moveFaceServerMessage.mID, moveFaceServerMessage.mX, moveFaceServerMessage.mY);
						}
					});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_STOP_FLY, StopFlyServerMessage.class, new IServerMessageHandler<SocketConnection>() {

						@Override
						public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
							final StopFlyServerMessage stopFlyServerMessage = (StopFlyServerMessage) pServerMessage;
							CopyOfMainActivity.this.stopFly(stopFlyServerMessage.mID, stopFlyServerMessage.mX, stopFlyServerMessage.mY);
						}
					});
			
			this.mServerConnector.getConnection().start();
			Log.d("KLIENT", "podlaczony!");
			CopyOfMainActivity.this.gameStarted = true;
			
		} catch (final Throwable t) {
			Debug.e(t);
		}
		
		try {
			Thread.sleep(300);
		} catch (final Throwable t) {
			Debug.e(t);
		}
	}

	private void log(final String pMessage) {
		Debug.d(pMessage);
	}

	private void toast(final String pMessage) {
		this.log(pMessage);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CopyOfMainActivity.this, pMessage, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// ===========================================================
	public static class StopFlyServerMessage extends ServerMessage {
		private int mID;
		private float mX;
		private float mY;

		public StopFlyServerMessage() {

		}

		public StopFlyServerMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;

		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_SERVER_STOP_FLY;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}

	// /////////////////////////////////////////////////////////////
	public static class AddFaceServerMessage extends ServerMessage {
		private int mID;
		private float mX;
		private float mY;

		public AddFaceServerMessage() {

		}

		public AddFaceServerMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_SERVER_ADD_FACE;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}

	// ///////////////////////////////////////////////////////////////
	public static class MoveFaceServerMessage extends ServerMessage {
		private int mID;
		private float mX;
		private float mY;

		public MoveFaceServerMessage() {

		}

		public MoveFaceServerMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_SERVER_MOVE_FACE;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}

	private class ExampleServerConnectorListener implements ISocketConnectionServerConnectorListener {
		@Override
		public void onStarted(final ServerConnector<SocketConnection> pConnector) {
			// CopyOfMainActivity.this.toast("CLIENT: Connected to server.");
		}

		@Override
		public void onTerminated(final ServerConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("CLIENT: Disconnected from Server...");
			CopyOfMainActivity.this.finish();
		}
	}

	private class ExampleServerStateListener implements ISocketServerListener<SocketConnectionClientConnector> {
		@Override
		public void onStarted(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			CopyOfMainActivity.this.toast("SERVER: Started.");
		}

		@Override
		public void onTerminated(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			CopyOfMainActivity.this.toast("SERVER: Terminated.");
		}

		@Override
		public void onException(final SocketServer<SocketConnectionClientConnector> pSocketServer, final Throwable pThrowable) {
			Debug.e(pThrowable);
			CopyOfMainActivity.this.toast("SERVER: Exception: " + pThrowable);
		}
	}

	private class ExampleClientConnectorListener implements ISocketConnectionClientConnectorListener {
		@Override
		public void onStarted(final ClientConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("SERVER: Client connected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}

		@Override
		public void onTerminated(final ClientConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("SERVER: Client disconnected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}
	}
	
	/*
	 * private static final String LOCALHOST_IP = "127.0.0.1";

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	private static final int SERVER_PORT = 4444;

	private static final short FLAG_MESSAGE_SERVER_ADD_FACE = 1;
	private static final short FLAG_MESSAGE_SERVER_MOVE_FACE = FLAG_MESSAGE_SERVER_ADD_FACE + 1;

	private static final int DIALOG_CHOOSE_SERVER_OR_CLIENT_ID = 0;
	private static final int DIALOG_ENTER_SERVER_IP_ID = DIALOG_CHOOSE_SERVER_OR_CLIENT_ID + 1;
	private static final int DIALOG_SHOW_SERVER_IP_ID = DIALOG_ENTER_SERVER_IP_ID + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	private BitmapTextureAtlas mBitmapTextureAtlas;
	private ITextureRegion mFaceTextureRegion;

	private int mFaceIDCounter;
	private final SparseArray<Sprite> mFaces = new SparseArray<Sprite>();

	private String mServerIP = LOCALHOST_IP;
	private SocketServer<SocketConnectionClientConnector> mSocketServer;
	private ServerConnector<SocketConnection> mServerConnector;
	private Socket mSocket;

	private final MessagePool<IMessage> mMessagePool = new MessagePool<IMessage>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public CopyOfMainActivity() {
		this.initMessagePool();
	}

	private void initMessagePool() {
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.showDialog(DIALOG_CHOOSE_SERVER_OR_CLIENT_ID);

		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 32, 32, TextureOptions.BILINEAR);
		this.mFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "face_box.png", 0, 0);

		this.mBitmapTextureAtlas.load();
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));

		// We allow only the server to actively send around messages.
		//if(CopyOfMainActivity.this.mSocketServer != null) {
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
					if(pSceneTouchEvent.isActionDown()) {
						if(CopyOfMainActivity.this.mSocketServer != null) {
							try {
								final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) CopyOfMainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
								addFaceServerMessage.set(CopyOfMainActivity.this.mFaceIDCounter++, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
	
								CopyOfMainActivity.this.mSocketServer.sendBroadcastServerMessage(addFaceServerMessage);
	
								CopyOfMainActivity.this.mMessagePool.recycleMessage(addFaceServerMessage);
								
							} catch (final IOException e) {
								Debug.e(e);
							}
						} 
					}
					return true;
					
				}
			});

			scene.setOnAreaTouchListener(new IOnAreaTouchListener() {
				@Override
				public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
					if(CopyOfMainActivity.this.mSocketServer != null) {
						try {
							final Sprite face = (Sprite)pTouchArea;
							final Integer faceID = (Integer)face.getUserData();
		
							final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage) CopyOfMainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_MOVE_FACE);
							moveFaceServerMessage.set(faceID, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
		
							CopyOfMainActivity.this.mSocketServer.sendBroadcastServerMessage(moveFaceServerMessage);
		
							CopyOfMainActivity.this.mMessagePool.recycleMessage(moveFaceServerMessage);
						} catch (final IOException e) {
							Debug.e(e);
							return false;
						}
					}
					return true;
				}
			});

			scene.setTouchAreaBindingOnActionDownEnabled(true);
		//}

		return scene;
	}

	@Override
	protected Dialog onCreateDialog(final int pID) {
		switch(pID) {
			case DIALOG_SHOW_SERVER_IP_ID:
				try {
					return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle("Your Server-IP ...")
					.setCancelable(false)
					.setMessage("The IP of your Server is:\n" + WifiUtils.getWifiIPv4Address(this))
					.setPositiveButton(android.R.string.ok, null)
					.create();
				} catch (final UnknownHostException e) {
					return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Your Server-IP ...")
					.setCancelable(false)
					.setMessage("Error retrieving IP of your Server: " + e)
					.setPositiveButton(android.R.string.ok, new OnClickListener() {
						@Override
						public void onClick(final DialogInterface pDialog, final int pWhich) {
							CopyOfMainActivity.this.finish();
						}
					})
					.create();
				}
			case DIALOG_ENTER_SERVER_IP_ID:
				final EditText ipEditText = new EditText(this);
				return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle("Enter Server-IP ...")
				.setCancelable(false)
				.setView(ipEditText)
				.setPositiveButton("Connect", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						CopyOfMainActivity.this.mServerIP = ipEditText.getText().toString();
						CopyOfMainActivity.this.initClient();
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						CopyOfMainActivity.this.finish();
					}
				})
				.create();
			case DIALOG_CHOOSE_SERVER_OR_CLIENT_ID:
				return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle("Be Server or Client ...")
				.setCancelable(false)
				.setPositiveButton("Client", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						CopyOfMainActivity.this.showDialog(DIALOG_ENTER_SERVER_IP_ID);
					}
				})
				.setNeutralButton("Server", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						CopyOfMainActivity.this.toast("You can add and move sprites, which are only shown on the clients.");
						CopyOfMainActivity.this.initServer();
						CopyOfMainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
					}
				})
				.setNegativeButton("Both", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						CopyOfMainActivity.this.toast("You can add sprites and move them, by dragging them.");
						CopyOfMainActivity.this.initServerAndClient();
						CopyOfMainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
					}
				})
				.create();
			default:
				return super.onCreateDialog(pID);
		}
	}

	@Override
	protected void onDestroy() {
		if(this.mSocketServer != null) {
			try {
				this.mSocketServer.sendBroadcastServerMessage(new ConnectionCloseServerMessage());
			} catch (final IOException e) {
				Debug.e(e);
			}
			this.mSocketServer.terminate();
		}

		if(this.mServerConnector != null) {
			this.mServerConnector.terminate();
		}

		super.onDestroy();
	}

	@Override
	public boolean onKeyUp(final int pKeyCode, final KeyEvent pEvent) {
		switch(pKeyCode) {
			case KeyEvent.KEYCODE_BACK:
				this.finish();
				return true;
		}
		return super.onKeyUp(pKeyCode, pEvent);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void addFace(final int pID, final float pX, final float pY) {
		final Scene scene = this.mEngine.getScene();
		// Create the face and add it to the scene. 
		final Sprite face = new Sprite(0, 0, this.mFaceTextureRegion, this.getVertexBufferObjectManager());
		face.setPosition(pX - face.getWidth() * 0.5f, pY - face.getHeight() * 0.5f);
		face.setUserData(pID);
		this.mFaces.put(pID, face);
		scene.registerTouchArea(face);
		scene.attachChild(face);
	}

	public void moveFace(final int pID, final float pX, final float pY) {
		// Find and move the face. 
		final Sprite face = this.mFaces.get(pID);
		face.setPosition(pX - face.getWidth() * 0.5f, pY - face.getHeight() * 0.5f);
	}

	private void initServerAndClient() {
		this.initServer();

		// Wait some time after the server has been started, so it actually can start up. 
		try {
			Thread.sleep(500);
		} catch (final Throwable t) {
			Debug.e(t);
		}

		this.initClient();
	}
	
	private void initServer() {
        this.mSocketServer = new SocketServer<SocketConnectionClientConnector>(SERVER_PORT, new ExampleClientConnectorListener(), new ExampleServerStateListener()) {
                @Override
                protected SocketConnectionClientConnector newClientConnector(final SocketConnection pSocketConnection) throws IOException {
                        final SocketConnectionClientConnector clientConnector = new SocketConnectionClientConnector(pSocketConnection);
                        //clientConnector.registerClientMessage(5, pClientMessageClass);
                        clientConnector.registerClientMessage(FLAG_MESSAGE_CLIENT_DRAWLINE, DrawLineClientMessage.class, new IClientMessageHandler<SocketConnection>() {
								@Override
								public void onHandleMessage(ClientConnector<SocketConnection> pClientConnector, IClientMessage pClientMessage)
										throws IOException {
									final DrawLineClientMessage drawLineClientMessage = (DrawLineClientMessage)pClientMessage;
									CopyOfMainActivity.this.addFace(drawLineClientMessage.mID, drawLineClientMessage.mX, drawLineClientMessage.mY);
									
								}
                        });
                        return clientConnector;
                }
        };
        this.mSocketServer.start();
	}
	
	/ *private void initServer() {
		this.mSocketServer = new SocketServer<SocketConnectionClientConnector>(SERVER_PORT, new ExampleClientConnectorListener(), new ExampleServerStateListener()) {
			@Override
			protected SocketConnectionClientConnector newClientConnector(final SocketConnection pSocketConnection) throws IOException {
				return new SocketConnectionClientConnector(pSocketConnection);
			}
		};

		this.mSocketServer.start();
	}* /

	private void initClient() {
		try {
			this.mServerConnector = new SocketConnectionServerConnector(new SocketConnection(new Socket(this.mServerIP, SERVER_PORT)), new ExampleServerConnectorListener());

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_CONNECTION_CLOSE, ConnectionCloseServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					CopyOfMainActivity.this.finish();
				}
			});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage)pServerMessage;
					CopyOfMainActivity.this.addFace(addFaceServerMessage.mID, addFaceServerMessage.mX, addFaceServerMessage.mY);
				}
			});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage)pServerMessage;
					CopyOfMainActivity.this.moveFace(moveFaceServerMessage.mID, moveFaceServerMessage.mX, moveFaceServerMessage.mY);
				}
			});

			this.mServerConnector.getConnection().start();
		} catch (final Throwable t) {
			Debug.e(t);
		}
	}

	private void log(final String pMessage) {
		Debug.d(pMessage);
	}

	private void toast(final String pMessage) {
		this.log(pMessage);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CopyOfMainActivity.this, pMessage, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	private  static final short FLAG_MESSAGE_CLIENT_DRAWLINE = 5;
	
	public static class DrawLineClientMessage extends ClientMessage {
		private int mID;
		private float mX;
		private float mY;

		public DrawLineClientMessage() {

		}

		public DrawLineClientMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_CLIENT_DRAWLINE;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}
	
	public static class AddFaceServerMessage extends ServerMessage {
		private int mID;
		private float mX;
		private float mY;

		public AddFaceServerMessage() {

		}

		public AddFaceServerMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_SERVER_ADD_FACE;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}

	public static class MoveFaceServerMessage extends ServerMessage {
		private int mID;
		private float mX;
		private float mY;

		public MoveFaceServerMessage() {

		}

		public MoveFaceServerMessage(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final float pX, final float pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		@Override
		public short getFlag() {
			return FLAG_MESSAGE_SERVER_MOVE_FACE;
		}

		@Override
		protected void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException {
			this.mID = pDataInputStream.readInt();
			this.mX = pDataInputStream.readFloat();
			this.mY = pDataInputStream.readFloat();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeFloat(this.mX);
			pDataOutputStream.writeFloat(this.mY);
		}
	}

	private class ExampleServerConnectorListener implements ISocketConnectionServerConnectorListener {
		@Override
		public void onStarted(final ServerConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("CLIENT: Connected to server.");
		}

		@Override
		public void onTerminated(final ServerConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("CLIENT: Disconnected from Server...");
			CopyOfMainActivity.this.finish();
		}
	}

	private class ExampleServerStateListener implements ISocketServerListener<SocketConnectionClientConnector> {
		@Override
		public void onStarted(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			CopyOfMainActivity.this.toast("SERVER: Started.");
		}

		@Override
		public void onTerminated(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			CopyOfMainActivity.this.toast("SERVER: Terminated.");
		}

		@Override
		public void onException(final SocketServer<SocketConnectionClientConnector> pSocketServer, final Throwable pThrowable) {
			Debug.e(pThrowable);
			CopyOfMainActivity.this.toast("SERVER: Exception: " + pThrowable);
		}
	}

	private class ExampleClientConnectorListener implements ISocketConnectionClientConnectorListener {
		@Override
		public void onStarted(final ClientConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("SERVER: Client connected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}

		@Override
		public void onTerminated(final ClientConnector<SocketConnection> pConnector) {
			CopyOfMainActivity.this.toast("SERVER: Client disconnected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}
	}
	 */
}
