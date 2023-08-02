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

public class DCTDecode 
{
  private static final int PUR = 8;
  private static final int PU = 4;
  private static final int PUL = 2;
  private static final int PL = 1;

  private short[] dequant_matrix = new short[64];

  private static final int[] ModeUsesMC = { 0, 0, 1, 1, 1, 0, 1, 1 };

  /* predictor multiplier up-left, up, up-right,left, shift
     Entries are packed in the order L, UL, U, UR, with missing entries
     moved to the end (before the shift parameters). */
  private static final short[][] pc = {
    {0,0,0,0,0,0},
    {1,0,0,0,0,0},      /* PL */
    {1,0,0,0,0,0},      /* PUL */
    {1,0,0,0,0,0},      /* PUL|PL */
    {1,0,0,0,0,0},      /* PU */
    {1,1,0,0,1,1},      /* PU|PL */
    {0,1,0,0,0,0},      /* PU|PUL */
    {29,-26,29,0,5,31}, /* PU|PUL|PL */
    {1,0,0,0,0,0},      /* PUR */
    {75,53,0,0,7,127},  /* PUR|PL */
    {1,1,0,0,1,1},      /* PUR|PUL */
    {75,0,53,0,7,127},  /* PUR|PUL|PL */
    {1,0,0,0,0,0},      /* PUR|PU */
    {75,0,53,0,7,127},  /* PUR|PU|PL */
    {3,10,3,0,4,15},    /* PUR|PU|PUL */
    {29,-26,29,0,5,31}  /* PUR|PU|PUL|PL */
  };

  /* boundary case bit masks. */
  private static final int[] bc_mask = {
   /* normal case no boundary condition */
    PUR|PU|PUL|PL,
    /* left column */
    PUR|PU,
    /* top row */
    PL,
    /* top row, left column */
    0,
    /* right column */
    PU|PUL|PL,
    /* right and left column */
    PU,
    /* top row, right column */
    PL,
    /* top row, right and left column */
    0
  };

  private static final short[] Mode2Frame = {
    1,  /* CODE_INTER_NO_MV     0 => Encoded diff from same MB last frame  */
    0,  /* CODE_INTRA           1 => DCT Encoded Block */
    1,  /* CODE_INTER_PLUS_MV   2 => Encoded diff from included MV MB last frame */
    1,  /* CODE_INTER_LAST_MV   3 => Encoded diff from MRU MV MB last frame */
    1,  /* CODE_INTER_PRIOR_MV  4 => Encoded diff from included 4 separate MV blocks */
    2,  /* CODE_USING_GOLDEN    5 => Encoded diff from same MB golden frame */
    2,  /* CODE_GOLDEN_MV       6 => Encoded diff from included MV MB golden frame */
    1   /* CODE_INTER_FOUR_MV   7 => Encoded diff from included 4 separate MV blocks */
  };

  private short[] ReconDataBuffer = new short[64];
  /* value left value up-left, value up, value up-right, missing
      values skipped. */
  private int[] v = new int[4];
  /* fragment number left, up-left, up, up-right */
  private int[] fn = new int[4];
  private short[] Last = new short[3];
  private iDCT idct = new iDCT();

