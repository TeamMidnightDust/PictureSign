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

public class Constants 
{
  public static final int CURRENT_ENCODE_VERSION =  1;

/* Baseline dct height and width. */
  public static final int BLOCK_HEIGHT_WIDTH = 8;
  public static final int HFRAGPIXELS        = 8;
  public static final int VFRAGPIXELS        = 8;

/* Baseline dct block size */
  public static final int BLOCK_SIZE         = (BLOCK_HEIGHT_WIDTH * BLOCK_HEIGHT_WIDTH);

/* Border is for unrestricted mv's */
  public static final int UMV_BORDER         = 16;
  public static final int STRIDE_EXTRA       = (UMV_BORDER * 2);
  public static final int Q_TABLE_SIZE       = 64;

  public static final int BASE_FRAME         = 0;
  public static final int NORMAL_FRAME       = 1;

  public static final int MAX_MODES          = 8;
  public static final int MODE_BITS          = 3;
  public static final int MODE_METHODS       = 8;
  public static final int MODE_METHOD_BITS   = 3;

  public static final int[] dequant_index = {
    0,  1,  8,  16,  9,  2,  3, 10,
    17, 24, 32, 25, 18, 11,  4,  5,
    12, 19, 26, 33, 40, 48, 41, 34,
    27, 20, 13,  6,  7, 14, 21, 28,
    35, 42, 49, 56, 57, 50, 43, 36,
    29, 22, 15, 23, 30, 37, 44, 51,
    58, 59, 52, 45, 38, 31, 39, 46,
    53, 60, 61, 54, 47, 55, 62, 63
  };
}
