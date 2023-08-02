/* Jheora
 * Copyright (C) 2004 Fluendo S.L.
 *  
 * Written by: 2004 Wim Taymans <wim@fluendo.com>
 *   
 * Many thanks to 
 *   The Xiph.Org Foundation http://www.xiph.org/
 * Jheora was based on their Theora reference decoder.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package eu.midnightdust.picturesign.ogv.jheora;

public class Version 
{
  public static final int VERSION_MAJOR = 3;
  public static final int VERSION_MINOR = 2;
  public static final int VERSION_SUB = 0;

  private static final String VENDOR_STRING = "Xiph.Org libTheora I 20040317 3 2 0";

  public static String getVersionString()
  {
    return VENDOR_STRING;
  }

  public static int getVersionNumber()
  {
    return (VERSION_MAJOR<<16) + (VERSION_MINOR<<8) + (VERSION_SUB);
  }
}