  private void ExpandKFBlock ( Playback pbi, int FragmentNumber ){
    int ReconPixelsPerLine;
    int     ReconPixelIndex;
    short[] dequant_coeffs;

    /* determine which quantizer was specified for this block */
    int qi = pbi.FragQs[FragmentNumber];
    
    /* Select the appropriate inverse Q matrix and line stride */
    if ( FragmentNumber<(int)pbi.YPlaneFragments ){
      ReconPixelsPerLine = pbi.YStride;
      // intra Y
      dequant_coeffs = pbi.info.dequant_tables[0][0][pbi.frameQIS[qi]];
      dequant_matrix[0] = pbi.info.dequant_tables[0][0][pbi.frameQIS[0]][0];
    }else if(FragmentNumber < pbi.YPlaneFragments + pbi.UVPlaneFragments) {
      ReconPixelsPerLine = pbi.UVStride;
      // intra U
      dequant_coeffs = pbi.info.dequant_tables[0][1][pbi.frameQIS[qi]];
      dequant_matrix[0] = pbi.info.dequant_tables[0][1][pbi.frameQIS[0]][0];
    } else {
      ReconPixelsPerLine = pbi.UVStride;
      // intra V
      dequant_coeffs = pbi.info.dequant_tables[0][2][pbi.frameQIS[qi]];
      dequant_matrix[0] = pbi.info.dequant_tables[0][2][pbi.frameQIS[0]][0];
    }

    // create copy with DC coefficient of primary frame qi
    System.arraycopy(dequant_coeffs, 1, dequant_matrix, 1, 63);

    /* Set up pointer into the quantisation buffer. */
    short[] quantized_list = pbi.QFragData[FragmentNumber];
  
    /* Invert quantisation and DCT to get pixel data. */
    switch(pbi.FragCoefEOB[FragmentNumber]){
    case 0:case 1:
      idct.IDct1(quantized_list, dequant_matrix, ReconDataBuffer );
      break;
    case 2: case 3:case 4:case 5:case 6:case 7:case 8: case 9:case 10:
      idct.IDct10(quantized_list, dequant_matrix, ReconDataBuffer );
      break;
    default:
      idct.IDctSlow(quantized_list, dequant_matrix, ReconDataBuffer );
    }
    /*
    for (int i=0; i<64; i++) {
      System.out.print(ReconDataBuffer[i]+" ");
    }
    System.out.println();
    */

    /* Convert fragment number to a pixel offset in a reconstruction buffer. */
    ReconPixelIndex = pbi.recon_pixel_index_table[FragmentNumber];
  
    /* Get the pixel index for the first pixel in the fragment. */
    Recon.ReconIntra (pbi.ThisFrameRecon, ReconPixelIndex, ReconDataBuffer, ReconPixelsPerLine);
  }
  
