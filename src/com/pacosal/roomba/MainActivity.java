package com.pacosal.roomba;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import com.pacosal.roomba.R.id;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	Handler h = new Handler();
	
	// accelerometer    
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private boolean running = false;
    private boolean cleaning = false;
	
	public static final int SETTINGS = 2;
	
	boolean connected = false;
	Socket socket = null; 	
	Thread cThread = null;

	byte[] c = new byte[5];	
	byte[] bOld = new byte[5];
	int speed = 0;
	int angulo = 0;
	int MAX_SPEED = 500;
	int MIN_SPEED = -500;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_main);
		
		Util.actividad = this;
        
        startSettings();  // cargar preferencias
		
        
        ////////////////////// cliente ////////////////////
        
        Button bScreen = (Button)findViewById(R.id.buttonConnect);
        bScreen.setText("Connect");
        bScreen.setOnClickListener(new OnClickListener() { 
			
			@Override
			public void onClick(View v) {

				if (!connected) {
		        	Util.logDebug("Cliente - conectando al servidor");
			         
			        // connect to server
	                if (!Util.serverIP.equals("")) {
	                	
	                    cThread = new Thread(new ClientThread());
	                    cThread.start();

	                }			        
	                
	                try {
						Thread.sleep(100);  

		                if (connected) {
		                	byte[] c = { (byte)128 }; // start
		                	send(c); 
		                	Thread.sleep(100);		        
		                	
		                	byte[] c1 = { (byte)132 }; //full mode
		                	send(c1); 
		                	Thread.sleep(100);

		                }
	                
	                	c[1] = 0;
	                	c[2] = 0;		                
		                c[3] = (byte)128;
		                c[4] = 0;
		                
		                enviarComando();
				        updateButtons();
		                
		                Util.start = false;
		                
	                } catch (Exception e) {
						Util.logDebug("Exception:  " + e.getMessage());
					}
	                
	                
				}
		        else {
		        	Util.logDebug("Cliente - desconectando del servidor");
		        	stop();
		        	// poner en pasivo
                	byte[] c1 = { (byte)131 }; //safe mode
                	send(c1); 
                	byte[] c0 = { (byte)133 }; //power
                	send(c0); 
		        	
			        // disconnect to server
			        connected = !connected;
	                Util.start = false;
			        updateButtons();
			        try {
			        	socket.close();
			        } catch(Exception e) {}
		        
		        }
				
			}
		});
        
		Button bClean = (Button) findViewById(id.buttonClean);
		bClean.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (connected) {
                	if (!cleaning) {
                		cleaning = !cleaning;
                		byte[] c = { (byte)138, (byte)7 }; //
                    	send(c); 
                	}
                	else {
                		cleaning = !cleaning;
                		byte[] c = { (byte)138, (byte)0 }; //
                    	send(c); 
                	}
                }
			}
		});

		Button bStart = (Button) findViewById(id.buttonStart);
		bStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (connected) {
                	if (Util.start) {
                		Util.start = false;
                    	stop();
                	}
                	else {
                		Util.start = true;
                	}
                	updateButtons();
                }
			}
		});

		Button bSlow = (Button) findViewById(id.buttonSlow);
		bSlow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (connected) {
                	Util.slow = !Util.slow;
                	updateButtons();
                }
			}
		});
		
		
		
		Button bDock = (Button) findViewById(id.buttonDock);
		bDock.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (connected) {
                		byte[] c = { (byte)143 }; //
                    	send(c); 
                }
			}
		});
		
		Button bSpot = (Button) findViewById(id.buttonSpot);
		bSpot.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (connected) {
                		byte[] c = { (byte)134 }; //
                    	send(c); 
                }
			}
		});

		updateButtons();
		startSensor();
		
		
        if (Util.unaVez == false) {  
	    	new AlertDialog.Builder(this).setMessage(this.getString(R.string.UnaVez))
	        .setCancelable(false)
	        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Util.actividad);
	            	SharedPreferences.Editor editor = settings.edit();
	            	Util.unaVez = true;
	            	editor.putBoolean("unaVez", Util.unaVez);
	            	editor.commit();
	            	
	            	settings();
		        } 
	                
	        }).create().show();  	  
        }         
		
		
	}

	
	public void enviarComando() {
        h.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (connected) {
					send(c);
					enviarComando();
				}
			}
		}, 500);
		
	}
	
	public void updateButtons() {
        Button bScreen = (Button)findViewById(R.id.buttonConnect);
        if (connected) {
        	bScreen.setText("Disconnect");
        }
        else {
        	bScreen.setText("Connect");
        }
		
        Button bStart = (Button)findViewById(R.id.buttonStart);
        if (Util.start) {
        	bStart.setText("Stop");
        }
        else {
        	bStart.setText("Start");
        }
        
		Button bDock = (Button) findViewById(id.buttonDock);
		Button bSpot = (Button) findViewById(id.buttonSpot);
		Button bClean = (Button) findViewById(id.buttonClean);
		Button bSlow = (Button) findViewById(id.buttonSlow);
        
        if (connected) {
        	bStart.setEnabled(true);
        	bDock.setEnabled(true);
        	bSpot.setEnabled(true);
        	bClean.setEnabled(true);
        	bSlow.setEnabled(true);
        }
        else {
        	bStart.setEnabled(false);
        	bDock.setEnabled(false);
        	bSpot.setEnabled(false);
        	bClean.setEnabled(false);
        	bSlow.setEnabled(false);
        }
        
        
        
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopSensor();
	}
    
    private void startSensor() {
    	Util.logDebug("startSensor");
    	try {
	        boolean supported = false;
	        if (getApplicationContext() != null) {
	            sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
	            List<Sensor> sensors = sensorManager.getSensorList(
	                    Sensor.TYPE_ACCELEROMETER);
	            supported = new Boolean(sensors.size() > 0);
	        } else {
	            supported = Boolean.FALSE;
	        }
	        Util.logDebug("supported: " + supported);
	        if (supported) {
	                sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
	                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
	                if (sensors.size() > 0) {
	                    sensor = sensors.get(0);
	                    running = sensorManager.registerListener(sensorEventListener, sensor,SensorManager.SENSOR_DELAY_UI);
	                    Util.logDebug("running: " + running);
	                }
	
	        }
    	} catch (Exception e) {}
    	
    }
    
    private void stopSensor() {
    	Util.logDebug("stopSensor");
        if (running) {
            try {
                if (sensorManager != null && sensorEventListener != null) {
                    sensorManager.unregisterListener(sensorEventListener);
                    running = false;
                    Util.logDebug("parando eventos: " + running);
                    
                }
            } catch (Exception e) {}

        }
    	
    }
	
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        
        private float x,y,z = 0;
 
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
 
        public void onSensorChanged(SensorEvent event) {

            z = event.values[2];
            x = event.values[0];
            y = event.values[1];
            update(x,y,z);
        }
     
    };
    
	
	private void stop() {
        if (connected) {
        	c[0] = (byte)137;
        	c[1] = 0;
        	c[2] = 0;		                
            c[3] = (byte)128;
            c[4] = 0;

            speed = 0;
            angulo = 0;
        }		
	}
	
	
	private void forward() {
        if (connected) {
            c[1] = (byte) ((speed>>8)&0x00FF);
            c[2] = (byte)((speed)&0x00FF);
        	c[0] = (byte) 137;
        }		
	}

	private void left() {
        if (connected) {
            c[3] = (byte)((angulo>>8)&0x00FF);
            c[4] = (byte)(angulo&0x00FF);
        }	
	}
	
	
	private void right() {
        if (connected) {
        	c[3] = (byte)(((angulo)>>8)&0x00FF);
        	c[4] = (byte)((angulo)&0x00FF);
        }		
	}

	private void recto() {
        if (connected) {
        	c[3] = (byte)128;
        	c[4] = (byte)0;
        }		
	}
	
	private void update(float x, float y, float z) {
		
		if (!Util.start) {
			//Util.logDebug("no start");
			return;
		}

		// delante
		x = x - 4;
		
		if (x > 5) {
			speed = -200;

			if (Util.slow)
				speed = -50;
			
			forward();
		}
		if (x > 3  && x <=5)
			stop();

		if (x <=3) {
			int s = (int)(x-3)*(-1);
			speed = s*50;
			if (speed > 500)
				speed = 500;
			
			if (Util.slow)
				speed = 50;
			
			forward();
		}
		
		// lados
		if (y >= -2 && y <= 2) {
			angulo = 0;
			recto();
		}
		if (y < -2) {
			int a = (int)(y + 2)*(-1);
			angulo = 800 - (a * 100);
			if (angulo <= 0)
				angulo = 10;
			left();
		}
		if (y > 2) {
			int a = (int)(y + (-2));
			angulo = (800 - (a * 100))*-1;
			if (angulo >= 0)
				angulo = -10;
			right();
		}
		
	}
    
	/**
	 * cliente: send a servidor
	 * @param s
	 */
	public void send(byte[] s) {
		//Util.logDebug("sending");
		
		try {
			if (socket != null) {
				OutputStream out = socket.getOutputStream();
                out.write(s);
                out.flush();
			}
		} catch (Exception e) {
            Util.logDebug("Cliente - Exception: " + e.getMessage()); 
		}
	}	
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case SETTINGS:
            	startSettings();
            	break;

            default:
                break; 
        }
    }   

    /**
     * start settings
     */ 
    public void startSettings() {
    	
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.debugMode = settings.getBoolean("debug", false);
        Util.serverIP = settings.getString("ip", Util.serverIP);
        Util.unaVez = settings.getBoolean("unaVez", Util.unaVez);
    }	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true; 
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menusettings:
	        settings();
	        return true; 
	    default:
	        return super.onOptionsItemSelected(item); 
	    }
	}	    
	
	/**
	 * settings
	 */
	private void settings() {
		Intent i = new Intent(MainActivity.this, MVPreferenceActivity.class);
        startActivityForResult(i, SETTINGS);
		
	}
	
	/**
	 * clase cliente
	 * @author paco_imac
	 *
	 */
	public class ClientThread implements Runnable {

	    public void run() {
	        try {
	            InetAddress serverAddr = InetAddress.getByName(Util.serverIP);
	            Util.logDebug("Cliente - Connecting...: " + serverAddr);
	            socket = new Socket(Util.serverIP, 9001);
	            connected = true;
            	InputStreamReader in = new InputStreamReader(socket.getInputStream());

            	Util.logDebug("socket:" + socket.getLocalPort() ); 
            	
            	while (connected) {
	                try {
	                	char[] buffer = new char[1000];
	                    while (in.read(buffer) != 0) {
		                	Util.logDebug("Cliente - Recibido: " + buffer.toString());
	                    }	
	                	
	                } catch (Exception e) {
	                	Util.logDebug("Cliente - Exception: " + e.getMessage());
	                }
	            }
	            Util.logDebug("Cliente - Closed.");
	            
	        } catch (Exception e) {
	        	Util.logDebug("Cliente - Exception: " + e.getMessage());
	        	runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Util.actividad.updateButtons();
					}
				});
	            connected = false;
	        }
	    }
	    
	}
	

}
