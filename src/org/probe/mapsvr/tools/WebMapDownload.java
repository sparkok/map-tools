package org.probe.mapsvr.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.probe.util.SmartString;

public class WebMapDownload {
	public final static Logger log = Logger.getLogger( "WebMapDownload" );
	public String m_strKeyPath = "c:/download.key";
	public Vector<String> GetWebKey(String p_strQueryTime,String p_strJSP,String p_strParam,String p_strUser,String p_strPass) throws Throwable
	{

		String strUrl = p_strJSP;
		URL url = new URL(strUrl);
		String data = URLEncoder.encode("params", "UTF-8") + "=" + URLEncoder.encode(p_strParam,"UTF-8");
		data += "&" + URLEncoder.encode("user", "UTF-8") + "=" + URLEncoder.encode(p_strUser,"UTF-8");
		data += "&" + URLEncoder.encode("pass", "UTF-8") + "=" + URLEncoder.encode(p_strPass,"UTF-8");
		data += "&" + URLEncoder.encode("querytime", "UTF-8") + "=" + URLEncoder.encode(p_strQueryTime,"UTF-8");
		log.info("url param:" + data);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();

		//通过连接返回新的key值和启动下载器的参数
		Vector<String> oRet = GetKeyAndParam(conn);
		wr.close();
		return oRet;
	}
	private Vector<String> GetKeyAndParam(URLConnection p_conn){
		//Get the response
		BufferedReader rd;
		try {
			rd = new BufferedReader(new InputStreamReader(p_conn.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return null;
		}
		String line;
		Vector<String> ret = new Vector<String>();
		//读取2行,第一行是key第二行是params
		SmartString oLine = new SmartString();
		String strLine;
		try {
			strLine = rd.readLine();
			rd.close();
		} catch (IOException e) {
			strLine = null;
		}
		if(strLine == null || strLine.length() == 0)
		{
				return null;
		}
		oLine.SetString(strLine);

		String strKey = null;
		String strParam = null;
		try {
			strKey = oLine.ReadStringParam(",",false,true);
			strParam = oLine.ReadStringParam(",",false,true);
		} catch (EOFException e) {
			strKey = null;
			strParam = null;
		}
		if((strKey == null) || (strParam == null)) return null;
		if((strKey.length() == 0) || (strParam == null)) return null;
		ret.add(strKey);
		ret.add(strParam);
		return ret;
	}
	public String MakeParam(String p_strKey,double p_dX1,double p_dY1,double p_dX2,double p_dY2,int p_nMinZoom,int p_nMaxZoom)
	{
		String StrParam = p_strKey + "|" + p_dX1 + "|" + p_dY1 + "|" + p_dX2 + "|" + p_dY2 + "|" + p_nMinZoom + "|" + p_nMaxZoom;
		return StrParam;
	}
	public String GetOldKey(String p_strKeyPath)
	{
		//读取本地文件的key提交给服务器
		BufferedReader in;
		String strOldKey = "init";
		try {
			in = new BufferedReader(new FileReader(new File(p_strKeyPath)));
			strOldKey = in.readLine();
			in.close();
		}
		catch (FileNotFoundException e) {

		}
		catch (IOException e) {

		}
		return strOldKey;
	}
	public void SetKey(String p_strKeyPath,String p_strKey)
	{
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(new File(p_strKeyPath)));
			out.write(p_strKey);
			out.close();
		} catch (FileNotFoundException e) {
			log.error("SetKey Failed!");
		}
		catch (IOException e) {
			log.error("SetKey Failed!");
		}

	}
	public String GetKeyFromHost(String p_strQueryTime,String p_strHost,double p_dX1,double p_dY1,double p_dX2,double p_dY2,int p_nZoomMin,int p_nZoomMax,String  p_strUser,String p_strPass) throws Throwable
	{
		//获取文件中的key值
		String strKey = GetOldKey(m_strKeyPath);
		//用本地key值请求新key值
		Vector<String> oRet = GetWebKey(p_strQueryTime,p_strHost,MakeParam(strKey,p_dX1,p_dY1,p_dX2,p_dY2,p_nZoomMin,p_nZoomMax),p_strUser,p_strPass);
		if(oRet == null || oRet.size() != 2) return "";
		//设置新key值
		SetKey(m_strKeyPath,oRet.get(0));
		//下载key数值写入本地文件
		//返回启动参数
		return oRet.get(1);
	}
	public IMapDownloader  FastMake4QueryNetWebSvr(String p_strHost,double p_dX1,double p_dY1,double p_dX2,double p_dY2,int p_nZoomMin,int p_nZoomMax,String p_strUser,String p_strPass,HashMap<String, String> p_params) throws Throwable
	{
		String strQueryTime = "" + System.currentTimeMillis();
		String strParams = GetKeyFromHost(strQueryTime,p_strHost,p_dX1,p_dY1,p_dX2,p_dY2,p_nZoomMin,p_nZoomMax,p_strUser,p_strPass);
		if(strParams == null || strParams.length() == 0)
		{
			log.warn("failed to get param!");
			return null;
		}
		if(CheckParam(strQueryTime,p_dX1,p_dY1,p_dX2,p_dY2,p_nZoomMin,p_nZoomMax,strParams))
		{
			IMapDownloader oDownLoader = new GoogleMap2XYZDownloaderV2();
			HashMap<String, String> params = p_params;
			params.put("bound",String.format("%f,%f,%f,%f",p_dX1,p_dY1,p_dX2,p_dY2));
			params.put("zoom",String.format("%d,%d",p_nZoomMin,p_nZoomMax));
			params.put("dir","c:/data/");
			params.put("thread","6");
			params.put("datatype","0");
			return oDownLoader;
		}
		return null;
	}
	public IMapDownloader  FastMake4QueryNetWebSvrWithoutGetKey(double p_dX1,double p_dY1,double p_dX2,double p_dY2,int p_nZoomMin,int p_nZoomMax,String p_strPass,HashMap<String, String> p_params) throws Throwable
	{
		String strQueryTime = "" + System.currentTimeMillis();
		IMapDownloader oDownLoader = new GoogleMap2XYZDownloaderV3();
		HashMap<String, String> params = p_params;
		params.put("bound",String.format("%f,%f,%f,%f",p_dX1,p_dY1,p_dX2,p_dY2));
		params.put("zoom",String.format("%d,%d",p_nZoomMin,p_nZoomMax));
		params.put("dir","c:/data/");
		params.put("thread","6");
		params.put("pwd",p_strPass);
		return oDownLoader;
	}