  private void ExpandBlock ( Playback pbi, int FragmentNumber ){
    short[] LastFrameRecPtr;   /* Pointer into previous frame
                                       reconstruction. */
    int   ReconPixelsPerLine; /* Pixels per line */
    int    ReconPixelIndex;    /* Offset for block into a
                                        reconstruction buffer */
    int    ReconPtr2Offset;    /* Offset for second
                                        reconstruction in half pixel
                                        MC */
    int    MVOffset;           /* Baseline motion vector offset */
    int    MvShiftX  ;          /* Shift to correct to 1/2 or 1/4 pixel */
    int    MvShiftY  ;          /* Shift to correct to 1/2 or 1/4 pixel */
    int    MvModMaskX;          /* Mask to determine whether 1/2
                                        pixel is used */
    int    MvModMaskY;
    short[] dequant_coeffs;
    CodingMode codingMode;
    
    /* determine which quantizer was specified for this block */
    int qi = pbi.FragQs[FragmentNumber];

    /* Get coding mode for this block */
    if (pbi.getFrameType() == Constants.BASE_FRAME ){
      codingMode = CodingMode.CODE_INTRA;
    }else{
      /* Get Motion vector and mode for this block. */
      codingMode = pbi.FragCodingMethod[FragmentNumber];
    }

    /* Select the appropriate inverse Q matrix and line stride */
    if ( FragmentNumber<(int)pbi.YPlaneFragments ) {
      ReconPixelsPerLine = pbi.YStride;
      MvShiftX = MvShiftY = 1;
      MvModMaskX = MvModMaskY = 0x00000001;

      /* Select appropriate dequantiser matrix. */
      if ( codingMode == CodingMode.CODE_INTRA ) {
        // intra Y
        dequant_coeffs = pbi.info.dequant_tables[0][0][pbi.frameQIS[qi]];
        dequant_matrix[0] = pbi.info.dequant_tables[0][0][pbi.frameQIS[0]][0];
      } else {
        // inter Y
        dequant_coeffs = pbi.info.dequant_tables[1][0][pbi.frameQIS[qi]];
        dequant_matrix[0] = pbi.info.dequant_tables[1][0][pbi.frameQIS[0]][0];
      }
    }else{
      ReconPixelsPerLine = pbi.UVStride;
      MvShiftX = pbi.UVShiftX + 1;
      MvShiftY = pbi.UVShiftY + 1;
      MvModMaskX = MvModMaskY = 0x00000003;
      if (MvShiftX == 1) MvModMaskX = 0x00000001;
      if (MvShiftY == 1) MvModMaskY = 0x00000001;

      /* Select appropriate dequantiser matrix. */


      if(FragmentNumber < pbi.YPlaneFragments + pbi.UVPlaneFragments) {
        if ( codingMode == CodingMode.CODE_INTRA ) {
          // intra U
          dequant_coeffs = pbi.info.dequant_tables[0][1][pbi.frameQIS[qi]];
          dequant_matrix[0] = pbi.info.dequant_tables[0][1][pbi.frameQIS[0]][0];
        } else {
          // inter U
          dequant_coeffs = pbi.info.dequant_tables[1][1][pbi.frameQIS[qi]];
          dequant_matrix[0] = pbi.info.dequant_tables[1][1][pbi.frameQIS[0]][0];
        }
      } else {
        if ( codingMode == CodingMode.CODE_INTRA ) {
          // intra V
          dequant_coeffs = pbi.info.dequant_tables[0][2][pbi.frameQIS[qi]];
          dequant_matrix[0] = pbi.info.dequant_tables[0][2][pbi.frameQIS[0]][0];
        } else {
          // inter V
          dequant_coeffs = pbi.info.dequant_tables[1][2][pbi.frameQIS[qi]];
          dequant_matrix[0] = pbi.info.dequant_tables[1][2][pbi.frameQIS[0]][0];
        }
      }
    }
    // create copy with DC coefficient of primary frame qi
    System.arraycopy(dequant_coeffs, 1, dequant_matrix, 1, 63);
    
    /* Set up pointer into the quantisation buffer. */
    short[] quantized_list = pbi.QFragData[FragmentNumber];

    /* Invert quantisation and DCT to get pixel data. */
    switch(pbi.FragCoefEOB[FragmentNumber]){
    case 0:case 1:
      idct.IDct1(quantized_list, dequant_matrix, ReconDataBuffer );
      break;
    case 2: case 3:case 4:case 5:case 6:case 7:case 8: case 9:case 10:
      idct.IDct10(quantized_list, dequant_matrix, ReconDataBuffer );
      break;
    default:
      idct.IDctSlow(quantized_list, dequant_matrix, ReconDataBuffer );
    }
    /*
    for (int i=0; i<64; i++) {
      System.out.print(ReconDataBuffer[i]+" ");
    }
    System.out.println();
    */

  
    /* Convert fragment number to a pixel offset in a reconstruction buffer. */
    ReconPixelIndex = pbi.recon_pixel_index_table[FragmentNumber];

    /* Action depends on decode mode. */
    if ( codingMode == CodingMode.CODE_INTER_NO_MV ){
      /* Inter with no motion vector */
      /* Reconstruct the pixel data using the last frame reconstruction
         and change data when the motion vector is (0,0), the recon is
         based on the lastframe without loop filtering---- for testing */
      Recon.ReconInter(pbi.ThisFrameRecon, ReconPixelIndex,
                pbi.LastFrameRecon, ReconPixelIndex,
                ReconDataBuffer, ReconPixelsPerLine );

    }else if (ModeUsesMC[codingMode.getValue()] != 0) {
      /* The mode uses a motion vector. */
      /* Get vector from list */
      int dir;
      
      /* Work out the base motion vector offset and the 1/2 pixel offset
         if any.  For the U and V planes the MV specifies 1/4 pixel
         accuracy. This is adjusted to 1/2 pixel as follows ( 0.0,
         1/4->1/2, 1/2->1/2, 3/4->1/2 ). */
      ReconPtr2Offset = 0;
      MVOffset = 0;

      dir = pbi.FragMVect[FragmentNumber].x;
      if (dir > 0) {
        MVOffset = dir >> MvShiftX;
        if ((dir & MvModMaskX) != 0 )
          ReconPtr2Offset = 1;
      } else if (dir < 0) {
        MVOffset = -((-dir) >> MvShiftX);
        if (((-dir) & MvModMaskX) != 0 )
          ReconPtr2Offset = -1;
      }

      dir = pbi.FragMVect[FragmentNumber].y;
      if ( dir > 0 ){
        MVOffset += (dir >>  MvShiftY) * ReconPixelsPerLine;
        if ((dir & MvModMaskY) != 0 )
          ReconPtr2Offset += ReconPixelsPerLine;
      } else if (dir < 0 ){
        MVOffset -= ((-dir) >> MvShiftY) * ReconPixelsPerLine;
        if (((-dir) & MvModMaskY) != 0 )
          ReconPtr2Offset -= ReconPixelsPerLine;
      }

      int LastFrameRecOffset = ReconPixelIndex + MVOffset;

      /* Set up the first of the two reconstruction buffer pointers. */
      if ( codingMode==CodingMode.CODE_GOLDEN_MV ) {
        LastFrameRecPtr = pbi.GoldenFrame;
      }else{
        LastFrameRecPtr = pbi.LastFrameRecon;
      }

      /*
      System.out.println(pbi.FragMVect[FragmentNumber].x+" "+
                         pbi.FragMVect[FragmentNumber].y+" "+
			 ReconPixelIndex+" "+LastFrameRecOffset+ " "+
			 ReconPtr2Offset);
			 */

      /* Select the appropriate reconstruction function */
      if (ReconPtr2Offset == 0 ) {
        /* Reconstruct the pixel dats from the reference frame and change data
           (no half pixel in this case as the two references were the same. */
        Recon.ReconInter(pbi.ThisFrameRecon, ReconPixelIndex,
                    LastFrameRecPtr, LastFrameRecOffset,
  		  ReconDataBuffer, ReconPixelsPerLine );
      }else{
        /* Fractional pixel reconstruction. */
        /* Note that we only use two pixels per reconstruction even for
           the diagonal. */
        Recon.ReconInterHalfPixel2(pbi.ThisFrameRecon, ReconPixelIndex,
                            LastFrameRecPtr, LastFrameRecOffset, 
			    LastFrameRecPtr, LastFrameRecOffset+ReconPtr2Offset,
                            ReconDataBuffer, ReconPixelsPerLine );
      }
    } else if ( codingMode == CodingMode.CODE_USING_GOLDEN ){
      /* Golden frame with motion vector */
      /* Reconstruct the pixel data using the golden frame
         reconstruction and change data */
      Recon.ReconInter(pbi.ThisFrameRecon, ReconPixelIndex,
                  pbi.GoldenFrame, ReconPixelIndex ,
                  ReconDataBuffer, ReconPixelsPerLine );
    } else {
      /* Simple Intra coding */
      /* Get the pixel index for the first pixel in the fragment. */
      Recon.ReconIntra(pbi.ThisFrameRecon, ReconPixelIndex,
                  ReconDataBuffer, ReconPixelsPerLine );
    }
  }

