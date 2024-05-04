package org.probe.mapsvr.tools;

import java.io.PrintStream;
import java.util.HashMap;

public interface IMapDownloader {

	boolean IsValid();

	void Exec(HashMap<String, String> params, PrintStream out);

	void Stop();

	void InitParams(HashMap<String, String> params);

	String GetStatus();

}
