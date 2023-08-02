/* Cortado - a video player java applet
 * Copyright (C) 2004 Fluendo S.L.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Street #330, Boston, MA 02111-1307, USA.
 */

package eu.midnightdust.picturesign.ogv.jheora;

public class Debug {
  public static final int NONE = 0;
  public static final int ERROR = 1;
  public static final int WARNING = 2;
  public static final int INFO = 3;
  public static final int DEBUG = 4;

  public static int level = INFO;

  /* static id counter */
  private static int counter = 0;
  private static long startTime = 0;

  public static final int genId() {
    synchronized (Debug.class) {
      return counter++;
    }
  }

  public static final String[] prefix = {
       "NONE",
       "ERRO",
       "WARN",
       "INFO",
       "DBUG"};

  public static String rpad(String s, int length) {
    if ( length > s.length() ) {
      int sz = length - s.length();
      char arr[] = new char[sz];
      for (int n=0; n<sz; ++n)
        arr[n] = ' ';
      return s + new String( arr );
    } else {
      return s;
    }
  }

  public static void log(int lev, String line) 
  {
    long t = System.currentTimeMillis();
    if ( startTime == 0 ) {
      startTime = t;
    }
    t -= startTime;
    
    if (lev <= level) {
      if (level >= DEBUG) {
        System.out.println( "[" + Debug.rpad( Thread.currentThread().getName(), 30 ) + " " 
	  + Debug.rpad( Long.toString( t ), 6 ) + " " + prefix[lev] + "] " + line );
      } else {
	System.out.println( "[" + prefix[lev] + "] " + line );
      }
    }
  }

  public static void error(String line) { Debug.log( ERROR, line ); }
  public static void warning(String line) { Debug.log( WARNING, line ); }
  public static void warn(String line) { Debug.log( WARNING, line ); }
  public static void info(String line) { Debug.log( INFO, line ); }
  public static void debug(String line) { Debug.log( DEBUG, line ); }
}