  private void UpdateUMV_HBorders( Playback pbi,
                                short[] DestReconPtr,
                                int  PlaneFragOffset ) {
    int  i;
    int  PixelIndex;

    int  PlaneStride;
    int  BlockVStep;
    int  PlaneFragments;
    int  LineFragments;
    int  PlaneBorderWidth;
    int  PlaneBorderHeight;

    short[] SrcPtr1;
    int    SrcOff1;
    short[] SrcPtr2;
    int    SrcOff2;
    short[] DestPtr1;
    int    DestOff1;
    short[] DestPtr2;
    int    DestOff2;

    /* Work out various plane specific values */
    if ( PlaneFragOffset == 0 ) {
      /* Y Plane */
      BlockVStep = (pbi.YStride *
                    (Constants.VFRAGPIXELS - 1));
      PlaneStride = pbi.YStride;
      PlaneBorderWidth = Constants.UMV_BORDER;
      PlaneBorderHeight = Constants.UMV_BORDER;
      PlaneFragments = pbi.YPlaneFragments;
      LineFragments = pbi.HFragments;
    }else{
      /* U or V plane. */
      BlockVStep = (pbi.UVStride *
                    (Constants.VFRAGPIXELS - 1));
      PlaneStride = pbi.UVStride;
      PlaneBorderWidth = Constants.UMV_BORDER >> pbi.UVShiftX;
      PlaneBorderHeight = Constants.UMV_BORDER >> pbi.UVShiftY;
      PlaneFragments = pbi.UVPlaneFragments;
      LineFragments = pbi.HFragments >> pbi.UVShiftX;
    }

    /* Setup the source and destination pointers for the top and bottom
       borders */
    PixelIndex = pbi.recon_pixel_index_table[PlaneFragOffset];
    SrcPtr1 = DestReconPtr;
    SrcOff1 = PixelIndex - PlaneBorderWidth;
    DestPtr1 = SrcPtr1;
    DestOff1 = SrcOff1 - (PlaneBorderHeight * PlaneStride);

    PixelIndex = pbi.recon_pixel_index_table[PlaneFragOffset +
                                           PlaneFragments - LineFragments] + 
					   BlockVStep;
    SrcPtr2 = DestReconPtr;
    SrcOff2 = PixelIndex - PlaneBorderWidth;
    DestPtr2 = SrcPtr2;
    DestOff2 = SrcOff2 + PlaneStride;

    /* Now copy the top and bottom source lines into each line of the
       respective borders */
    for ( i = 0; i < PlaneBorderHeight; i++ ) {
      System.arraycopy(SrcPtr1, SrcOff1, DestPtr1, DestOff1, PlaneStride);
      System.arraycopy(SrcPtr2, SrcOff2, DestPtr2, DestOff2, PlaneStride);
      DestOff1 += PlaneStride;
      DestOff2 += PlaneStride;
    }
  }

