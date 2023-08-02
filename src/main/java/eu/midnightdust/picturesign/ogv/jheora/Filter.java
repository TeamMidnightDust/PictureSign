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

public final class Filter 
{
  /* in-loop filter tables. one of these is used in dct_decode.c */
  private static final byte[] LoopFilterLimitValuesV1 = {
    30, 25, 20, 20, 15, 15, 14, 14,
    13, 13, 12, 12, 11, 11, 10, 10,
    9,  9,  8,  8,  7,  7,  7,  7,
    6,  6,  6,  6,  5,  5,  5,  5,
    4,  4,  4,  4,  3,  3,  3,  3,
    2,  2,  2,  2,  2,  2,  2,  2,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0
  };

  /* Loop filter bounding values */
  private byte[] LoopFilterLimits = new byte[Constants.Q_TABLE_SIZE];
  private int[] FiltBoundingValue = new int[512];

  private void SetupBoundingValueArray_Generic(int FLimit)
  {
    /* Set up the bounding value array. */
    MemUtils.set (FiltBoundingValue, 0, 0, 512);
    for (int i = 0; i < FLimit; i++ ){
      FiltBoundingValue[256-i-FLimit] = (-FLimit+i);
      FiltBoundingValue[256-i] = -i;
      FiltBoundingValue[256+i] = i;
      FiltBoundingValue[256+i+FLimit] = FLimit-i;
    }
  }

  /* copy in-loop filter limits from the bitstream header into our instance */
  public void copyFilterTables(TheoraInfo ci) {
    System.arraycopy(ci.LoopFilterLimitValues, 0, LoopFilterLimits, 0, Constants.Q_TABLE_SIZE);
  }

  /* initialize the filter limits from our static table */
  public void InitFilterTables() {
    System.arraycopy(LoopFilterLimitValuesV1, 0, LoopFilterLimits, 0, Constants.Q_TABLE_SIZE);
  }

  public void SetupLoopFilter(int FrameQIndex){
    int FLimit;

    /* nb: this was using the V2 values rather than V1
       we think is was a mistake; the results were not used */
    FLimit = LoopFilterLimits[FrameQIndex];
    SetupBoundingValueArray_Generic(FLimit);
  }

  private static final short clamp255(int val) {
    return (short)((~(val>>31)) & 255 & (val | ((255-val)>>31)));
  }

  private void FilterHoriz(short[] PixelPtr, int idx,
                        int LineLength,
                        int[] BoundingValuePtr)
  {
    int j;
    int FiltVal;

    for ( j = 0; j < 8; j++ ){
      FiltVal =
        ( PixelPtr[0 + idx] ) -
        ( PixelPtr[1 + idx] * 3 ) +
        ( PixelPtr[2 + idx] * 3 ) -
        ( PixelPtr[3 + idx] );

      FiltVal = BoundingValuePtr[256 + ((FiltVal + 4) >> 3)];

      PixelPtr[1 + idx] = clamp255(PixelPtr[1 + idx] + FiltVal);
      PixelPtr[2 + idx] = clamp255(PixelPtr[2 + idx] - FiltVal);
  
      idx += LineLength;
    }
  }

  private void FilterVert(short[] PixelPtr, int idx,
                int LineLength,
                int[] BoundingValuePtr){
    int j;
    int FiltVal;

    /* the math was correct, but negative array indicies are forbidden
       by ANSI/C99 and will break optimization on several modern
       compilers */

    idx -= 2*LineLength;

    for ( j = 0; j < 8; j++ ) {
      FiltVal = 
        ( PixelPtr[idx + 0] ) -
        ( PixelPtr[idx + LineLength] * 3 ) +
        ( PixelPtr[idx + 2 * LineLength] * 3 ) -
        ( PixelPtr[idx + 3 * LineLength] );

      FiltVal = BoundingValuePtr[256 + ((FiltVal + 4) >> 3)];

      PixelPtr[idx + LineLength] = clamp255(PixelPtr[idx + LineLength] + FiltVal);
      PixelPtr[idx + 2 * LineLength] = clamp255(PixelPtr[idx + 2*LineLength] - FiltVal);

      idx++;
    }
  }

