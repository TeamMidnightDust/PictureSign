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

import eu.midnightdust.picturesign.ogv.jogg.*;

public class HuffEntry 
{
  HuffEntry[] Child = new HuffEntry[2];
  HuffEntry previous;
  HuffEntry next;
  int       value;
  int       frequency;

  public HuffEntry copy() 
  {
    HuffEntry huffDst;
    huffDst = new HuffEntry();
    huffDst.value = value;
    if (value < 0) {
      huffDst.Child[0] = Child[0].copy();
      huffDst.Child[1] = Child[1].copy();
    }
    return huffDst;
  }

  public int read(int depth, OggBuffer opb) 
  {
    int bit;
    int ret;

    bit = opb.readB(1);
    if(bit < 0) {
      return Result.BADHEADER;
    }
    else if(bit == 0) {
      if (++depth > 32) 
        return Result.BADHEADER;

      Child[0] = new HuffEntry();
      ret = Child[0].read(depth, opb);
      if (ret < 0) 
        return ret;

      Child[1] = new HuffEntry();
      ret = Child[1].read(depth, opb);
      if (ret < 0) 
        return ret;

      value = -1;
    } 
    else {
      Child[0] = null;
      Child[1] = null;
      value = opb.readB(5);
      if (value < 0) 
        return Result.BADHEADER;
    }
    return 0;
  }
}
