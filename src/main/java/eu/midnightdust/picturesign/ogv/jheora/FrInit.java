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

public class FrInit {
  static void InitializeFragCoordinates(Playback pbi)
  {
    int i, j;

    int HorizFrags = pbi.HFragments;
    int VertFrags = pbi.VFragments;
    int StartFrag = 0;

    /* Y */

    for(i = 0; i< VertFrags; i++){
      for(j = 0; j< HorizFrags; j++){
        int ThisFrag = i * HorizFrags + j;
        pbi.FragCoordinates[ ThisFrag ] =
		new Coordinate(j * Constants.BLOCK_HEIGHT_WIDTH,
		               i * Constants.BLOCK_HEIGHT_WIDTH);
      }
    }

    /* U */
    HorizFrags >>= pbi.UVShiftX;
    VertFrags >>= pbi.UVShiftY;
    StartFrag = pbi.YPlaneFragments;

    for(i = 0; i< VertFrags; i++) {
      for(j = 0; j< HorizFrags; j++) {
        int ThisFrag = StartFrag + i * HorizFrags + j;
        pbi.FragCoordinates[ ThisFrag ] =
		new Coordinate(j * Constants.BLOCK_HEIGHT_WIDTH,
		               i * Constants.BLOCK_HEIGHT_WIDTH);
      }
    }

    /* V */
    StartFrag = pbi.YPlaneFragments + pbi.UVPlaneFragments;
    for(i = 0; i< VertFrags; i++) {
      for(j = 0; j< HorizFrags; j++) {
        int ThisFrag = StartFrag + i * HorizFrags + j;
        pbi.FragCoordinates[ ThisFrag ] =
		new Coordinate(j * Constants.BLOCK_HEIGHT_WIDTH,
		               i * Constants.BLOCK_HEIGHT_WIDTH);
      }
    }
  }

  static void CalcPixelIndexTable(Playback pbi){
    int i, off;
    int[] PixelIndexTablePtr;

    /* Calculate the pixel index table for normal image buffers */
    PixelIndexTablePtr = pbi.pixel_index_table;
    for ( i = 0; i < pbi.YPlaneFragments; i++ ) {
      PixelIndexTablePtr[ i ] =
        ((i / pbi.HFragments) * Constants.VFRAGPIXELS *
         pbi.info.width);
      PixelIndexTablePtr[ i ] +=
        ((i % pbi.HFragments) * Constants.HFRAGPIXELS);
    }

    off = pbi.YPlaneFragments;
    for ( i = 0; i < ((pbi.HFragments >> pbi.UVShiftX) * pbi.VFragments); i++ ) {
      PixelIndexTablePtr[ i + off] =
        ((i / (pbi.HFragments >> pbi.UVShiftX) ) *
         (Constants.VFRAGPIXELS *
          (pbi.info.width >> pbi.UVShiftX)) );
      PixelIndexTablePtr[ i + off] +=
        ((i % (pbi.HFragments >> pbi.UVShiftX) ) *
         Constants.HFRAGPIXELS) + pbi.YPlaneSize;
    }

    /************************************************************************/
    /* Now calculate the pixel index table for image reconstruction buffers */
    PixelIndexTablePtr = pbi.recon_pixel_index_table;
    for ( i = 0; i < pbi.YPlaneFragments; i++ ){
      PixelIndexTablePtr[ i ] =
        ((i / pbi.HFragments) * Constants.VFRAGPIXELS *
         pbi.YStride);
      PixelIndexTablePtr[ i ] +=
        ((i % pbi.HFragments) * Constants.HFRAGPIXELS) +
        pbi.ReconYDataOffset;
    }
  
    /* U blocks */
    off = pbi.YPlaneFragments;
    for ( i = 0; i < pbi.UVPlaneFragments; i++ ) {
      PixelIndexTablePtr[i+off] =
        ((i / (pbi.HFragments >> pbi.UVShiftX) ) *
         (Constants.VFRAGPIXELS * (pbi.UVStride)) );
      PixelIndexTablePtr[i+off] +=
        ((i % (pbi.HFragments >> pbi.UVShiftX) ) *
         Constants.HFRAGPIXELS) + pbi.ReconUDataOffset;
    }
  
    /* V blocks */
    off = pbi.YPlaneFragments + pbi.UVPlaneFragments;
    for ( i = 0; i < pbi.UVPlaneFragments; i++ ) {
      PixelIndexTablePtr[ i +off] =
        ((i / (pbi.HFragments >> pbi.UVShiftX) ) *
         (Constants.VFRAGPIXELS * (pbi.UVStride)) );
      PixelIndexTablePtr[ i +off] +=
        ((i % (pbi.HFragments >> pbi.UVShiftX) ) * Constants.HFRAGPIXELS) +
        pbi.ReconVDataOffset;
    }
  }