  public void LoopFilter(Playback pbi){
    int FragsAcross=pbi.HFragments;
    int FromFragment;
    int FragsDown = pbi.VFragments;
    int LineFragments;
    int LineLength;
    int FLimit;
    int QIndex;
    int i,j,m,n;
    int index;

    /* Set the limit value for the loop filter based upon the current
       quantizer. */
    
    QIndex = pbi.frameQIS[0];

    FLimit = LoopFilterLimits[QIndex];
    if ( FLimit == 0 ) return;
    SetupBoundingValueArray_Generic(FLimit);

    for ( j = 0; j < 3 ; j++){
      switch(j) {
      case 0: /* y */
        FromFragment = 0;
        FragsAcross = pbi.HFragments;
        FragsDown = pbi.VFragments;
        LineLength = pbi.YStride;
        LineFragments = pbi.HFragments;
        break;
      case 1: /* u */
        FromFragment = pbi.YPlaneFragments;
        FragsAcross = pbi.HFragments >> 1;
        FragsDown = pbi.VFragments >> 1;
        LineLength = pbi.UVStride;
        LineFragments = pbi.HFragments / 2;
        break;
      /*case 2:  v */
      default:
        FromFragment = pbi.YPlaneFragments + pbi.UVPlaneFragments;
        FragsAcross = pbi.HFragments >> 1;
        FragsDown = pbi.VFragments >> 1;
        LineLength = pbi.UVStride;
        LineFragments = pbi.HFragments / 2;
        break;
      }

      i=FromFragment;

      /**************************************************************
       First Row
      **************************************************************/
      /* first column conditions */
      /* only do 2 prediction if fragment coded and on non intra or if
         all fragments are intra */
      if( pbi.display_fragments[i] != 0){
        /* Filter right hand border only if the block to the right is
           not coded */
        if ( pbi.display_fragments[ i + 1 ] == 0){
          FilterHoriz(pbi.LastFrameRecon, 
	              pbi.recon_pixel_index_table[i]+6,
                      LineLength,FiltBoundingValue);
        }

        /* Bottom done if next row set */
        if( pbi.display_fragments[ i + LineFragments] == 0){
          FilterVert(pbi.LastFrameRecon,
	              pbi.recon_pixel_index_table[i+LineFragments],
                     LineLength, FiltBoundingValue);
        }
      }
      i++;

      /***************************************************************/
      /* middle columns  */
      for ( n = 1 ; n < FragsAcross - 1 ; n++) {
        if( pbi.display_fragments[i] != 0){
          index = pbi.recon_pixel_index_table[i];

          /* Filter Left edge always */
          FilterHoriz(pbi.LastFrameRecon, index-2,
                      LineLength, FiltBoundingValue);
  
          /* Filter right hand border only if the block to the right is
             not coded */
          if (pbi.display_fragments[ i + 1 ] == 0){
            FilterHoriz(pbi.LastFrameRecon,
                        index+6,
                        LineLength, FiltBoundingValue);
          }
  
          /* Bottom done if next row set */
          if(pbi.display_fragments[ i + LineFragments] == 0){
            FilterVert(pbi.LastFrameRecon, 
	               pbi.recon_pixel_index_table[i+LineFragments],
                       LineLength, FiltBoundingValue);
          }
  
        }
	i++;
      }
  
      /***************************************************************/
      /* Last Column */
      if( pbi.display_fragments[i] != 0){
        /* Filter Left edge always */
        FilterHoriz(pbi.LastFrameRecon,
                    pbi.recon_pixel_index_table[i] - 2 ,
                    LineLength, FiltBoundingValue);
  
        /* Bottom done if next row set */
        if(pbi.display_fragments[ i + LineFragments] == 0){
          FilterVert(pbi.LastFrameRecon, 
                     pbi.recon_pixel_index_table[i+LineFragments],
                     LineLength, FiltBoundingValue);
        }
      }
      i++;
  
      /***************************************************************/
      /* Middle Rows */
      /***************************************************************/
      for ( m = 1 ; m < FragsDown-1 ; m++) {

        /*****************************************************************/
        /* first column conditions */
        /* only do 2 prediction if fragment coded and on non intra or if
           all fragments are intra */
        if(pbi.display_fragments[i] != 0){
          index = pbi.recon_pixel_index_table[i];

          /* TopRow is always done */
          FilterVert(pbi.LastFrameRecon, index,
                     LineLength, FiltBoundingValue);
  
          /* Filter right hand border only if the block to the right is
             not coded */
          if (pbi.display_fragments[ i + 1 ] == 0){
            FilterHoriz(pbi.LastFrameRecon, index + 6,
                        LineLength, FiltBoundingValue);
          }
  
          /* Bottom done if next row set */
          if(pbi.display_fragments[ i + LineFragments] == 0){
            FilterVert(pbi.LastFrameRecon, 
                       pbi.recon_pixel_index_table[i+LineFragments],
                       LineLength, FiltBoundingValue);
          }
        }
        i++;
  
        /*****************************************************************/
        /* middle columns  */
        for ( n = 1 ; n < FragsAcross - 1 ; n++, i++){

          if( pbi.display_fragments[i] != 0){
            index = pbi.recon_pixel_index_table[i];
            /* Filter Left edge always */
            FilterHoriz(pbi.LastFrameRecon, index - 2,
                        LineLength, FiltBoundingValue);
  
            /* TopRow is always done */
            FilterVert(pbi.LastFrameRecon, index,
                       LineLength, FiltBoundingValue);
  
            /* Filter right hand border only if the block to the right
               is not coded */
            if (pbi.display_fragments[ i + 1 ] == 0){
              FilterHoriz(pbi.LastFrameRecon, index + 6,
                          LineLength, FiltBoundingValue);
            }
  
            /* Bottom done if next row set */
            if(pbi.display_fragments[ i + LineFragments] == 0){
              FilterVert(pbi.LastFrameRecon, 
                         pbi.recon_pixel_index_table[i+LineFragments],
                         LineLength, FiltBoundingValue);
            }
          }
        }
  
        /******************************************************************/
        /* Last Column */
        if( pbi.display_fragments[i] != 0){
          index = pbi.recon_pixel_index_table[i];

          /* Filter Left edge always*/
          FilterHoriz(pbi.LastFrameRecon, index - 2,
                      LineLength, FiltBoundingValue);
  
          /* TopRow is always done */
          FilterVert(pbi.LastFrameRecon, index,
                     LineLength, FiltBoundingValue);
  
          /* Bottom done if next row set */
          if(pbi.display_fragments[ i + LineFragments] == 0){
            FilterVert(pbi.LastFrameRecon, 
                       pbi.recon_pixel_index_table[i+LineFragments],
                       LineLength, FiltBoundingValue);
          }
        }
        i++;
      }
  
      /*******************************************************************/
      /* Last Row  */
  
      /* first column conditions */
      /* only do 2 prediction if fragment coded and on non intra or if
         all fragments are intra */
      if(pbi.display_fragments[i] != 0){
        index = pbi.recon_pixel_index_table[i];
  
        /* TopRow is always done */
        FilterVert(pbi.LastFrameRecon, index,
                   LineLength, FiltBoundingValue);
  
        /* Filter right hand border only if the block to the right is
           not coded */
        if (pbi.display_fragments[ i + 1 ] == 0){
          FilterHoriz(pbi.LastFrameRecon, index+6,
                      LineLength, FiltBoundingValue);
        }
      }
      i++;
  
      /******************************************************************/
      /* middle columns  */
      for ( n = 1 ; n < FragsAcross - 1 ; n++, i++){
        if( pbi.display_fragments[i] != 0){
          index = pbi.recon_pixel_index_table[i];

          /* Filter Left edge always */
          FilterHoriz(pbi.LastFrameRecon, index-2,
                      LineLength, FiltBoundingValue);
  
          /* TopRow is always done */
          FilterVert(pbi.LastFrameRecon, index,
                     LineLength, FiltBoundingValue);
  
          /* Filter right hand border only if the block to the right is
             not coded */
          if (pbi.display_fragments[ i + 1 ] == 0){
            FilterHoriz(pbi.LastFrameRecon, index+6,
                        LineLength, FiltBoundingValue);
          }
        }
      }
  
      /******************************************************************/
      /* Last Column */
      if(pbi.display_fragments[i] != 0){
        index = pbi.recon_pixel_index_table[i];

        /* Filter Left edge always */
        FilterHoriz(pbi.LastFrameRecon, index - 2,
                    LineLength, FiltBoundingValue);
  
        /* TopRow is always done */
        FilterVert(pbi.LastFrameRecon, index,
                   LineLength, FiltBoundingValue);
      }
    }
  }
}