  private void UpdateUMV_VBorders( Playback pbi,
                                short[] DestReconPtr,
                                int  PlaneFragOffset ){
    int  i;
    int  PixelIndex;

    int  PlaneStride;
    int  LineFragments;
    int  PlaneBorderWidth;
    int  PlaneHeight;

    short[] SrcPtr1;
    int     SrcOff1;
    short[] SrcPtr2;
    int     SrcOff2;
    short[] DestPtr1;
    int     DestOff1;
    short[] DestPtr2;
    int     DestOff2;

    /* Work out various plane specific values */
    if ( PlaneFragOffset == 0 ) {
      /* Y Plane */
      PlaneStride = pbi.YStride;
      PlaneBorderWidth = Constants.UMV_BORDER;
      LineFragments = pbi.HFragments;
      PlaneHeight = pbi.info.height;
    }else{
      /* U or V plane. */
      PlaneStride = pbi.UVStride;
      PlaneBorderWidth = Constants.UMV_BORDER >> pbi.UVShiftX;
      LineFragments = pbi.HFragments >> pbi.UVShiftX;
      PlaneHeight = pbi.info.height >> pbi.UVShiftY;
    }

    /* Setup the source data values and destination pointers for the
       left and right edge borders */
    PixelIndex = pbi.recon_pixel_index_table[PlaneFragOffset];
    SrcPtr1 = DestReconPtr;
    SrcOff1 = PixelIndex;
    DestPtr1 = DestReconPtr;
    DestOff1 = PixelIndex - PlaneBorderWidth;

    PixelIndex = pbi.recon_pixel_index_table[PlaneFragOffset +
                                           LineFragments - 1] + 
					   (Constants.HFRAGPIXELS - 1);
    SrcPtr2 = DestReconPtr;
    SrcOff2 = PixelIndex;
    DestPtr2 = DestReconPtr;
    DestOff2 = PixelIndex + 1;

    /* Now copy the top and bottom source lines into each line of the
       respective borders */
    for ( i = 0; i < PlaneHeight; i++ ) {
      MemUtils.set(DestPtr1, DestOff1, SrcPtr1[SrcOff1], PlaneBorderWidth );
      MemUtils.set(DestPtr2, DestOff2, SrcPtr2[SrcOff2], PlaneBorderWidth );
      DestOff1 += PlaneStride;
      DestOff2 += PlaneStride;
      SrcOff1 += PlaneStride;
      SrcOff2 += PlaneStride;
    }
  }

  private void UpdateUMVBorder( Playback pbi,
                      short[] DestReconPtr ) {
    int  PlaneFragOffset;
  
    /* Y plane */
    PlaneFragOffset = 0;
    UpdateUMV_VBorders( pbi, DestReconPtr, PlaneFragOffset );
    UpdateUMV_HBorders( pbi, DestReconPtr, PlaneFragOffset );

    /* Then the U and V Planes */
    PlaneFragOffset = pbi.YPlaneFragments;
    UpdateUMV_VBorders( pbi, DestReconPtr, PlaneFragOffset );
    UpdateUMV_HBorders( pbi, DestReconPtr, PlaneFragOffset );

    PlaneFragOffset = pbi.YPlaneFragments + pbi.UVPlaneFragments;
    UpdateUMV_VBorders( pbi, DestReconPtr, PlaneFragOffset );
    UpdateUMV_HBorders( pbi, DestReconPtr, PlaneFragOffset );
  }

