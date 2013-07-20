/* 
   Utils. Static modules of common use. 
   Copyright (C) 2013 Michael Glickman (Australia) <palmcrust@gmail.com>

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>
*/    


package com.palmcrust.yawadb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.widget.Toast;

public class Utils {
	@SuppressWarnings("deprecation")
	public static int getAPIVersion() {
		try {
			return android.os.Build.VERSION.class.getField("SDK_INT").getInt(null);
		} catch (Exception ex) {
			return Integer.parseInt(android.os.Build.VERSION.SDK);
		}
	}
	
			
	public static int getAdbdPid() {
		 Process p = runCommand("ps", "adbd");
		 if (p==null) return -1;
		 BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 try { 
			 String line;
			 while((line=rd.readLine())!=null) {
				 if (line.indexOf("/adbd") >= 0) {
					 String[] tokens = line.split("[ \\t]+");
					 if (tokens.length >= 2) 
						 return Integer.parseInt(tokens[1]);
				 }
			 }
			 return -1;
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 return -2;
		 } finally {
			 try { rd.close();} catch(IOException ex) {}
		 }
	}

	public static boolean isEmpty(String str) {
		return (str==null) || (str.length() <= 0);
	}

	
	public static String getProp(String name) {
		 Process p = runCommand("getprop", name);
		 if (p==null) return null;
		 BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 String line=null;
		 try { line = rd.readLine();
		 	if (line != null) line = line.split("\r")[0];
		 } catch(IOException ex) {ex.printStackTrace();}
		 try { rd.close(); } catch(IOException ex) {}
		 return (line != null && line.length() > 0) ? line : null;
	}
	
	public static Process runCommand(String... cmd)  {
		try {
			 return Runtime.getRuntime().exec(cmd);
		 } catch (IOException ex) {
			 ex.printStackTrace();
			 return null;
		 }
	 }

	 public static boolean runBatchSequence(
			 final String shell, String[] cmdSequence) throws InterruptedException {
		 DataOutputStream os = null;            
		 try {
			 Process p = Runtime.getRuntime().exec(shell);
			 os = new DataOutputStream(p.getOutputStream());            
			 for (String cmd : cmdSequence) {
                 os.writeBytes(cmd);
                 os.writeByte('\n');
    			 os.flush();
			 }           
			 os.writeBytes("exit\n");  
			 os.flush();
			 p.waitFor();
			 int exitValue = p.exitValue();
			 return (exitValue == 0);
		 } catch (IOException ex) {
			 ex.printStackTrace();
			 return false;
		 } finally {
			 if (os != null)
				try {os.close();}catch(IOException ex){} 
		 }
	 
	}

	public static void showTooltip(Context context, int msgId, int duration) {
		Toast.makeText(context, msgId, duration).show();
	}

	public static void showTooltip(Context context, String msg, int duration) {
		Toast.makeText(context, msg, duration).show();
	}


	public static boolean validateExecPath(String path) {
		if (isEmpty(path)) return false;
		if (path.charAt(0) == File.separatorChar) 
			return isExecutable(new File(path));
			
		String pathString = System.getenv("PATH");
		if (pathString == null) return false;
		String[] allDirPaths = pathString.split("[:;]"); 
		
		for (String dirPath : allDirPaths) {
			if(isExecutable(new File(dirPath, path))) return true;
		}
		return false;
	}

	private static boolean isExecutable(File file) {
		if (!file.exists()) return false;

		try {
			return ((Boolean)File.class.
				getMethod("canExecute").invoke(file)).booleanValue();
		} catch (Exception ex) {
			return true;
		}
		
	}

}