  static void ClearFragmentInfo(Playback pbi){

    /* free prior allocs if present */
    pbi.display_fragments = null;
    pbi.pixel_index_table = null;
    pbi.recon_pixel_index_table = null;
    pbi.FragTokenCounts = null;
    pbi.CodedBlockList = null;
    pbi.FragMVect = null;
    pbi.FragCoefEOB = null;
    pbi.QFragData = null;
    pbi.FragCodingMethod = null;
    pbi.FragCoordinates = null;

    pbi.FragQIndex = null;

    pbi.BlockMap = null;
  
    pbi.SBCodedFlags = null;
    pbi.SBFullyFlags = null;
    pbi.MBFullyFlags = null;
    pbi.MBCodedFlags = null;
  }
  
  static void InitFragmentInfo(Playback pbi){
    /* clear any existing info */
    ClearFragmentInfo(pbi);

    /* Perform Fragment Allocations */
    pbi.display_fragments = new byte[pbi.UnitFragments];
    pbi.pixel_index_table = new int[pbi.UnitFragments];
    pbi.recon_pixel_index_table = new int[pbi.UnitFragments];
    pbi.FragTokenCounts = new int[pbi.UnitFragments];
    pbi.CodedBlockList = new int[pbi.UnitFragments];
    pbi.FragMVect = new MotionVector[pbi.UnitFragments];
    for (int i=0; i<pbi.UnitFragments; i++) {
      pbi.FragMVect[i] = new MotionVector();
    }
    pbi.FragQs = new byte[pbi.UnitFragments];
    pbi.FragCoefEOB = new byte[pbi.UnitFragments];
    pbi.QFragData = new short[pbi.UnitFragments][64];
    pbi.FragCodingMethod = new CodingMode[pbi.UnitFragments];
    pbi.FragCoordinates = new Coordinate[pbi.UnitFragments];
    pbi.FragQIndex = new int[pbi.UnitFragments];

    /* Super Block Initialization */
    pbi.SBCodedFlags = new byte[pbi.SuperBlocks];
    pbi.SBFullyFlags = new byte[pbi.SuperBlocks];

    /* Macro Block Initialization */
    pbi.MBCodedFlags = new byte[pbi.MacroBlocks];
    pbi.MBFullyFlags = new byte[pbi.MacroBlocks];
  }

  static void ClearFrameInfo(Playback pbi){
    pbi.ThisFrameRecon = null;
    pbi.GoldenFrame = null;
    pbi.LastFrameRecon = null;
    pbi.PostProcessBuffer = null;
  }

  static void InitFrameInfo(Playback pbi, int FrameSize){

    /* clear any existing info */
    ClearFrameInfo(pbi);

    /* allocate frames */
    pbi.ThisFrameRecon = new short[FrameSize];
    pbi.GoldenFrame = new short[FrameSize];
    pbi.LastFrameRecon = new short[FrameSize];
    pbi.PostProcessBuffer = new short[FrameSize];
  }

