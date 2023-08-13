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

public final class Recon 
{
  private static final short clamp255(int val) {
    return (short)((~(val>>31)) & 255 & (val | ((255-val)>>31)));
  }

  public static final void CopyBlock(short[] src,
                 short[] dest, int idx,
	         int srcstride)
  {
    int i, off=idx;

    for (i=0; i<8; i++){
      dest[off+0] = src[off+0];
      dest[off+1] = src[off+1];
      dest[off+2] = src[off+2];
      dest[off+3] = src[off+3];
      dest[off+4] = src[off+4];
      dest[off+5] = src[off+5];
      dest[off+6] = src[off+6];
      dest[off+7] = src[off+7];
      off+=srcstride;
    }
  }

  public static final void ReconIntra(short[] ReconPtr, int idx,
                 short[] ChangePtr, int LineStep) 
  {
    int i, roff=idx, coff=0;

    for (i=0; i<8; i++ ){
      /* Convert the data back to 8 bit unsigned */
      /* Saturate the output to unsigned 8 bit values */
      ReconPtr[roff+0] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+1] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+2] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+3] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+4] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+5] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+6] = clamp255(ChangePtr[coff++] + 128);
      ReconPtr[roff+7] = clamp255(ChangePtr[coff++] + 128);
      roff += LineStep;
    }
  }

  public static final void ReconInter(short[] ReconPtr, int idx1,
                short[] RefPtr, int idx2, short[] ChangePtr,
                int LineStep ) {
    int coff=0, roff1=idx1, roff2=idx2, i;

    for (i = 0; i < 8; i++) {
      ReconPtr[roff1+0] = clamp255(RefPtr[roff2+0] + ChangePtr[coff++]);
      ReconPtr[roff1+1] = clamp255(RefPtr[roff2+1] + ChangePtr[coff++]);
      ReconPtr[roff1+2] = clamp255(RefPtr[roff2+2] + ChangePtr[coff++]);
      ReconPtr[roff1+3] = clamp255(RefPtr[roff2+3] + ChangePtr[coff++]);
      ReconPtr[roff1+4] = clamp255(RefPtr[roff2+4] + ChangePtr[coff++]);
      ReconPtr[roff1+5] = clamp255(RefPtr[roff2+5] + ChangePtr[coff++]);
      ReconPtr[roff1+6] = clamp255(RefPtr[roff2+6] + ChangePtr[coff++]);
      ReconPtr[roff1+7] = clamp255(RefPtr[roff2+7] + ChangePtr[coff++]);
      roff1 += LineStep;
      roff2 += LineStep;
    }
  }

  public static final void ReconInterHalfPixel2(short[] ReconPtr, int idx1,
                           short[] RefPtr1, int idx2, short[] RefPtr2, int idx3,
                           short[] ChangePtr, int LineStep ) {
    int coff=0, roff1=idx1, roff2=idx2, roff3=idx3, i;

    for (i = 0; i < 8; i++ ){
      ReconPtr[roff1+0] = clamp255(((RefPtr1[roff2+0] + RefPtr2[roff3+0]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+1] = clamp255(((RefPtr1[roff2+1] + RefPtr2[roff3+1]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+2] = clamp255(((RefPtr1[roff2+2] + RefPtr2[roff3+2]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+3] = clamp255(((RefPtr1[roff2+3] + RefPtr2[roff3+3]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+4] = clamp255(((RefPtr1[roff2+4] + RefPtr2[roff3+4]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+5] = clamp255(((RefPtr1[roff2+5] + RefPtr2[roff3+5]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+6] = clamp255(((RefPtr1[roff2+6] + RefPtr2[roff3+6]) >> 1) + ChangePtr[coff++]);
      ReconPtr[roff1+7] = clamp255(((RefPtr1[roff2+7] + RefPtr2[roff3+7]) >> 1) + ChangePtr[coff++]);
      roff1 += LineStep;
      roff2 += LineStep;
      roff3 += LineStep;
    }
  }
}
