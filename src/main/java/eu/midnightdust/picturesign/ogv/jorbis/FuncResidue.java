/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/* JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *  
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 *   
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package eu.midnightdust.picturesign.ogv.jorbis;

import eu.midnightdust.picturesign.ogv.jogg.*;

abstract class FuncResidue{
  public static FuncResidue[] residue_P= {new Residue0(), new Residue1(),
      new Residue2()};

  abstract void pack(Object vr, OggBuffer opb);

  abstract Object unpack(VorbisInfo vi, OggBuffer opb);

  abstract Object look(VorbisDspState vd, InfoMode vm, Object vr);

  abstract void free_info(Object i);

  abstract void free_look(Object i);

  abstract int inverse(VorbisBlock vb, Object vl, float[][] in, int[] nonzero, int ch);
}
