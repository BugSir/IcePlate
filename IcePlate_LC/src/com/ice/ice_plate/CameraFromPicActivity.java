package com.ice.ice_plate;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ice.entity.PlateRecognitionParameter;
import com.ice.iceplate.R;
import com.ice.iceplate.RecogService;
import com.ice.util.CarIceUtils;
import com.ice.util.Utils;

public class CameraFromPicActivity extends Activity {
	private TextView tvShowCarid;
	private String number = "", color = "";
	private int iInitPlateIDSDK = -1;
	private String[] fieldvalue = new String[10];
	private Vibrator mVibrator;
	private PlateRecognitionParameter prp = new PlateRecognitionParameter();
	private byte[] tempData;
	public RecogService.MyBinder recogBinder;
	private static StringBuffer sbfile = new StringBuffer();
	private StringBuffer sb = new StringBuffer();
	private boolean isFirstPic = true;// �ж��ǲ��ǵ�һ��Ԥ����ͼƬ
	public ServiceConnection recogConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			recogConn = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			recogBinder = (RecogService.MyBinder) service;

			iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();

			if (iInitPlateIDSDK != 0) {

				String[] str = { "" + iInitPlateIDSDK };
				getResult(str);
			}
		}
	};
	
	private String type;
	private String path;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_camerafrompic);
		
		type = getIntent().getStringExtra("type");
		path = getIntent().getStringExtra("path");

		findView();
	}
	
	private byte[] filePath2Bytes(String filePath) {  
		BufferedInputStream in;
		byte[] content = null;
		try {
			in = new BufferedInputStream(new FileInputStream(filePath));
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);    
		       
	        byte[] temp = new byte[1024];        
	        int size = 0;        
	        while ((size = in.read(temp)) != -1) {        
	            out.write(temp, 0, size);        
	        }        
	        in.close();  
	        content = out.toByteArray();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;        
	} 

	private Thread tt;
	private void findView() {
		tvShowCarid = (TextView) findViewById(R.id.tv_show_carid);
		List<String> picList = new ArrayList<String>() ;
		if(type.equals("file")){
			picList = getImagePathFromSD(1);
		}else{
			try {
				picList = GetFileList(path,false,true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (isFirstPic) {
			Intent authIntent = new Intent(CameraFromPicActivity.this,RecogService.class);
			bindService(authIntent, recogConn, Service.BIND_AUTO_CREATE);
			isFirstPic = false;
		}
		
//		startICEThread(picList);
		
		new Thread(new Runnable() {	
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					String responseStr = CarIceUtils.getCarInfo("/sdcard/qaz123.jpg",1);
					JSONObject obj = new JSONObject(responseStr);
			    	String result = obj.optString("result");
			    	JSONObject objResult = new JSONObject(result);
			    	String number = objResult.optString("number");
			    	String color = objResult.optString("color");
			    	
				}catch (Exception e) {
			    	e.printStackTrace();
			    }
			}
		}).start();
	}
	
	void startICEThread(final List<String> picList){
		tt = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					tt.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (int i = 0; i < picList.size(); i++) {
					byte[] bt= filePath2Bytes(picList.get(i));
					tempData = bt;
					prp.picByte = bt;
					if (bt != null) {
						fieldvalue = recogBinder.doRecogDetail(prp);
						getResult(fieldvalue);
					}
				}
			}
		});
		tt.start();
	}
	
	/** 
	 * ��sd����ȡͼƬ��Դ 
	 * @return 
	 */  
	private List<String> getImagePathFromSD(int type) {  
		// ͼƬ�б�   
		List<String> imagePathList = new ArrayList<String>();  
		// �õ�sd����image�ļ��е�·��   File.separator(/)
//		String filePath = null;
//		if(type == 1){
//			filePath = Environment.getExternalStorageDirectory().toString() + File.separator+ "image";  
//		}else if(type == 2){
//			filePath = Environment.getExternalStorageDirectory().toString() + File.separator+ "video";
//		}
		// �õ���·���ļ��������е��ļ�   
		sbfile.setLength(0);
		File fileAll = new File(path);  
		File[] files = fileAll.listFiles();  
		// �����е��ļ�����ArrayList��,����������ͼƬ��ʽ���ļ�   
		for (int i = 0; i < files.length; i++) {  
			File file = files[i];
			if(type == 1){
				if (checkIsImageFile(file.getPath())) {  
					imagePathList.add(file.getPath());  
					sbfile.append(file.getName()+"#");
				}  
			}
		}  
//		tvShowCarid.setText(sbfile.toString());
		// ���صõ���ͼƬ�б�   
		return imagePathList;  
	}  
	
	/**
     * ȡ��ѹ�����е� �ļ��б�(�ļ���,�ļ���ѡ)
     * 
     * @param zipFileString
     *            ѹ��������
     * @param bContainFolder
     *            �Ƿ���� �ļ���
     * @param bContainFile
     *            �Ƿ���� �ļ�
     * @return
     * @throws Exception
     */
    private java.util.List<String> GetFileList(
            String zipFileString, boolean bContainFolder, boolean bContainFile)
            throws Exception {
    	List<String> fileList = new ArrayList<String>();  
    	sbfile.setLength(0);
        InputStream in = new BufferedInputStream(new FileInputStream(zipFileString));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
 
        while ((ze = zin.getNextEntry()) != null) {  
            String szName = ze.getName();
 
            if (ze.isDirectory()) {
            	
                szName = szName.substring(0, szName.length() - 1);
                java.io.File folder = new java.io.File(szName);
                if (bContainFolder) {
                    fileList.add(folder.getName());
                }
 
            } else {
                java.io.File file = new java.io.File(szName);
                if (bContainFile) {
                    fileList.add(file.getName());
                    sbfile.append(file.getName()+"#");
                }
            }
        }
        zin.close();
//        tvShowCarid.setText(sbfile.toString());
        return fileList;
    }
	
	/** 
	* �����չ�����õ�ͼƬ��ʽ���ļ� 
	* @param fName  �ļ��� 
	* @return 
	*/  
	@SuppressLint("DefaultLocale")  
	private boolean checkIsImageFile(String fName) {  
		boolean isImageFile = false;  
		// ��ȡ��չ��   
		String FileEnd = fName.substring(fName.lastIndexOf(".") + 1,  
		fName.length()).toLowerCase();  
		if (FileEnd.equals("jpg") || FileEnd.equals("png") || FileEnd.equals("gif")|| FileEnd.equals("jpeg")|| FileEnd.equals("bmp") ) {  
			isImageFile = true;  
		} else {  
			isImageFile = false;  
		}  
		return isImageFile;  
	}

	int[] fieldname = { R.string.plate_number, R.string.plate_color,
			R.string.plate_color_code, R.string.plate_type_code,
			R.string.plate_reliability, R.string.plate_leftupper_pointX,
			R.string.plate_leftupper_pointY, R.string.plate_rightdown_pointX,
			R.string.plate_rightdown_pointY, R.string.plate_car_color };

	/**
	 * 
	 * @Title: getResult
	 * @Description: (��ȡ���)
	 * @param @param fieldvalue ����ʶ��ӿڷ��ص�����
	 * @return void ��������
	 * @throws
	 */
	private void getResult(String[] fieldvalue) {

		if (iInitPlateIDSDK != 0) {

			String nretString = iInitPlateIDSDK + "";
			if (nretString.equals("1793")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								getString(R.string.failed_file_timeout),
								Toast.LENGTH_SHORT).show();
					}
				});
			} else if (nretString.equals("276")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								getString(R.string.failed_noFile_find),
								Toast.LENGTH_SHORT).show();
					}
				});
				
			} else if (nretString.equals("-10002")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								getString(R.string.failed_noAuth), Toast.LENGTH_SHORT)
								.show();
					}
				});
				
			} else if (nretString.equals("-10004")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								getString(R.string.failed_File_error),
								Toast.LENGTH_SHORT).show();
					}
				});
				
			} else if (nretString.equals("-10003")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								getString(R.string.failed_init_error),
								Toast.LENGTH_SHORT).show();
					}
				});
				
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"���򼤻�ʧ�ܣ�������Ϊ��" + iInitPlateIDSDK, Toast.LENGTH_SHORT)
								.show();
					}
				});
				
			}

		} else {
			String[] resultString;
			String boolString = "";
			boolString = fieldvalue[0];// ���ƺ�
			sb.append(boolString);
			if (boolString != null && !boolString.equals("")) {
				resultString = boolString.split(";");
				int lenght = resultString.length;
				if (lenght > 0) {

					String[] strarray = fieldvalue[4].split(";");
					if (Float.valueOf(strarray[0]) > 14) {
						if (lenght == 1) {
							if (null != fieldname) {
								Intent intent = new Intent(CameraFromPicActivity.this,ResultActivity.class);
								number = fieldvalue[0];
								color = fieldvalue[1];

								int left = Integer.valueOf(fieldvalue[5]);
								int top = Integer.valueOf(fieldvalue[6]) - 5;
								int w = Integer.valueOf(fieldvalue[7])
										- Integer.valueOf(fieldvalue[5]);
								int h = Integer.valueOf(fieldvalue[8])
										- Integer.valueOf(fieldvalue[6]) + 10;
								
								sbfile.append("���ƺţ�"+number+"#");

								intent.putExtra("number", number);
								intent.putExtra("color", color);
								intent.putExtra("left", left);
								intent.putExtra("top", top);
								intent.putExtra("width", w);
								intent.putExtra("height", h);

//								startActivity(intent);
							}

						} else {

							String path = null;

							String itemString = "";

							mVibrator = (Vibrator) getApplication()
									.getSystemService(Service.VIBRATOR_SERVICE);
							mVibrator.vibrate(100);
							Intent intent = new Intent(CameraFromPicActivity.this,
									ResultActivity.class);
							for (int i = 0; i < lenght; i++) {

								itemString = fieldvalue[0];
								resultString = itemString.split(";");
								number += resultString[i] + ";\n";

								itemString = fieldvalue[1];
								resultString = itemString.split(";");
								color += resultString[i] + ";";
							}

							int left = Integer.parseInt(fieldvalue[5]
									.split(";")[0]);
							int top = Integer
									.parseInt(fieldvalue[6].split(";")[0]);
							int w = Integer
									.parseInt(fieldvalue[7].split(";")[0])
									- Integer
											.parseInt(fieldvalue[5].split(";")[0]);
							int h = Integer
									.parseInt(fieldvalue[8].split(";")[0])
									- Integer
											.parseInt(fieldvalue[6].split(";")[0]);
							
							sbfile.append("���ƺţ�"+number+"#");

							intent.putExtra("number", number);
							intent.putExtra("color", color);
							intent.putExtra("path", path);
							intent.putExtra("left", left);
							intent.putExtra("top", top);
							intent.putExtra("width", w);
							intent.putExtra("height", h);

//							startActivity(intent);
						}
					}

				}

			}else {
					if (null != fieldname) {
						mVibrator = (Vibrator) getApplication()
								.getSystemService(Service.VIBRATOR_SERVICE);
						mVibrator.vibrate(100);
						Intent intent = new Intent(CameraFromPicActivity.this,ResultActivity.class);
						number = fieldvalue[0];
						color = fieldvalue[1];
						if (fieldvalue[0] == null) {
							number = "null";
						}
						if (fieldvalue[1] == null) {
							color = "null";
						}
						int left = prp.plateIDCfg.left;
						int top = prp.plateIDCfg.top;
						int w = prp.plateIDCfg.right - prp.plateIDCfg.left;
						int h = prp.plateIDCfg.bottom - prp.plateIDCfg.top;
						
						sbfile.append("���ƺţ�"+number+"#");

						intent.putExtra("number", number);
						intent.putExtra("color", color);
						intent.putExtra("left", left);
						intent.putExtra("top", top);
						intent.putExtra("width", w);
						intent.putExtra("height", h);

//						startActivity(intent);
					}
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tvShowCarid.setText(sb.toString());
				}
			});
		}
		fieldvalue = null;
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (recogBinder != null) {
			unbindService(recogConn);
		}

		CameraFromPicActivity.this.finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Utils.save(sbfile.toString(), "����ʶ��");//������ź�id����γ�ȵ�sd���ļ�
	}

}