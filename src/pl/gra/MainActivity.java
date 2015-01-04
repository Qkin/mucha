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
 * Ekran g��wny gry
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
	public static final short FLAG_MESSAGE_SERVER_MOVE_FACE = FLAG_MESSAGE_SERVER_ADD_FACE + 1;
	public static final short FLAG_MESSAGE_SERVER_STOP_FLY = FLAG_MESSAGE_SERVER_MOVE_FACE + 1;

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
	
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private ITextureRegion mFaceTextureRegion;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public MainActivity() {
		this.initMessagePool();
	}

	private void initMessagePool() {
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_ADD_FACE, AddFaceServerMessage.class);
		this.mMessagePool.registerMessage(FLAG_MESSAGE_SERVER_MOVE_FACE, MoveFaceServerMessage.class);
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
		//if(MainActivity.this.mSocketServer != null) {
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
					if(pSceneTouchEvent.isActionDown()) {

						if(MainActivity.this.mSocketServer != null) {
							try {
								final AddFaceServerMessage addFaceServerMessage = (AddFaceServerMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_ADD_FACE);
								addFaceServerMessage.set(MainActivity.this.mFaceIDCounter++, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
	
								MainActivity.this.mSocketServer.sendBroadcastServerMessage(addFaceServerMessage);
								MainActivity.this.mMessagePool.recycleMessage(addFaceServerMessage);
								
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
						//try {
							//final Sprite fly = (Sprite)pTouchArea;
							//final Integer faceID = (Integer)fly.getUserData();
		
							/*final MoveFaceServerMessage moveFaceServerMessage = (MoveFaceServerMessage) MainActivity.this.mMessagePool.obtainMessage(FLAG_MESSAGE_SERVER_MOVE_FACE);
							moveFaceServerMessage.set(faceID, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
		
							MainActivity.this.mSocketServer.sendBroadcastServerMessage(moveFaceServerMessage);
		
							MainActivity.this.mMessagePool.recycleMessage(moveFaceServerMessage);*/
						/*} catch (final IOException e) {
							Debug.e(e);
							return false;
						}*/
					}
					
					if(MainActivity.this.mServerConnector != null) {
						final Fly fly = (Fly)pTouchArea;
						final Integer faceID = (Integer)fly.getUserData();
						float x = fly.getX();
						float y = fly.getY();
						MainActivity.this.mFaces.remove(faceID);
						fly.killTheFly();
						fly.detachSelf();
						
						final Sprite face = new Sprite(0, 0, resources.mDeadFlyTextureRegion, MainActivity.this.getVertexBufferObjectManager());
						face.setPosition( x,  y);
						
						scene.attachChild(face);
						face.setZIndex(-1);
					/*	final Fly deadFly = new Fly(0, 0, resources.mDeadFlyTextureRegion, MainActivity.this.getVertexBufferObjectManager(), MainActivity.this.mFaceIDCounter++);
						deadFly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
						deadFly.setPosition(pSceneTouchEvent.getX() - deadFly.getWidth() * 0.5f, pSceneTouchEvent.getY() - deadFly.getHeight() * 0.5f);
						scene.attachChild(deadFly);
						*/
						//Co ma robi� klient
						/* Create the face and add it to the scene. */
						//final Sprite face = new Sprite(0, 0, MainActivity.this.mFaceTextureRegion, MainActivity.this.getVertexBufferObjectManager());
						//face.setPosition( pSceneTouchEvent.getX() - face.getWidth() * 0.5f,  pSceneTouchEvent.getY() - face.getHeight() * 0.5f);
						//scene.registerTouchArea(face);
						//scene.attachChild(face);
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
		final Fly fly = new Fly(0, 0, resources.mFlyTextureRegion, this.getVertexBufferObjectManager(), pID);
		fly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
		fly.setActivity(MainActivity.this);
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
		// Find and move the face. 
		final Fly fly = this.mFaces.get(pID);
		fly.mPhysicsHandler.setVelocityX(pX * fly.getSpeed());
		fly.mPhysicsHandler.setVelocityY(pY * fly.getSpeed());
	}
	
	public void stopFly(final int pID, final float pX, final float pY) {
		final Scene scene = this.mEngine.getScene();
		final Fly fly = this.mFaces.get(pID);
		fly.killTheFly();
		this.mFaces.remove(pID);
		fly.detachSelf();
		
		final Fly deadFly = new Fly(0, 0, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager(), MainActivity.this.mFaceIDCounter++);
		deadFly.setCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
		deadFly.setPosition(pX - deadFly.getWidth() * 0.5f, pY - deadFly.getHeight() * 0.5f);
		scene.attachChild(deadFly);
		//final Fly face = this.mFaces.get(pID);
		// CopyOfMainActivity.this.toast("tekst");
		//face.killTheFly();
		//this.mFaces.remove(pID);
		
		//face.detachSelf();
		//ustawienie pozycji na osiach X i Y dla nie�ywej muchy
		//float x = face.getX();
		//float y = face.getY() + face.getHeight()/2;
		//final Sprite deadFly = new Sprite(x, y, resources.mDeadFlyTextureRegion, this.getVertexBufferObjectManager());

		//dodanie do sceny gry grafiki dla nie�ywej muchy

		// float x = face.getX();
		// float y = face.getY() + face.getHeight()/2;

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
                        clientConnector.registerClientMessage(FLAG_MESSAGE_CLIENT_DRAWLINE, DrawLineClientMessage.class, new IClientMessageHandler<SocketConnection>() {
								@Override
								public void onHandleMessage(
										ClientConnector<SocketConnection> pClientConnector,
										IClientMessage pClientMessage)
										throws IOException {
									final DrawLineClientMessage drawLineClientMessage = (DrawLineClientMessage)pClientMessage;
									MainActivity.this.addFace(drawLineClientMessage.mID, drawLineClientMessage.mX, drawLineClientMessage.mY);
									
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
			MainActivity.this.toast("CLIENT: Connected to server.");
		}

		@Override
		public void onTerminated(final ServerConnector<SocketConnection> pConnector) {
			MainActivity.this.toast("CLIENT: Disconnected from Server...");
			MainActivity.this.finish();
		}
	}

	private class ExampleServerStateListener implements ISocketServerListener<SocketConnectionClientConnector> {
		@Override
		public void onStarted(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			MainActivity.this.toast("SERVER: Started.");
		}

		@Override
		public void onTerminated(final SocketServer<SocketConnectionClientConnector> pSocketServer) {
			MainActivity.this.toast("SERVER: Terminated.");
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
			MainActivity.this.toast("SERVER: Client disconnected: " + pConnector.getConnection().getSocket().getInetAddress().getHostAddress());
		}
	}
	 
}