  private void CopyRecon( Playback pbi, short[] DestReconPtr,
                short[] SrcReconPtr ) {
    int  i;
    int  PlaneLineStep; /* Pixels per line */
    int  PixelIndex;

    /* Copy over only updated blocks.*/

    /* First Y plane */
    PlaneLineStep = pbi.YStride;
    for ( i = 0; i < pbi.YPlaneFragments; i++ ) {
      if ( pbi.display_fragments[i] != 0) {
        PixelIndex = pbi.recon_pixel_index_table[i];
        Recon.CopyBlock(SrcReconPtr, DestReconPtr, PixelIndex, PlaneLineStep);
      }
    }

    /* Then U and V */
    PlaneLineStep = pbi.UVStride;
    for ( i = pbi.YPlaneFragments; i < pbi.UnitFragments; i++ ) {
      if ( pbi.display_fragments[i] != 0) {
        PixelIndex = pbi.recon_pixel_index_table[i];
        Recon.CopyBlock(SrcReconPtr, DestReconPtr, PixelIndex, PlaneLineStep);
      }
    }
  }

  private void CopyNotRecon( Playback pbi, short[] DestReconPtr,
                   short[] SrcReconPtr ) {
    int  i;
    int  PlaneLineStep; /* Pixels per line */
    int  PixelIndex;

    /* Copy over only updated blocks. */

    /* First Y plane */
    PlaneLineStep = pbi.YStride;
    for (i = 0; i < pbi.YPlaneFragments; i++) {
      if (pbi.display_fragments[i] == 0) {
        PixelIndex = pbi.recon_pixel_index_table[i];
        Recon.CopyBlock(SrcReconPtr, DestReconPtr, PixelIndex, PlaneLineStep);
      }
    }

    /* Then U and V */
    PlaneLineStep = pbi.UVStride;
    for (i = pbi.YPlaneFragments; i < pbi.UnitFragments; i++) {
      if (pbi.display_fragments[i] == 0) {
        PixelIndex = pbi.recon_pixel_index_table[i];
        Recon.CopyBlock(SrcReconPtr, DestReconPtr, PixelIndex, PlaneLineStep);
      }
    }
  }

