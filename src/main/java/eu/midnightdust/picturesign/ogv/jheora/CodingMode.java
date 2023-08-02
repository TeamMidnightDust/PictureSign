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

public class CodingMode {
   private int value;

   public static final CodingMode CODE_INTER_NO_MV =
      new CodingMode(0x0); /* INTER prediction, (0,0) motion vector implied.  */

   public static final CodingMode CODE_INTRA =
      new CodingMode(0x1); /* INTRA i.e. no prediction. */

   public static final CodingMode CODE_INTER_PLUS_MV =
      new CodingMode(0x2); /* INTER prediction, non zero motion vector. */

   public static final CodingMode CODE_INTER_LAST_MV =
      new CodingMode(0x3); /* Use Last Motion vector */

   public static final CodingMode CODE_INTER_PRIOR_LAST =
      new CodingMode(0x4); /* Prior last motion vector */

   public static final CodingMode CODE_USING_GOLDEN     =
      new CodingMode(0x5); /* 'Golden frame' prediction (no MV). */

   public static final CodingMode CODE_GOLDEN_MV        =
      new CodingMode(0x6); /* 'Golden frame' prediction plus MV. */

   public static final CodingMode CODE_INTER_FOURMV     =
      new CodingMode(0x7);  /* Inter prediction 4MV per macro block. */

   public static final CodingMode[] MODES = {
      CODE_INTER_NO_MV,
      CODE_INTRA,
      CODE_INTER_PLUS_MV,
      CODE_INTER_LAST_MV,
      CODE_INTER_PRIOR_LAST,
      CODE_USING_GOLDEN,
      CODE_GOLDEN_MV,
      CODE_INTER_FOURMV
   };

   private CodingMode(int i) {
      value=i;
   }

   public int getValue() {
      return value;
   }
}