	public IMapDownloader  FastMake4LocalMechine(double p_dX1,double p_dY1,double p_dX2,double p_dY2,int p_nZoomMin,int p_nZoomMax,String p_strMapPath,HashMap<String, String> p_params) throws Throwable
	{
			IMapDownloader oDownLoader = new GoogleMap2XYZDownloaderV2();
			HashMap<String, String> params = p_params;
			params.put("bound",String.format("%f,%f,%f,%f",p_dX1,p_dY1,p_dX2,p_dY2));
			params.put("zoom",String.format("%d,%d",p_nZoomMin,p_nZoomMax));
			params.put("dir",p_strMapPath);
			params.put("thread","6");
			params.put("datatype","0");
			return oDownLoader;
	}

	public String CalculateParam(String p_strQueryTime,double p_dx1, double p_dy1, double p_dx2, double p_dy2, int zoomMin, int zoomMax)
	{
		String strResult = "" + p_strQueryTime;
		strResult += "," + strResult.hashCode();
		strResult += "," + p_dx1;
		strResult += "," + strResult.hashCode();
		strResult += "" + p_dy1;
		strResult += "," + strResult.hashCode();
		strResult += "," + p_dx2;
		strResult += "," + strResult.hashCode();
		strResult += "," + p_dy2;
		strResult += "," + strResult.hashCode();
		strResult += "," + zoomMin;
		strResult += "," + strResult.hashCode();
		strResult += "," + zoomMax;
		strResult += "," + strResult.hashCode();
		return URLEncoder.encode(org.probe.util.Base64.encode(strResult));
	}
	private boolean CheckParam(String p_strQueryTime,double p_dx1, double p_dy1, double p_dx2, double p_dy2, int zoomMin, int zoomMax, String p_strParams) {
		if(p_strParams == null) return false;
		String strParam = CalculateParam(p_strQueryTime,p_dx1,p_dy1,p_dx2,p_dy2,zoomMin,zoomMax);
		if(p_strParams.compareTo(strParam) == 0)
		{
			return true;
		}
		else
		{
			//测试使用
			return false;
		}

	}
	public static void downloadWithKey()
	{
		WebMapDownload mapdownload = new WebMapDownload();
		mapdownload.m_strKeyPath = "c:/download.key";
		try {
			HashMap<String, String> params = new HashMap<String, String>();
			//IMapDownloader downloader = mapdownload.FastMake4QueryNetWebSvr("http://localhost:8081/DownloadAccess/access",20,20,30,30,0,1,"user2","pass2",params);
			double dX1,dY1,dX2,dY2;
			dX1 = 135.05;
			dX2 = 73.667;
			dY1 = 3.86666;
			dY2 = 53.5;
			IMapDownloader downloader = mapdownload.FastMake4QueryNetWebSvr("http://58.53.128.195:8081/DownloadAccess/access",dX1,dY1,dX2,dY2,11,12,"user2","pass2",params);

			if(downloader != null)
			params.put("dir","c:/ms4w/Apache/htdocs/mycache/basic");
			params.put("thread","20");
			downloader.Exec(params,null);
			//输入指令退出
			boolean bQuit = false;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			PrintStream out = System.out;
			do
			{
					out.print("command:");
					String strInput = in.readLine();
					if(strInput.compareTo("exit") == 0)
					{
						bQuit = true;
					}
					else if(strInput.compareTo("state") == 0)
					{
						CmdState(in,out,downloader);
					}
			}
			while(!bQuit);
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}
	public static void downloadWithoutKey()
	{
		WebMapDownload mapdownload = new WebMapDownload();
		try {
			HashMap<String, String> params = new HashMap<String, String>();
			double dX1,dY1,dX2,dY2;
			dX1 = 135.05;
			dX2 = 73.667;
			dY1 = 3.86666;
			dY2 = 53.5;
			IMapDownloader downloader = mapdownload.FastMake4QueryNetWebSvrWithoutGetKey(dX1,dY1,dX2,dY2,11,12,"c5h8no4na",params);

			if(downloader != null)
			params.put("dir","c:/dddd");
			params.put("mapURL0","http://mt1.google.cn/vt/lyrs=m@115&hl=zh-CN&x={x}&y={y}&z={z}&s=Galile");
			params.put("mapURL1","http://mt2.google.cn/vt/lyrs=m@115&hl=zh-CN&x={x}&y={y}&z={z}&s=Galile");
			params.put("mapURL2","http://mt3.google.cn/vt/lyrs=m@115&hl=zh-CN&x={x}&y={y}&z={z}&s=Galile");
			params.put("proxyHost","202.155.223.108");
			params.put("proxyPort","110");
			params.put("proxyUser","chenjd");
			params.put("proxyPassword","c2h5oh++");


			downloader.Exec(params,null);
			//输入指令退出
			boolean bQuit = false;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			PrintStream out = System.out;
			do
			{
					out.print("command:");
					String strInput = in.readLine();
					if(strInput.compareTo("exit") == 0)
					{
						bQuit = true;
					}
					else if(strInput.compareTo("state") == 0)
					{
						CmdState(in,out,downloader);
					}
			}
			while(!bQuit);
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		if(args.length == 0)
		{
			Properties propLog4j = new Properties();
			propLog4j.setProperty("log4j.rootLogger","debug,consoleAppender");
			propLog4j.setProperty("log4j.appender.consoleAppender","org.apache.log4j.ConsoleAppender");
			propLog4j.setProperty("log4j.appender.consoleAppender.Threshold","debug");
			propLog4j.setProperty("log4j.appender.consoleAppender.layout","org.apache.log4j.PatternLayout");
			propLog4j.setProperty("log4j.appender.consoleAppender.layout.ConversionPattern","%d %-5p %m %n");
			propLog4j.setProperty("log4j.appender.consoleAppender.ImmediateFlush","true");
			PropertyConfigurator.configure(propLog4j);
		}
		else
		{
			PropertyConfigurator.configure(args[0]);
		}
		downloadWithoutKey();
		
	}
	private static void CmdState(BufferedReader in, PrintStream out,IMapDownloader downloader) {
		out.println(downloader.GetStatus());
	}
	public IMapDownloader FastMake4QueryNetWebSvrWithoutGetKey() {
		// TODO Auto-generated method stub
		return null;
	}
}