  public void ExpandToken( short[] ExpandedBlock,
                  byte[] CoeffIndex, int FragIndex, int Token,
                  int ExtraBits ){
    /* Is the token is a combination run and value token. */
    if ( Token >= Huffman.DCT_RUN_CATEGORY1 ){
      /* Expand the token and additional bits to a zero run length and
         data value.  */
      if ( Token < Huffman.DCT_RUN_CATEGORY2 ) {
        /* Decoding method depends on token */
        if ( Token < Huffman.DCT_RUN_CATEGORY1B ) {
          /* Step on by the zero run length */
          CoeffIndex[FragIndex] += (byte)((Token - Huffman.DCT_RUN_CATEGORY1) + 1);
          /* The extra bit determines the sign. */
          ExpandedBlock[CoeffIndex[FragIndex]] = (short)-(((ExtraBits&0x01)<<1)-1);
        } 
	else if ( Token == Huffman.DCT_RUN_CATEGORY1B ) {
          /* Bits 0-1 determines the zero run length */
          CoeffIndex[FragIndex] += (6 + (ExtraBits & 0x03));
          /* Bit 2 determines the sign */
          ExpandedBlock[CoeffIndex[FragIndex]] = (short)-(((ExtraBits&0x04)>>1)-1);
        }else{
          /* Bits 0-2 determines the zero run length */
          CoeffIndex[FragIndex] += (10 + (ExtraBits & 0x07));
          /* Bit 3 determines the sign */
          ExpandedBlock[CoeffIndex[FragIndex]] = (short)-(((ExtraBits&0x08)>>2)-1);
        }
      }else{
        /* If token == Huffman.DCT_RUN_CATEGORY2 we have a single 0 followed by
           a value */
        if ( Token == Huffman.DCT_RUN_CATEGORY2 ){
          /* Step on by the zero run length */
          CoeffIndex[FragIndex] += 1;
          /* Bit 1 determines sign, bit 0 the value */
          ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((2+(ExtraBits & 0x01)) * -((ExtraBits&0x02)-1));
        }else{
          /* else we have 2->3 zeros followed by a value */
          /* Bit 0 determines the zero run length */
          CoeffIndex[FragIndex] += 2 + (ExtraBits & 0x01);
          /* Bit 2 determines the sign, bit 1 the value */
          ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((2+((ExtraBits&0x02)>>1))*-(((ExtraBits&0x04)>>1)-1));
        }
      }
  
      /* Step on over value */
      CoeffIndex[FragIndex] += 1;

    } else if ( Token == Huffman.DCT_SHORT_ZRL_TOKEN ) {
      /* Token is a ZRL token so step on by the appropriate number of zeros */
      CoeffIndex[FragIndex] += ExtraBits + 1;
    } else if ( Token == Huffman.DCT_ZRL_TOKEN ) {
      /* Token is a ZRL token so step on by the appropriate number of zeros */
      CoeffIndex[FragIndex] += ExtraBits + 1;
    } else if ( Token < Huffman.LOW_VAL_TOKENS ) {
      /* Token is a small single value token. */
      switch ( Token ) {
      case Huffman.ONE_TOKEN:
        ExpandedBlock[CoeffIndex[FragIndex]] = 1;
        break;
      case Huffman.MINUS_ONE_TOKEN:
        ExpandedBlock[CoeffIndex[FragIndex]] = -1;
        break;
      case Huffman.TWO_TOKEN:
        ExpandedBlock[CoeffIndex[FragIndex]] = 2;
        break;
      case Huffman.MINUS_TWO_TOKEN:
        ExpandedBlock[CoeffIndex[FragIndex]] = -2;
        break;
      }
  
      /* Step on the coefficient index. */
      CoeffIndex[FragIndex] += 1;
    }else{
      /* Token is a larger single value token */
      /* Expand the token and additional bits to a data value. */
      if ( Token < Huffman.DCT_VAL_CATEGORY3 ) {
        /* Offset from LOW_VAL_TOKENS determines value */
        Token = Token - Huffman.LOW_VAL_TOKENS;
  
        /* Extra bit determines sign */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Token + Huffman.DCT_VAL_CAT2_MIN) * 
	   	               -(((ExtraBits)<<1)-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY3 ) {
        /* Bit 1 determines sign, Bit 0 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT3_MIN + (ExtraBits & 0x01)) *
	                       -(((ExtraBits&0x02))-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY4 ) {
        /* Bit 2 determines sign, Bit 0-1 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT4_MIN + (ExtraBits & 0x03)) *
	                       -(((ExtraBits&0x04)>>1)-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY5 ) {
        /* Bit 3 determines sign, Bit 0-2 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT5_MIN + (ExtraBits & 0x07)) *
	                       -(((ExtraBits&0x08)>>2)-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY6 ) {
        /* Bit 4 determines sign, Bit 0-3 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT6_MIN + (ExtraBits & 0x0F)) *
	                       -(((ExtraBits&0x10)>>3)-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY7 ) {
        /* Bit 5 determines sign, Bit 0-4 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT7_MIN + (ExtraBits & 0x1F)) *
	                       -(((ExtraBits&0x20)>>4)-1));
      } else if ( Token == Huffman.DCT_VAL_CATEGORY8 ) {
        /* Bit 9 determines sign, Bit 0-8 the value */
        ExpandedBlock[CoeffIndex[FragIndex]] = (short) ((Huffman.DCT_VAL_CAT8_MIN + (ExtraBits & 0x1FF)) *
	                       -(((ExtraBits&0x200)>>8)-1));
      }
  
      /* Step on the coefficient index. */
      CoeffIndex[FragIndex] += 1;
    }
  }

