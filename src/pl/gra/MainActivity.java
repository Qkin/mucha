package pl.gra;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicManager;
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
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.examples.adt.messages.client.ClientMessageFlags;
import org.andengine.examples.adt.messages.server.ConnectionCloseServerMessage;
import org.andengine.examples.adt.messages.server.ServerMessageFlags;
import org.andengine.extension.multiplayer.protocol.adt.message.IMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.client.ClientMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.client.IClientMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.server.IServerMessage;
import org.andengine.extension.multiplayer.protocol.adt.message.server.ServerMessage;
import org.andengine.extension.multiplayer.protocol.client.IServerMessageHandler;
import org.andengine.extension.multiplayer.protocol.client.connector.ServerConnector;
import org.andengine.extension.multiplayer.protocol.client.connector.SocketConnectionServerConnector;
import org.andengine.extension.multiplayer.protocol.client.connector.SocketConnectionServerConnector.ISocketConnectionServerConnectorListener;
import org.andengine.extension.multiplayer.protocol.server.IClientMessageHandler;
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
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.debug.Debug;

import pl.gra.CopyOfMainActivity.AddFaceServerMessage;
import pl.gra.CopyOfMainActivity.MyTimer;

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
public class MainActivity extends SimpleBaseGameActivity implements
		ClientMessageFlags, ServerMessageFlags {

	private static final String LOCALHOST_IP = "127.0.0.1";

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	private static final int SERVER_PORT = 4444;

	public static final short FLAG_MESSAGE_SERVER_ADD_FACE = 1;
	public static final short FLAG_MESSAGE_SERVER_MOVE_FACE = 2;
	public static final short FLAG_MESSAGE_SERVER_STOP_FLY = 3;
	public static final short FLAG_MESSAGE_CLIENT_STOP_FLY = 4;

	private static final int DIALOG_CHOOSE_SERVER_OR_CLIENT_ID = 0;
	private static final int DIALOG_ENTER_SERVER_IP_ID = DIALOG_CHOOSE_SERVER_OR_CLIENT_ID + 1;
	private static final int DIALOG_SHOW_SERVER_IP_ID = DIALOG_ENTER_SERVER_IP_ID + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	//private BitmapTextureAtlas mBitmapTextureAtlas;
	//private ITextureRegion mFlyTextureRegion;

	private int mFaceIDCounter;
	private final SparseArray<Fly> mFaces = new SparseArray<Fly>();
	private final SparseArray<Byte> mFacesExec = new SparseArray<Byte>();
	private final SparseArray<Timer> timersList = new SparseArray<Timer>();

	private String mServerIP = LOCALHOST_IP;
	public SocketServer<SocketConnectionClientConnector> mSocketServer;
	private ServerConnector<SocketConnection> mServerConnector;
	private Socket mSocket;

	public final MessagePool<IMessage> mMessagePool = new MessagePool<IMessage>();

	
	private Resources resources;
	private HUD gameHUD;
	private Text scoreText;
	private int score;
	private Camera camera;
	
	//private Music mMusic;
	//private MusicManager musicManager;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public MainActivity() {
		this.initMessagePool();
	}

	private void initMessagePool() {
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_CLIENT_STOP_FLY, StopFlyClientMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_STOP_FLY, StopFlyServerMessage.class);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.showDialog(DIALOG_CHOOSE_SERVER_OR_CLIENT_ID);

		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public void onCreateResources() {
		resources = new Resources();
		resources.loadGameGraphics(this);

		// resources.loadFonts(this);
	}
	
	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));

		// We allow only the server to actively send around messages.
		//if(MainActivity.this.mSocketServer != null) {
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
					if(pSceneTouchEvent.isActionDown()) {

						if(MainActivity.this.mSocketServer != null) {
							try {
								float[] startPos = flyRandomStartPosition();
								final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
								//addFaceServerMessage.set(MainActivity.this.mFaceIDCounter++, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
								addFaceServerMessage.set(MainActivity.this.mFaceIDCounter++, startPos[0], startPos[1]);
	
								MainActivity.this.mSocketServer.sendBroadcastServerMessage(addFaceServerMessage);
								MainActivity.this.mMessagePool.recycleMessage(addFaceServerMessage);
								
								Timer timer = new Timer();
							    timer.schedule(new MoveFlyTimer(MainActivity.this.mFaceIDCounter-1), 0, 300);
							    MainActivity.this.timersList.put(MainActivity.this.mFaceIDCounter-1, timer);
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
					if(MainActivity.this.mSocketServer != null) {

					}
					
					if(MainActivity.this.mServerConnector != null) {
						
						final Fly fly = (Fly)pTouchArea;
						final Integer faceID = (Integer)fly.getUserData();
						
						//warunek pomoga uniknac kilkukrotnego wykonania instrukcji po 1 nacisnieciu
						if(MainActivity.this.mFacesExec.get(faceID) != null) {
							MainActivity.this.mFacesExec.remove(faceID);
							try {			
					 			StopFlyClientMessage stopFlyClientMessage = (StopFlyClientMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_CLIENT_STOP_FLY);
					 			stopFlyClientMessage.set(faceID, pTouchAreaLocalX, pTouchAreaLocalY);
								mServerConnector.sendClientMessage(stopFlyClientMessage);
								MainActivity.this.mMessagePool.recycleMessage(stopFlyClientMessage);
							} catch (IOException e) {
								Debug.e(e);
								return false;
							}
						}
					}
					
					
					return true;
				}
			});

			scene.setTouchAreaBindingOnActionDownEnabled(true);
		//}

		return scene;
	}
	
	class MoveFlyTimer extends TimerTask {
		private int faceId;
		
		MoveFlyTimer(int faceId) {
			this.faceId = faceId;
		}
			@Override
			public void run() {
				if (MainActivity.this.mSocketServer != null) {
					try {
						byte[] xyTurns = randomXYTurn();
						final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_MOVE_FACE);
						moveFaceServerMessage.set(this.faceId, xyTurns[0], xyTurns[1]);				
						MainActivity.this.mSocketServer.sendBroadcastServerMessage(moveFaceServerMessage);
						MainActivity.this.mMessagePool.recycleMessage(moveFaceServerMessage);
					} catch (final IOException e) {
						Debug.e(e);
					}
				}
			}
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
							MainActivity.this.finish();
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
						MainActivity.this.mServerIP = ipEditText.getText().toString();
						MainActivity.this.initClient();
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						MainActivity.this.finish();
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
						MainActivity.this.showDialog(DIALOG_ENTER_SERVER_IP_ID);
					}
				})
				.setNeutralButton("Server", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						MainActivity.this.initServer();
						MainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
					}
				})
				.setNegativeButton("Both", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface pDialog, final int pWhich) {
						MainActivity.this.initServerAndClient();
						MainActivity.this.showDialog(DIALOG_SHOW_SERVER_IP_ID);
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
		
		//Anulujemy wszystkie dzia³aj¹ce timery
		for(int i = 0; i < MainActivity.this.timersList.size(); i++) {
		    Timer timer = MainActivity.this.timersList.valueAt(i);
		    timer.cancel();
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
	
	public void stopFlyServer(final int pID, final float pX, final float pY) {
		
		if(MainActivity.this.timersList.get(pID) != null) {
			final Timer timer = MainActivity.this.timersList.get(pID);
			MainActivity.this.timersList.remove(pID);
			timer.cancel();
			timer.purge();
			
			try {
				final StopFlyServerMessage stopFlyServerMessage = (StopFlyServerMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_STOP_FLY);
				stopFlyServerMessage.set(pID, pX, pY);
				MainActivity.this.mSocketServer.sendBroadcastServerMessage(stopFlyServerMessage);
				MainActivity.this.mMessagePool.recycleMessage(stopFlyServerMessage);
				
			} catch (final IOException e) {
				Debug.e(e);
			}
		}
		
	}
	
	public void addFace(final int pID, final float pX, final float pY) {
		final Scene scene = this.mEngine.getScene();
		final Fly fly = new Fly(0, 0, resources.mFlyTextureRegion, MainActivity.this.getVertexBufferObjectManager(), pID);
		fly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
		fly.setPosition(pX - fly.getWidth() * 0.5f, pY - fly.getHeight() * 0.5f);
		fly.setUserData(pID);
		MainActivity.this.mFaces.put(pID, fly);
		MainActivity.this.mFacesExec.put(pID, (byte)1);
		scene.registerTouchArea(fly);
		scene.attachChild(fly);
		fly.setSpeed(100);
		fly.startFlying();
	}

	public void moveFace(final int pID, final byte pX, final byte pY) {
		final Fly fly = this.mFaces.get(pID);
		fly.setXYTurn(pX, pY);
	}
	
	public void stopFly(final int pID, final float pX, final float pY) {
		final Scene scene = MainActivity.this.mEngine.getScene();
		if(MainActivity.this.mFaces.get(pID) != null) {
			final Fly fly = MainActivity.this.mFaces.get(pID);
			MainActivity.this.mFaces.remove(pID);
			float x = fly.getX();
			float y = fly.getY();
			fly.killTheFly();
			fly.detachSelf();
			
			//dodanie do sceny gry grafiki dla nie¿ywej muchy
			final Sprite deadFly = new Sprite(x, y, resources.mDeadFlyTextureRegion, MainActivity.this.getVertexBufferObjectManager());
			scene.attachChild(deadFly);
			
		}
		

		//ustawienie pozycji na osiach X i Y dla nie¿ywej muchy
		//float x = face.getX();
		//float y = face.getY() + face.getHeight()/2;

		//final Sprite deadFly = new Sprite(pX, pY, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager());
		//deadFly.setPosition(pX - face.getWidth() * 0.5f, pY - face.getHeight()* 0.5f);

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
                        clientConnector.registerClientMessage(FLAG_MESSAGE_CLIENT_STOP_FLY, StopFlyClientMessage.class, new IClientMessageHandler<SocketConnection>() {
								@Override
								public void onHandleMessage(ClientConnector<SocketConnection> pClientConnector,IClientMessage pClientMessage)
									throws IOException {
										final StopFlyClientMessage stopFlyClientMessage = (StopFlyClientMessage)pClientMessage;
										MainActivity.this.stopFlyServer(stopFlyClientMessage.mID, stopFlyClientMessage.mX, stopFlyClientMessage.mY);
									}
		                        });
                        return clientConnector;
                }
        };
        this.mSocketServer.start();
	}
	
	/*private void initServer() {
		this.mSocketServer = new SocketServer<SocketConnectionClientConnector>(SERVER_PORT, new ExampleClientConnectorListener(), new ExampleServerStateListener()) {
			@Override
			protected SocketConnectionClientConnector newClientConnector(final SocketConnection pSocketConnection) throws IOException {
				return new SocketConnectionClientConnector(pSocketConnection);
			}
		};

		this.mSocketServer.start();
	}*/

	private void initClient() {
		try {
			this.mServerConnector = new SocketConnectionServerConnector(new SocketConnection(new Socket(this.mServerIP, SERVER_PORT)), new ExampleServerConnectorListener());

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_CONNECTION_CLOSE, ConnectionCloseServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					MainActivity.this.finish();
				}
			});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage)pServerMessage;
					MainActivity.this.addFace(addFaceServerMessage.mID, addFaceServerMessage.mX, addFaceServerMessage.mY);
				}
			});

			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage)pServerMessage;
					MainActivity.this.moveFace(moveFaceServerMessage.mID, moveFaceServerMessage.mX, moveFaceServerMessage.mY);
				}
			});
			
			this.mServerConnector.registerServerMessage(FLAG_MESSAGE_SERVER_STOP_FLY, StopFlyServerMessage.class, new IServerMessageHandler<SocketConnection>() {
				@Override
				public void onHandleMessage(final ServerConnector<SocketConnection> pServerConnector, final IServerMessage pServerMessage) throws IOException {
					final StopFlyServerMessage stopFlyServerMessage = (StopFlyServerMessage)pServerMessage;
					MainActivity.this.stopFly(stopFlyServerMessage.mID, stopFlyServerMessage.mX, stopFlyServerMessage.mY);
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
				Toast.makeText(MainActivity.this, pMessage, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	
	public static class StopFlyClientMessage extends ClientMessage {
		private int mID;
		private float mX;
		private float mY;

		public StopFlyClientMessage() {

		}

		public StopFlyClientMessage(final int pID, final float pX, final float pY) {
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
			return FLAG_MESSAGE_CLIENT_STOP_FLY;
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
		private byte mX;
		private byte mY;

		public MoveFaceServerMessage() {

		}

		public MoveFaceServerMessage(final int pID, final byte pX, final byte pY) {
			this.mID = pID;
			this.mX = pX;
			this.mY = pY;
		}

		public void set(final int pID, final byte pX, final byte pY) {
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
			this.mX = pDataInputStream.readByte();
			this.mY = pDataInputStream.readByte();
		}

		@Override
		protected void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException {
			pDataOutputStream.writeInt(this.mID);
			pDataOutputStream.writeByte(this.mX);
			pDataOutputStream.writeByte(this.mY);
		}
	}
	
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
	
	
	private class ExampleServerConnectorListener implements ISocketConnectionServerConnectorListener {
		@Override
		public void onStarted(final ServerConnector<SocketConnection> pConnector) {
			//MainActivity.this.toast("CLIENT: Connected to server.");
		}

		@Override
		public void onTerminated(final ServerConnector<SocketConnection> pConnector) {
			//MainActivity.this.toast("CLIENT: Disconnected from Server...");
			MainActivity.this.finish();
		}
	}

	private class ExampleServerStateListener implements ISocketServerListener<SocketConnectionClientConnector> {
		@Override
		public void onStarted(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			//MainActivity.this.toast("SERVER: Started.");
		}

		@Override
		public void onTerminated(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			//MainActivity.this.toast("SERVER: Terminated.");
		}

		@Override
		public void onException(final SocketServer<SocketConnectionClientConnector> pSocketServer, final Throwable pThrowable) {
			Debug.e(pThrowable);
			MainActivity.this.toast("SERVER: Exception: " + pThrowable);
		}
	}

	private class ExampleClientConnectorListener implements ISocketConnectionClientConnectorListener {
		@Override
		public void onStarted(final ClientConnector<SocketConnection> pConnector) {
			MainActivity.this.toast("SERVER: Client connected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}

		@Override
		public void onTerminated(final ClientConnector<SocketConnection> pConnector) {
			//MainActivity.this.toast("SERVER: Client disconnected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}
	}
	
	//Losuje krawedz ekranu z ktorej ma startowac mucha i zwraca wspolrzedne w postaci tablicy 2 elementowej
	private float[] flyRandomStartPosition() {
		float startPosX = 0; 
		float startPosY = 0;
		Random rand = new Random(); 
		int direction = rand.nextInt(4);
		switch(direction) {
			case 0: //lewa krawedz 
				startPosX = 0;
				startPosY = rand.nextInt(MainActivity.CAMERA_HEIGHT); 
			break; 
			case 1: // gorna krawedz 
				startPosX = rand.nextInt(MainActivity.CAMERA_WIDTH); 
				startPosY = 0;
			break; 
			case 2: // prawa krawedz 
				startPosX = MainActivity.CAMERA_WIDTH; 
				startPosY = rand.nextInt(MainActivity.CAMERA_HEIGHT);
			break; 
			case 3: //dolna krawedz 
				startPosX = rand.nextInt(MainActivity.CAMERA_WIDTH); 
				startPosY = MainActivity.CAMERA_HEIGHT; 
			break; 
		}
		float[] coords = {startPosX, startPosY};
		//Log.v("X", Float.toString(startPosX));
		//Log.v("Y", Float.toString(startPosY));
		return coords;
	}
	
	//losujemy czy zwrot muchy ma byc zgodny czy przeciwny do osi.
	private byte[] randomXYTurn() {
		Random rand = new Random();
		byte xTurn = (byte) rand.nextInt(2);
		byte yTurn = (byte) rand.nextInt(2);
		
		if (xTurn == 0) {
			xTurn = -1;
		}
		if (yTurn == 0) {
			yTurn = -1;
		}
		
		byte[] xyTurns = {xTurn, yTurn};
		return xyTurns;
	}
	 
}
