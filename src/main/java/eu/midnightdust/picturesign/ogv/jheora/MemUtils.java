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

public class MemUtils {
  private static final char[] bytes = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

  public static final int cmp (byte[] mem1, byte[] mem2, int len)
  {
    for (int i=0; i<len; i++) {
      if (mem1[i] != mem2[i]) {
        if (mem1[i] < mem2[i]) 
	  return -i;
	else
	  return i;
      }
    }
    return 0;
  }
  public static final void set (byte[] mem, int offset, int val, int len)
  {
    len += offset;

    for (int i=offset; i<len; i++) {
      mem[i] = (byte)val;
    }
  }
  public static final void set (short[] mem, int offset, int val, int len)
  {
    len += offset;

    for (int i=offset; i<len; i++) {
      mem[i] = (short)val;
    }
  }
  public static final void set (int[] mem, int offset, int val, int len)
  {
    len += offset;

    for (int i=offset; i<len; i++) {
      mem[i] = (int)val;
    }
  }
  public static final void set (Object[] mem, int offset, Object val, int len)
  {
    len += offset;

    for (int i=offset; i<len; i++) {
      mem[i] = val;
    }
  }
  /* check if a given arr starts with the given pattern */
  public static final boolean startsWith (byte[] arr, int offset, int len, byte[] pattern)
  {
    int length = pattern.length;
    int i;

    if (len < length)
      return false;

    for (i=0; i < length; i++)
      if (arr[offset+i] != pattern[i])
        break;

    return i == length;
  }

  public static final void dump (byte[] mem, int start, int len)
  {
    int i, j;
    StringBuffer string = new StringBuffer(50);
    StringBuffer chars = new StringBuffer(18);
    String vis = new String (mem, start, len);

    i = j = 0;
    while (i < len) {
      int b = ((int)mem[i+start]); if (b<0) b+=256;

      if (b > 0x20 && b < 0x7f)
	chars.append (vis.charAt(i));
      else
	chars.append (".");

      string.append (bytes[b/16]);
      string.append (bytes[b%16]);
      string.append (" ");

      j++;
      i++;

      if (j == 16 || i == len) {
	System.out.println ("" + (i-j) + "  "+ string.toString() + chars.toString());

        string.setLength(0);
        chars.setLength(0);
        j = 0;
      }
    }
  }
}