  public void ReconRefFrames (Playback pbi){
    int i;
    int j,k,m,n;

    /* predictor count. */
    int pcount;
  
    short wpc;
    short PredictedDC;
    int FragsAcross=pbi.HFragments;
    int FromFragment;
    int FragsDown = pbi.VFragments;
  
    int WhichFrame;
    int WhichCase;
    boolean isBaseFrame;
  
    isBaseFrame = pbi.getFrameType() == Constants.BASE_FRAME;

    pbi.filter.SetupLoopFilter(pbi.FrameQIndex);
  
    /* for y,u,v */
    for ( j = 0; j < 3 ; j++) {
      /* pick which fragments based on Y, U, V */
      switch(j){
      case 0: /* y */
        FromFragment = 0;
        FragsAcross = pbi.HFragments;
        FragsDown = pbi.VFragments;
        break;
      case 1: /* u */
        FromFragment = pbi.YPlaneFragments;
        FragsAcross = pbi.HFragments >> pbi.UVShiftX;
        FragsDown = pbi.VFragments >> pbi.UVShiftY;
        break;
      /*case 2:  v */
      default:    
        FromFragment = pbi.YPlaneFragments + pbi.UVPlaneFragments;
        FragsAcross = pbi.HFragments >> pbi.UVShiftX;
        FragsDown = pbi.VFragments >> pbi.UVShiftY;
        break;
      }
  
      /* initialize our array of last used DC Components */
      for(k=0;k<3;k++)
        Last[k]=0;
  
      i=FromFragment;
  
      /* do prediction on all of Y, U or V */
      for ( m = 0 ; m < FragsDown ; m++) {
        for ( n = 0 ; n < FragsAcross ; n++, i++){
  
          /* only do 2 prediction if fragment coded and on non intra or
             if all fragments are intra */
          if((pbi.display_fragments[i] != 0) || (pbi.getFrameType() == Constants.BASE_FRAME) ){
            /* Type of Fragment */
            WhichFrame = Mode2Frame[pbi.FragCodingMethod[i].getValue()];
  
            /* Check Borderline Cases */
            WhichCase = (n==0?1:0) + ((m==0?1:0) << 1) + ((n+1 == FragsAcross?1:0) << 2);
  
            fn[0]=i-1;
            fn[1]=i-FragsAcross-1;
            fn[2]=i-FragsAcross;
            fn[3]=i-FragsAcross+1;
  
            /* fragment valid for prediction use if coded and it comes
               from same frame as the one we are predicting */
            for(k=pcount=wpc=0; k<4; k++) {
              int pflag;
              pflag=1<<k;
              if((bc_mask[WhichCase]&pflag) != 0 &&
                 pbi.display_fragments[fn[k]] != 0 &&
                 (Mode2Frame[pbi.FragCodingMethod[fn[k]].getValue()] == WhichFrame))
	      {
                v[pcount]=pbi.QFragData[fn[k]][0];
                wpc|=pflag;
                pcount++;
              }
            }
  
            if(wpc==0){
              /* fall back to the last coded fragment */
              pbi.QFragData[i][0] += Last[WhichFrame];
  
            }else{
  
              /* don't do divide if divisor is 1 or 0 */
              PredictedDC = (short) (pc[wpc][0]*v[0]);
              for(k=1; k<pcount; k++){
                PredictedDC += pc[wpc][k]*v[k];
              }
  
              /* if we need to do a shift */
              if(pc[wpc][4] != 0 ){
  
                /* If negative add in the negative correction factor */
		if (PredictedDC < 0)
                  PredictedDC += pc[wpc][5];

                /* Shift in lieu of a divide */
                PredictedDC >>= pc[wpc][4];
              }
  
              /* check for outranging on the two predictors that can outrange */
              if((wpc&(PU|PUL|PL)) == (PU|PUL|PL)){
                if(Math.abs(PredictedDC - v[2]) > 128) {
                  PredictedDC = (short)v[2];
                } else if( Math.abs(PredictedDC - v[0]) > 128) {
                  PredictedDC = (short)v[0];
                } else if( Math.abs(PredictedDC - v[1]) > 128) {
                  PredictedDC = (short)v[1];
                }
              }
  
              pbi.QFragData[i][0] += PredictedDC;
            }

            /* Save the last fragment coded for whatever frame we are
               predicting from */
            Last[WhichFrame] = pbi.QFragData[i][0];

            /* Inverse DCT and reconstitute buffer in thisframe */
            if (isBaseFrame)
              ExpandKFBlock (pbi, i);
            else
              ExpandBlock (pbi, i);
          }
        }
      }
    }

    /* Copy the current reconstruction back to the last frame recon buffer. */
    if(pbi.CodedBlockIndex > (int) (pbi.UnitFragments >> 1)){
      short[] SwapReconBuffersTemp = pbi.ThisFrameRecon;
      pbi.ThisFrameRecon = pbi.LastFrameRecon;
      pbi.LastFrameRecon = SwapReconBuffersTemp;
      CopyNotRecon(pbi, pbi.LastFrameRecon, pbi.ThisFrameRecon);
    }else{
      CopyRecon(pbi, pbi.LastFrameRecon, pbi.ThisFrameRecon);
    }

    /* Apply a loop filter to edge pixels of updated blocks */
    pbi.filter.LoopFilter(pbi);

    /* We may need to update the UMV border */ 
    UpdateUMVBorder(pbi, pbi.LastFrameRecon);

    /* Reconstruct the golden frame if necessary.
       For VFW codec only on key frames */
    if (isBaseFrame) {
      CopyRecon( pbi, pbi.GoldenFrame, pbi.LastFrameRecon );
      UpdateUMVBorder(pbi, pbi.GoldenFrame);
    }
  }
}