  static void InitFrameDetails(Playback pbi){
    int FrameSize, uv_fact;

    /*pbi.PostProcessingLevel = 0;
      pbi.PostProcessingLevel = 4;
      pbi.PostProcessingLevel = 5;
      pbi.PostProcessingLevel = 6;*/

    pbi.PostProcessingLevel = 0;

    pbi.UVShiftX = pbi.UVShiftY = 1;
    if (pbi.info.pixel_fmt == PixelFormat.TH_PF_422)
      pbi.UVShiftY = 0;
    if (pbi.info.pixel_fmt == PixelFormat.TH_PF_444)
      pbi.UVShiftX = pbi.UVShiftY = 0;

    uv_fact = 1 << (pbi.UVShiftX + pbi.UVShiftY);

    /* Set the frame size etc. */

    pbi.YPlaneSize = pbi.info.width *
      pbi.info.height;
    pbi.UVPlaneSize = pbi.YPlaneSize / uv_fact;
    pbi.HFragments = pbi.info.width / Constants.HFRAGPIXELS;
    pbi.VFragments = pbi.info.height / Constants.VFRAGPIXELS;
    pbi.YPlaneFragments = pbi.HFragments * pbi.VFragments;
    pbi.UVPlaneFragments = pbi.YPlaneFragments / uv_fact;
    pbi.UnitFragments = pbi.YPlaneFragments + 2*pbi.UVPlaneFragments;

    pbi.YStride = (pbi.info.width + Constants.STRIDE_EXTRA);
    pbi.UVStride = pbi.YStride >> pbi.UVShiftX;
    pbi.ReconYPlaneSize = pbi.YStride *
    (pbi.info.height + Constants.STRIDE_EXTRA);
    pbi.ReconUVPlaneSize = pbi.ReconYPlaneSize / uv_fact;
    FrameSize = pbi.ReconYPlaneSize + 2 * pbi.ReconUVPlaneSize;

    pbi.YDataOffset = 0;
    pbi.UDataOffset = pbi.YPlaneSize;
    pbi.VDataOffset = pbi.YPlaneSize + pbi.UVPlaneSize;
    pbi.ReconYDataOffset =
      (pbi.YStride * Constants.UMV_BORDER) + Constants.UMV_BORDER;
    pbi.ReconUDataOffset = pbi.ReconYPlaneSize +
      (pbi.UVStride * (Constants.UMV_BORDER>>pbi.UVShiftY)) +
      (Constants.UMV_BORDER>>pbi.UVShiftX);
    pbi.ReconVDataOffset = pbi.ReconYPlaneSize + pbi.ReconUVPlaneSize +
      (pbi.UVStride * (Constants.UMV_BORDER>>pbi.UVShiftY)) +
      (Constants.UMV_BORDER>>pbi.UVShiftX);
  
    /* Image dimensions in Super-Blocks */
    pbi.YSBRows = (pbi.info.height/32)  +
      ( pbi.info.height%32 !=0 ? 1 : 0 );
    pbi.YSBCols = (pbi.info.width/32)  +
      ( pbi.info.width%32 !=0 ? 1 : 0 );
    pbi.UVSBRows = ((pbi.info.height>>pbi.UVShiftY)/32)  +
      ( (pbi.info.height>>pbi.UVShiftY)%32 !=0 ? 1 : 0 );
    pbi.UVSBCols = ((pbi.info.width>>pbi.UVShiftX)/32)  +
      ( (pbi.info.width>>pbi.UVShiftX)%32 !=0 ? 1 : 0 );

    /* Super-Blocks per component */
    pbi.YSuperBlocks = pbi.YSBRows * pbi.YSBCols;
    pbi.UVSuperBlocks = pbi.UVSBRows * pbi.UVSBCols;
    pbi.SuperBlocks = pbi.YSuperBlocks+2*pbi.UVSuperBlocks;

    /* Useful externals */
    pbi.YMacroBlocks = ((pbi.VFragments+1)/2)*((pbi.HFragments+1)/2);
    pbi.UVMacroBlocks = 
      (((pbi.VFragments>>pbi.UVShiftY)+1)/2)*(((pbi.HFragments>>pbi.UVShiftX)+1)/2);
    pbi.MacroBlocks = pbi.YMacroBlocks+2*pbi.UVMacroBlocks;

    InitFragmentInfo(pbi);
    InitFrameInfo(pbi, FrameSize);
    InitializeFragCoordinates(pbi);
  
    /* Configure mapping between quad-tree and fragments */
    pbi.BlockMap = new BlockMapping (pbi.YSuperBlocks,
                       pbi.UVSuperBlocks, pbi.HFragments, pbi.VFragments,
                       pbi.UVShiftX, pbi.UVShiftY);

    /* Re-initialise the pixel index table. */

    CalcPixelIndexTable( pbi );
  }
}
